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

import java.awt.Color;
import java.util.List;
import java.util.Locale;

import org.eclipse.mat.query.IContextObject;
import org.eclipse.mat.query.IContextObjectSet;
import org.eclipse.mat.query.IResultPie;
import org.eclipse.mat.query.IResultPie.ColoredSlice;
import org.eclipse.mat.query.IResultPie.Slice;

public class PieResultSerializer
{
    public boolean writeJson(JsonWriter writer, IResultPie pie, SerializationOptions options)
    {
        return writeAgentJson(writer, pie, options);
    }

    public boolean writeAgentJson(JsonWriter writer, IResultPie pie, SerializationOptions options)
    {
        List<? extends Slice> slices = pie.getSlices();
        int limit = Math.min(slices.size(), options.getEffectiveLimit());
        boolean truncated = slices.size() > limit;

        writer.name("items").beginArray(); //$NON-NLS-1$
        for (int ii = 0; ii < limit; ii++)
        {
            writeSlice(writer, slices.get(ii), options);
        }
        writer.endArray();
        return truncated;
    }

    public String toText(IResultPie pie, SerializationOptions options)
    {
        StringBuilder builder = new StringBuilder();
        List<? extends Slice> slices = pie.getSlices();
        int limit = Math.min(slices.size(), options.getEffectiveLimit());
        for (int ii = 0; ii < limit; ii++)
        {
            Slice slice = slices.get(ii);
            builder.append("- "); //$NON-NLS-1$
            builder.append(slice.getLabel());
            builder.append(": "); //$NON-NLS-1$
            builder.append(formatValue(slice.getValue()));
            builder.append('\n');
        }
        if (slices.size() > limit)
        {
            builder.append("... ").append(slices.size() - limit).append(" more slices\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return builder.toString();
    }

    private void writeSlice(JsonWriter writer, Slice slice, SerializationOptions options)
    {
        writer.beginObject();
        writer.name("label").value(slice.getLabel()); //$NON-NLS-1$
        writer.name("value").value(slice.getValue()); //$NON-NLS-1$
        writeStringField(writer, "description", slice.getDescription()); //$NON-NLS-1$
        writeAgentAddress(writer, slice.getContext(), options);
        writeStringField(writer, "color", color(slice)); //$NON-NLS-1$
        writer.endObject();
    }

    private void writeAgentAddress(JsonWriter writer, IContextObject context, SerializationOptions options)
    {
        Integer objectId = contextObjectId(context);
        if (objectId == null)
            return;
        String objectAddress = options == null ? null : options.resolveObjectAddress(objectId.intValue());
        if (objectAddress != null)
            writer.name("_address").value(objectAddress); //$NON-NLS-1$
    }

    private void writeStringField(JsonWriter writer, String name, String value)
    {
        if (value == null || value.length() == 0)
            return;
        writer.name(name).value(value);
    }

    private Integer contextObjectId(IContextObject context)
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

    private String color(Slice slice)
    {
        if (!(slice instanceof ColoredSlice))
            return null;

        Color color = ((ColoredSlice) slice).getColor();
        if (color == null)
            return null;

        return String.format(Locale.ENGLISH, "#%02x%02x%02x", Integer.valueOf(color.getRed()), //$NON-NLS-1$
                        Integer.valueOf(color.getGreen()), Integer.valueOf(color.getBlue()));
    }

    private String formatValue(double value)
    {
        if (Math.rint(value) == value)
            return Long.toString((long) value);
        return Double.toString(value);
    }
}
