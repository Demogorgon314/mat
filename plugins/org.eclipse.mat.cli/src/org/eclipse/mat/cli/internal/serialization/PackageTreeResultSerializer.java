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

import org.eclipse.mat.cli.internal.PackageTreeResult;

public final class PackageTreeResultSerializer
{
    private static final String SEPARATOR = "--------------------------------------------------------------------------------"; //$NON-NLS-1$
    private static final String MARKDOWN_COLUMNS = "Columns: package | retained% | retained bytes | #top-dominators"; //$NON-NLS-1$

    private final NumberFormat percentFormatter = NumberFormat.getIntegerInstance();
    private final NumberFormat numberFormatter = NumberFormat.getNumberInstance();

    public PackageTreeResultSerializer()
    {
        percentFormatter.setMinimumFractionDigits(2);
        percentFormatter.setMaximumFractionDigits(2);
    }

    public boolean writeJson(JsonWriter writer, PackageTreeResult result, SerializationOptions options)
    {
        writer.name("root"); //$NON-NLS-1$
        PackageTreeResult.Node root = result.getRoot();
        if (root == null)
        {
            writer.nullValue();
            return false;
        }

        TruncationState state = new TruncationState(options.getEffectiveTreeNodeLimit());
        writeNode(writer, root, options, state, 0);
        return state.truncated;
    }

    public String toText(PackageTreeResult result, SerializationOptions options)
    {
        StringBuilder builder = new StringBuilder(1024);
        builder.append(SEPARATOR).append('\n');
        builder.append("package,  retained%,  retained bytes, #top-dominators\n"); //$NON-NLS-1$
        builder.append(SEPARATOR).append('\n');

        PackageTreeResult.Node root = result.getRoot();
        if (root == null)
            return builder.toString();

        TruncationState state = new TruncationState(options.getEffectiveTreeNodeLimit());
        appendNode(builder, root, new StringBuilder(), options, state, 0);
        return builder.toString();
    }

    public String toMarkdown(PackageTreeResult result, SerializationOptions options)
    {
        PackageTreeResult.Node root = result.getRoot();
        if (root == null)
            return MARKDOWN_COLUMNS + "\n\n- No packages found."; //$NON-NLS-1$

        StringBuilder builder = new StringBuilder(1024);
        builder.append(MARKDOWN_COLUMNS).append('\n').append('\n');
        TruncationState state = new TruncationState(options.getEffectiveTreeNodeLimit());
        appendMarkdownNode(builder, root, options, state, 0);
        return builder.toString();
    }

    private void writeNode(JsonWriter writer, PackageTreeResult.Node node, SerializationOptions options,
                    TruncationState state, int depth)
    {
        if (state.remainingNodes <= 0)
        {
            state.truncated = true;
            return;
        }

        state.remainingNodes--;
        List<PackageTreeResult.Node> children = node.getChildren();
        int limit = Math.min(children.size(), options.getEffectiveLimit());
        boolean childrenTruncated = children.size() > limit;

        writer.beginObject();
        writer.name("name").value(node.getName()); //$NON-NLS-1$
        writer.name("retainedPercent").value(node.getRetainedPercent()); //$NON-NLS-1$
        writer.name("retainedBytes").value(node.getRetainedBytes()); //$NON-NLS-1$
        writer.name("topDominators").value(node.getTopDominators()); //$NON-NLS-1$
        if (depth + 1 < options.getTreeDepthLimit() && limit > 0)
        {
            writer.name("_children").beginArray(); //$NON-NLS-1$
            for (int ii = 0; ii < limit; ii++)
            {
                if (state.remainingNodes <= 0)
                {
                    state.truncated = true;
                    childrenTruncated = true;
                    break;
                }
                writeNode(writer, children.get(ii), options, state, depth + 1);
            }
            writer.endArray();
        }
        else if (!children.isEmpty())
        {
            childrenTruncated = true;
        }
        if (childrenTruncated)
        {
            writer.name("_childrenTruncated").value(true); //$NON-NLS-1$
            state.truncated = true;
        }
        writer.endObject();
    }

    private void appendNode(StringBuilder builder, PackageTreeResult.Node node, StringBuilder prefix,
                    SerializationOptions options, TruncationState state, int depth)
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
        builder.append(options.getBytesFormatter().format(node.getRetainedBytes())).append("  "); //$NON-NLS-1$
        builder.append(numberFormatter.format(node.getTopDominators())).append('\n');

        List<PackageTreeResult.Node> children = node.getChildren();
        int limit = Math.min(children.size(), options.getEffectiveLimit());
        if (children.size() > limit)
            state.truncated = true;
        if (depth + 1 >= options.getTreeDepthLimit())
        {
            if (!children.isEmpty())
                state.truncated = true;
            return;
        }

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

            prefix.append(ii == limit - 1 ? '\'' : '|');
            prefix.append("- "); //$NON-NLS-1$
            appendNode(builder, children.get(ii), prefix, options, state, depth + 1);
            prefix.setLength(originalLength);
        }
    }

    private void appendMarkdownNode(StringBuilder builder, PackageTreeResult.Node node, SerializationOptions options,
                    TruncationState state, int depth)
    {
        if (state.remainingNodes <= 0)
        {
            state.truncated = true;
            return;
        }

        state.remainingNodes--;
        for (int ii = 0; ii < depth; ii++)
            builder.append("  "); //$NON-NLS-1$
        builder.append("- ").append(node.getName()).append(" (").append(formatPercent(node.getRetainedPercent())) //$NON-NLS-1$ //$NON-NLS-2$
                        .append(") ").append(options.getBytesFormatter().format(node.getRetainedBytes())).append(' ') //$NON-NLS-1$
                        .append(numberFormatter.format(node.getTopDominators())).append('\n');

        List<PackageTreeResult.Node> children = node.getChildren();
        int limit = Math.min(children.size(), options.getEffectiveLimit());
        if (children.size() > limit)
            state.truncated = true;
        if (depth + 1 >= options.getTreeDepthLimit())
        {
            if (!children.isEmpty())
            {
                state.truncated = true;
                for (int ii = 0; ii <= depth; ii++)
                    builder.append("  "); //$NON-NLS-1$
                builder.append("- Section truncated by depth.\n"); //$NON-NLS-1$
            }
            return;
        }

        for (int ii = 0; ii < limit; ii++)
        {
            appendMarkdownNode(builder, children.get(ii), options, state, depth + 1);
        }
        if (children.size() > limit)
        {
            for (int ii = 0; ii <= depth; ii++)
                builder.append("  "); //$NON-NLS-1$
            builder.append("- ... ").append(children.size() - limit).append(" more nodes\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private String formatPercent(double fraction)
    {
        return percentFormatter.format(fraction * 100d) + "%"; //$NON-NLS-1$
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
}
