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

import java.text.Format;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.IContextObject;
import org.eclipse.mat.query.IDecorator;
import org.eclipse.mat.query.IStructuredResult;

abstract class StructuredResultSerializer
{
    protected static final class ColumnSchema
    {
        private final Column column;
        private final String id;
        private final String jsonType;
        private final String sourceType;

        private ColumnSchema(Column column, String id, String jsonType, String sourceType)
        {
            this.column = column;
            this.id = id;
            this.jsonType = jsonType;
            this.sourceType = sourceType;
        }
    }

    protected void writeColumns(JsonWriter writer, Column[] columns)
    {
        writer.name("columns").beginArray(); //$NON-NLS-1$
        for (Column column : columns)
        {
            writer.beginObject();
            writer.name("label").value(column.getLabel()); //$NON-NLS-1$
            writer.name("type").value(column.getType().getName()); //$NON-NLS-1$
            writer.endObject();
        }
        writer.endArray();
    }

    protected ColumnSchema[] buildColumnSchemas(Column[] columns)
    {
        ColumnSchema[] schemas = new ColumnSchema[columns.length];
        Map<String, Integer> ids = new LinkedHashMap<String, Integer>();
        for (int ii = 0; ii < columns.length; ii++)
        {
            Column column = columns[ii];
            String baseId = normalizeColumnId(column == null ? null : column.getLabel(), ii);
            Integer previous = ids.get(baseId);
            ids.put(baseId, Integer.valueOf(previous == null ? 1 : previous.intValue() + 1));
            String id = previous == null ? baseId : baseId + "_" + (previous.intValue() + 1); //$NON-NLS-1$
            Class<?> type = column == null ? null : column.getType();
            schemas[ii] = new ColumnSchema(column, id, jsonType(type), type == null ? null : type.getName());
        }
        return schemas;
    }

    protected void writeAgentSchema(JsonWriter writer, ColumnSchema[] columns)
    {
        writer.name("schema").beginObject(); //$NON-NLS-1$
        writer.name("columns").beginArray(); //$NON-NLS-1$
        for (ColumnSchema column : columns)
        {
            writer.beginObject();
            writer.name("id").value(column.id); //$NON-NLS-1$
            writer.name("label").value(column.column == null ? null : column.column.getLabel()); //$NON-NLS-1$
            writer.name("jsonType").value(column.jsonType); //$NON-NLS-1$
            writer.name("sourceType").value(column.sourceType); //$NON-NLS-1$
            writer.endObject();
        }
        writer.endArray();
        writer.endObject();
    }

    protected void writeAgentRow(JsonWriter writer, IStructuredResult result, ColumnSchema[] columns, Object row)
    {
        for (int ii = 0; ii < columns.length; ii++)
        {
            writer.name(columns[ii].id);
            writer.rawValue(rawValue(safeColumnValue(result, row, ii)));
        }
        writeAgentContext(writer, result, row);
    }

    protected Object rawValue(Object value)
    {
        if (value instanceof Bytes)
            return Long.valueOf(((Bytes) value).getValue());
        if (value instanceof Number || value instanceof Boolean || value instanceof String)
            return value;
        if (value == null)
            return null;
        return String.valueOf(value);
    }

    protected String displayValue(Column column, Object row, Object value)
    {
        if (value == null)
            return null;

        String rendered;
        Format formatter = column.getFormatter();
        if (formatter != null)
        {
            rendered = formatter.format(value);
        }
        else
        {
            rendered = String.valueOf(rawValue(value));
        }

        IDecorator decorator = column.getDecorator();
        if (decorator == null)
            return rendered;

        String prefix = decorator.prefix(row);
        String suffix = decorator.suffix(row);
        StringBuilder builder = new StringBuilder();
        if (prefix != null)
            builder.append(prefix);
        builder.append(rendered);
        if (suffix != null)
            builder.append(suffix);
        return builder.toString();
    }

