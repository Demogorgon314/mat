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

public final class CliHelp
{
    private CliHelp()
    {}

    public static String generalHelp()
    {
        StringBuilder help = new StringBuilder();
        help.append("Usage:\n"); //$NON-NLS-1$
        help.append("  mat-cli <command> <heap> [options]\n"); //$NON-NLS-1$
        help.append("  mat-cli --help\n\n"); //$NON-NLS-1$
        help.append("Commands:\n"); //$NON-NLS-1$
        help.append("  summary <heap>\n"); //$NON-NLS-1$
        help.append("  histogram <heap> [--limit N] [--format text|json]\n"); //$NON-NLS-1$
        help.append("  top-consumers <heap> [--format text|json]\n"); //$NON-NLS-1$
        help.append("  path2gc <heap> --object 0x... [--format text|json]\n"); //$NON-NLS-1$
        help.append("  oql <heap> --query \"...\" [--format text|json]\n"); //$NON-NLS-1$
        help.append("  query <heap> --command \"...\" [--format text|json]\n\n"); //$NON-NLS-1$
        help.append("Global options:\n"); //$NON-NLS-1$
        help.append("  --format text|json   Output format (default: text)\n"); //$NON-NLS-1$
        help.append("  --limit N            Maximum rows or children per level (default: 20)\n"); //$NON-NLS-1$
        help.append("  --verbose            Print detailed diagnostics on failure\n"); //$NON-NLS-1$
        help.append("  --help               Show this help\n"); //$NON-NLS-1$
        return help.toString();
    }
}
