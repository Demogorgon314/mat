/*******************************************************************************
 * Copyright (c) 2026 Eclipse Memory Analyzer Project.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.mat.tests.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.cli.internal.CliArgumentParser;
import org.eclipse.mat.cli.internal.CliArguments;
import org.eclipse.mat.cli.internal.CliCommandExecutor;
import org.eclipse.mat.cli.internal.CliException;
import org.eclipse.mat.cli.internal.CliExecution;
import org.eclipse.mat.cli.internal.SnapshotSession;
import org.eclipse.mat.cli.internal.serialization.ResultSerializer;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.tests.TestSnapshots;
import org.eclipse.mat.util.IProgressListener;
import org.eclipse.mat.util.VoidProgressListener;
import org.junit.Test;

public class CliCommandExecutorTest
{
    @Test
    public void executesSummaryCommandAgainstHprofSnapshot() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "summary", heap.getAbsolutePath(), "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertTrue(json.contains("\"schemaVersion\":\"mat-cli/v1\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultKind\":\"summary\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"summary\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"path\":\"" + heap.getAbsolutePath().replace("\\", "\\\\") + "\"")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertTrue(json.contains("\"numberOfObjects\":")); //$NON-NLS-1$
    }

    @Test
    public void executesThreadsCommandAgainstHprofSnapshotAsText() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String text = execute(new String[] { "threads", heap.getAbsolutePath(), "--format", "text", "--limit", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertTrue(text.contains("best-effort from heap dump, not a full jstack equivalent")); //$NON-NLS-1$
        assertTrue(text.contains("Overview")); //$NON-NLS-1$
        assertTrue(text.contains("Retained Heap")); //$NON-NLS-1$
        assertTrue(text.contains("Stack:")); //$NON-NLS-1$
    }

    @Test
    public void executesThreadsCommandAgainstHprofSnapshotAsJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "threads", heap.getAbsolutePath(), "--format", "json", "--limit", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertTrue(json.contains("\"schemaVersion\":\"mat-cli/v1\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultKind\":\"threads\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"notice\":\"best-effort from heap dump, not a full jstack equivalent\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"summary\":{\"totalThreads\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"returnedThreads\":2")); //$NON-NLS-1$
        assertTrue(json.contains("\"threads\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"objectAddress\":\"0x")); //$NON-NLS-1$
        assertTrue(json.contains("\"stackAvailable\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"stackUnavailableReason\":\"") || json.contains("\"stackFrames\":[")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(json.contains("\"stackFrames\":[]")); //$NON-NLS-1$
    }

    @Test
    public void executesObjectsCommandAgainstHprofSnapshotAsJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "objects", heap.getAbsolutePath(), "--format", "json", "--limit", "5" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertTrue(json.contains("\"schemaVersion\":\"mat-cli/v1\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultKind\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"items\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"class\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"retained_size\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"schema\":")); //$NON-NLS-1$
    }

    @Test
    public void executesInstancesCommandAgainstHprofSnapshotAsJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "instances", heap.getAbsolutePath(), "--format", "json", "--class",
                        "java.lang.String", "--limit", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(json.contains("\"resultKind\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"items\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"object_address\":\"0x")); //$NON-NLS-1$
        assertTrue(json.contains("\"class_name\":\"java.lang.String\"")); //$NON-NLS-1$
        assertFalse(json.contains("\"_context\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"schema\":")); //$NON-NLS-1$
    }

    @Test
    public void executesInstancesCommandAgainstHprofSnapshotAsText() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String address = firstObjectAddress(heap, "java.lang.String"); //$NON-NLS-1$
        String text = execute(new String[] { "instances", heap.getAbsolutePath(), "--class", "java.lang.String",
                        "--limit", "1", "--format", "text" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(text.contains("Object Address")); //$NON-NLS-1$
        assertTrue(text.contains(address)); //$NON-NLS-1$
    }

    @Test
    public void executesInstancesRegexCommandAgainstHprofSnapshotAsJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "instances", heap.getAbsolutePath(), "--format", "json",
                        "--class-regex", "java\\.lang\\.String", "--limit", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(json.contains("\"resultKind\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"items\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"object_address\":\"0x")); //$NON-NLS-1$
        assertTrue(json.contains("\"class_name\":\"java.lang.String\"")); //$NON-NLS-1$
        assertFalse(json.contains("\"schema\":")); //$NON-NLS-1$
    }

    @Test
    public void executesInstancesRegexCommandAgainstHprofSnapshotAsText() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String address = firstObjectAddress(heap, "java.lang.String"); //$NON-NLS-1$
        String text = execute(new String[] { "instances", heap.getAbsolutePath(), "--class-regex",
                        "java\\.lang\\.String", "--limit", "1", "--format", "text" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(text.contains("Object Address")); //$NON-NLS-1$
        assertTrue(text.contains(address)); //$NON-NLS-1$
    }

    @Test
    public void executesInstancesRegexCommandAgainstMultipleClasses() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        int limit = totalInstanceCount(heap, "java.lang.Runtime", "java.lang.Thread"); //$NON-NLS-1$ //$NON-NLS-2$
        String json = executeJson(new String[] { "instances", heap.getAbsolutePath(), "--format", "json",
                        "--class-regex", "java\\.lang\\.(Runtime|Thread)", "--limit", Integer.toString(limit) }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(json.contains("\"class_name\":\"java.lang.Runtime\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"class_name\":\"java.lang.Thread\"")); //$NON-NLS-1$
    }

    @Test
    public void ignoresInstanceLimitInDumpMode() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "instances", heap.getAbsolutePath(), "--format", "json", "--class",
                        "java.lang.String", "--limit", "1", "--dump" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

        assertTrue(countOccurrences(json, "\"object_address\":\"0x") > 1); //$NON-NLS-1$
        assertTrue(json.contains("\"truncated\":false")); //$NON-NLS-1$
    }

    @Test
    public void executesInstancesContainsCommandAgainstHprofSnapshotAsJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "instances", heap.getAbsolutePath(), "--format", "json",
                        "--class-contains", "String", "--limit", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(json.contains("\"resultKind\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"items\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"class_name\":\"java.lang.String\"")); //$NON-NLS-1$
    }

    @Test
    public void executesInstancesContainsCommandAgainstMultipleClasses() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        int limit = totalInstanceCount(heap, "java.lang.Runtime", "java.lang.RuntimePermission"); //$NON-NLS-1$ //$NON-NLS-2$
        String json = executeJson(new String[] { "instances", heap.getAbsolutePath(), "--format", "json",
                        "--class-contains", "Runtime", "--limit", Integer.toString(limit) }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(json.contains("\"class_name\":\"java.lang.Runtime\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"class_name\":\"java.lang.RuntimePermission\"")); //$NON-NLS-1$
    }

    @Test
    public void executesInspectObjectCommandAgainstHprofSnapshotAsJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String address = firstObjectAddress(heap, "java.lang.String"); //$NON-NLS-1$
        String json = executeJson(new String[] { "inspect-object", heap.getAbsolutePath(), "--format", "json", "--object",
                        address, "--limit", "5", "--depth", "3" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

        assertTrue(json.contains("\"resultKind\":\"tree\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"kind\":\"object\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"<object>\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"type\":\"java.lang.String\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"value\":\"\\\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"_meta\":{\"value\":{\"kind\":\"text_preview\"")); //$NON-NLS-1$
        assertFalse(json.contains("\"_children\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"path\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"valueKind\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"hasChildren\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"schema\":")); //$NON-NLS-1$
    }

    @Test
    public void executesInspectObjectCommandAgainstHprofSnapshotAsText() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String text = execute(new String[] { "inspect-object", heap.getAbsolutePath(), "--object",
                        firstObjectAddress(heap, "java.lang.String"), "--format", "text", "--depth", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(text.contains("object <object> = \"")); //$NON-NLS-1$
        assertTrue(text.contains(" : java.lang.String")); //$NON-NLS-1$
        assertFalse(text.contains(".value -> char[]")); //$NON-NLS-1$
        assertFalse(text.contains(".value -> byte[]")); //$NON-NLS-1$
        assertFalse(text.contains("Kind | Name")); //$NON-NLS-1$
    }

    @Test
    public void inlinesNestedStringFieldsWhenInspectingFileAsText() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String text = execute(new String[] { "inspect-object", heap.getAbsolutePath(), "--object",
                        firstObjectAddress(heap, "java.io.File"), "--format", "text", "--depth", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(text, text.contains(".path = \"")); //$NON-NLS-1$
        assertTrue(text, text.contains("\" : java.lang.String")); //$NON-NLS-1$
        assertFalse(text.contains(".path -> java.lang.String")); //$NON-NLS-1$
    }

    @Test
    public void executesInspectObjectCommandAgainstWrapperObjectAsText() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String text = execute(new String[] { "inspect-object", heap.getAbsolutePath(), "--object",
                        firstObjectAddress(heap, "java.lang.Integer"), "--format", "text" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertTrue(text.matches("(?s).*object <object> = -?\\d+ : java\\.lang\\.Integer.*")); //$NON-NLS-1$
        assertFalse(text.contains(".value = ")); //$NON-NLS-1$
    }

    @Test
    public void executesInspectObjectCommandAgainstEnumAsTextAndJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.ORACLE_JDK8_05_64BIT);
        String className = "java.io.File$PathStatus"; //$NON-NLS-1$
        String objectAddress = firstObjectAddress(heap, className);

        String text = execute(new String[] { "inspect-object", heap.getAbsolutePath(), "--object", objectAddress, "--format",
                        "text" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        assertTrue(text, text.matches("(?s).*object <object> = [A-Z_]+ : " + className.replace("$", "\\$") + ".*")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(text.contains("-> " + className)); //$NON-NLS-1$

        String json = executeJson(new String[] { "inspect-object", heap.getAbsolutePath(), "--format", "json",
                        "--object", objectAddress }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        assertTrue(json.contains("\"type\":\"" + className + "\"")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(json.matches("(?s).*\"value\":\"[A-Z_]+\".*")); //$NON-NLS-1$
        assertFalse(json.contains("\"_children\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"valueKind\":")); //$NON-NLS-1$
    }

    @Test
    public void executesInspectObjectSelectFieldsAgainstHprofSnapshotAsJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String address = firstObjectAddress(heap, "java.lang.String"); //$NON-NLS-1$
        String json = executeJson(new String[] { "inspect-object", heap.getAbsolutePath(), "--format", "json", "--object",
                        address, "--select-fields", "value", "--limit", "5" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

        assertTrue(json.contains("\"kind\":\"field\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"value\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"type\":\"char[]\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"_children\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"_meta\":{\"value\":{\"kind\":\"text_preview\"")); //$NON-NLS-1$
        assertFalse(json.contains("\"path\":")); //$NON-NLS-1$
    }

    @Test
    public void executesInspectObjectFieldPathsToPrimitiveAsJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String address = firstObjectAddress(heap, "java.lang.String"); //$NON-NLS-1$
        String json = executeJson(new String[] { "inspect-object", heap.getAbsolutePath(), "--format", "json", "--object",
                        address, "--field-paths", "count" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(json.contains("\"kind\":\"field\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"count\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"type\":\"int\"")); //$NON-NLS-1$
        assertFalse(json.contains("\"_context\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"_children\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"path\":")); //$NON-NLS-1$
    }

    @Test
    public void executesInspectObjectFieldPathsToPrimitiveAsText() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String text = execute(new String[] { "inspect-object", heap.getAbsolutePath(), "--object",
                        firstObjectAddress(heap, "java.lang.String"), "--field-paths", "count", "--format", "text" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

        assertTrue(text.contains(".count = ")); //$NON-NLS-1$
        assertTrue(text.contains(" : int")); //$NON-NLS-1$
        assertFalse(text.contains("Kind | Name")); //$NON-NLS-1$
    }

    @Test
    public void executesInspectObjectRepeatedSelectFieldsAsJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String address = firstObjectAddress(heap, "java.lang.String"); //$NON-NLS-1$
        String json = executeJson(new String[] { "inspect-object", heap.getAbsolutePath(), "--format", "json", "--object",
                        address, "--select-fields", "count", "--select-fields", "offset" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

        assertTrue(json.contains("\"name\":\"count\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"offset\"")); //$NON-NLS-1$
        assertFalse(json.contains("\"path\":")); //$NON-NLS-1$
    }

    @Test
    public void executesInspectObjectRepeatedFieldPathsAsText() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String text = execute(new String[] { "inspect-object", heap.getAbsolutePath(), "--object",
                        firstObjectAddress(heap, "java.lang.String"), "--field-paths", "count", "--field-paths", "offset",
                        "--format", "text" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$

        assertTrue(text.contains(".count = ")); //$NON-NLS-1$
        assertTrue(text.contains(".offset = ")); //$NON-NLS-1$
    }

    @Test
    public void ignoresInspectObjectLimitInDumpModeForTextAndJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String objectAddress = firstObjectAddress(heap, "java.lang.String"); //$NON-NLS-1$

        String text = execute(new String[] { "inspect-object", heap.getAbsolutePath(), "--object", objectAddress,
                        "--select-fields", "value", "--limit", "1", "--depth", "3", "--dump", "--format", "text" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
        String json = executeJson(new String[] { "inspect-object", heap.getAbsolutePath(), "--format", "json",
                        "--object", objectAddress, "--select-fields", "value", "--limit", "1", "--depth", "3",
                        "--dump" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$

        assertTrue(text.contains("[1] = ")); //$NON-NLS-1$
        assertTrue(countOccurrences(json, "\"kind\":\"element\"") > 1); //$NON-NLS-1$
        assertFalse(json.contains("\"_meta\":{\"value\":{\"kind\":\"text_preview\"")); //$NON-NLS-1$
        assertFalse(json.contains("\"_meta\":{\"value\":{\"kind\":\"hex_preview\"")); //$NON-NLS-1$
    }

    @Test
    public void failsMissingInspectObjectFieldPathsWithExecutionError() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        CliArguments parsed = new CliArgumentParser().parse(new String[] { "inspect-object", heap.getAbsolutePath(),
                        "--object", firstObjectAddress(heap, "java.lang.String"), "--field-paths", "missingField" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        CliCommandExecutor executor = new CliCommandExecutor();

        try (SnapshotSession session = executor.openSnapshot(parsed))
        {
            try
            {
                executor.execute(parsed, session);
                fail("Expected missing inspect-object field path"); //$NON-NLS-1$
            }
            catch (CliException e)
            {
                assertEquals(3, e.getExitCode());
                assertTrue(e.getMessage().contains("Field 'missingField' not found")); //$NON-NLS-1$
            }
        }
    }

    @Test
    public void executesObjectsClassLoaderCommandAgainstHprofSnapshotAsJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "objects", heap.getAbsolutePath(), "--by", "class-loader", "--format",
                        "json", "--limit", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(json.contains("\"schemaVersion\":\"mat-cli/v1\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultKind\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"class_loader\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"classes\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"classes_w_o_instances\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"objects\":0")); //$NON-NLS-1$
    }

    @Test
    public void executesObjectsClassLoaderGroupingWithFilterAsClassRowsAgainstHprofSnapshotAsJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "objects", heap.getAbsolutePath(), "--by", "class-loader",
                        "--class-loader", "<system class loader>", "--format", "json", "--limit",
                        "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$

        assertTrue(json.contains("\"schemaVersion\":\"mat-cli/v1\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultKind\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"class\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"class_loader\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"classes_w_o_instances\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"objects\":0")); //$NON-NLS-1$
    }

    @Test
    public void executesObjectsClassCommandWithClassLoaderFilterAgainstHprofSnapshotAsJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "objects", heap.getAbsolutePath(), "--class-loader",
                        "<system class loader>", "--format", "json", "--limit", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(json.contains("\"schemaVersion\":\"mat-cli/v1\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultKind\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"class\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"class_loader\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"objects\":0")); //$NON-NLS-1$
    }

    @Test
    public void executesDescribeCommandInJsonMode() throws Exception
    {
        String json = executeJson(new String[] { "describe", "objects", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertTrue(json.contains("\"schemaVersion\":\"mat-cli/v1\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultKind\":\"describe\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"objects\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"usage\":\"mat-cli objects <heap> [--by class|package|class-loader] [--package PKG] [--class-loader TEXT] [--limit N] [--dump] [--bytes-display bytes|kilobytes|megabytes|gigabytes|smart] [--format text|json]\"")); //$NON-NLS-1$
    }

    @Test
    public void describesInspectObjectCommandWithPluralFieldSelectors() throws Exception
    {
        String json = executeJson(new String[] { "describe", "inspect-object", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertTrue(json.contains("\"name\":\"inspect-object\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"usage\":\"mat-cli inspect-object <heap> --object 0x... [--select-fields FIELD | --field-paths PATH] [--show-nulls] [--limit N] [--depth N] [--dump] [--bytes-display bytes|kilobytes|megabytes|gigabytes|smart] [--format text|json]\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"--select-fields\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"--field-paths\"")); //$NON-NLS-1$
        assertFalse(json.contains("\"name\":\"--select-field\"")); //$NON-NLS-1$
        assertFalse(json.contains("\"name\":\"--field-path\"")); //$NON-NLS-1$
    }

    @Test
    public void describesInstancesCommandWithClassRegexOption() throws Exception
    {
        String json = executeJson(new String[] { "describe", "instances", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertTrue(json.contains("\"resultKind\":\"describe\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"instances\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"usage\":\"mat-cli instances <heap> [--class <fqcn> | --class-regex <regex> | --class-contains <text>] [--include-subclasses] [--limit N] [--dump] [--bytes-display bytes|kilobytes|megabytes|gigabytes|smart] [--format text|json]\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"--class-regex\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"--class-contains\"")); //$NON-NLS-1$
    }

    @Test
    public void executesSchemaCommandInJsonMode() throws Exception
    {
        String json = executeJson(new String[] { "schema", "biggest-objects", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertTrue(json.contains("\"resultKind\":\"schema\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"biggest-objects\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"payloadKind\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains(
                        "\"payloadFields\":[\"items[]\",\"items[]._address when no address column\",\"items[].retained_size\",\"items[].shallow_size\",\"items[].retained\",\"items[].level\"]")); //$NON-NLS-1$
        assertTrue(json.contains("\"jsonEnvelope\":[\"schemaVersion\",\"resultKind\",\"truncated\"")); //$NON-NLS-1$
    }

    @Test
    public void executesThreadsSchemaCommandInJsonMode() throws Exception
    {
        String json = executeJson(new String[] { "schema", "threads", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertTrue(json.contains("\"resultKind\":\"schema\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"payloadKind\":\"threads\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"threads[].stackFrames[] when non-empty\"")); //$NON-NLS-1$
    }

    @Test
    public void executesObjectsPackageTextAndJsonWithMatchingRoot() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String text = execute(new String[] { "objects", heap.getAbsolutePath(), "--by", "package", "--format", "text",
                        "--limit", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        String json = execute(new String[] { "objects", heap.getAbsolutePath(), "--by", "package", "--format", "json",
                        "--limit", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(text.contains("package,  retained%,  retained bytes, #top-dominators")); //$NON-NLS-1$
        assertTrue(text.contains("<all>  (100.00%)")); //$NON-NLS-1$
        assertTrue(text.contains("|-") || text.contains("'-")); //$NON-NLS-1$ //$NON-NLS-2$
        String firstChild = firstTreeChild(text);
        assertTrue(firstChild, firstChild != null);
        assertFalse(firstChild, firstChild.contains("  0 B  ")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultKind\":\"tree\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"root\":{\"name\":\"<all>\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"retainedBytes\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"topDominators\":")); //$NON-NLS-1$
    }

    @Test
    public void executesObjectsPackageCommandWithPackageReroot() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "objects", heap.getAbsolutePath(), "--by", "package", "--package",
                        "java", "--format", "json", "--limit", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

        assertTrue(json.contains("\"resultKind\":\"tree\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"root\":{\"name\":\"java\"")); //$NON-NLS-1$
    }

    @Test
    public void ignoresPackageTreeLimitInDumpModeButStillHonorsDepth() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String expanded = executeJson(new String[] { "objects", heap.getAbsolutePath(), "--by", "package", "--format",
                        "json", "--limit", "1", "--depth", "2", "--dump" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
        String depthLimited = executeJson(new String[] { "objects", heap.getAbsolutePath(), "--by", "package", "--format",
                        "json", "--limit", "1", "--depth", "1", "--dump" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$

        assertTrue(countOccurrences(expanded, "\"topDominators\":") > 2); //$NON-NLS-1$
        assertTrue(depthLimited.contains("\"_childrenTruncated\":true")); //$NON-NLS-1$
    }

    @Test
    public void executesObjectsCommandWithJsonTableContract() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "objects", heap.getAbsolutePath(), "--format", "json", "--limit", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertTrue(json.contains("\"schemaVersion\":\"mat-cli/v1\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultKind\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"items\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"retained_size\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"objects\":0")); //$NON-NLS-1$
        assertFalse(json.contains("\"suggestedNextCommands\":")); //$NON-NLS-1$
    }

    @Test
    public void executesBiggestObjectsCommandWithJsonContract() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "biggest-objects", heap.getAbsolutePath(), "--format", "json", "--limit",
                        "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertTrue(json.contains("\"resultKind\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"items\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"retained_size\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"shallow_size\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"retained\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"level\":1")); //$NON-NLS-1$
        assertTrue(json.contains("\"_address\":\"0x")); //$NON-NLS-1$
        assertFalse(json.contains("\"suggestedNextCommands\":")); //$NON-NLS-1$
    }

    @Test
    public void executesBiggestObjectsCommandAsTextWithIndentedNames() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String text = execute(new String[] { "biggest-objects", heap.getAbsolutePath(), "--format", "text", "--depth",
                        "2", "--limit", "100" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(text.contains("Name")); //$NON-NLS-1$
        assertTrue(text.contains("Retained Size")); //$NON-NLS-1$
        assertTrue(text.contains("Shallow Size")); //$NON-NLS-1$
        assertTrue(text.contains("Retained %")); //$NON-NLS-1$
        assertTrue(text.contains("Level")); //$NON-NLS-1$

        String levelTwoLine = firstTableRowWithLevel(text, 2);
        assertTrue(levelTwoLine, levelTwoLine != null);
        assertTrue(levelTwoLine, levelTwoLine.startsWith("  ")); //$NON-NLS-1$
    }

    @Test
    public void executesBiggestObjectsCommandWithExpandedDepth() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "biggest-objects", heap.getAbsolutePath(), "--format", "json", "--depth",
                        "2", "--limit", "1000" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(json.contains("\"level\":1")); //$NON-NLS-1$
        assertTrue(json.contains("\"level\":2")); //$NON-NLS-1$
    }

    @Test
    public void executesOqlCommandAgainstHprofSnapshotInsideLiveSession() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "oql", heap.getAbsolutePath(), "--format", "json", "--query",
                        "select s.@objectAddress as ADDRESS, toString(s) as VALUE from java.lang.String s", "--limit",
                        "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(json.contains("\"resultKind\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"items\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"address\":\"0x")); //$NON-NLS-1$
        assertTrue(json.contains("\"value\":")); //$NON-NLS-1$
    }

    @Test
    public void ignoresOqlLimitInDumpModeForTableResults() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "oql", heap.getAbsolutePath(), "--format", "json", "--query",
                        "select s.@objectAddress as ADDRESS, toString(s) as VALUE from java.lang.String s", "--limit",
                        "1", "--dump" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

        assertTrue(countOccurrences(json, "\"address\":\"0x") > 1); //$NON-NLS-1$
        assertTrue(json.contains("\"truncated\":false")); //$NON-NLS-1$
    }

    @Test
    public void executesHashEntriesQueryAgainstHprofSnapshotInsideLiveSession() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "query", heap.getAbsolutePath(), "--format", "json", "--command",
                        "hash_entries java.util.AbstractMap -include_subclasses", "--limit", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(json.contains("\"resultKind\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"items\":[{")); //$NON-NLS-1$
    }

    @Test
    public void executesListQueriesCommandInJsonMode() throws Exception
    {
        String json = executeJson(new String[] { "list-queries", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertTrue(json.contains("\"resultKind\":\"query-list\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"queries\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"identifier\":\"histogram\"")); //$NON-NLS-1$
    }

    @Test
    public void executesDescribeQueryCommandInJsonMode() throws Exception
    {
        String json = executeJson(new String[] { "describe-query", "hash_entries", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertTrue(json.contains("\"resultKind\":\"query-description\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"query\":{\"identifier\":\"hash_entries\"")); //$NON-NLS-1$
    }

    @Test
    public void executesCompletionCommandInTextModeForBash() throws Exception
    {
        String script = execute(new String[] { "completion", "bash" }); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(script.contains("# bash completion for mat-cli")); //$NON-NLS-1$
        assertTrue(script.contains("complete -F _mat_cli_completion mat-cli")); //$NON-NLS-1$
        assertTrue(script.contains("|completion)")); //$NON-NLS-1$
        assertTrue(script.contains("objects|")); //$NON-NLS-1$
        assertTrue(script.contains("biggest-objects")); //$NON-NLS-1$
        assertTrue(script.contains("--format")); //$NON-NLS-1$
    }

    @Test
    public void executesCompletionCommandInTextModeForZsh() throws Exception
    {
        String script = execute(new String[] { "completion", "zsh" }); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(script.contains("#compdef mat-cli")); //$NON-NLS-1$
        assertTrue(script.contains("compdef _mat-cli mat-cli")); //$NON-NLS-1$
        assertTrue(script.contains("|completion)")); //$NON-NLS-1$
        assertTrue(script.contains("--query-file")); //$NON-NLS-1$
        assertTrue(script.contains("biggest-objects")); //$NON-NLS-1$
    }

    @Test
    public void executesCompletionCommandInJsonMode() throws Exception
    {
        String json = executeJson(new String[] { "completion", "bash", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertTrue(json.contains("\"resultKind\":\"text\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"content\":\"# bash completion for mat-cli")); //$NON-NLS-1$
        assertTrue(json.contains("complete -F _mat_cli_completion mat-cli")); //$NON-NLS-1$
    }

    @Test
    public void executesTopConsumersHtmlThroughQueryCommandAsRawSectionJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "query", heap.getAbsolutePath(), "--format", "json", "--command",
                        "top_consumers_html", "--limit", "5" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(json.contains("\"resultKind\":\"section\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"sections\":")); //$NON-NLS-1$
    }

    @Test
    public void failsInvalidOqlWithExecutionError() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        CliArguments parsed = new CliArgumentParser().parse(
                        new String[] { "oql", heap.getAbsolutePath(), "--query", "INVALID SYNTAX HERE @@@" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        CliCommandExecutor executor = new CliCommandExecutor();

        try (SnapshotSession session = executor.openSnapshot(parsed))
        {
            try
            {
                executor.execute(parsed, session);
                fail("Expected OQL syntax failure"); //$NON-NLS-1$
            }
            catch (CliException e)
            {
                assertEquals(3, e.getExitCode());
                assertTrue(e.getMessage().contains("Encountered \"INVALID\"")); //$NON-NLS-1$
            }
        }
    }

    @Test
    public void executesFindStringsWithoutSubjectArgument() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "query", heap.getAbsolutePath(), "--format", "json", "--command",
                        "find_strings -pattern p0", "--limit", "5" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertTrue(json.contains("\"resultKind\":\"tree\"") || json.contains("\"resultKind\":\"section\"")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(json.contains("\"items\":") || json.contains("\"sections\":")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void rewritesInvalidPath2GcAddressAsJsonError() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        CliArguments parsed = new CliArgumentParser().parse(
                        new String[] { "path2gc", heap.getAbsolutePath(), "--format", "json", "--object", "0xdeadbeef" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        CliCommandExecutor executor = new CliCommandExecutor();
        ResultSerializer serializer = new ResultSerializer();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (SnapshotSession session = executor.openSnapshot(parsed);
                        PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8.name()))
        {
            try
            {
                executor.execute(parsed, session);
                fail("Expected invalid path2gc address"); //$NON-NLS-1$
            }
            catch (CliException e)
            {
                serializer.serializeError(parsed, parsed.getFormat(), e.getExitCode(), e, stream);
            }
        }

        String json = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(json.contains("\"resultKind\":\"error\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"kind\":\"invalid_argument\"")); //$NON-NLS-1$
        assertTrue(json.contains("No object found at address 0xdeadbeef")); //$NON-NLS-1$
        assertTrue(json.contains("mat-cli objects")); //$NON-NLS-1$
        assertTrue(json.contains("mat-cli biggest-objects")); //$NON-NLS-1$
    }

    @Test
    public void summaryJsonSuggestionsIncludeThreadOverview() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "summary", heap.getAbsolutePath(), "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertTrue(json.contains("thread_overview")); //$NON-NLS-1$
        assertTrue(json.contains("mat-cli objects")); //$NON-NLS-1$
        assertTrue(json.contains("mat-cli biggest-objects")); //$NON-NLS-1$
        assertFalse(json.contains("--format json")); //$NON-NLS-1$
    }

    @Test
    public void omitsSuggestionsForNonEmptyQueryResults() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "query", heap.getAbsolutePath(), "--format", "json", "--command", "histogram" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertFalse(json.contains("\"suggestedNextCommands\":")); //$NON-NLS-1$
    }

    @Test
    public void executesShowDominatorTreeCommandAgainstHprofSnapshotWithObjectAddress() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        CliCommandExecutor executor = new CliCommandExecutor();
        String address;

        try (SnapshotSession session = executor.openSnapshot(
                        new CliArgumentParser().parse(new String[] { "summary", heap.getAbsolutePath() }))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            int objectId = session.getSnapshot().getGCRoots()[0];
            address = "0x" + Long.toHexString(session.getSnapshot().mapIdToAddress(objectId)); //$NON-NLS-1$
        }

        String json = executeJson(new String[] { "query", heap.getAbsolutePath(), "--format", "json", "--command",
                        "show_dominator_tree " + address, "--limit", "1" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertTrue(json.contains("\"resultKind\":\"tree\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"items\":[{")); //$NON-NLS-1$
    }

    @Test
    public void addsPath2GcNoteForGcRootObjects() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        CliCommandExecutor executor = new CliCommandExecutor();
        ResultSerializer serializer = new ResultSerializer();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (SnapshotSession session = executor.openSnapshot(
                        new CliArgumentParser().parse(new String[] { "summary", heap.getAbsolutePath() })); //$NON-NLS-1$ //$NON-NLS-2$
                        PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8.name()))
        {
            ISnapshot snapshot = session.getSnapshot();
            int gcRootId = snapshot.getGCRoots()[0];
            String address = "0x" + Long.toHexString(snapshot.mapIdToAddress(gcRootId)); //$NON-NLS-1$
            CliArguments parsed = new CliArgumentParser()
                            .parse(new String[] { "path2gc", heap.getAbsolutePath(), "--format", "json", "--object", address }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            CliExecution execution = executor.execute(parsed, session);
            serializer.serialize(parsed, execution, stream);
        }

        String json = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(json.contains("\"note\":\"Object is already a GC root")); //$NON-NLS-1$
    }

    @Test
    public void executesThreadsCommandAgainstPhdWithCompanionJavacore() throws Exception
    {
        File heap = copyHeap(TestSnapshots.IBM_JDK8_64BIT_HEAP_AND_JAVA);
        String json = executeJson(new String[] { "threads", heap.getAbsolutePath(), "--format", "json", "--limit", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertTrue(json.contains("\"resultKind\":\"threads\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"summary\":{\"totalThreads\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"threads\":[")); //$NON-NLS-1$
    }

    @Test
    public void retriesConcurrentParsingErrorWhenOpeningSnapshot() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        CliArguments parsed = new CliArgumentParser().parse(new String[] { "summary", heap.getAbsolutePath() }); //$NON-NLS-1$ //$NON-NLS-2$
        RetryingCliCommandExecutor executor = new RetryingCliCommandExecutor(1);

        try (SnapshotSession session = executor.openSnapshot(parsed))
        {
            assertEquals(heap.getAbsolutePath(), session.getSnapshot().getSnapshotInfo().getPath());
        }

        assertEquals(2, executor.getOpenAttempts());
        assertEquals(1, executor.getSleepCalls());
        assertTrue(executor.getListener().containsMessage("waiting for the lock to clear")); //$NON-NLS-1$
    }

    @Test
    public void failsWhenConcurrentParsingRetryTimeoutExpires() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        CliArguments parsed = new CliArgumentParser().parse(new String[] { "summary", heap.getAbsolutePath() }); //$NON-NLS-1$ //$NON-NLS-2$
        TimeoutCliCommandExecutor executor = new TimeoutCliCommandExecutor();

        try
        {
            executor.openSnapshot(parsed);
            fail("Expected concurrent parsing timeout"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertEquals(3, e.getExitCode());
            assertTrue(e.getMessage().contains("Timed out after waiting 5 ms")); //$NON-NLS-1$
            assertTrue(e.getMessage().contains(heap.getAbsolutePath()));
        }

        assertTrue(executor.getOpenAttempts() >= 2);
        assertTrue(executor.getListener().containsMessage("waiting for the lock to clear")); //$NON-NLS-1$
    }

    private String executeJson(String[] args) throws Exception
    {
        return execute(args);
    }

    private String execute(String[] args) throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments parsed = parser.parse(args);
        CliCommandExecutor executor = new CliCommandExecutor();
        ResultSerializer serializer = new ResultSerializer();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8.name()))
        {
            if (parsed.getCommand().requiresSnapshot())
            {
                try (SnapshotSession session = executor.openSnapshot(parsed))
                {
                    CliExecution execution = executor.execute(parsed, session);
                    serializer.serialize(parsed, execution, stream);
                }
            }
            else
            {
                CliExecution execution = executor.execute(parsed);
                serializer.serialize(parsed, execution, stream);
            }
        }

        return output.toString(StandardCharsets.UTF_8.name());
    }

    private File copyHeap(String resourceName) throws Exception
    {
        File directory = TestSnapshots.createGeneratedName("cli", null); //$NON-NLS-1$
        String[] resources = resourceName.split(";"); //$NON-NLS-1$
        File heap = null;
        for (String resource : resources)
        {
            File source = TestSnapshots.getResourceFile(resource);
            File target = new File(directory, new File(resource).getName());
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            if (heap == null)
                heap = target;
        }
        return heap;
    }

    private String firstObjectAddress(File heap, String className) throws Exception
    {
        return firstResolvedObjectAddress(heap, className).objectAddress;
    }

    private ResolvedObjectAddress firstResolvedObjectAddress(File heap, String... classNames) throws Exception
    {
        CliCommandExecutor executor = new CliCommandExecutor();
        CliArguments arguments = new CliArgumentParser().parse(new String[] { "summary", heap.getAbsolutePath() }); //$NON-NLS-1$ //$NON-NLS-2$
        try (SnapshotSession session = executor.openSnapshot(arguments))
        {
            ISnapshot snapshot = session.getSnapshot();
            for (String className : classNames)
            {
                Collection<IClass> classes = snapshot.getClassesByName(className, false);
                if (classes == null || classes.isEmpty())
                    continue;

                for (IClass clazz : classes)
                {
                    int[] objectIds = clazz.getObjectIds();
                    if (objectIds.length > 0)
                    {
                        return new ResolvedObjectAddress(className,
                                        "0x" + Long.toHexString(snapshot.mapIdToAddress(objectIds[0]))); //$NON-NLS-1$
                    }
                }
            }
        }

        fail("Expected at least one instance for " + Arrays.toString(classNames)); //$NON-NLS-1$
        return null;
    }

    private int countOccurrences(String text, String token)
    {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0)
        {
            count++;
            index += token.length();
        }
        return count;
    }

    private static final class ResolvedObjectAddress
    {
        private final String className;
        private final String objectAddress;

        private ResolvedObjectAddress(String className, String objectAddress)
        {
            this.className = className;
            this.objectAddress = objectAddress;
        }
    }

    private static final class RecordingProgressListener extends VoidProgressListener
    {
        private final List<String> messages = new ArrayList<String>();

        @Override
        public void sendUserMessage(Severity severity, String message, Throwable exception)
        {
            messages.add(message);
        }

        private boolean containsMessage(String text)
        {
            for (String message : messages)
            {
                if (message != null && message.contains(text))
                    return true;
            }
            return false;
        }
    }

    private static class RetryingCliCommandExecutor extends CliCommandExecutor
    {
        private final int failuresBeforeSuccess;
        private final RecordingProgressListener listener = new RecordingProgressListener();
        private int openAttempts;
        private int sleepCalls;

        private RetryingCliCommandExecutor(int failuresBeforeSuccess)
        {
            this.failuresBeforeSuccess = failuresBeforeSuccess;
        }

        @Override
        protected IProgressListener createProgressListener(CliArguments arguments)
        {
            return listener;
        }

        @Override
        protected ISnapshot openSnapshot(File heapFile, IProgressListener listener) throws SnapshotException
        {
            openAttempts++;
            if (openAttempts <= failuresBeforeSuccess)
            {
                throw new SnapshotException("Concurrent parsing error, lock file: /tmp/test.lock reason: busy"); //$NON-NLS-1$
            }
            return super.openSnapshot(heapFile, listener);
        }

        @Override
        protected long concurrentParsingRetryIntervalMillis()
        {
            return 1L;
        }

        @Override
        protected void sleep(long millis)
        {
            sleepCalls++;
        }

        private int getOpenAttempts()
        {
            return openAttempts;
        }

        private int getSleepCalls()
        {
            return sleepCalls;
        }

        private RecordingProgressListener getListener()
        {
            return listener;
        }
    }

    private static final class TimeoutCliCommandExecutor extends CliCommandExecutor
    {
        private final RecordingProgressListener listener = new RecordingProgressListener();
        private long currentTimeMillis;
        private int openAttempts;

        @Override
        protected IProgressListener createProgressListener(CliArguments arguments)
        {
            return listener;
        }

        @Override
        protected ISnapshot openSnapshot(File heapFile, IProgressListener listener) throws SnapshotException
        {
            openAttempts++;
            throw new SnapshotException("Concurrent parsing error, lock file: /tmp/test.lock reason: busy"); //$NON-NLS-1$
        }

        @Override
        protected long concurrentParsingRetryIntervalMillis()
        {
            return 2L;
        }

        @Override
        protected long concurrentParsingRetryTimeoutMillis()
        {
            return 5L;
        }

        @Override
        protected long currentTimeMillis()
        {
            return currentTimeMillis;
        }

        @Override
        protected void sleep(long millis)
        {
            currentTimeMillis += millis;
        }

        private int getOpenAttempts()
        {
            return openAttempts;
        }

        private RecordingProgressListener getListener()
        {
            return listener;
        }
    }

    private int totalInstanceCount(File heap, String... classNames) throws Exception
    {
        int total = 0;
        CliCommandExecutor executor = new CliCommandExecutor();
        CliArguments arguments = new CliArgumentParser().parse(new String[] { "summary", heap.getAbsolutePath() }); //$NON-NLS-1$ //$NON-NLS-2$
        try (SnapshotSession session = executor.openSnapshot(arguments))
        {
            ISnapshot snapshot = session.getSnapshot();
            for (String className : classNames)
            {
                Collection<IClass> classes = snapshot.getClassesByName(className, false);
                assertTrue(classes != null && !classes.isEmpty());
                for (IClass clazz : classes)
                {
                    total += clazz.getObjectIds().length;
                }
            }
        }
        return total;
    }

    private String firstTreeChild(String text)
    {
        String[] lines = text.split("\\R"); //$NON-NLS-1$
        for (String line : lines)
        {
            if (line.startsWith("|-") || line.startsWith("'-")) //$NON-NLS-1$ //$NON-NLS-2$
                return line;
        }
        return null;
    }

    private String firstTableRowWithLevel(String text, int level)
    {
        String[] lines = text.split("\\R"); //$NON-NLS-1$
        for (String line : lines)
        {
            if (line.indexOf('|') < 0 || line.startsWith("-")) //$NON-NLS-1$
                continue;

            String[] cells = line.split("\\|"); //$NON-NLS-1$
            if (cells.length == 0)
                continue;

            String last = cells[cells.length - 1].trim();
            if (Integer.toString(level).equals(last))
                return line;
        }
        return null;
    }
}
