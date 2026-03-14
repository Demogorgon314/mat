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
import java.util.Collections;
import java.util.List;

import org.eclipse.mat.cli.internal.CliException;
import org.eclipse.mat.query.IResult;
import org.eclipse.mat.query.IResultTable;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.query.results.TextResult;
import org.eclipse.mat.report.QuerySpec;
import org.eclipse.mat.report.SectionSpec;
import org.eclipse.mat.report.Spec;

public class SpecResultSerializer
{
    private final TableResultSerializer tableSerializer;
    private final TreeResultSerializer treeSerializer;
    private final TextResultSerializer textSerializer;

    public SpecResultSerializer(TableResultSerializer tableSerializer, TreeResultSerializer treeSerializer,
                    TextResultSerializer textSerializer)
    {
        this.tableSerializer = tableSerializer;
        this.treeSerializer = treeSerializer;
        this.textSerializer = textSerializer;
    }

    public String rootResultType(Spec spec)
    {
        if (spec instanceof SectionSpec)
            return "section"; //$NON-NLS-1$
        if (spec instanceof QuerySpec)
            return "query"; //$NON-NLS-1$
        return "spec"; //$NON-NLS-1$
    }

    public boolean writeJson(JsonWriter writer, Spec spec, SerializationOptions options) throws CliException
    {
        if (spec instanceof SectionSpec)
            return writeSection(writer, (SectionSpec) spec, options);
        if (spec instanceof QuerySpec)
            return writeQuery(writer, (QuerySpec) spec, options);

        writeSpecMetadata(writer, spec);
        writer.name("sections").beginArray().endArray(); //$NON-NLS-1$
        return false;
    }

    public String toText(Spec spec, SerializationOptions options)
    {
        StringBuilder builder = new StringBuilder();
        appendText(builder, spec, options, 0);
        return builder.toString();
    }

    private boolean writeSection(JsonWriter writer, SectionSpec spec, SerializationOptions options) throws CliException
    {
        boolean truncated = false;

        writeSpecMetadata(writer, spec);
        writer.name("sections").beginArray(); //$NON-NLS-1$
        for (Spec child : spec.getChildren())
        {
            writer.beginObject();
            writer.name("kind").value(rootResultType(child)); //$NON-NLS-1$
            boolean childTruncated = writeJson(writer, child, options);
            writer.name("truncated").value(childTruncated); //$NON-NLS-1$
            writer.endObject();
            truncated |= childTruncated;
        }
        writer.endArray();

        return truncated;
    }

    private boolean writeQuery(JsonWriter writer, QuerySpec spec, SerializationOptions options) throws CliException
    {
        writeSpecMetadata(writer, spec);
        writer.name("queryCommand").value(spec.getCommand()); //$NON-NLS-1$

        IResult result = spec.getResult();
        if (result == null)
        {
            writer.name("resultType").value("empty"); //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }

        if (result instanceof TextResult)
        {
            writer.name("resultType").value("text"); //$NON-NLS-1$ //$NON-NLS-2$
            return textSerializer.writeJson(writer, (TextResult) result);
        }
        if (result instanceof IResultTable)
        {
            writer.name("resultType").value("table"); //$NON-NLS-1$ //$NON-NLS-2$
            return tableSerializer.writeJson(writer, (IResultTable) result, options);
        }
        if (result instanceof IResultTree)
        {
            writer.name("resultType").value("tree"); //$NON-NLS-1$ //$NON-NLS-2$
            return treeSerializer.writeJson(writer, (IResultTree) result, options);
        }
        if (result instanceof Spec)
        {
            writer.name("resultType").value(rootResultType((Spec) result)); //$NON-NLS-1$
            return writeJson(writer, (Spec) result, options);
        }

        writer.name("resultType").value("unsupported"); //$NON-NLS-1$ //$NON-NLS-2$
        writer.name("unsupportedType").value(result.getClass().getName()); //$NON-NLS-1$
        return false;
    }

    private void writeSpecMetadata(JsonWriter writer, Spec spec)
    {
        writer.name("name").value(spec.getName()); //$NON-NLS-1$
        writer.name("template").value(spec.getTemplate()); //$NON-NLS-1$
        writer.name("params").beginObject(); //$NON-NLS-1$
        List<String> keys = new ArrayList<String>(spec.getParams().keySet());
        Collections.sort(keys);
        for (String key : keys)
        {
            writer.name(key).value(spec.getParams().get(key));
        }
        writer.endObject();
    }

    private void appendText(StringBuilder builder, Spec spec, SerializationOptions options, int depth)
    {
        indent(builder, depth);
        builder.append(spec.getName() == null ? "<unnamed>" : spec.getName()).append('\n'); //$NON-NLS-1$

        if (spec instanceof SectionSpec)
        {
            for (Spec child : ((SectionSpec) spec).getChildren())
            {
                builder.append('\n');
                appendText(builder, child, options, depth + 1);
            }
            return;
        }

        if (!(spec instanceof QuerySpec))
            return;

        QuerySpec query = (QuerySpec) spec;
        IResult result = query.getResult();
        if (result == null)
            return;

        indent(builder, depth + 1);
        if (result instanceof TextResult)
        {
            builder.append(textSerializer.toText((TextResult) result)).append('\n');
        }
        else if (result instanceof IResultTable)
        {
            builder.append(tableSerializer.toText((IResultTable) result, options));
        }
        else if (result instanceof IResultTree)
        {
            builder.append(treeSerializer.toText((IResultTree) result, options));
        }
        else if (result instanceof Spec)
        {
            builder.append('\n');
            appendText(builder, (Spec) result, options, depth + 1);
        }
        else
        {
            builder.append("[unsupported result: ").append(result.getClass().getName()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private void indent(StringBuilder builder, int depth)
    {
        for (int ii = 0; ii < depth; ii++)
            builder.append("  "); //$NON-NLS-1$
    }
}
