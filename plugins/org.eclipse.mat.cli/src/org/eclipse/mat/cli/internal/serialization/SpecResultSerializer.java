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
import org.eclipse.mat.query.IResultPie;
import org.eclipse.mat.query.IResultTable;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.query.results.CompositeResult;
import org.eclipse.mat.query.results.DisplayFileResult;
import org.eclipse.mat.query.results.TextResult;
import org.eclipse.mat.report.QuerySpec;
import org.eclipse.mat.report.SectionSpec;
import org.eclipse.mat.report.Spec;

public class SpecResultSerializer
{
    private final TableResultSerializer tableSerializer;
    private final TreeResultSerializer treeSerializer;
    private final TextResultSerializer textSerializer;
    private final DisplayFileResultSerializer displayFileSerializer = new DisplayFileResultSerializer();
    private final PieResultSerializer pieSerializer;

    public SpecResultSerializer(TableResultSerializer tableSerializer, TreeResultSerializer treeSerializer,
                    TextResultSerializer textSerializer)
    {
        this(tableSerializer, treeSerializer, textSerializer, new PieResultSerializer());
    }

    public SpecResultSerializer(TableResultSerializer tableSerializer, TreeResultSerializer treeSerializer,
                    TextResultSerializer textSerializer, PieResultSerializer pieSerializer)
    {
        this.tableSerializer = tableSerializer;
        this.treeSerializer = treeSerializer;
        this.textSerializer = textSerializer;
        this.pieSerializer = pieSerializer;
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
        return writeJson(writer, spec, options, 0);
    }

    private boolean writeJson(JsonWriter writer, Spec spec, SerializationOptions options, int sectionDepth)
                    throws CliException
    {
        if (spec instanceof SectionSpec)
            return writeSection(writer, (SectionSpec) spec, options, sectionDepth);
        if (spec instanceof QuerySpec)
            return writeQuery(writer, (QuerySpec) spec, options, sectionDepth);

        writeSpecMetadata(writer, spec);
        return false;
    }

    public String toText(Spec spec, SerializationOptions options)
    {
        StringBuilder builder = new StringBuilder();
        appendText(builder, spec, options, 0, 0);
        return builder.toString();
    }

    public boolean writeCompositeResult(JsonWriter writer, CompositeResult result, SerializationOptions options)
                    throws CliException
    {
        return writeJson(writer, asSection(result, null), options);
    }

    public String compositeToText(CompositeResult result, SerializationOptions options)
    {
        return toText(asSection(result, null), options);
    }

    public void appendMarkdown(MarkdownDocument document, Spec spec, SerializationOptions options) throws CliException
    {
        appendMarkdown(document, spec, options, null, 0);
    }

    public void appendCompositeMarkdown(MarkdownDocument document, CompositeResult result, SerializationOptions options)
                    throws CliException
    {
        appendMarkdownSection(document, asSection(result, "Result"), options, "Result", 0); //$NON-NLS-1$ //$NON-NLS-2$
    }

    SectionSpec asSection(CompositeResult result, String fallbackName)
    {
        String sectionName = result.getName() != null ? result.getName() : fallbackName;
        if (sectionName == null)
            sectionName = "Result"; //$NON-NLS-1$

        SectionSpec section = new SectionSpec(sectionName);
        section.setStatus(result.getStatus());

        int index = 1;
        for (CompositeResult.Entry entry : result.getResultEntries())
        {
            String label = entry.getName();
            if (label == null && entry.getResult() instanceof Spec)
                label = ((Spec) entry.getResult()).getName();
            if (label == null)
                label = sectionName + " " + index; //$NON-NLS-1$

            if (entry.getResult() instanceof QuerySpec)
            {
                QuerySpec original = (QuerySpec) entry.getResult();
                QuerySpec child = new QuerySpec(label);
                child.setTemplate(original.getTemplate());
                child.putAll(original.getParams());
                child.setCommand(original.getCommand());
                child.setResult(original.getResult());
                section.add(child);
            }
            else
            {
                QuerySpec child = new QuerySpec(label);
                child.setResult(entry.getResult());
                section.add(child);
            }
            index++;
        }

        return section;
    }

