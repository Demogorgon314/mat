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

import org.eclipse.mat.cli.internal.CliArgumentParser;
import org.eclipse.mat.cli.internal.CliArguments;
import org.eclipse.mat.cli.internal.CliCommandExecutor;
import org.eclipse.mat.cli.internal.CliException;
import org.eclipse.mat.cli.internal.CliExecution;
import org.eclipse.mat.cli.internal.SnapshotSession;
import org.eclipse.mat.cli.internal.serialization.ResultSerializer;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.tests.TestSnapshots;
import org.junit.Test;

public class CliCommandExecutorTest
{
    @Test
    public void executesSummaryCommandAgainstHprofSnapshot() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "summary", heap.getAbsolutePath(), "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertTrue(json.contains("\"resultType\":\"summary\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"summary\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"path\":\"" + heap.getAbsolutePath().replace("\\", "\\\\") + "\"")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertTrue(json.contains("\"numberOfObjects\":")); //$NON-NLS-1$
    }

    @Test
    public void executesHistogramCommandAgainstHprofSnapshot() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "histogram", heap.getAbsolutePath(), "--format", "json", "--limit", "5" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertTrue(json.contains("\"resultType\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"columns\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"rows\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"label\":\"Retained Heap\"")); //$NON-NLS-1$
    }

    @Test
    public void executesTopConsumersCommandAgainstHprofSnapshotAsStructuredJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "top-consumers", heap.getAbsolutePath(), "--format", "json",
                        "--limit", "5" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertTrue(json.contains("\"resultType\":\"top-consumers\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"biggestObjects\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"classes\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"classLoaders\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"packages\":")); //$NON-NLS-1$
    }

    @Test
    public void executesDescribeCommandInAgentProfile() throws Exception
    {
        String json = executeJson(new String[] { "describe", "histogram", "--agent" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertTrue(json.contains("\"schemaVersion\":\"mat-cli/v1\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"command\":\"describe\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"subject\":\"histogram\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultKind\":\"describe\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"usage\":\"mat-cli histogram <heap> [--limit N] [--format text|json]\"")); //$NON-NLS-1$
    }

    @Test
    public void executesSchemaCommandInAgentProfile() throws Exception
    {
        String json = executeJson(new String[] { "schema", "top-consumers", "--agent" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertTrue(json.contains("\"subject\":\"top-consumers\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultKind\":\"schema\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"payloadKind\":\"top-consumers\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"payloadFields\":[\"totalRetainedHeap\",\"biggestObjects[]\",\"biggestObjectsTruncated\",\"classes[]\",\"classesTruncated\",\"classLoaders[]\",\"classLoadersTruncated\",\"packages\",\"packagesTruncated\"]")); //$NON-NLS-1$
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
        assertTrue(json.contains("\"retainedPercentText\":\"57.41%\"")); //$NON-NLS-1$

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
    public void executesHistogramCommandWithAgentTableContract() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "histogram", heap.getAbsolutePath(), "--agent", "--limit", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertTrue(json.contains("\"schemaVersion\":\"mat-cli/v1\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultKind\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"schema\":{\"columns\":[{\"id\":\"class_name\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"id\":\"retained_heap\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"items\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"_context\":{\"objectId\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"suggestedNextCommands\":")); //$NON-NLS-1$
    }

    @Test
    public void executesTopConsumersCommandWithAgentCompactJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "top-consumers", heap.getAbsolutePath(), "--agent", "--limit", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertTrue(json.contains("\"resultKind\":\"top-consumers\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"biggestObjects\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"classes\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"classLoaders\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"packages\":{\"name\":\"<all>\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"packagesTruncated\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"suggestedNextCommands\":")); //$NON-NLS-1$
    }

    @Test
    public void executesOqlCommandAgainstHprofSnapshotInsideLiveSession() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "oql", heap.getAbsolutePath(), "--format", "json", "--query",
                        "select s.@objectAddress as ADDRESS, toString(s) as VALUE from java.lang.String s", "--limit",
                        "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(json.contains("\"resultType\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"rows\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"VALUE\"")); //$NON-NLS-1$
    }

    @Test
    public void executesHashEntriesQueryAgainstHprofSnapshotInsideLiveSession() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "query", heap.getAbsolutePath(), "--format", "json", "--command",
                        "hash_entries java.util.AbstractMap -include_subclasses", "--limit", "2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(json.contains("\"resultType\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"rows\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"columns\":")); //$NON-NLS-1$
    }

    @Test
    public void executesListQueriesCommandInAgentProfile() throws Exception
    {
        String json = executeJson(new String[] { "list-queries", "--agent" }); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(json.contains("\"resultKind\":\"query-list\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"queries\":")); //$NON-NLS-1$
        assertTrue(json.contains("\"identifier\":\"histogram\"")); //$NON-NLS-1$
    }

    @Test
    public void executesDescribeQueryCommandInAgentProfile() throws Exception
    {
        String json = executeJson(new String[] { "describe-query", "hash_entries", "--agent" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertTrue(json.contains("\"resultKind\":\"query-description\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"subject\":\"hash_entries\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"query\":{\"identifier\":\"hash_entries\"")); //$NON-NLS-1$
    }

    @Test
    public void executesTopConsumersHtmlThroughQueryCommandAsRawSectionJson() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "query", heap.getAbsolutePath(), "--format", "json", "--command",
                        "top_consumers_html", "--limit", "5" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(json.contains("\"resultType\":\"section\"")); //$NON-NLS-1$
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

        assertTrue(json.contains("\"resultType\":\"tree\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"rows\":")); //$NON-NLS-1$
    }

    @Test
    public void rewritesInvalidPath2GcAddressAsAgentError() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        CliArguments parsed = new CliArgumentParser().parse(
                        new String[] { "path2gc", heap.getAbsolutePath(), "--agent", "--object", "0xdeadbeef" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
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
                serializer.serializeError(parsed, parsed.getProfile(), e.getExitCode(), e, stream);
            }
        }

        String json = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(json.contains("\"command\":\"path2gc\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"kind\":\"invalid_argument\"")); //$NON-NLS-1$
        assertTrue(json.contains("No object found at address 0xdeadbeef")); //$NON-NLS-1$
        assertTrue(json.contains("mat-cli histogram")); //$NON-NLS-1$
        assertTrue(json.contains("mat-cli top-consumers")); //$NON-NLS-1$
    }

    @Test
    public void summaryAgentSuggestionsIncludeThreadOverview() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "summary", heap.getAbsolutePath(), "--agent" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertTrue(json.contains("thread_overview")); //$NON-NLS-1$
    }

    @Test
    public void suggestsNextStepsFromQueryContext() throws Exception
    {
        File heap = copyHeap(TestSnapshots.SUN_JDK5_13_32BIT);
        String json = executeJson(new String[] { "query", heap.getAbsolutePath(), "--agent", "--command", "histogram" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertTrue(json.contains("mat-cli path2gc")); //$NON-NLS-1$
        assertTrue(json.contains("dominator_tree 0x")); //$NON-NLS-1$
        assertFalse(json.contains("describe top-consumers")); //$NON-NLS-1$
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
                            .parse(new String[] { "path2gc", heap.getAbsolutePath(), "--agent", "--object", address }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            CliExecution execution = executor.execute(parsed, session);
            serializer.serialize(parsed, execution, stream);
        }

        String json = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(json.contains("\"note\":\"Object is already a GC root")); //$NON-NLS-1$
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
        File source = TestSnapshots.getResourceFile(resourceName);
        File directory = TestSnapshots.createGeneratedName("cli", null); //$NON-NLS-1$
        File heap = new File(directory, new File(resourceName).getName());
        Files.copy(source.toPath(), heap.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return heap;
    }
}
