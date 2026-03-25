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
import java.util.List;

import org.eclipse.mat.cli.internal.QueryMetadataResult;

public class QueryMetadataSerializer
{
    public String resultKind(QueryMetadataResult result)
    {
        return result.getKind() == QueryMetadataResult.Kind.LIST ? "query-list" : "query-description"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public boolean writeJson(JsonWriter writer, QueryMetadataResult result)
    {
        if (result.getKind() == QueryMetadataResult.Kind.LIST)
        {
            if (result.getQueries() != null && !result.getQueries().isEmpty())
            {
                writer.name("queries").beginArray(); //$NON-NLS-1$
                for (QueryMetadataResult.QueryDefinition query : result.getQueries())
                {
                    writer.beginObject();
                    writeQuery(writer, query);
                    writer.endObject();
                }
                writer.endArray();
            }
        }
        else
        {
            writer.name("query").beginObject(); //$NON-NLS-1$
            writeQuery(writer, result.getQuery());
            writer.endObject();
        }
        return false;
    }

    public String toText(QueryMetadataResult result)
    {
        StringBuilder builder = new StringBuilder(1024);
        if (result.getKind() == QueryMetadataResult.Kind.LIST)
        {
            for (QueryMetadataResult.QueryDefinition query : result.getQueries())
            {
                builder.append(query.getIdentifier());
                if (query.getSummary() != null && query.getSummary().length() > 0)
                    builder.append(" - ").append(query.getSummary()); //$NON-NLS-1$
                builder.append('\n');
            }
            return builder.toString();
        }

        QueryMetadataResult.QueryDefinition query = result.getQuery();
        builder.append("Query: ").append(query.getIdentifier()).append('\n'); //$NON-NLS-1$
        builder.append("Name: ").append(query.getName()).append('\n'); //$NON-NLS-1$
        builder.append("Usage: ").append(query.getUsage()).append('\n'); //$NON-NLS-1$
        builder.append("Category: ").append(query.getCategory()).append('\n'); //$NON-NLS-1$
        builder.append("Class: ").append(query.getCommandClass()).append('\n'); //$NON-NLS-1$
        appendList(builder, "Subjects", query.getSubjects()); //$NON-NLS-1$
        appendArguments(builder, query.getArguments());
        if (query.getHelp() != null && query.getHelp().length() > 0)
            builder.append("Help: ").append(query.getHelp()).append('\n'); //$NON-NLS-1$
        if (query.getHelpUrl() != null && query.getHelpUrl().length() > 0)
            builder.append("Help URL: ").append(query.getHelpUrl()).append('\n'); //$NON-NLS-1$
        return builder.toString();
    }

    public void appendMarkdown(MarkdownDocument document, QueryMetadataResult result)
    {
        if (result.getKind() == QueryMetadataResult.Kind.LIST)
        {
            List<List<String>> rows = new ArrayList<List<String>>();
            for (QueryMetadataResult.QueryDefinition query : result.getQueries())
            {
                List<String> row = new ArrayList<String>(3);
                row.add(query.getIdentifier());
                row.add(query.getSummary());
                row.add(query.getUsage());
                rows.add(row);
            }
            document.addSection("Result", MarkdownDocument.table(java.util.Arrays.asList("Identifier", "Summary", "Usage"), rows)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return;
        }

        QueryMetadataResult.QueryDefinition query = result.getQuery();
        List<String> details = new ArrayList<String>(5);
        details.add("Identifier: " + query.getIdentifier()); //$NON-NLS-1$
        details.add("Name: " + query.getName()); //$NON-NLS-1$
        details.add("Usage: " + query.getUsage()); //$NON-NLS-1$
        details.add("Category: " + query.getCategory()); //$NON-NLS-1$
        details.add("Class: " + query.getCommandClass()); //$NON-NLS-1$
        document.addSection("Query", MarkdownDocument.bullets(details)); //$NON-NLS-1$
        document.addSection("Subjects", query.getSubjects() == null || query.getSubjects().isEmpty() ? "- None." //$NON-NLS-1$
                        : MarkdownDocument.bullets(query.getSubjects()));
        document.addSection("Arguments", argumentsMarkdown(query.getArguments())); //$NON-NLS-1$
        if (query.getHelp() != null && query.getHelp().length() > 0)
            document.addSection("Help", query.getHelp()); //$NON-NLS-1$
        if (query.getHelpUrl() != null && query.getHelpUrl().length() > 0)
            document.addSection("Help URL", MarkdownDocument.inlineCode(query.getHelpUrl())); //$NON-NLS-1$
    }

    private void writeQuery(JsonWriter writer, QueryMetadataResult.QueryDefinition query)
    {
        writer.name("identifier").value(query.getIdentifier()); //$NON-NLS-1$
        writeStringField(writer, "name", query.getName()); //$NON-NLS-1$
        writeStringField(writer, "category", query.getCategory()); //$NON-NLS-1$
        writeStringField(writer, "usage", query.getUsage()); //$NON-NLS-1$
        writeStringField(writer, "summary", query.getSummary()); //$NON-NLS-1$
        writeStringField(writer, "help", query.getHelp()); //$NON-NLS-1$
        writeStringField(writer, "helpUrl", query.getHelpUrl()); //$NON-NLS-1$
        writeStringField(writer, "commandClass", query.getCommandClass()); //$NON-NLS-1$
        writer.name("shallow").value(query.isShallow()); //$NON-NLS-1$
        if (query.getSubjects() != null && !query.getSubjects().isEmpty())
        {
            writer.name("subjects").beginArray(); //$NON-NLS-1$
            for (String subject : query.getSubjects())
            {
                writer.value(subject);
            }
            writer.endArray();
        }
        if (query.getArguments() != null && !query.getArguments().isEmpty())
        {
            writer.name("arguments").beginArray(); //$NON-NLS-1$
            for (QueryMetadataResult.QueryArgument argument : query.getArguments())
            {
                writer.beginObject();
                writer.name("name").value(argument.getName()); //$NON-NLS-1$
                writeStringField(writer, "flag", argument.getFlag()); //$NON-NLS-1$
                writeStringField(writer, "type", argument.getType()); //$NON-NLS-1$
                writeStringField(writer, "advice", argument.getAdvice()); //$NON-NLS-1$
                writer.name("mandatory").value(argument.isMandatory()); //$NON-NLS-1$
                writer.name("multiple").value(argument.isMultiple()); //$NON-NLS-1$
                writer.name("boolean").value(argument.isBoolean()); //$NON-NLS-1$
                writer.name("enum").value(argument.isEnumeration()); //$NON-NLS-1$
                writeStringField(writer, "defaultValue", argument.getDefaultValue()); //$NON-NLS-1$
                writeStringField(writer, "help", argument.getHelp()); //$NON-NLS-1$
                writer.endObject();
            }
            writer.endArray();
        }
    }

    private void writeStringField(JsonWriter writer, String name, String value)
    {
        if (value == null || value.length() == 0)
            return;
        writer.name(name).value(value);
    }

    private void appendList(StringBuilder builder, String label, List<String> values)
    {
        builder.append(label).append(':').append('\n'); //$NON-NLS-1$
        for (String value : values)
        {
            builder.append("  ").append(value).append('\n'); //$NON-NLS-1$
        }
    }

    private void appendArguments(StringBuilder builder, List<QueryMetadataResult.QueryArgument> arguments)
    {
        builder.append("Arguments:\n"); //$NON-NLS-1$
        for (QueryMetadataResult.QueryArgument argument : arguments)
        {
            builder.append("  ").append(argument.getName()); //$NON-NLS-1$
            if (argument.getFlag() != null)
                builder.append(" (-").append(argument.getFlag()).append(')'); //$NON-NLS-1$
            builder.append(": ").append(argument.getType()); //$NON-NLS-1$
            if (argument.isMandatory())
                builder.append(" required"); //$NON-NLS-1$
            if (argument.isMultiple())
                builder.append(" multiple"); //$NON-NLS-1$
            if (argument.getHelp() != null && argument.getHelp().length() > 0)
                builder.append(" - ").append(argument.getHelp()); //$NON-NLS-1$
            builder.append('\n');
        }
    }

    private String argumentsMarkdown(List<QueryMetadataResult.QueryArgument> arguments)
    {
        if (arguments == null || arguments.isEmpty())
            return "- None."; //$NON-NLS-1$

        List<List<String>> rows = new ArrayList<List<String>>(arguments.size());
        for (QueryMetadataResult.QueryArgument argument : arguments)
        {
            List<String> row = new ArrayList<String>(7);
            row.add(argument.getName());
            row.add(argument.getFlag());
            row.add(argument.getType());
            row.add(Boolean.toString(argument.isMandatory()));
            row.add(Boolean.toString(argument.isMultiple()));
            row.add(Boolean.toString(argument.isBoolean()));
            row.add(argument.getHelp());
            rows.add(row);
        }
        return MarkdownDocument.table(
                        java.util.Arrays.asList("Name", "Flag", "Type", "Mandatory", "Multiple", "Boolean", "Help"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
                        rows);
    }
}
