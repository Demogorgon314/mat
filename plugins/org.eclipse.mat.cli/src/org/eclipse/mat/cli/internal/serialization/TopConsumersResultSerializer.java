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

import java.text.NumberFormat;
import java.util.List;

import org.eclipse.mat.cli.internal.TopConsumersResult;
import org.eclipse.mat.query.BytesFormat;

public class TopConsumersResultSerializer
{
    private static final String SEPARATOR = "--------------------------------------------------------------------------------"; //$NON-NLS-1$

    private final NumberFormat percentFormatter = NumberFormat.getIntegerInstance();
    private final NumberFormat numberFormatter = NumberFormat.getNumberInstance();
    private final BytesFormat bytesFormatter = BytesFormat.getInstance();

    public TopConsumersResultSerializer()
    {
        percentFormatter.setMinimumFractionDigits(2);
        percentFormatter.setMaximumFractionDigits(2);
    }

    public boolean writeJson(JsonWriter writer, TopConsumersResult result, SerializationOptions options)
    {
        writer.name("totalRetainedHeap").value(result.getTotalRetainedHeap()); //$NON-NLS-1$

        boolean truncated = false;
        truncated |= writeObjectSection(writer, "biggestObjects", "Biggest Objects", result.getBiggestObjects(), //$NON-NLS-1$ //$NON-NLS-2$
                        options);
        truncated |= writeDominatorSection(writer, "classes", "Biggest Top-Level Dominator Classes", //$NON-NLS-1$ //$NON-NLS-2$
                        result.getClasses(), options);
        truncated |= writeDominatorSection(writer, "classLoaders", "Biggest Top-Level Dominator Class Loaders", //$NON-NLS-1$ //$NON-NLS-2$
                        result.getClassLoaders(), options);
        truncated |= writePackageSection(writer, result.getPackages(), options);
        return truncated;
    }

    public String toText(TopConsumersResult result, SerializationOptions options)
    {
        StringBuilder builder = new StringBuilder(2048);
        appendObjectSection(builder, "Biggest Objects", result.getBiggestObjects(), options); //$NON-NLS-1$
        appendDominatorSection(builder, "Biggest Top-Level Dominator Classes", result.getClasses(), options); //$NON-NLS-1$
        appendDominatorSection(builder, "Biggest Top-Level Dominator Class Loaders", result.getClassLoaders(), options); //$NON-NLS-1$
        appendPackageSection(builder, result.getPackages(), options);
        return builder.toString();
    }

    private boolean writeObjectSection(JsonWriter writer, String fieldName, String name,
                    List<TopConsumersResult.ObjectRow> rows, SerializationOptions options)
    {
        writer.name(fieldName).beginObject();
        writer.name("name").value(name); //$NON-NLS-1$
        int limit = Math.min(rows.size(), options.getLimit());
        boolean truncated = rows.size() > limit;
        writer.name("rows").beginArray(); //$NON-NLS-1$
        for (int ii = 0; ii < limit; ii++)
        {
            TopConsumersResult.ObjectRow row = rows.get(ii);
            writer.beginObject();
            writer.name("objectId").value(row.getObjectId()); //$NON-NLS-1$
            writer.name("label").value(row.getLabel()); //$NON-NLS-1$
            writer.name("retainedBytes").value(row.getRetainedBytes()); //$NON-NLS-1$
            writer.name("retainedBytesText").value(formatBytes(row.getRetainedBytes())); //$NON-NLS-1$
            writer.name("retainedPercent").value(row.getRetainedPercent()); //$NON-NLS-1$
            writer.name("retainedPercentText").value(formatPercent(row.getRetainedPercent())); //$NON-NLS-1$
            writer.endObject();
        }
        writer.endArray();
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();
        return truncated;
    }

