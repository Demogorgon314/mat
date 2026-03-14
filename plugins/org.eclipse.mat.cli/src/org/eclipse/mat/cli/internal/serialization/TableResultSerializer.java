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

import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.IResultTable;

public class TableResultSerializer extends StructuredResultSerializer
{
    public boolean writeJson(JsonWriter writer, IResultTable table, SerializationOptions options)
    {
        Column[] columns = table.getColumns();
        writeColumns(writer, columns);

        int rowCount = table.getRowCount();
        int limit = Math.min(rowCount, options.getLimit());
        boolean truncated = rowCount > limit;

        writer.name("rows").beginArray(); //$NON-NLS-1$
        for (int ii = 0; ii < limit; ii++)
        {
            Object row = table.getRow(ii);
            writer.beginObject();
            writeRowValues(writer, table, columns, row);
            writeContext(writer, table, row);
            writer.endObject();
        }
        writer.endArray();

        return truncated;
    }

    public String toText(IResultTable table, SerializationOptions options)
    {
        Column[] columns = table.getColumns();
        int limit = Math.min(table.getRowCount(), options.getLimit());
        List<String[]> rows = new ArrayList<String[]>(limit);
        int[] widths = new int[columns.length];

        for (int ii = 0; ii < columns.length; ii++)
        {
            widths[ii] = columns[ii].getLabel().length();
        }

        for (int ii = 0; ii < limit; ii++)
        {
            Object row = table.getRow(ii);
            String[] values = new String[columns.length];
            for (int jj = 0; jj < columns.length; jj++)
            {
                values[jj] = safe(displayValue(columns[jj], row, table.getColumnValue(row, jj)));
                widths[jj] = Math.min(Math.max(widths[jj], values[jj].length()), 80);
            }
            rows.add(values);
        }

        StringBuilder builder = new StringBuilder();
        appendRow(builder, widths, labels(columns));
        appendSeparator(builder, widths);
        for (String[] row : rows)
        {
            appendRow(builder, widths, row);
        }
        if (table.getRowCount() > limit)
        {
            builder.append("... ").append(table.getRowCount() - limit).append(" more rows\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return builder.toString();
    }

    private String[] labels(Column[] columns)
    {
        String[] labels = new String[columns.length];
        for (int ii = 0; ii < columns.length; ii++)
        {
            labels[ii] = columns[ii].getLabel();
        }
        return labels;
    }

    private void appendSeparator(StringBuilder builder, int[] widths)
    {
        for (int width : widths)
        {
            for (int ii = 0; ii < width; ii++)
                builder.append('-');
            builder.append("-+-"); //$NON-NLS-1$
        }
        builder.setLength(builder.length() - 1);
        builder.append('\n');
    }

    private void appendRow(StringBuilder builder, int[] widths, String[] values)
    {
        for (int ii = 0; ii < values.length; ii++)
        {
            String value = truncate(values[ii], widths[ii]);
            builder.append(value);
            for (int pad = value.length(); pad < widths[ii]; pad++)
                builder.append(' ');
            if (ii < values.length - 1)
                builder.append(" | "); //$NON-NLS-1$
        }
        builder.append('\n');
    }

    private String truncate(String value, int width)
    {
        if (value.length() <= width)
            return value;
        if (width < 4)
            return value.substring(0, width);
        return value.substring(0, width - 3) + "..."; //$NON-NLS-1$
    }

    private String safe(String value)
    {
        return value == null ? "" : value; //$NON-NLS-1$
    }
}