    private boolean writeSection(JsonWriter writer, SectionSpec spec, SerializationOptions options, int sectionDepth)
                    throws CliException
    {
        writeSpecMetadata(writer, spec);
        if (sectionDepth >= options.getTreeDepthLimit())
            return !spec.getChildren().isEmpty();
        return writeSectionContents(writer, spec, options, sectionDepth);
    }

    private boolean writeSectionContents(JsonWriter writer, SectionSpec spec, SerializationOptions options,
                    int sectionDepth)
                    throws CliException
    {
        boolean truncated = false;
        List<Spec> children = spec.getChildren();
        int limit = Math.min(children.size(), options.getEffectiveLimit());
        if (children.size() > limit)
            truncated = true;

        if (limit > 0)
        {
            writer.name("sections").beginArray(); //$NON-NLS-1$
            for (int ii = 0; ii < limit; ii++)
            {
                Spec child = children.get(ii);
                writer.beginObject();
                writer.name("kind").value(rootResultType(child)); //$NON-NLS-1$
                int childSectionDepth = child instanceof SectionSpec ? sectionDepth + 1 : sectionDepth;
                boolean childTruncated = writeJson(writer, child, options, childSectionDepth);
                writer.name("truncated").value(childTruncated); //$NON-NLS-1$
                writer.endObject();
                truncated |= childTruncated;
            }
            writer.endArray();
        }

        return truncated;
    }

    private boolean writeQuery(JsonWriter writer, QuerySpec spec, SerializationOptions options, int sectionDepth)
                    throws CliException
    {
        writeSpecMetadata(writer, spec);
        writeStringField(writer, "queryCommand", spec.getCommand()); //$NON-NLS-1$
        return writeResult(writer, spec.getResult(), options, spec.getName(), sectionDepth);
    }

    private boolean writeResult(JsonWriter writer, IResult result, SerializationOptions options, String fallbackName)
                    throws CliException
    {
        return writeResult(writer, result, options, fallbackName, 0);
    }

