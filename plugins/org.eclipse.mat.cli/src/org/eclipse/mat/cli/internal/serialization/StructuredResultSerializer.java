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

import org.eclipse.mat.cli.internal.DisplayValue;
import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.IContextObject;
import org.eclipse.mat.query.IContextObjectSet;
import org.eclipse.mat.query.IDecorator;
import org.eclipse.mat.query.IStructuredResult;

abstract class StructuredResultSerializer
{
    protected static final class CellError
    {
        private final String className;
        private final String message;

        private CellError(String className, String message)
        {
            this.className = className;
            this.message = message;
        }
    }

    protected static final class CellValue
    {
        private final Object value;
        private final CellError error;

        private CellValue(Object value, CellError error)
        {
            this.value = value;
            this.error = error;
        }
    }

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
            schemas[ii] = new ColumnSchema(column, id, jsonType(column), type == null ? null : type.getName());
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

    protected void writeAgentRow(JsonWriter writer, IStructuredResult result, ColumnSchema[] columns, Object row,
                    SerializationOptions options)
    {
        CellValue[] cells = readCells(result, columns, row);
        for (int ii = 0; ii < columns.length; ii++)
        {
            writer.name(columns[ii].id);
            writer.rawValue(agentRawValue(columns[ii].column, cells[ii].value));
        }
        writeAgentAddress(writer, result, columns, row, options);
        writeAgentCellMetadata(writer, columns, cells);
        writeAgentCellErrors(writer, columns, cells);
    }

    protected Object rawValue(Object value)
    {
        if (value instanceof DisplayValue)
            return rawValue(((DisplayValue) value).getText());
        if (value instanceof Bytes)
            return Long.valueOf(((Bytes) value).getValue());
        if (value instanceof Number || value instanceof Boolean || value instanceof String)
            return value;
        if (value == null)
            return null;
        return String.valueOf(value);
    }

    protected Object rawValue(Column column, Object value)
    {
        if (isAddressValue(column, value))
            return formatObjectAddress(((Number) value).longValue());
        return rawValue(value);
    }

    protected Object agentRawValue(Column column, Object value)
    {
        if (value instanceof Bytes)
        {
            long bytes = ((Bytes) value).getValue();
            if (bytes < 0)
                return Long.valueOf(-bytes);
        }
        return rawValue(column, value);
    }

    protected String displayValue(Column column, Object row, Object value)
    {
        return displayValue(column, row, value, true);
    }

    protected String pathDisplayValue(Column column, Object row, Object value)
    {
        return displayValue(column, row, value, false);
    }

    private String displayValue(Column column, Object row, Object value, boolean spacedDecorator)
    {
        if (value == null)
            return null;

        String rendered;
        if (isAddressValue(column, value))
        {
            rendered = String.valueOf(rawValue(column, value));
        }
        else
        {
            Format formatter = column.getFormatter();
            if (formatter != null)
            {
                rendered = formatter.format(value);
            }
            else
            {
                rendered = String.valueOf(rawValue(column, value));
            }
        }

        IDecorator decorator = column.getDecorator();
        if (decorator == null)
            return rendered;

        String prefix = decorator.prefix(row);
        String suffix = decorator.suffix(row);
        if (prefix != null)
            if (suffix != null)
                return spacedDecorator ? prefix + " " + rendered + " " + suffix : prefix + rendered + suffix; //$NON-NLS-1$ //$NON-NLS-2$
            else
                return spacedDecorator ? prefix + " " + rendered : prefix + rendered; //$NON-NLS-1$
        else if (suffix != null)
            return spacedDecorator ? rendered + " " + suffix : rendered + suffix; //$NON-NLS-1$
        return rendered;
    }

    protected void writeContext(JsonWriter writer, IStructuredResult result, Object row, SerializationOptions options)
    {
        Integer objectId = contextObjectId(safeContext(result, row));
        if (objectId == null)
        {
            writer.name("context").nullValue(); //$NON-NLS-1$
            return;
        }

        writer.name("context").beginObject(); //$NON-NLS-1$
        writer.name("objectId").value(objectId.intValue()); //$NON-NLS-1$
        String objectAddress = resolveObjectAddress(options, objectId);
        if (objectAddress != null)
            writer.name("objectAddress").value(objectAddress); //$NON-NLS-1$
        writer.endObject();
    }

