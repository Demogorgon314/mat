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
        help.append("  mat-cli describe <command> [options]\n"); //$NON-NLS-1$
        help.append("  mat-cli schema <command> [options]\n"); //$NON-NLS-1$
        help.append("  mat-cli list-queries [options]\n"); //$NON-NLS-1$
        help.append("  mat-cli describe-query <query-id> [options]\n"); //$NON-NLS-1$
        help.append("  mat-cli --help\n\n"); //$NON-NLS-1$
        help.append("Commands:\n"); //$NON-NLS-1$
        help.append("  summary <heap>\n"); //$NON-NLS-1$
        help.append("  threads <heap> [--limit N] [--format text|json]\n"); //$NON-NLS-1$
        help.append("  histogram <heap> [--limit N] [--format text|json]\n"); //$NON-NLS-1$
        help.append("  instances <heap> --class <fqcn> [--include-subclasses] [--limit N] [--format text|json]\n"); //$NON-NLS-1$
        help.append("  inspect-object <heap> --object 0x... [--select-field FIELD | --field-path PATH] [--limit N] [--depth N] [--format text|json]\n"); //$NON-NLS-1$
        help.append("  top-consumers <heap> [--limit N] [--depth N] [--format text|json]\n"); //$NON-NLS-1$
        help.append("  path2gc <heap> --object 0x... [--limit N] [--depth N] [--format text|json]\n"); //$NON-NLS-1$
        help.append("  oql <heap> --query \"...\" [--limit N] [--depth N] [--format text|json]\n"); //$NON-NLS-1$
        help.append("  query <heap> --command \"...\" [--limit N] [--depth N] [--format text|json]\n"); //$NON-NLS-1$
        help.append("  describe <command> [--format text|json]\n"); //$NON-NLS-1$
        help.append("  schema <command> [--format text|json]\n"); //$NON-NLS-1$
        help.append("  list-queries [--format text|json]\n"); //$NON-NLS-1$
        help.append("  describe-query <query-id> [--format text|json]\n\n"); //$NON-NLS-1$
        help.append("Global options:\n"); //$NON-NLS-1$
        help.append("  --format text|json   Output format (default: text, agent profile: json)\n"); //$NON-NLS-1$
        help.append("  --limit N            Maximum rows or children per level (default: 20, max: 10000)\n"); //$NON-NLS-1$
        help.append("  --depth N            Maximum tree or section depth (default: 8, agent profile: 4, inspect-object: 3)\n"); //$NON-NLS-1$
        help.append("  --profile default|agent  Output profile (default: default)\n"); //$NON-NLS-1$
        help.append("  --agent              Shortcut for --profile agent\n"); //$NON-NLS-1$
        help.append("  --verbose            Print detailed diagnostics on failure\n"); //$NON-NLS-1$
        help.append("  --help               Show this help\n\n"); //$NON-NLS-1$
        help.append("Inspect-object options:\n"); //$NON-NLS-1$
        help.append("  --select-field FIELD Inspect one direct field from the root object\n"); //$NON-NLS-1$
        help.append("  --field-path PATH    Inspect a dotted field path such as cleaner.offsetMap\n\n"); //$NON-NLS-1$
        help.append("Input options:\n"); //$NON-NLS-1$
        help.append("  --query-file PATH    Read OQL text from a UTF-8 file\n"); //$NON-NLS-1$
        help.append("  --query-stdin        Read OQL text from stdin\n"); //$NON-NLS-1$
        help.append("  --command-file PATH  Read MAT query text from a UTF-8 file\n"); //$NON-NLS-1$
        help.append("  --command-stdin      Read MAT query text from stdin\n"); //$NON-NLS-1$
        help.append("  Inner class names containing '$' must be quoted or escaped in shell arguments.\n"); //$NON-NLS-1$
        help.append("  Prefer --query-file/--query-stdin or --command-file/--command-stdin for complex queries.\n\n"); //$NON-NLS-1$
        help.append("Eclipse runtime options:\n"); //$NON-NLS-1$
        help.append("  Pass -configuration DIR and -data DIR through the launcher if needed.\n"); //$NON-NLS-1$
        help.append("  Wrapper env vars: MAT_CLI_VMARGS, MAT_CLI_CONFIG_DIR, MAT_CLI_DATA_DIR\n"); //$NON-NLS-1$
        return help.toString();
    }
}
