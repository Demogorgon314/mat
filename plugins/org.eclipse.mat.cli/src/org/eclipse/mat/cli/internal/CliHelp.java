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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.mat.cli.internal.CliCommandCatalog.CommandDefinition;
import org.eclipse.mat.cli.internal.CliCommandCatalog.OptionDefinition;
import org.eclipse.mat.cli.internal.CliCommandCatalog.OutputDefinition;

public final class CliHelp
{
    private CliHelp()
    {}

    public static String generalHelp()
    {
        StringBuilder help = new StringBuilder();
        help.append("Usage:\n"); //$NON-NLS-1$
        help.append("  mat-cli <command> <heap> [options]\n"); //$NON-NLS-1$
        help.append("  mat-cli <command> [options]\n"); //$NON-NLS-1$
        help.append("  mat-cli <command> --help\n"); //$NON-NLS-1$
        help.append("  mat-cli --help\n\n"); //$NON-NLS-1$
        help.append("Commands:\n"); //$NON-NLS-1$
        appendCommandSynopsis(help);
        help.append("Use 'mat-cli <command> --help' for command-specific help.\n\n"); //$NON-NLS-1$
        help.append("Global options:\n"); //$NON-NLS-1$
        help.append("  --format text|json   Output format (default: text)\n"); //$NON-NLS-1$
        help.append("  --bytes-display MODE Byte display mode for text output: bytes|kilobytes|megabytes|gigabytes|smart (default: smart)\n"); //$NON-NLS-1$
        help.append("  --limit N            Maximum rows or children per level (default: 20, max: 10000)\n"); //$NON-NLS-1$
        help.append("  --depth N            Maximum tree or section depth (default: 8, inspect-object: 3, biggest-objects: 1)\n"); //$NON-NLS-1$
        help.append("  --verbose            Print detailed diagnostics on failure\n"); //$NON-NLS-1$
        help.append("  --help               Show general or command-specific help\n\n"); //$NON-NLS-1$
        help.append("Structured-output options:\n"); //$NON-NLS-1$
        help.append("  --dump               Emit full structured values for supported commands, ignore --limit, and only honor --depth\n\n"); //$NON-NLS-1$
        help.append("Inspect-object options:\n"); //$NON-NLS-1$
        help.append("  --select-fields FIELD Inspect direct fields from the root object; may be repeated\n"); //$NON-NLS-1$
        help.append("  --field-paths PATH    Inspect dotted field paths such as cleaner.offsetMap; may be repeated\n"); //$NON-NLS-1$
        help.append("  --show-nulls         Show nested null fields and array slots in text output\n\n"); //$NON-NLS-1$
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

    public static String commandHelp(CliCommand command)
    {
        CommandDefinition definition = command == null ? null : CliCommandCatalog.lookup(command);
        return definition == null ? generalHelp() : renderCommandMetadata(definition, false);
    }

    public static String renderCommandMetadata(CommandDefinition definition, boolean includeSchemaDetails)
    {
        StringBuilder builder = new StringBuilder(512);
        builder.append("Command: ").append(definition.getCommand().getToken()).append('\n'); //$NON-NLS-1$
        builder.append("Summary: ").append(definition.getSummary()).append('\n'); //$NON-NLS-1$
        builder.append("Usage: ").append(definition.getUsage()).append('\n'); //$NON-NLS-1$
        builder.append("Requires snapshot: ").append(definition.requiresSnapshot() ? "yes" : "no").append('\n'); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        appendStrings(builder, "Positional arguments", definition.getPositionalArguments()); //$NON-NLS-1$
        builder.append("Options:\n"); //$NON-NLS-1$
        for (OptionDefinition option : definition.getOptions())
        {
            builder.append("  ").append(option.getName()); //$NON-NLS-1$
            if (option.getValueHint() != null)
                builder.append(' ').append(option.getValueHint());
            builder.append(option.isRequired() ? " (required): " : ": "); //$NON-NLS-1$ //$NON-NLS-2$
            builder.append(option.getDescription()).append('\n');
        }
        builder.append("Outputs:\n"); //$NON-NLS-1$
        for (OutputDefinition output : uniqueOutputs(definition.getOutputs()))
        {
            builder.append("  ").append(output.getFormat()).append(": "); //$NON-NLS-1$
            builder.append(output.getResultKind()).append(" - ").append(output.getDescription()).append('\n'); //$NON-NLS-1$
        }
        if (includeSchemaDetails)
        {
            builder.append("JSON payload kind: ").append(jsonPayloadKind(definition)).append('\n'); //$NON-NLS-1$
            builder.append("JSON payload: ").append(definition.getAgentPayloadDescription()).append('\n'); //$NON-NLS-1$
            appendStrings(builder, "JSON payload fields", definition.getAgentPayloadFields()); //$NON-NLS-1$
        }
        appendStrings(builder, "Suggested next commands", definition.getSuggestedNextCommands()); //$NON-NLS-1$
        return builder.toString();
    }

    private static void appendCommandSynopsis(StringBuilder help)
    {
        int maxTokenLength = 0;
        for (CliCommand command : CliCommand.values())
        {
            maxTokenLength = Math.max(maxTokenLength, command.getToken().length());
        }
        for (CliCommand command : CliCommand.values())
        {
            CommandDefinition definition = CliCommandCatalog.lookup(command);
            if (definition != null)
            {
                help.append("  ").append(definition.getCommand().getToken()); //$NON-NLS-1$
                appendPadding(help, maxTokenLength - definition.getCommand().getToken().length() + 2);
                help.append(definition.getSummary()).append('\n');
            }
        }
        help.append('\n');
    }

    private static void appendPadding(StringBuilder builder, int count)
    {
        for (int index = 0; index < count; index++)
        {
            builder.append(' ');
        }
    }

    private static String jsonPayloadKind(CommandDefinition definition)
    {
        for (OutputDefinition output : definition.getOutputs())
        {
            if ("json".equals(output.getFormat())) //$NON-NLS-1$
                return output.getResultKind();
        }
        return "unknown"; //$NON-NLS-1$
    }

    private static List<OutputDefinition> uniqueOutputs(List<OutputDefinition> outputs)
    {
        Map<String, OutputDefinition> unique = new LinkedHashMap<String, OutputDefinition>();
        for (OutputDefinition output : outputs)
        {
            unique.put(output.getFormat() + '\u0000' + output.getResultKind(), output);
        }
        return java.util.Arrays.asList(unique.values().toArray(new OutputDefinition[0]));
    }

    private static void appendStrings(StringBuilder builder, String label, List<String> values)
    {
        builder.append(label).append(":\n"); //$NON-NLS-1$
        for (String value : values)
        {
            builder.append("  ").append(value).append('\n'); //$NON-NLS-1$
        }
    }
}
