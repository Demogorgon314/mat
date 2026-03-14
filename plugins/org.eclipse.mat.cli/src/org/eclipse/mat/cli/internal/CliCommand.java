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

public enum CliCommand
{
    SUMMARY("summary"), //$NON-NLS-1$
    HISTOGRAM("histogram"), //$NON-NLS-1$
    TOP_CONSUMERS("top-consumers"), //$NON-NLS-1$
    PATH2GC("path2gc"), //$NON-NLS-1$
    OQL("oql"), //$NON-NLS-1$
    QUERY("query"); //$NON-NLS-1$

    private final String token;

    private CliCommand(String token)
    {
        this.token = token;
    }

    public String getToken()
    {
        return token;
    }

    public static CliCommand parse(String token) throws CliException
    {
        for (CliCommand command : values())
        {
            if (command.token.equals(token))
                return command;
        }
        throw CliException.usage("Unknown command: " + token); //$NON-NLS-1$
    }
}
