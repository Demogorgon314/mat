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
import org.eclipse.mat.query.IResultPie;
import org.eclipse.mat.query.IResultPie.ColoredSlice;
import org.eclipse.mat.query.IResultPie.Slice;

public class PieResultSerializer
{
    public boolean writeJson(JsonWriter writer, IResultPie pie, SerializationOptions options)
    {
        List<? extends Slice> slices = pie.getSlices();
        int limit = Math.min(slices.size(), options.getLimit());
        boolean truncated = slices.size() > limit;

        writer.name("slices").beginArray(); //$NON-NLS-1$
        for (int ii = 0; ii < limit; ii++)
        {
            writeSlice(writer, slices.get(ii), false);
        }
        writer.endArray();
        return truncated;
    }

    public boolean writeAgentJson(JsonWriter writer, IResultPie pie, SerializationOptions options)
    {
        List<? extends Slice> slices = pie.getSlices();
        int limit = Math.min(slices.size(), options.getLimit());
        boolean truncated = slices.size() > limit;

        writer.name("items").beginArray(); //$NON-NLS-1$
        for (int ii = 0; ii < limit; ii++)
        {
            writeSlice(writer, slices.get(ii), true);
        }
        writer.endArray();
        return truncated;
    }

    public String toText(IResultPie pie, SerializationOptions options)
    {
        StringBuilder builder = new StringBuilder();
        List<? extends Slice> slices = pie.getSlices();
        int limit = Math.min(slices.size(), options.getLimit());
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

    private void writeSlice(JsonWriter writer, Slice slice, boolean agentProfile)
    {
        writer.beginObject();
        writer.name("label").value(slice.getLabel()); //$NON-NLS-1$
        writer.name("value").value(slice.getValue()); //$NON-NLS-1$
        writer.name("description").value(slice.getDescription()); //$NON-NLS-1$
        if (agentProfile)
            writeAgentContext(writer, slice.getContext());
        else
            writeContext(writer, slice.getContext());
        writer.name("color").value(color(slice)); //$NON-NLS-1$
        writer.endObject();
    }

    private void writeContext(JsonWriter writer, IContextObject context)
    {
        if (context == null || context.getObjectId() < 0)
        {
            writer.name("context").nullValue(); //$NON-NLS-1$
            return;
        }

        writer.name("context").beginObject(); //$NON-NLS-1$
        writer.name("objectId").value(context.getObjectId()); //$NON-NLS-1$
        writer.endObject();
    }

    private void writeAgentContext(JsonWriter writer, IContextObject context)
    {
        if (context == null || context.getObjectId() < 0)
        {
            writer.name("_context").nullValue(); //$NON-NLS-1$
            return;
        }

        writer.name("_context").beginObject(); //$NON-NLS-1$
        writer.name("objectId").value(context.getObjectId()); //$NON-NLS-1$
        writer.endObject();
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
