/*******************************************************************************
 * Copyright (c) 2026 Eclipse Memory Analyzer Project.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.mat.cli.internal;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.mat.cli.internal.serialization.ResultSerializer;
import org.junit.Test;

public class CliApplicationErrorContextTest
{
    @Test
    public void partialParsePreservesCommandForJsonUsageErrors() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.partialParse(new String[] { "path2gc", "sample.hprof", "--format", "json" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                        CliArguments.OutputFormat.JSON);
        ResultSerializer serializer = new ResultSerializer();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8.name()))
        {
            serializer.serializeError(arguments, CliArguments.OutputFormat.JSON, CliExitCodes.USAGE,
                            CliException.usage("path2gc requires --object 0x..."), stream); //$NON-NLS-1$
        }

        String json = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(json.contains("\"resultKind\":\"error\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"kind\":\"usage\"")); //$NON-NLS-1$
        assertTrue(json.contains("mat-cli path2gc --help")); //$NON-NLS-1$
        assertTrue(json.contains("mat-cli inspect-object")); //$NON-NLS-1$
    }

    @Test
    public void partialParsePreservesCommandForMarkdownUsageErrors() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.partialParse(
                        new String[] { "path2gc", "sample.hprof", "--format", "markdown" }, CliArguments.OutputFormat.MARKDOWN); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        ResultSerializer serializer = new ResultSerializer();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8.name()))
        {
            serializer.serializeError(arguments, CliArguments.OutputFormat.MARKDOWN, CliExitCodes.USAGE,
                            CliException.usage("path2gc requires --object 0x..."), stream); //$NON-NLS-1$
        }

        String markdown = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(markdown.contains("### Error")); //$NON-NLS-1$
        assertTrue(markdown.contains("mat-cli path2gc --help")); //$NON-NLS-1$
        assertTrue(markdown.contains("mat-cli inspect-object")); //$NON-NLS-1$
    }
}
