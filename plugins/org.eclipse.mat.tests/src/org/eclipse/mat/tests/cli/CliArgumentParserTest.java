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

import org.eclipse.mat.cli.internal.CliArgumentParser;
import org.eclipse.mat.cli.internal.CliArguments;
import org.eclipse.mat.cli.internal.CliCommand;
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
    public void parsesHelpWithoutCommand() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[0]);

        assertTrue(arguments.isHelp());
    }
}
