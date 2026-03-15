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
import java.util.Collection;

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
import org.junit.Test;

public class CliCommandExecutorTest
{
    @Test
    public void executesSummaryCommandAgainstHprofSnapshot() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "summary", heap.getAbsolutePath(), "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertTrue(json.contains("\"schemaVersion\":\"mat-cli/v2\"")); //$NON-NLS-1$
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

        assertTrue(json.contains("\"schemaVersion\":\"mat-cli/v2\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultKind\":\"threads\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"notice\":\"best-effort from heap dump, not a full jstack equivalent\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"summary\":{\"totalThreads\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"returnedThreads\":2")); //$NON-NLS-1$
        assertTrue(json.contains("\"threads\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"objectAddress\":\"0x")); //$NON-NLS-1$
        assertTrue(json.contains("\"stackAvailable\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"stackFrames\":[")); //$NON-NLS-1$
    }

    @Test
    public void executesHistogramCommandAgainstHprofSnapshot() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "histogram", heap.getAbsolutePath(), "--format", "json", "--limit", "5" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertTrue(json.contains("\"schemaVersion\":\"mat-cli/v2\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultKind\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"items\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"class_name\":")); //$NON-NLS-1$
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
        assertTrue(json.contains("\"object_address\":\"" + address + "\"")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(json.contains("\"_children\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"value\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"_meta\":{\"value\":{\"kind\":\"text_preview\"")); //$NON-NLS-1$
        assertFalse(json.contains("\"schema\":")); //$NON-NLS-1$
    }

    @Test
    public void executesInspectObjectSelectFieldAgainstHprofSnapshotAsJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String address = firstObjectAddress(heap, "java.lang.String"); //$NON-NLS-1$
        String json = executeJson(new String[] { "inspect-object", heap.getAbsolutePath(), "--format", "json", "--object",
                        address, "--select-field", "value", "--limit", "5" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

        assertTrue(json.contains("\"kind\":\"field\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"value\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"type\":\"char[]\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"_children\":[{")); //$NON-NLS-1$
        assertFalse(json.contains("\"_hasChildren\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"_meta\":{\"value\":{\"kind\":\"text_preview\"")); //$NON-NLS-1$
    }

    @Test
    public void executesInspectObjectFieldPathToPrimitiveAsJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String address = firstObjectAddress(heap, "java.lang.String"); //$NON-NLS-1$
        String json = executeJson(new String[] { "inspect-object", heap.getAbsolutePath(), "--format", "json", "--object",
                        address, "--field-path", "count" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(json.contains("\"kind\":\"field\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"count\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"type\":\"int\"")); //$NON-NLS-1$
        assertFalse(json.contains("\"_context\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"_children\":")); //$NON-NLS-1$
    }

    @Test
    public void failsMissingInspectObjectFieldPathWithExecutionError() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        CliArguments parsed = new CliArgumentParser().parse(new String[] { "inspect-object", heap.getAbsolutePath(),
                        "--object", firstObjectAddress(heap, "java.lang.String"), "--field-path", "missingField" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
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
    public void executesTopConsumersCommandAgainstHprofSnapshotAsStructuredJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "top-consumers", heap.getAbsolutePath(), "--format", "json",
                        "--limit", "5" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertTrue(json.contains("\"schemaVersion\":\"mat-cli/v2\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultKind\":\"top-consumers\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"biggestObjects\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"classes\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"classLoaders\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"packages\":")); //$NON-NLS-1$
    }

    @Test
    public void executesDescribeCommandInJsonMode() throws Exception
    {
        String json = executeJson(new String[] { "describe", "histogram", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertTrue(json.contains("\"schemaVersion\":\"mat-cli/v2\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultKind\":\"describe\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"histogram\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"usage\":\"mat-cli histogram <heap> [--limit N] [--format text|json]\"")); //$NON-NLS-1$
    }

    @Test
    public void describesInstancesCommandWithClassRegexOption() throws Exception
    {
        String json = executeJson(new String[] { "describe", "instances", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertTrue(json.contains("\"resultKind\":\"describe\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"instances\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"usage\":\"mat-cli instances <heap> [--class <fqcn> | --class-regex <regex> | --class-contains <text>] [--include-subclasses] [--limit N] [--format text|json]\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"--class-regex\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"--class-contains\"")); //$NON-NLS-1$
    }

    @Test
    public void executesSchemaCommandInJsonMode() throws Exception
    {
        String json = executeJson(new String[] { "schema", "top-consumers", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertTrue(json.contains("\"resultKind\":\"schema\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"top-consumers\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"payloadKind\":\"top-consumers\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"payloadFields\":[\"totalRetainedHeap\",\"biggestObjects[]\",\"biggestObjects[].objectAddress\",\"biggestObjectsTruncated\",\"classes[]\",\"classes[].objectAddress\",\"classesTruncated\",\"classLoaders[]\",\"classLoaders[].objectAddress\",\"classLoadersTruncated\",\"packages\",\"packagesTruncated\"]")); //$NON-NLS-1$
        assertTrue(json.contains("\"jsonEnvelope\":[\"schemaVersion\",\"resultKind\",\"truncated\"")); //$NON-NLS-1$
    }

    @Test
    public void executesThreadsSchemaCommandInJsonMode() throws Exception
    {
        String json = executeJson(new String[] { "schema", "threads", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertTrue(json.contains("\"resultKind\":\"schema\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"payloadKind\":\"threads\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"threads[].stackFrames[]\"")); //$NON-NLS-1$
    }

    @Test
    public void executesTopConsumersTextAndJsonWithMatchingAggregates() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String text = execute(new String[] { "top-consumers", heap.getAbsolutePath(), "--format", "text", "--limit",
                        "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        String json = execute(new String[] { "top-consumers", heap.getAbsolutePath(), "--format", "json", "--limit",
                        "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertTrue(text.contains("57.41%  67,312  class java.lang.System @ 0x2c59b178")); //$NON-NLS-1$
        assertTrue(json.contains("\"label\":\"class java.lang.System @ 0x2c59b178\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"retainedBytes\":67312")); //$NON-NLS-1$
        assertFalse(json.contains("\"retainedPercentText\":")); //$NON-NLS-1$

        assertTrue(text.contains("75.18%  88,144  302  java.lang.Class")); //$NON-NLS-1$
        assertTrue(json.contains("\"label\":\"java.lang.Class\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"count\":302")); //$NON-NLS-1$
        assertTrue(json.contains("\"retainedBytes\":88144")); //$NON-NLS-1$

        assertTrue(text.contains("100.00%  117,248  357  <system class loader>")); //$NON-NLS-1$
        assertTrue(json.contains("\"label\":\"<system class loader>\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"retainedBytes\":117248")); //$NON-NLS-1$
        assertTrue(json.contains("\"topDominators\":357")); //$NON-NLS-1$
    }

    @Test
    public void executesHistogramCommandWithJsonTableContract() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "histogram", heap.getAbsolutePath(), "--format", "json", "--limit", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertTrue(json.contains("\"schemaVersion\":\"mat-cli/v2\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultKind\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"items\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"retained_heap\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"suggestedNextCommands\":")); //$NON-NLS-1$
    }

    @Test
    public void executesTopConsumersCommandWithJsonContract() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "top-consumers", heap.getAbsolutePath(), "--format", "json", "--limit", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertTrue(json.contains("\"resultKind\":\"top-consumers\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"biggestObjects\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"classes\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"classLoaders\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"objectAddress\":\"0x")); //$NON-NLS-1$
        assertTrue(json.contains("\"packages\":{\"name\":\"<all>\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"packagesTruncated\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"objectId\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"_context\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"suggestedNextCommands\":")); //$NON-NLS-1$
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
        assertTrue(json.contains("mat-cli histogram")); //$NON-NLS-1$
        assertTrue(json.contains("mat-cli top-consumers")); //$NON-NLS-1$
    }

    @Test
    public void summaryJsonSuggestionsIncludeThreadOverview() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "summary", heap.getAbsolutePath(), "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertTrue(json.contains("thread_overview")); //$NON-NLS-1$
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
        CliCommandExecutor executor = new CliCommandExecutor();
        CliArguments arguments = new CliArgumentParser().parse(new String[] { "summary", heap.getAbsolutePath() }); //$NON-NLS-1$ //$NON-NLS-2$
        try (SnapshotSession session = executor.openSnapshot(arguments))
        {
            ISnapshot snapshot = session.getSnapshot();
            Collection<IClass> classes = snapshot.getClassesByName(className, false);
            assertTrue(classes != null && !classes.isEmpty());
            int[] objectIds = classes.iterator().next().getObjectIds();
            assertTrue(objectIds.length > 0);
            return "0x" + Long.toHexString(snapshot.mapIdToAddress(objectIds[0])); //$NON-NLS-1$
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
}
