/*******************************************************************************
 * Copyright (c) 2026 Eclipse Memory Analyzer Project.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.mat.cli.internal.serialization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.mat.cli.internal.CliHelp;
import org.eclipse.mat.cli.internal.CliCommandCatalog.CommandDefinition;
import org.eclipse.mat.cli.internal.CliCommandCatalog.OptionDefinition;
import org.eclipse.mat.cli.internal.CliCommandCatalog.OutputDefinition;
import org.eclipse.mat.cli.internal.CommandMetadataResult;

public class CommandMetadataSerializer
{
    public String resultKind(CommandMetadataResult result)
    {
        return result.getKind() == CommandMetadataResult.Kind.DESCRIBE ? "describe" : "schema"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public boolean writeJson(JsonWriter writer, CommandMetadataResult result)
    {
        CommandDefinition definition = result.getDefinition();
        writer.name("name").value(definition.getCommand().getToken()); //$NON-NLS-1$
        writeStringField(writer, "summary", definition.getSummary()); //$NON-NLS-1$
        writeStringField(writer, "usage", definition.getUsage()); //$NON-NLS-1$
        writer.name("requiresSnapshot").value(definition.requiresSnapshot()); //$NON-NLS-1$
        writeStrings(writer, "positionalArguments", definition.getPositionalArguments()); //$NON-NLS-1$
        writeOptions(writer, definition.getOptions());
        writeOutputs(writer, definition.getOutputs());
        if (result.getKind() == CommandMetadataResult.Kind.SCHEMA)
        {
            writer.name("jsonEnvelope").beginArray(); //$NON-NLS-1$
            writer.value("schemaVersion"); //$NON-NLS-1$
            writer.value("resultKind"); //$NON-NLS-1$
            writer.value("truncated"); //$NON-NLS-1$
            writer.value("note when present"); //$NON-NLS-1$
            writer.value("suggestedNextCommands when present"); //$NON-NLS-1$
            writer.endArray();
            writer.name("payloadKind").value(jsonPayloadKind(definition)); //$NON-NLS-1$
            writeStringField(writer, "payloadDescription", definition.getAgentPayloadDescription()); //$NON-NLS-1$
            writeStrings(writer, "payloadFields", definition.getAgentPayloadFields()); //$NON-NLS-1$
        }
        return false;
    }

    public String toText(CommandMetadataResult result)
    {
        return CliHelp.renderCommandMetadata(result.getDefinition(),
                        result.getKind() == CommandMetadataResult.Kind.SCHEMA);
    }

    public void appendMarkdown(MarkdownDocument document, CommandMetadataResult result)
    {
        CommandDefinition definition = result.getDefinition();
        List<String> command = new ArrayList<String>(3);
        command.add("Name: " + definition.getCommand().getToken()); //$NON-NLS-1$
        command.add("Summary: " + definition.getSummary()); //$NON-NLS-1$
        command.add("Usage: " + definition.getUsage()); //$NON-NLS-1$
        command.add("Requires snapshot: " + (definition.requiresSnapshot() ? "yes" : "no")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        document.addSection("Command", MarkdownDocument.bullets(command)); //$NON-NLS-1$
        document.addSection("Positional arguments", positionalArgumentsMarkdown(definition)); //$NON-NLS-1$
        document.addSection("Options", optionsMarkdown(definition.getOptions())); //$NON-NLS-1$
        document.addSection("Outputs", outputsMarkdown(uniqueOutputs(definition.getOutputs()))); //$NON-NLS-1$
        if (result.getKind() == CommandMetadataResult.Kind.SCHEMA)
            document.addSection("JSON payload", schemaMarkdown(definition)); //$NON-NLS-1$
        document.addSection("Suggested next commands", MarkdownDocument.bullets(definition.getSuggestedNextCommands(), true)); //$NON-NLS-1$
    }

    private String jsonPayloadKind(CommandDefinition definition)
    {
        for (OutputDefinition output : definition.getOutputs())
        {
            if ("json".equals(output.getFormat())) //$NON-NLS-1$
                return output.getResultKind();
        }
        return "unknown"; //$NON-NLS-1$
    }

    private void writeOptions(JsonWriter writer, List<OptionDefinition> options)
    {
        if (options == null || options.isEmpty())
            return;
        writer.name("options").beginArray(); //$NON-NLS-1$
        for (OptionDefinition option : options)
        {
            writer.beginObject();
            writer.name("name").value(option.getName()); //$NON-NLS-1$
            writeStringField(writer, "valueHint", option.getValueHint()); //$NON-NLS-1$
            writer.name("required").value(option.isRequired()); //$NON-NLS-1$
            writeStringField(writer, "description", option.getDescription()); //$NON-NLS-1$
            writer.endObject();
        }
        writer.endArray();
    }

    private void writeOutputs(JsonWriter writer, List<OutputDefinition> outputs)
    {
        if (outputs == null || outputs.isEmpty())
            return;
        writer.name("outputs").beginArray(); //$NON-NLS-1$
        for (OutputDefinition output : uniqueOutputs(outputs))
        {
            writer.beginObject();
            writer.name("format").value(output.getFormat()); //$NON-NLS-1$
            writer.name("resultKind").value(output.getResultKind()); //$NON-NLS-1$
            writeStringField(writer, "description", output.getDescription()); //$NON-NLS-1$
            writer.endObject();
        }
        writer.endArray();
    }

    private List<OutputDefinition> uniqueOutputs(List<OutputDefinition> outputs)
    {
        Map<String, OutputDefinition> unique = new LinkedHashMap<String, OutputDefinition>();
        for (OutputDefinition output : outputs)
        {
            unique.put(output.getFormat() + '\u0000' + output.getResultKind(), output);
        }
        return java.util.Arrays.asList(unique.values().toArray(new OutputDefinition[0]));
    }

    private void writeStrings(JsonWriter writer, String name, List<String> values)
    {
        if (values == null || values.isEmpty())
            return;
        writer.name(name).beginArray();
        for (String value : values)
        {
            writer.value(value);
        }
        writer.endArray();
    }

    private void writeStringField(JsonWriter writer, String name, String value)
    {
        if (value == null || value.length() == 0)
            return;
        writer.name(name).value(value);
    }

    private String positionalArgumentsMarkdown(CommandDefinition definition)
    {
        if (definition.getPositionalArguments().isEmpty())
            return "- None."; //$NON-NLS-1$
        return MarkdownDocument.bullets(definition.getPositionalArguments());
    }

    private String optionsMarkdown(List<OptionDefinition> options)
    {
        List<List<String>> rows = new ArrayList<List<String>>(options.size());
        for (OptionDefinition option : options)
        {
            List<String> row = new ArrayList<String>(4);
            row.add(option.getName());
            row.add(option.getValueHint());
            row.add(Boolean.toString(option.isRequired()));
            row.add(option.getDescription());
            rows.add(row);
        }
        return MarkdownDocument.table(java.util.Arrays.asList("Name", "Value", "Required", "Description"), rows); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    private String outputsMarkdown(List<OutputDefinition> outputs)
    {
        List<List<String>> rows = new ArrayList<List<String>>(outputs.size());
        for (OutputDefinition output : outputs)
        {
            List<String> row = new ArrayList<String>(3);
            row.add(output.getFormat());
            row.add(output.getResultKind());
            row.add(output.getDescription());
            rows.add(row);
        }
        return MarkdownDocument.table(java.util.Arrays.asList("Format", "Result kind", "Description"), rows); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    private String schemaMarkdown(CommandDefinition definition)
    {
        List<String> bullets = new ArrayList<String>();
        bullets.add("Envelope fields: schemaVersion, resultKind, truncated, note when present, suggestedNextCommands when present"); //$NON-NLS-1$
        bullets.add("Payload kind: " + jsonPayloadKind(definition)); //$NON-NLS-1$
        if (definition.getAgentPayloadDescription() != null && definition.getAgentPayloadDescription().length() > 0)
            bullets.add("Payload description: " + definition.getAgentPayloadDescription()); //$NON-NLS-1$
        StringBuilder builder = new StringBuilder();
        builder.append(MarkdownDocument.bullets(bullets));
        if (!definition.getAgentPayloadFields().isEmpty())
        {
            builder.append('\n').append('\n');
            builder.append(MarkdownDocument.bullets(definition.getAgentPayloadFields()));
        }
        return builder.toString();
    }

    private void appendStrings(StringBuilder builder, String label, List<String> values)
    {
        builder.append(label).append(":\n"); //$NON-NLS-1$
        for (String value : values)
        {
            builder.append("  ").append(value).append('\n'); //$NON-NLS-1$
        }
    }
}
