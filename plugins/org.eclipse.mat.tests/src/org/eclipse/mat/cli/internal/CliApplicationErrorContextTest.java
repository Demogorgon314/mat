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
    public void partialParsePreservesCommandForAgentUsageErrors() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.partialParse(new String[] { "path2gc", "sample.hprof", "--agent" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        CliArguments.OutputProfile.AGENT, CliArguments.OutputFormat.JSON);
        ResultSerializer serializer = new ResultSerializer();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8.name()))
        {
            serializer.serializeError(arguments, CliArguments.OutputProfile.AGENT, CliExitCodes.USAGE,
                            CliException.usage("path2gc requires --object 0x..."), stream); //$NON-NLS-1$
        }

        String json = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(json.contains("\"command\":\"path2gc\"")); //$NON-NLS-1$
    }
}
