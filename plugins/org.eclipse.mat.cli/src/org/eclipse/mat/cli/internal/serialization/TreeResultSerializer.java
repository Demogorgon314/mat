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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.IContextObject;
import org.eclipse.mat.query.IResultTree;

public class TreeResultSerializer extends StructuredResultSerializer
{
    public boolean writeJson(JsonWriter writer, IResultTree tree, SerializationOptions options)
    {
        writeColumns(writer, tree.getColumns());
        writer.name("rows").beginArray(); //$NON-NLS-1$
        TruncationState state = new TruncationState(options.getTreeNodeLimit());
        writeNodes(writer, tree, tree.getColumns(), tree.getElements(), 0, options, state, new PathState());
        writer.endArray();
        return state.truncated;
    }

    public boolean writeAgentJson(JsonWriter writer, IResultTree tree, SerializationOptions options)
    {
        ColumnSchema[] schema = buildColumnSchemas(tree.getColumns());
        writeAgentSchema(writer, schema);
        writer.name("items").beginArray(); //$NON-NLS-1$
        TruncationState state = new TruncationState(options.getTreeNodeLimit());
        writeAgentNodes(writer, tree, schema, tree.getElements(), 0, options, state, new PathState());
        writer.endArray();
        return state.truncated;
    }

    public String toText(IResultTree tree, SerializationOptions options)
    {
        StringBuilder builder = new StringBuilder();
        appendColumnHeader(builder, tree.getColumns());
        TruncationState state = new TruncationState(Integer.MAX_VALUE);
        appendNodes(builder, tree, tree.getColumns(), tree.getElements(), 0, options, state, new PathState());
        return builder.toString();
    }

    private void writeNodes(JsonWriter writer, IResultTree tree, Column[] columns, List<?> rows, int depth,
                    SerializationOptions options, TruncationState state, PathState path)
    {
        int limit = Math.min(rows.size(), options.getLimit());
        if (rows.size() > limit)
            state.truncated = true;

        for (int ii = 0; ii < limit; ii++)
        {
            if (state.remainingNodes <= 0)
            {
                state.truncated = true;
                break;
            }

            Object row = rows.get(ii);
            state.remainingNodes--;
            writer.beginObject();
            writeRowValues(writer, tree, columns, row);
            writeContext(writer, tree, row, options);
            Integer objectId = rowObjectId(tree, row);
            boolean cycle = path.isCycle(objectId);
            writer.name("_cycle").value(cycle); //$NON-NLS-1$
            if (cycle)
            {
                writer.name("children").beginArray().endArray(); //$NON-NLS-1$
            }
            else if (depth + 1 >= options.getTreeDepthLimit())
            {
                if (tree.hasChildren(row))
                    state.truncated = true;
                writer.name("children").beginArray().endArray(); //$NON-NLS-1$
            }
            else
            {
                writer.name("children").beginArray(); //$NON-NLS-1$
                List<?> children = tree.hasChildren(row) ? tree.getChildren(row) : null;
                path.push(objectId);
                if (children != null)
                {
                    if (state.remainingNodes > 0)
                        writeNodes(writer, tree, columns, children, depth + 1, options, state, path);
                    else
                        state.truncated = true;
                }
                path.pop(objectId);
                writer.endArray();
            }
            writer.endObject();
        }
    }

    private void writeAgentNodes(JsonWriter writer, IResultTree tree, ColumnSchema[] columns, List<?> rows, int depth,
                    SerializationOptions options, TruncationState state, PathState path)
    {
        int limit = Math.min(rows.size(), options.getLimit());
        if (rows.size() > limit)
            state.truncated = true;

        for (int ii = 0; ii < limit; ii++)
        {
            if (state.remainingNodes <= 0)
            {
                state.truncated = true;
                break;
            }

            Object row = rows.get(ii);
            boolean hasChildren = tree.hasChildren(row);
            state.remainingNodes--;

            writer.beginObject();
            writeAgentRow(writer, tree, columns, row, options);
            writer.name("_hasChildren").value(hasChildren); //$NON-NLS-1$
            Integer objectId = rowObjectId(tree, row);
            boolean cycle = path.isCycle(objectId);
            writer.name("_cycle").value(cycle); //$NON-NLS-1$

            boolean childrenTruncated = false;
            writer.name("_children").beginArray(); //$NON-NLS-1$
            if (hasChildren && !cycle)
            {
                if (depth + 1 >= options.getTreeDepthLimit())
                {
                    childrenTruncated = true;
                    state.truncated = true;
                }
                else
                {
                    List<?> children = tree.getChildren(row);
                    int childLimit = Math.min(children.size(), options.getLimit());
                    if (children.size() > childLimit)
                    {
                        childrenTruncated = true;
                        state.truncated = true;
                    }

                    for (int childIndex = 0; childIndex < childLimit; childIndex++)
                    {
                        if (state.remainingNodes <= 0)
                        {
                            childrenTruncated = true;
                            state.truncated = true;
                            break;
                        }
                        path.push(objectId);
                        writeAgentNodes(writer, tree, columns, children.subList(childIndex, childIndex + 1), depth + 1,
                                        options, state, path);
                        path.pop(objectId);
                    }
                }
            }
            writer.endArray();

            writer.name("_childrenTruncated").value(childrenTruncated); //$NON-NLS-1$
            writer.endObject();
        }
    }

