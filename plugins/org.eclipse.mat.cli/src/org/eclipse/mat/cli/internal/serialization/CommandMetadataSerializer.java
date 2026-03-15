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
        writer.name("summary").value(definition.getSummary()); //$NON-NLS-1$
        writer.name("usage").value(definition.getUsage()); //$NON-NLS-1$
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
            writer.name("payloadDescription").value(definition.getAgentPayloadDescription()); //$NON-NLS-1$
            writeStrings(writer, "payloadFields", definition.getAgentPayloadFields()); //$NON-NLS-1$
        }
        return false;
    }

    public String toText(CommandMetadataResult result)
    {
        return CliHelp.renderCommandMetadata(result.getDefinition(),
                        result.getKind() == CommandMetadataResult.Kind.SCHEMA);
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
        writer.name("options").beginArray(); //$NON-NLS-1$
        for (OptionDefinition option : options)
        {
            writer.beginObject();
            writer.name("name").value(option.getName()); //$NON-NLS-1$
            writer.name("valueHint").value(option.getValueHint()); //$NON-NLS-1$
            writer.name("required").value(option.isRequired()); //$NON-NLS-1$
            writer.name("description").value(option.getDescription()); //$NON-NLS-1$
            writer.endObject();
        }
        writer.endArray();
    }

    private void writeOutputs(JsonWriter writer, List<OutputDefinition> outputs)
    {
        writer.name("outputs").beginArray(); //$NON-NLS-1$
        for (OutputDefinition output : uniqueOutputs(outputs))
        {
            writer.beginObject();
            writer.name("format").value(output.getFormat()); //$NON-NLS-1$
            writer.name("resultKind").value(output.getResultKind()); //$NON-NLS-1$
            writer.name("description").value(output.getDescription()); //$NON-NLS-1$
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
        writer.name(name).beginArray();
        for (String value : values)
        {
            writer.value(value);
        }
        writer.endArray();
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
