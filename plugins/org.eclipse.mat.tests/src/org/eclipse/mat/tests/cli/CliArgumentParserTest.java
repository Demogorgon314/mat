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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.mat.cli.internal.CliArgumentParser;
import org.eclipse.mat.cli.internal.CliArguments;
import org.eclipse.mat.cli.internal.CliCommand;
import org.eclipse.mat.cli.internal.CliException;
import org.eclipse.mat.cli.internal.CliHelp;
import org.junit.Test;

public class CliArgumentParserTest
{
    @Test
    public void parsesHistogramArguments() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "histogram", "sample.hprof", "--limit", "5", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertEquals(CliCommand.HISTOGRAM, arguments.getCommand());
        assertEquals(CliArguments.OutputFormat.JSON, arguments.getFormat());
        assertEquals(CliArguments.OutputProfile.DEFAULT, arguments.getProfile());
        assertEquals(5, arguments.getLimit());
        assertEquals("sample.hprof", arguments.getHeapFile().getName()); //$NON-NLS-1$
    }

    @Test
    public void parsesPathToGcArguments() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "path2gc", "sample.hprof", "--object", "0x2a" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertEquals(CliCommand.PATH2GC, arguments.getCommand());
        assertEquals("0x2a", arguments.getObjectAddress()); //$NON-NLS-1$
    }

    @Test
    public void enablesAgentProfileAndDefaultsToJson() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "--agent", "histogram", "sample.hprof" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertEquals(CliCommand.HISTOGRAM, arguments.getCommand());
        assertEquals(CliArguments.OutputProfile.AGENT, arguments.getProfile());
        assertEquals(CliArguments.OutputFormat.JSON, arguments.getFormat());
        assertEquals(4, arguments.getTreeDepthLimit());
    }

    @Test
    public void parsesDescribeCommandWithoutHeap() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "describe", "histogram", "--profile", "agent" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertEquals(CliCommand.DESCRIBE, arguments.getCommand());
        assertEquals(CliCommand.HISTOGRAM, arguments.getSubjectCommand());
        assertEquals(CliArguments.OutputProfile.AGENT, arguments.getProfile());
        assertEquals(CliArguments.OutputFormat.JSON, arguments.getFormat());
    }

    @Test
    public void parsesDescribeQueryCommandWithoutHeap() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "describe-query", "hash_entries", "--agent" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertEquals(CliCommand.DESCRIBE_QUERY, arguments.getCommand());
        assertEquals("hash_entries", arguments.getSubjectName()); //$NON-NLS-1$
        assertEquals(CliArguments.OutputProfile.AGENT, arguments.getProfile());
        assertEquals(CliArguments.OutputFormat.JSON, arguments.getFormat());
    }

    @Test
    public void parsesListQueriesCommandWithoutHeap() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "list-queries", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertEquals(CliCommand.LIST_QUERIES, arguments.getCommand());
        assertEquals(CliArguments.OutputFormat.JSON, arguments.getFormat());
    }

    @Test
    public void readsOqlFromFile() throws Exception
    {
        Path queryFile = Files.createTempFile("mat-cli-oql-", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        Files.write(queryFile, "SELECT * FROM java.lang.String".getBytes(StandardCharsets.UTF_8)); //$NON-NLS-1$

        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(
                        new String[] { "oql", "sample.hprof", "--query-file", queryFile.toString() }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertEquals("SELECT * FROM java.lang.String", arguments.getOqlQuery()); //$NON-NLS-1$
    }

    @Test
    public void readsQueryCommandFromStandardInput() throws Exception
    {
        InputStream original = System.in;
        try
        {
            System.setIn(new ByteArrayInputStream("hash_entries java.util.AbstractMap -include_subclasses" //$NON-NLS-1$
                            .getBytes(StandardCharsets.UTF_8)));
            CliArgumentParser parser = new CliArgumentParser();
            CliArguments arguments = parser.parse(
                            new String[] { "query", "sample.hprof", "--command-stdin" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            assertEquals("hash_entries java.util.AbstractMap -include_subclasses", arguments.getQueryCommand()); //$NON-NLS-1$
        }
        finally
        {
            System.setIn(original);
        }
    }

    @Test
    public void detectsRequestedProfileAndFormatBeforeFullParse()
    {
        CliArgumentParser parser = new CliArgumentParser();

        assertEquals(CliArguments.OutputProfile.AGENT,
                        parser.detectProfile(new String[] { "--agent", "summary", "sample.hprof" })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals(CliArguments.OutputFormat.TEXT, parser.detectFormat(
                        new String[] { "--profile", "agent", "--format", "text", "summary", "sample.hprof" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
                        CliArguments.OutputProfile.AGENT));
    }

    @Test
    public void capsExplicitLimitAtMaximum()
                    throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "histogram", "sample.hprof", "--limit", "500000" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertEquals(10000, arguments.getLimit());
    }

    @Test
    public void appliesQuerySpecificDefaultLimit()
                    throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "query", "sample.hprof", "--command", "thread_overview" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertEquals(100, arguments.getLimit());
    }

    @Test
    public void parsesExplicitDepth()
                    throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(
                        new String[] { "query", "sample.hprof", "--command", "gc_roots", "--depth", "3" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertEquals(3, arguments.getTreeDepthLimit());
    }

    @Test
    public void rejectsInvalidDepth()
                    throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();

        try
        {
            parser.parse(new String[] { "query", "sample.hprof", "--command", "gc_roots", "--depth", "0" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            fail("Expected invalid depth"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertEquals(2, e.getExitCode());
            assertTrue(e.getMessage().contains("Depth must be >= 1")); //$NON-NLS-1$
        }
    }

    @Test
    public void parsesHelpWithoutCommand() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[0]);

        assertTrue(arguments.isHelp());
    }

    @Test
    public void helpIncludesDepthAndTopConsumersLimit()
    {
        String help = CliHelp.generalHelp();

        assertTrue(help.contains("top-consumers <heap> [--limit N] [--depth N] [--format text|json]")); //$NON-NLS-1$
        assertTrue(help.contains("--depth N            Maximum tree or section depth")); //$NON-NLS-1$
    }
}