    private boolean writeDominatorSection(JsonWriter writer, String fieldName, String name,
                    List<TopConsumersResult.DominatorRow> rows, SerializationOptions options)
    {
        writer.name(fieldName).beginObject();
        writer.name("name").value(name); //$NON-NLS-1$
        int limit = Math.min(rows.size(), options.getLimit());
        boolean truncated = rows.size() > limit;
        writer.name("rows").beginArray(); //$NON-NLS-1$
        for (int ii = 0; ii < limit; ii++)
        {
            TopConsumersResult.DominatorRow row = rows.get(ii);
            writer.beginObject();
            writer.name("objectId").value(row.getObjectId()); //$NON-NLS-1$
            writer.name("label").value(row.getLabel()); //$NON-NLS-1$
            writer.name("count").value(row.getCount()); //$NON-NLS-1$
            writer.name("countText").value(numberFormatter.format(row.getCount())); //$NON-NLS-1$
            writer.name("retainedBytes").value(row.getRetainedBytes()); //$NON-NLS-1$
            writer.name("retainedBytesText").value(formatBytes(row.getRetainedBytes())); //$NON-NLS-1$
            writer.name("retainedPercent").value(row.getRetainedPercent()); //$NON-NLS-1$
            writer.name("retainedPercentText").value(formatPercent(row.getRetainedPercent())); //$NON-NLS-1$
            writer.endObject();
        }
        writer.endArray();
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();
        return truncated;
    }

    private boolean writePackageSection(JsonWriter writer, TopConsumersResult.PackageNode root, SerializationOptions options)
    {
        writer.name("packages").beginObject(); //$NON-NLS-1$
        writer.name("name").value("Biggest Top-Level Dominator Packages"); //$NON-NLS-1$ //$NON-NLS-2$
        PackageTruncationState state = new PackageTruncationState(options.getTreeNodeLimit());
        writer.name("root"); //$NON-NLS-1$
        if (root == null)
            writer.nullValue();
        else
            writePackageNode(writer, root, options, state, true);
        writer.name("truncated").value(state.truncated); //$NON-NLS-1$
        writer.endObject();
        return state.truncated;
    }

    private void writePackageNode(JsonWriter writer, TopConsumersResult.PackageNode node, SerializationOptions options,
                    PackageTruncationState state, boolean includeChildren)
    {
        if (state.remainingNodes <= 0)
        {
            state.truncated = true;
            return;
        }

        state.remainingNodes--;
        writer.beginObject();
        writer.name("name").value(node.getName()); //$NON-NLS-1$
        writer.name("retainedBytes").value(node.getRetainedBytes()); //$NON-NLS-1$
        writer.name("retainedBytesText").value(formatBytes(node.getRetainedBytes())); //$NON-NLS-1$
        writer.name("retainedPercent").value(node.getRetainedPercent()); //$NON-NLS-1$
        writer.name("retainedPercentText").value(formatPercent(node.getRetainedPercent())); //$NON-NLS-1$
        writer.name("topDominators").value(node.getTopDominators()); //$NON-NLS-1$
        writer.name("topDominatorsText").value(numberFormatter.format(node.getTopDominators())); //$NON-NLS-1$
        writer.name("children").beginArray(); //$NON-NLS-1$
        if (includeChildren)
        {
            List<TopConsumersResult.PackageNode> children = node.getChildren();
            int limit = Math.min(children.size(), options.getLimit());
            if (children.size() > limit)
                state.truncated = true;
            for (int ii = 0; ii < limit; ii++)
            {
                if (state.remainingNodes <= 0)
                {
                    state.truncated = true;
                    break;
                }
                writePackageNode(writer, children.get(ii), options, state, true);
            }
        }
        writer.endArray();
        writer.endObject();
    }

    private void appendObjectSection(StringBuilder builder, String name, List<TopConsumersResult.ObjectRow> rows,
                    SerializationOptions options)
    {
        appendHeader(builder, name);
        int limit = Math.min(rows.size(), options.getLimit());
        for (int ii = 0; ii < limit; ii++)
        {
            TopConsumersResult.ObjectRow row = rows.get(ii);
            builder.append(formatPercent(row.getRetainedPercent())).append("  "); //$NON-NLS-1$
            builder.append(formatBytes(row.getRetainedBytes())).append("  "); //$NON-NLS-1$
            builder.append(row.getLabel()).append('\n');
        }
        builder.append('\n');
    }