    protected void writeAgentContext(JsonWriter writer, IStructuredResult result, Object row,
                    SerializationOptions options)
    {
        Integer objectId = contextObjectId(safeContext(result, row));
        if (objectId == null)
        {
            writer.name("_context").nullValue(); //$NON-NLS-1$
            return;
        }

        writer.name("_context").beginObject(); //$NON-NLS-1$
        writer.name("objectId").value(objectId.intValue()); //$NON-NLS-1$
        String objectAddress = resolveObjectAddress(options, objectId);
        if (objectAddress != null)
            writer.name("objectAddress").value(objectAddress); //$NON-NLS-1$
        writer.endObject();
    }

    protected void writeAgentAddress(JsonWriter writer, IStructuredResult result, ColumnSchema[] columns, Object row,
                    SerializationOptions options)
    {
        if (hasAddressColumn(columns))
            return;

        Integer objectId = contextObjectId(safeContext(result, row));
        String objectAddress = resolveObjectAddress(options, objectId);
        if (objectAddress != null)
            writer.name("_address").value(objectAddress); //$NON-NLS-1$
    }

    protected void writeRowValues(JsonWriter writer, IStructuredResult result, Column[] columns, Object row)
    {
        CellValue[] cells = readCells(result, columns, row);
        writer.name("values").beginArray(); //$NON-NLS-1$
        for (int ii = 0; ii < columns.length; ii++)
        {
            writer.rawValue(rawValue(columns[ii], cells[ii].value));
        }
        writer.endArray();

        writer.name("displayValues").beginArray(); //$NON-NLS-1$
        for (int ii = 0; ii < columns.length; ii++)
        {
            writer.value(cells[ii].error == null ? displayValue(columns[ii], row, cells[ii].value) : null);
        }
        writer.endArray();
        writeCellErrors(writer, columns, cells);
    }

