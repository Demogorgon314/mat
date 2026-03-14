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

import java.util.List;

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

    public boolean writeJson(JsonWriter writer, CommandMetadataResult result, boolean agentProfile)
    {
        CommandDefinition definition = result.getDefinition();
        if (!agentProfile)
        {
            writer.name("subject").value(definition.getCommand().getToken()); //$NON-NLS-1$
            writer.name("resultType").value(resultKind(result)); //$NON-NLS-1$
        }

        writer.name("name").value(definition.getCommand().getToken()); //$NON-NLS-1$
        writer.name("summary").value(definition.getSummary()); //$NON-NLS-1$
        writer.name("usage").value(definition.getUsage()); //$NON-NLS-1$
        writer.name("requiresSnapshot").value(definition.requiresSnapshot()); //$NON-NLS-1$
        writeStrings(writer, "positionalArguments", definition.getPositionalArguments()); //$NON-NLS-1$
        writeOptions(writer, definition.getOptions());
        writeOutputs(writer, definition.getOutputs());
        if (!agentProfile)
            writeStrings(writer, "suggestedNextCommands", definition.getSuggestedNextCommands()); //$NON-NLS-1$
        if (result.getKind() == CommandMetadataResult.Kind.SCHEMA)
        {
            writer.name("agentEnvelope").beginArray(); //$NON-NLS-1$
            writer.value("schemaVersion"); //$NON-NLS-1$
            writer.value("profile"); //$NON-NLS-1$
            writer.value("command"); //$NON-NLS-1$
            writer.value("heap"); //$NON-NLS-1$
            writer.value("subject"); //$NON-NLS-1$
            writer.value("resultKind"); //$NON-NLS-1$
            writer.value("truncated"); //$NON-NLS-1$
            writer.value("suggestedNextCommands"); //$NON-NLS-1$
            writer.endArray();
            writer.name("payloadKind").value(agentPayloadKind(definition)); //$NON-NLS-1$
            writer.name("payloadDescription").value(definition.getAgentPayloadDescription()); //$NON-NLS-1$
            writeStrings(writer, "payloadFields", definition.getAgentPayloadFields()); //$NON-NLS-1$
        }
        return false;
    }

    public String toText(CommandMetadataResult result)
    {
        CommandDefinition definition = result.getDefinition();
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
        for (OutputDefinition output : definition.getOutputs())
        {
            builder.append("  ").append(output.getProfile()).append('/').append(output.getFormat()).append(": "); //$NON-NLS-1$
            builder.append(output.getResultKind()).append(" - ").append(output.getDescription()).append('\n'); //$NON-NLS-1$
        }
        if (result.getKind() == CommandMetadataResult.Kind.SCHEMA)
        {
            builder.append("Agent payload kind: ").append(agentPayloadKind(definition)).append('\n'); //$NON-NLS-1$
            builder.append("Agent payload: ").append(definition.getAgentPayloadDescription()).append('\n'); //$NON-NLS-1$
            appendStrings(builder, "Agent payload fields", definition.getAgentPayloadFields()); //$NON-NLS-1$
        }
        appendStrings(builder, "Suggested next commands", definition.getSuggestedNextCommands()); //$NON-NLS-1$
        return builder.toString();
    }

    private String agentPayloadKind(CommandDefinition definition)
    {
        for (OutputDefinition output : definition.getOutputs())
        {
            if ("agent".equals(output.getProfile()) && "json".equals(output.getFormat())) //$NON-NLS-1$ //$NON-NLS-2$
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
        for (OutputDefinition output : outputs)
        {
            writer.beginObject();
            writer.name("profile").value(output.getProfile()); //$NON-NLS-1$
            writer.name("format").value(output.getFormat()); //$NON-NLS-1$
            writer.name("resultKind").value(output.getResultKind()); //$NON-NLS-1$
            writer.name("description").value(output.getDescription()); //$NON-NLS-1$
            writer.endObject();
        }
        writer.endArray();
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