    private void appendDominatorSection(StringBuilder builder, String name, List<TopConsumersResult.DominatorRow> rows,
                    SerializationOptions options)
    {
        appendHeader(builder, name);
        int limit = Math.min(rows.size(), options.getLimit());
        for (int ii = 0; ii < limit; ii++)
        {
            TopConsumersResult.DominatorRow row = rows.get(ii);
            builder.append(formatPercent(row.getRetainedPercent())).append("  "); //$NON-NLS-1$
            builder.append(formatBytes(row.getRetainedBytes())).append("  "); //$NON-NLS-1$
            builder.append(numberFormatter.format(row.getCount())).append("  "); //$NON-NLS-1$
            builder.append(row.getLabel()).append('\n');
        }
        builder.append('\n');
    }

    private void appendPackageSection(StringBuilder builder, TopConsumersResult.PackageNode root, SerializationOptions options)
    {
        appendHeader(builder, "Biggest Top-Level Dominator Packages"); //$NON-NLS-1$
        builder.append("package,  retained%,  retained bytes, #top-dominators\n"); //$NON-NLS-1$
        builder.append(SEPARATOR).append('\n');
        if (root == null)
        {
            builder.append('\n');
            return;
        }

        PackageTruncationState state = new PackageTruncationState(options.getTreeNodeLimit());
        appendPackageNode(builder, root, new StringBuilder(), options, state);
        builder.append('\n');
    }

    private void appendPackageNode(StringBuilder builder, TopConsumersResult.PackageNode node, StringBuilder prefix,
                    SerializationOptions options, PackageTruncationState state)
    {
        if (state.remainingNodes <= 0)
        {
            state.truncated = true;
            return;
        }

        state.remainingNodes--;
        builder.append(prefix);
        builder.append(node.getName());
        builder.append("  (").append(formatPercent(node.getRetainedPercent())).append(")  "); //$NON-NLS-1$ //$NON-NLS-2$
        builder.append(formatBytes(node.getRetainedBytes())).append("  "); //$NON-NLS-1$
        builder.append(numberFormatter.format(node.getTopDominators())).append('\n');

        List<TopConsumersResult.PackageNode> children = node.getChildren();
        int limit = Math.min(children.size(), options.getLimit());
        if (children.size() > limit)
            state.truncated = true;

        for (int ii = 0; ii < limit; ii++)
        {
            int originalLength = prefix.length();
            int branchIndex = prefix.indexOf("'-"); //$NON-NLS-1$
            if (branchIndex != -1)
                prefix.replace(branchIndex, branchIndex + 2, "  "); //$NON-NLS-1$
            else
            {
                branchIndex = prefix.indexOf("|-"); //$NON-NLS-1$
                if (branchIndex != -1)
                    prefix.replace(branchIndex + 1, branchIndex + 2, " "); //$NON-NLS-1$
            }

            if (ii == limit - 1)
                prefix.append('\'');
            else
                prefix.append('|');
            prefix.append("- "); //$NON-NLS-1$

            appendPackageNode(builder, children.get(ii), prefix, options, state);
            prefix.setLength(originalLength);
        }
    }

    private void appendHeader(StringBuilder builder, String name)
    {
        builder.append('\n');
        builder.append(name).append(":\n"); //$NON-NLS-1$
        builder.append(SEPARATOR).append('\n');
    }

    private String formatBytes(long bytes)
    {
        return bytesFormatter.format(bytes);
    }

    private String formatPercent(double fraction)
    {
        return percentFormatter.format(fraction * 100d) + "%"; //$NON-NLS-1$
    }

    private static final class PackageTruncationState
    {
        private int remainingNodes;
        private boolean truncated;

        private PackageTruncationState(int remainingNodes)
        {
            this.remainingNodes = remainingNodes;
        }
    }
}