    protected Object safeColumnValue(IStructuredResult result, Object row, int columnIndex)
    {
        CellValue cell = readCell(result, row, columnIndex);
        return cell.error == null ? cell.value : formatCellError(cell.error);
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

    protected CellValue[] readCells(IStructuredResult result, ColumnSchema[] columns, Object row)
    {
        CellValue[] cells = new CellValue[columns.length];
        for (int ii = 0; ii < columns.length; ii++)
        {
            cells[ii] = readCell(result, row, ii);
        }
        return cells;
    }

    protected Object cellValue(CellValue cell)
    {
        return cell == null ? null : cell.value;
    }

    protected CellError cellError(CellValue cell)
    {
        return cell == null ? null : cell.error;
    }

    protected String columnId(ColumnSchema schema)
    {
        return schema == null ? null : schema.id;
    }

    protected CellValue[] readCells(IStructuredResult result, Column[] columns, Object row)
    {
        CellValue[] cells = new CellValue[columns.length];
        for (int ii = 0; ii < columns.length; ii++)
        {
            cells[ii] = readCell(result, row, ii);
        }
        return cells;
    }

    protected CellValue readCell(IStructuredResult result, Object row, int columnIndex)
    {
        try
        {
            return new CellValue(result.getColumnValue(row, columnIndex), null);
        }
        catch (RuntimeException e)
        {
            String message = e.getMessage();
            if (message == null || message.length() == 0)
                message = e.getClass().getSimpleName();
            return new CellValue(null, new CellError(e.getClass().getName(), message));
        }
    }

    protected void writeCellErrors(JsonWriter writer, Column[] columns, CellValue[] cells)
    {
        boolean hasErrors = false;
        for (CellValue cell : cells)
        {
            if (cell.error != null)
            {
                hasErrors = true;
                break;
            }
        }
        if (!hasErrors)
            return;

        writer.name("cellErrors").beginArray(); //$NON-NLS-1$
        for (int ii = 0; ii < cells.length; ii++)
        {
            if (cells[ii].error == null)
                continue;
            writer.beginObject();
            writer.name("columnIndex").value(ii); //$NON-NLS-1$
            writer.name("columnLabel").value(columns[ii].getLabel()); //$NON-NLS-1$
            writeCellError(writer, cells[ii].error);
            writer.endObject();
        }
        writer.endArray();
    }

    protected void writeAgentCellErrors(JsonWriter writer, ColumnSchema[] columns, CellValue[] cells)
    {
        boolean hasErrors = false;
        for (CellValue cell : cells)
        {
            if (cell.error != null)
            {
                hasErrors = true;
                break;
            }
        }
        if (!hasErrors)
            return;

        writer.name("_errors").beginObject(); //$NON-NLS-1$
        for (int ii = 0; ii < cells.length; ii++)
        {
            if (cells[ii].error == null)
                continue;
            writer.name(columns[ii].id).beginObject();
            writeCellError(writer, cells[ii].error);
            writer.endObject();
        }
        writer.endObject();
    }

    protected void writeAgentCellMetadata(JsonWriter writer, ColumnSchema[] columns, CellValue[] cells)
    {
        boolean hasMetadata = false;
        for (CellValue cell : cells)
        {
            if (isApproximateBytes(cell.value) || displayMetadata(cell.value) != null)
            {
                hasMetadata = true;
                break;
            }
        }
        if (!hasMetadata)
            return;

        writer.name("_meta").beginObject(); //$NON-NLS-1$
        for (int ii = 0; ii < cells.length; ii++)
        {
            DisplayValue.Metadata metadata = displayMetadata(cells[ii].value);
            if (!isApproximateBytes(cells[ii].value) && metadata == null)
                continue;
            writer.name(columns[ii].id).beginObject();
            if (isApproximateBytes(cells[ii].value))
                writer.name("kind").value("approximate_lower_bound"); //$NON-NLS-1$ //$NON-NLS-2$
            if (metadata != null)
            {
                if (metadata.getKind() != null)
                    writer.name("kind").value(metadata.getKind()); //$NON-NLS-1$
                if (metadata.getLength() != null)
                    writer.name("length").value(metadata.getLength().intValue()); //$NON-NLS-1$
                if (metadata.isTruncated() != null)
                    writer.name("truncated").value(metadata.isTruncated().booleanValue()); //$NON-NLS-1$
                if (metadata.getEncoding() != null)
                    writer.name("encoding").value(metadata.getEncoding()); //$NON-NLS-1$
            }
            writer.endObject();
        }
        writer.endObject();
    }

    protected DisplayValue.Metadata displayMetadata(Object value)
    {
        if (value instanceof DisplayValue)
            return ((DisplayValue) value).getMetadata();
        return null;
    }

    protected void writeCellError(JsonWriter writer, CellError error)
    {
        writer.name("class").value(error.className); //$NON-NLS-1$
        writer.name("message").value(error.message); //$NON-NLS-1$
    }

    protected String formatCellError(CellError error)
    {
        return "<error: " + error.message + ">"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    protected Integer contextObjectId(IContextObject context)
    {
        if (context == null)
            return null;
        if (context.getObjectId() >= 0)
            return Integer.valueOf(context.getObjectId());
        if (context instanceof IContextObjectSet)
        {
            int[] objectIds = ((IContextObjectSet) context).getObjectIds();
            if (objectIds != null && objectIds.length == 1 && objectIds[0] >= 0)
                return Integer.valueOf(objectIds[0]);
        }
        return null;
    }

    protected String resolveObjectAddress(SerializationOptions options, Integer objectId)
    {
        return options == null || objectId == null ? null : options.resolveObjectAddress(objectId.intValue());
    }

    protected String formatObjectAddress(long objectAddress)
    {
        return "0x" + Long.toHexString(objectAddress); //$NON-NLS-1$
    }

    protected String normalizeColumnId(String label, int index)
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

    private boolean isAddressValue(Column column, Object value)
    {
        return value instanceof Number && isAddressColumn(column);
    }

    protected boolean hasAddressColumn(ColumnSchema[] columns)
    {
        for (ColumnSchema column : columns)
        {
            if (isAddressColumn(column.column))
                return true;
        }
        return false;
    }

    protected boolean isApproximateBytes(Object value)
    {
        return value instanceof Bytes && ((Bytes) value).getValue() < 0;
    }

    protected boolean isAddressColumn(Column column)
    {
        if (column == null || column.getType() == null || !isIntegerType(column.getType()))
            return false;

        String label = column.getLabel();
        if (label == null)
            return false;

        StringBuilder normalized = new StringBuilder(label.length());
        for (int ii = 0; ii < label.length(); ii++)
        {
            char ch = Character.toLowerCase(label.charAt(ii));
            if (Character.isLetterOrDigit(ch))
                normalized.append(ch);
        }
        return normalized.toString().endsWith("address"); //$NON-NLS-1$
    }

    private String jsonType(Column column)
    {
        Class<?> type = column == null ? null : column.getType();
        if (isAddressColumn(column))
            return "string"; //$NON-NLS-1$
        if (type == null)
            return "string"; //$NON-NLS-1$
        if (Bytes.class.isAssignableFrom(type))
            return "integer"; //$NON-NLS-1$
        if (isIntegerType(type))
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

    private boolean isIntegerType(Class<?> type)
    {
        return type == byte.class || type == short.class || type == int.class || type == long.class //$NON-NLS-1$
                        || type == Byte.class || type == Short.class || type == Integer.class || type == Long.class;
    }
}
