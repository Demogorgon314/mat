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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;

import org.eclipse.mat.cli.internal.CliArgumentParser;
import org.eclipse.mat.cli.internal.CliArguments;
import org.eclipse.mat.cli.internal.CliCommandExecutor;
import org.eclipse.mat.cli.internal.CliExecution;
import org.eclipse.mat.cli.internal.SnapshotSession;
import org.eclipse.mat.cli.internal.serialization.ResultSerializer;
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
    }

    private String executeJson(String[] args) throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments parsed = parser.parse(args);
        CliCommandExecutor executor = new CliCommandExecutor();
        ResultSerializer serializer = new ResultSerializer();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (SnapshotSession session = executor.openSnapshot(parsed);
                        PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8.name()))
        {
            CliExecution execution = executor.execute(parsed, session);
            serializer.serialize(parsed, execution, stream);
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