    protected void writeContext(JsonWriter writer, IStructuredResult result, Object row)
    {
        IContextObject context = safeContext(result, row);
        if (context == null || context.getObjectId() < 0)
        {
            writer.name("context").nullValue(); //$NON-NLS-1$
            return;
        }

        writer.name("context").beginObject(); //$NON-NLS-1$
        writer.name("objectId").value(context.getObjectId()); //$NON-NLS-1$
        writer.endObject();
    }

    protected void writeAgentContext(JsonWriter writer, IStructuredResult result, Object row)
    {
        IContextObject context = safeContext(result, row);
        if (context == null || context.getObjectId() < 0)
        {
            writer.name("_context").nullValue(); //$NON-NLS-1$
            return;
        }

        writer.name("_context").beginObject(); //$NON-NLS-1$
        writer.name("objectId").value(context.getObjectId()); //$NON-NLS-1$
        writer.endObject();
    }

    protected void writeRowValues(JsonWriter writer, IStructuredResult result, Column[] columns, Object row)
    {
        writer.name("values").beginArray(); //$NON-NLS-1$
        for (int ii = 0; ii < columns.length; ii++)
        {
            writer.rawValue(rawValue(safeColumnValue(result, row, ii)));
        }
        writer.endArray();

        writer.name("displayValues").beginArray(); //$NON-NLS-1$
        for (int ii = 0; ii < columns.length; ii++)
        {
            writer.value(displayValue(columns[ii], row, safeColumnValue(result, row, ii)));
        }
        writer.endArray();
    }

    protected Object safeColumnValue(IStructuredResult result, Object row, int columnIndex)
    {
        try
        {
            return result.getColumnValue(row, columnIndex);
        }
        catch (RuntimeException e)
        {
            String message = e.getMessage();
            if (message == null || message.length() == 0)
                message = e.getClass().getSimpleName();
            return "<error: " + message + ">"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    protected IContextObject safeContext(IStructuredResult result, Object row)
    {
        try
        {
            return result.getContext(row);
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    private String normalizeColumnId(String label, int index)
    {
        if (label == null)
            return "col_" + (index + 1); //$NON-NLS-1$

        String value = label.trim().toLowerCase(Locale.ENGLISH);
        StringBuilder builder = new StringBuilder(value.length());
        boolean underscore = false;
        for (int ii = 0; ii < value.length(); ii++)
        {
            char ch = value.charAt(ii);
            if (Character.isLetterOrDigit(ch))
            {
                builder.append(ch);
                underscore = false;
            }
            else if (!underscore && builder.length() > 0)
            {
                builder.append('_');
                underscore = true;
            }
        }

        while (builder.length() > 0 && builder.charAt(builder.length() - 1) == '_')
        {
            builder.setLength(builder.length() - 1);
        }

        if (builder.length() == 0)
            return "col_" + (index + 1); //$NON-NLS-1$
        return builder.toString();
    }

    private String jsonType(Class<?> type)
    {
        if (type == null)
            return "string"; //$NON-NLS-1$
        if (Bytes.class.isAssignableFrom(type))
            return "integer"; //$NON-NLS-1$
        if (type == byte.class || type == short.class || type == int.class || type == long.class //$NON-NLS-1$
                        || type == Byte.class || type == Short.class || type == Integer.class || type == Long.class)
            return "integer"; //$NON-NLS-1$
        if (type == float.class || type == double.class || type == Float.class || type == Double.class //$NON-NLS-1$
                        || (Number.class.isAssignableFrom(type) && !Long.class.isAssignableFrom(type)
                                        && !Integer.class.isAssignableFrom(type) && !Short.class.isAssignableFrom(type)
                                        && !Byte.class.isAssignableFrom(type)))
            return "number"; //$NON-NLS-1$
        if (type == boolean.class || type == Boolean.class)
            return "boolean"; //$NON-NLS-1$
        return "string"; //$NON-NLS-1$
    }
}