    private void appendNodes(StringBuilder builder, IResultTree tree, Column[] columns, List<?> rows, int depth,
                    SerializationOptions options, TruncationState state, PathState path)
    {
        int limit = Math.min(rows.size(), options.getLimit());
        if (rows.size() > limit)
            state.truncated = true;

        for (int ii = 0; ii < limit; ii++)
        {
            Object row = rows.get(ii);
            for (int pad = 0; pad < depth; pad++)
                builder.append("  "); //$NON-NLS-1$
            builder.append("- "); //$NON-NLS-1$
            builder.append(formatRow(columns, tree, row)).append('\n');
            Integer objectId = rowObjectId(tree, row);
            if (path.isCycle(objectId))
            {
                for (int pad = 0; pad < depth + 1; pad++)
                    builder.append("  "); //$NON-NLS-1$
                builder.append("[cycle]\n"); //$NON-NLS-1$
            }
            else if (depth + 1 < options.getTreeDepthLimit() && tree.hasChildren(row))
            {
                path.push(objectId);
                appendNodes(builder, tree, columns, tree.getChildren(row), depth + 1, options, state, path);
                path.pop(objectId);
            }
            else if (tree.hasChildren(row))
            {
                state.truncated = true;
            }
        }

        if (rows.size() > limit)
        {
            for (int pad = 0; pad < depth; pad++)
                builder.append("  "); //$NON-NLS-1$
            builder.append("... ").append(rows.size() - limit).append(" more nodes\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private String formatRow(Column[] columns, IResultTree tree, Object row)
    {
        StringBuilder builder = new StringBuilder();
        for (int ii = 0; ii < columns.length; ii++)
        {
            if (ii > 0)
                builder.append(" | "); //$NON-NLS-1$
            String value = displayValue(columns[ii], row, safeColumnValue(tree, row, ii));
            builder.append(value == null ? "" : value); //$NON-NLS-1$
        }
        return builder.toString();
    }

    private void appendColumnHeader(StringBuilder builder, Column[] columns)
    {
        if (columns == null || columns.length <= 1)
            return;

        int headerLength = 0;
        for (int ii = 0; ii < columns.length; ii++)
        {
            if (ii > 0)
            {
                builder.append(" | "); //$NON-NLS-1$
                headerLength += 3;
            }
            builder.append(columns[ii].getLabel());
            headerLength += columns[ii].getLabel().length();
        }
        builder.append('\n');
        for (int ii = 0; ii < headerLength; ii++)
            builder.append('-');
        builder.append('\n');
    }

    private static final class TruncationState
    {
        private int remainingNodes;
        private boolean truncated;

        private TruncationState(int remainingNodes)
        {
            this.remainingNodes = remainingNodes;
        }
    }

    private Integer rowObjectId(IResultTree tree, Object row)
    {
        return contextObjectId(safeContext(tree, row));
    }

    private static final class PathState
    {
        private final Set<Integer> ancestors = new HashSet<Integer>();

        private boolean isCycle(Integer objectId)
        {
            return objectId != null && ancestors.contains(objectId);
        }

        private void push(Integer objectId)
        {
            if (objectId != null)
                ancestors.add(objectId);
        }

        private void pop(Integer objectId)
        {
            if (objectId != null)
                ancestors.remove(objectId);
        }
    }
}
