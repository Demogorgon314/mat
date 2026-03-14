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

import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.IContextObject;
import org.eclipse.mat.query.IDecorator;
import org.eclipse.mat.query.IStructuredResult;

abstract class StructuredResultSerializer
{
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
        IContextObject context = result.getContext(row);
        if (context == null || context.getObjectId() < 0)
        {
            writer.name("context").nullValue(); //$NON-NLS-1$
            return;
        }

        writer.name("context").beginObject(); //$NON-NLS-1$
        writer.name("objectId").value(context.getObjectId()); //$NON-NLS-1$
        writer.endObject();
    }

    protected void writeRowValues(JsonWriter writer, IStructuredResult result, Column[] columns, Object row)
    {
        writer.name("values").beginArray(); //$NON-NLS-1$
        for (int ii = 0; ii < columns.length; ii++)
        {
            writer.rawValue(rawValue(result.getColumnValue(row, ii)));
        }
        writer.endArray();

        writer.name("displayValues").beginArray(); //$NON-NLS-1$
        for (int ii = 0; ii < columns.length; ii++)
        {
            writer.value(displayValue(columns[ii], row, result.getColumnValue(row, ii)));
        }
        writer.endArray();
    }
}