    private boolean writeResult(JsonWriter writer, IResult result, SerializationOptions options, String fallbackName,
                    int sectionDepth)
                    throws CliException
    {
        if (result == null)
        {
            writer.name("resultType").value("empty"); //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }

        if (result instanceof TextResult)
        {
            writer.name("resultType").value("text"); //$NON-NLS-1$ //$NON-NLS-2$
            if (options.isAgentProfile())
            {
                writer.name("content").value(textSerializer.toText((TextResult) result)); //$NON-NLS-1$
                return false;
            }
            return textSerializer.writeJson(writer, (TextResult) result);
        }
        if (result instanceof DisplayFileResult)
        {
            writer.name("resultType").value("text"); //$NON-NLS-1$ //$NON-NLS-2$
            writer.name("content").value(displayFileSerializer.toText((DisplayFileResult) result)); //$NON-NLS-1$
            return false;
        }
        if (result instanceof IResultTable)
        {
            writer.name("resultType").value("table"); //$NON-NLS-1$ //$NON-NLS-2$
            if (options.isAgentProfile())
                return tableSerializer.writeAgentJson(writer, (IResultTable) result, options);
            return tableSerializer.writeJson(writer, (IResultTable) result, options);
        }
        if (result instanceof IResultTree)
        {
            writer.name("resultType").value("tree"); //$NON-NLS-1$ //$NON-NLS-2$
            if (options.isAgentProfile())
                return treeSerializer.writeAgentJson(writer, (IResultTree) result, options);
            return treeSerializer.writeJson(writer, (IResultTree) result, options);
        }
        if (result instanceof IResultPie)
        {
            writer.name("resultType").value("pie"); //$NON-NLS-1$ //$NON-NLS-2$
            if (options.isAgentProfile())
                return pieSerializer.writeAgentJson(writer, (IResultPie) result, options);
            return pieSerializer.writeJson(writer, (IResultPie) result, options);
        }
        if (result instanceof CompositeResult)
        {
            writer.name("resultType").value("section"); //$NON-NLS-1$ //$NON-NLS-2$
            return writeNestedSection(writer, asSection((CompositeResult) result, fallbackName), options, sectionDepth);
        }
        if (result instanceof SectionSpec)
        {
            writer.name("resultType").value("section"); //$NON-NLS-1$ //$NON-NLS-2$
            return writeNestedSection(writer, (SectionSpec) result, options, sectionDepth);
        }
        if (result instanceof Spec)
        {
            writer.name("resultType").value("section"); //$NON-NLS-1$ //$NON-NLS-2$
            return writeNestedSection(writer, wrapSpec((Spec) result, fallbackName), options, sectionDepth);
        }

        writer.name("resultType").value("unsupported"); //$NON-NLS-1$ //$NON-NLS-2$
        writer.name("unsupportedType").value(result.getClass().getName()); //$NON-NLS-1$
        return false;
    }

    private void writeSpecMetadata(JsonWriter writer, Spec spec)
    {
        writeStringField(writer, "name", spec.getName()); //$NON-NLS-1$
        writeStringField(writer, "template", spec.getTemplate()); //$NON-NLS-1$
        List<String> keys = new ArrayList<String>(spec.getParams().keySet());
        Collections.sort(keys);
        if (!keys.isEmpty())
        {
            writer.name("params").beginObject(); //$NON-NLS-1$
            for (String key : keys)
            {
                writer.name(key).value(spec.getParams().get(key));
            }
            writer.endObject();
        }
    }

    private boolean writeNestedSection(JsonWriter writer, SectionSpec spec, SerializationOptions options, int sectionDepth)
                    throws CliException
    {
        if (sectionDepth + 1 >= options.getTreeDepthLimit())
            return !spec.getChildren().isEmpty();
        return writeSectionContents(writer, spec, options, sectionDepth + 1);
    }

    private void writeStringField(JsonWriter writer, String name, String value)
    {
        if (value == null || value.length() == 0)
            return;
        writer.name(name).value(value);
    }

    private void appendText(StringBuilder builder, Spec spec, SerializationOptions options, int depth, int sectionDepth)
    {
        indent(builder, depth);
        builder.append(spec.getName() == null ? "<unnamed>" : spec.getName()).append('\n'); //$NON-NLS-1$

        if (spec instanceof SectionSpec)
        {
            List<Spec> children = ((SectionSpec) spec).getChildren();
            if (sectionDepth >= options.getTreeDepthLimit())
            {
                if (!children.isEmpty())
                {
                    builder.append('\n');
                    indent(builder, depth + 1);
                    builder.append("[section truncated by depth]\n"); //$NON-NLS-1$
                }
                return;
            }
            int limit = Math.min(children.size(), options.getEffectiveLimit());
            for (int ii = 0; ii < limit; ii++)
            {
                builder.append('\n');
                Spec child = children.get(ii);
                int childSectionDepth = child instanceof SectionSpec ? sectionDepth + 1 : sectionDepth;
                appendText(builder, child, options, depth + 1, childSectionDepth);
            }
            if (children.size() > limit)
            {
                builder.append('\n');
                indent(builder, depth + 1);
                builder.append("... ").append(children.size() - limit).append(" more sections\n"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return;
        }

        if (!(spec instanceof QuerySpec))
            return;

        appendResultText(builder, ((QuerySpec) spec).getResult(), options, depth, spec.getName(), sectionDepth);
    }

    private void appendResultText(StringBuilder builder, IResult result, SerializationOptions options, int depth,
                    String fallbackName, int sectionDepth)
    {
        if (result == null)
            return;

        indent(builder, depth + 1);
        if (result instanceof TextResult)
        {
            builder.append(textSerializer.toText((TextResult) result)).append('\n');
        }
        else if (result instanceof DisplayFileResult)
        {
            builder.append(displayFileSerializer.toText((DisplayFileResult) result)).append('\n');
        }
        else if (result instanceof IResultTable)
        {
            builder.append(tableSerializer.toText((IResultTable) result, options));
        }
        else if (result instanceof IResultTree)
        {
            builder.append(treeSerializer.toText((IResultTree) result, options));
        }
        else if (result instanceof IResultPie)
        {
            builder.append(pieSerializer.toText((IResultPie) result, options));
        }
        else if (result instanceof CompositeResult)
        {
            builder.append('\n');
            appendNestedSection(builder, asSection((CompositeResult) result, fallbackName), options, depth, sectionDepth);
        }
        else if (result instanceof Spec)
        {
            builder.append('\n');
            appendNestedSection(builder, wrapSpec((Spec) result, fallbackName), options, depth, sectionDepth);
        }
        else
        {
            builder.append("[unsupported result: ").append(result.getClass().getName()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private void appendNestedSection(StringBuilder builder, SectionSpec spec, SerializationOptions options, int depth,
                    int sectionDepth)
    {
        if (sectionDepth + 1 >= options.getTreeDepthLimit())
        {
            indent(builder, depth + 1);
            builder.append("[section truncated by depth]\n"); //$NON-NLS-1$
            return;
        }
        appendText(builder, spec, options, depth + 1, sectionDepth + 1);
    }

    private SectionSpec wrapSpec(Spec spec, String fallbackName)
    {
        if (spec instanceof SectionSpec)
            return (SectionSpec) spec;

        String name = spec.getName() != null ? spec.getName() : fallbackName;
        if (name == null)
            name = "Result"; //$NON-NLS-1$

        SectionSpec wrapper = new SectionSpec(name);
        if (spec instanceof QuerySpec)
            wrapper.add((QuerySpec) spec);
        return wrapper;
    }

    private void indent(StringBuilder builder, int depth)
    {
        for (int ii = 0; ii < depth; ii++)
            builder.append("  "); //$NON-NLS-1$
    }

    private void appendMarkdown(MarkdownDocument document, Spec spec, SerializationOptions options, String parentPath,
                    int sectionDepth) throws CliException
    {
        String currentPath = joinPath(parentPath, spec.getName());
        if (spec instanceof SectionSpec)
        {
            appendMarkdownSection(document, (SectionSpec) spec, options, currentPath, sectionDepth);
            return;
        }

        if (!(spec instanceof QuerySpec))
            return;

        QuerySpec query = (QuerySpec) spec;
        IResult result = query.getResult();
        if (result instanceof CompositeResult)
        {
            appendMarkdownSection(document, asSection((CompositeResult) result, currentPath), options, currentPath,
                            sectionDepth);
            return;
        }
        if (result instanceof SectionSpec)
        {
            appendMarkdownSection(document, (SectionSpec) result, options, currentPath, sectionDepth);
            return;
        }
        if (result instanceof Spec)
        {
            appendMarkdown(document, (Spec) result, options, currentPath, sectionDepth);
            return;
        }

        document.addSection(currentPath == null ? "Result" : currentPath, markdownForLeaf(result, options)); //$NON-NLS-1$
    }

    private void appendMarkdownSection(MarkdownDocument document, SectionSpec section, SerializationOptions options,
                    String currentPath, int sectionDepth) throws CliException
    {
        List<Spec> children = section.getChildren();
        String path = currentPath == null || currentPath.length() == 0 ? "Result" : currentPath; //$NON-NLS-1$
        if (sectionDepth >= options.getTreeDepthLimit())
        {
            if (!children.isEmpty())
                document.addSection(path, "Section truncated by depth."); //$NON-NLS-1$
            return;
        }

        int limit = Math.min(children.size(), options.getEffectiveLimit());
        for (int ii = 0; ii < limit; ii++)
        {
            Spec child = children.get(ii);
            int childSectionDepth = child instanceof SectionSpec ? sectionDepth + 1 : sectionDepth;
            appendMarkdown(document, child, options, path, childSectionDepth);
        }
        if (children.size() > limit)
            document.addSection(path, "Section truncated by limit."); //$NON-NLS-1$
    }

    private String markdownForLeaf(IResult result, SerializationOptions options) throws CliException
    {
        if (result == null)
            return "- Empty result."; //$NON-NLS-1$
        if (result instanceof TextResult)
            return textSerializer.toMarkdown((TextResult) result, null);
        if (result instanceof DisplayFileResult)
            return displayFileSerializer.toMarkdown((DisplayFileResult) result);
        if (result instanceof IResultTable)
            return tableSerializer.toMarkdown((IResultTable) result, options);
        if (result instanceof IResultTree)
            return treeSerializer.toMarkdown((IResultTree) result, options);
        if (result instanceof IResultPie)
            return pieSerializer.toMarkdown((IResultPie) result, options);
        throw CliException.unsupported("Unsupported result type: " + result.getClass().getName()); //$NON-NLS-1$
    }

    private String joinPath(String parentPath, String name)
    {
        String segment = name == null || name.length() == 0 ? "Result" : name; //$NON-NLS-1$
        if (parentPath == null || parentPath.length() == 0)
            return segment;
        return parentPath + " / " + segment; //$NON-NLS-1$
    }
}
