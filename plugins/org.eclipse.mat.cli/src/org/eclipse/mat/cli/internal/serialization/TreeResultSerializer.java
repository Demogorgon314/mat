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

import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.IResultTree;

public class TreeResultSerializer extends StructuredResultSerializer
{
    public boolean writeJson(JsonWriter writer, IResultTree tree, SerializationOptions options)
    {
        writeColumns(writer, tree.getColumns());
        writer.name("rows").beginArray(); //$NON-NLS-1$
        TruncationState state = new TruncationState();
        writeNodes(writer, tree, tree.getColumns(), tree.getElements(), 0, options, state);
        writer.endArray();
        return state.truncated;
    }

    public String toText(IResultTree tree, SerializationOptions options)
    {
        StringBuilder builder = new StringBuilder();
        TruncationState state = new TruncationState();
        appendNodes(builder, tree, tree.getColumns(), tree.getElements(), 0, options, state);
        return builder.toString();
    }

    private void writeNodes(JsonWriter writer, IResultTree tree, Column[] columns, List<?> rows, int depth,
                    SerializationOptions options, TruncationState state)
    {
        int limit = Math.min(rows.size(), options.getLimit());
        if (rows.size() > limit)
            state.truncated = true;

        for (int ii = 0; ii < limit; ii++)
        {
            Object row = rows.get(ii);
            writer.beginObject();
            writeRowValues(writer, tree, columns, row);
            writeContext(writer, tree, row);
            if (depth + 1 >= options.getTreeDepthLimit())
            {
                if (tree.hasChildren(row))
                    state.truncated = true;
                writer.name("children").beginArray().endArray(); //$NON-NLS-1$
            }
            else
            {
                writer.name("children").beginArray(); //$NON-NLS-1$
                List<?> children = tree.hasChildren(row) ? tree.getChildren(row) : null;
                if (children != null)
                    writeNodes(writer, tree, columns, children, depth + 1, options, state);
                writer.endArray();
            }
            writer.endObject();
        }
    }

    private void appendNodes(StringBuilder builder, IResultTree tree, Column[] columns, List<?> rows, int depth,
                    SerializationOptions options, TruncationState state)
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
            if (depth + 1 < options.getTreeDepthLimit() && tree.hasChildren(row))
            {
                appendNodes(builder, tree, columns, tree.getChildren(row), depth + 1, options, state);
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
            String value = displayValue(columns[ii], row, tree.getColumnValue(row, ii));
            builder.append(value == null ? "" : value); //$NON-NLS-1$
        }
        return builder.toString();
    }

    private static final class TruncationState
    {
        private boolean truncated;
    }
}
