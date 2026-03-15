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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.cli.internal.serialization.TreeTextStyleProvider.TreeTextStyle;

public class TreeResultSerializer extends StructuredResultSerializer
{
    private static final String ROOT_PATH = "<root>"; //$NON-NLS-1$

    public boolean writeJson(JsonWriter writer, IResultTree tree, SerializationOptions options)
    {
        Column[] columns = tree.getColumns();
        ColumnSchema[] schema = buildColumnSchemas(columns);
        writeColumns(writer, columns);
        writer.name("rows").beginArray(); //$NON-NLS-1$
        TruncationState state = new TruncationState(options.getTreeNodeLimit());
        writeNodes(writer, tree, columns, schema, tree.getElements(), 0, options, state, new PathState(), null);
        writer.endArray();
        return state.truncated;
    }

    public boolean writeAgentJson(JsonWriter writer, IResultTree tree, SerializationOptions options)
    {
        Column[] columns = tree.getColumns();
        ColumnSchema[] schema = buildColumnSchemas(columns);
        writer.name("items").beginArray(); //$NON-NLS-1$
        TruncationState state = new TruncationState(options.getTreeNodeLimit());
        writeAgentNodes(writer, tree, columns, schema, tree.getElements(), 0, options, state, new PathState(), null);
        writer.endArray();
        return state.truncated;
    }

    public String toText(IResultTree tree, SerializationOptions options)
    {
        return toText(tree, options, false);
    }

    public String toText(IResultTree tree, SerializationOptions options, boolean showNulls)
    {
        Column[] columns = tree.getColumns();
        ColumnSchema[] schema = buildColumnSchemas(columns);
        StringBuilder builder = new StringBuilder();
        TruncationState state = new TruncationState(Integer.MAX_VALUE);
        appendNodes(builder, tree, columns, schema, tree.getElements(), 0, options, state, new PathState(), null,
                        textStyle(tree), showNulls);
        return builder.toString();
    }

    private void writeNodes(JsonWriter writer, IResultTree tree, Column[] columns, ColumnSchema[] schema, List<?> rows,
                    int depth, SerializationOptions options, TruncationState state, PathState path, String parentPath)
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
            CellValue[] cells = readCells(tree, columns, row);
            boolean hasChildren = tree.hasChildren(row);
            Integer objectId = rowObjectId(tree, row);
            String nodePath = pathFor(parentPath, rows.size(), ii, columns, schema, row, cells);
            state.remainingNodes--;

            writer.beginObject();
            writer.name("path").value(nodePath); //$NON-NLS-1$
            writer.name("valueKind").value(valueKind(columns, schema, cells, objectId)); //$NON-NLS-1$
            writer.name("hasChildren").value(hasChildren); //$NON-NLS-1$
            writeRowValues(writer, tree, columns, row);
            writeContext(writer, tree, row, options);

            boolean cycle = path.isCycle(objectId);
            writer.name("_cycle").value(cycle); //$NON-NLS-1$
            if (cycle)
            {
                writer.name("children").beginArray().endArray(); //$NON-NLS-1$
            }
            else if (depth + 1 >= options.getTreeDepthLimit())
            {
                if (hasChildren)
                    state.truncated = true;
                writer.name("children").beginArray().endArray(); //$NON-NLS-1$
            }
            else
            {
                writer.name("children").beginArray(); //$NON-NLS-1$
                List<?> children = hasChildren ? tree.getChildren(row) : null;
                path.push(objectId);
                if (children != null)
                {
                    if (state.remainingNodes > 0)
                        writeNodes(writer, tree, columns, schema, children, depth + 1, options, state, path, nodePath);
                    else
                        state.truncated = true;
                }
                path.pop(objectId);
                writer.endArray();
            }
            writer.endObject();
        }
    }

    private void writeAgentNodes(JsonWriter writer, IResultTree tree, Column[] columns, ColumnSchema[] schema,
                    List<?> rows, int depth, SerializationOptions options, TruncationState state, PathState path,
                    String parentPath)
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
            CellValue[] cells = readCells(tree, columns, row);
            boolean hasChildren = tree.hasChildren(row);
            Integer objectId = rowObjectId(tree, row);
            String nodePath = pathFor(parentPath, rows.size(), ii, columns, schema, row, cells);
            state.remainingNodes--;

            writer.beginObject();
            writer.name("path").value(nodePath); //$NON-NLS-1$
            writer.name("valueKind").value(valueKind(columns, schema, cells, objectId)); //$NON-NLS-1$
            writer.name("hasChildren").value(hasChildren); //$NON-NLS-1$
            writeAgentRow(writer, tree, schema, row, options);

            boolean cycle = path.isCycle(objectId);
            if (cycle)
                writer.name("_cycle").value(true); //$NON-NLS-1$

            boolean childrenTruncated = false;
            boolean wroteChildren = false;
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

                    if (childLimit > 0 && state.remainingNodes > 0)
                    {
                        writer.name("_children").beginArray(); //$NON-NLS-1$
                        wroteChildren = true;
                    }
                    else if (childLimit > 0)
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
                        writeAgentNodes(writer, tree, columns, schema, children.subList(childIndex, childIndex + 1),
                                        depth + 1, options, state, path, nodePath);
                        path.pop(objectId);
                    }
                }
            }
            if (wroteChildren)
                writer.endArray();
            if (childrenTruncated)
                writer.name("_childrenTruncated").value(true); //$NON-NLS-1$
            writer.endObject();
        }
    }

    private void appendNodes(StringBuilder builder, IResultTree tree, Column[] columns, ColumnSchema[] schema,
                    List<?> rows, int depth, SerializationOptions options, TruncationState state, PathState path,
                    String parentPath, TreeTextStyle style, boolean showNulls)
    {
        List<TextNode> visibleRows = visibleRows(tree, columns, schema, rows, parentPath, style, showNulls);
        int limit = Math.min(visibleRows.size(), options.getLimit());
        if (visibleRows.size() > limit)
            state.truncated = true;

        for (int ii = 0; ii < limit; ii++)
        {
            TextNode node = visibleRows.get(ii);
            Object row = node.row;
            CellValue[] cells = node.cells;
            boolean hasChildren = node.hasChildren;
            Integer objectId = node.objectId;
            String nodePath = node.path;

            for (int pad = 0; pad < depth; pad++)
                builder.append("  "); //$NON-NLS-1$
            if (style == TreeTextStyle.COMPACT)
                builder.append("- "); //$NON-NLS-1$
            builder.append(formatRow(style, columns, schema, row, cells, objectId, node.valueKind)).append('\n');

            if (path.isCycle(objectId))
            {
                for (int pad = 0; pad < depth + 1; pad++)
                    builder.append("  "); //$NON-NLS-1$
                builder.append("[cycle]\n"); //$NON-NLS-1$
            }
            else if (depth + 1 < options.getTreeDepthLimit() && hasChildren)
            {
                path.push(objectId);
                appendNodes(builder, tree, columns, schema, tree.getChildren(row), depth + 1, options, state, path,
                                nodePath, style, showNulls);
                path.pop(objectId);
            }
            else if (hasChildren)
            {
                state.truncated = true;
            }
        }

        if (visibleRows.size() > limit)
        {
            for (int pad = 0; pad < depth; pad++)
                builder.append("  "); //$NON-NLS-1$
            builder.append("... ").append(visibleRows.size() - limit).append(" more nodes\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private String formatRow(TreeTextStyle style, Column[] columns, ColumnSchema[] schema, Object row, CellValue[] cells,
                    Integer objectId, String valueKind)
    {
        if (style == TreeTextStyle.INSPECTOR)
            return formatInspectorRow(columns, schema, row, cells, valueKind);
        return formatCompactRow(columns, schema, row, cells);
    }

    private List<TextNode> visibleRows(IResultTree tree, Column[] columns, ColumnSchema[] schema, List<?> rows,
                    String parentPath, TreeTextStyle style, boolean showNulls)
    {
        List<TextNode> visibleRows = new ArrayList<TextNode>(rows.size());
        TextNode fallbackHiddenRow = null;
        for (int ii = 0; ii < rows.size(); ii++)
        {
            Object row = rows.get(ii);
            CellValue[] cells = readCells(tree, columns, row);
            Integer objectId = rowObjectId(tree, row);
            String rowValueKind = valueKind(columns, schema, cells, objectId);
            TextNode node = new TextNode(row, cells, tree.hasChildren(row), objectId,
                            pathFor(parentPath, rows.size(), ii, columns, schema, row, cells), rowValueKind);
            if (shouldHideTextRow(style, showNulls, rowValueKind))
            {
                if (fallbackHiddenRow == null)
                    fallbackHiddenRow = node;
                continue;
            }

            visibleRows.add(node);
        }

        if (visibleRows.isEmpty() && parentPath == null && style == TreeTextStyle.INSPECTOR && !showNulls
                        && fallbackHiddenRow != null)
            visibleRows.add(fallbackHiddenRow);

        return visibleRows;
    }

    private boolean shouldHideTextRow(TreeTextStyle style, boolean showNulls, String valueKind)
    {
        if (style != TreeTextStyle.INSPECTOR || showNulls)
            return false;
        return "null".equals(valueKind); //$NON-NLS-1$
    }

    private String formatCompactRow(Column[] columns, ColumnSchema[] schema, Object row, CellValue[] cells)
    {
        List<Integer> primaryColumns = primaryColumns(columns, schema, row, cells);
        StringBuilder builder = new StringBuilder();

        if (primaryColumns.isEmpty())
        {
            builder.append("<row>"); //$NON-NLS-1$
        }
        else
        {
            for (int ii = 0; ii < primaryColumns.size(); ii++)
            {
                if (ii > 0)
                    builder.append(" | "); //$NON-NLS-1$
                int columnIndex = primaryColumns.get(ii).intValue();
                builder.append(displayText(columns[columnIndex], schema[columnIndex], row, cells[columnIndex], true));
            }
        }

        List<String> annotations = new ArrayList<String>();
        for (int ii = 0; ii < columns.length; ii++)
        {
            if (primaryColumns.contains(Integer.valueOf(ii)))
                continue;

            String display = displayText(columns[ii], schema[ii], row, cells[ii], false);
            if (display == null || display.length() == 0)
                continue;
            annotations.add(columns[ii].getLabel() + "=" + display); //$NON-NLS-1$
        }

        if (!annotations.isEmpty())
        {
            builder.append(" ["); //$NON-NLS-1$
            for (int ii = 0; ii < annotations.size(); ii++)
            {
                if (ii > 0)
                    builder.append(' ');
                builder.append(annotations.get(ii));
            }
            builder.append(']');
        }
        return builder.toString();
    }

    private String formatInspectorRow(Column[] columns, ColumnSchema[] schema, Object row, CellValue[] cells,
                    String valueKind)
    {
        String kind = columnDisplay(columns, schema, row, cells, "kind", false); //$NON-NLS-1$
        String name = columnDisplay(columns, schema, row, cells, "name", false); //$NON-NLS-1$
        String type = columnDisplay(columns, schema, row, cells, "type", false); //$NON-NLS-1$
        String address = columnDisplay(columns, schema, row, cells, "object_address", false); //$NON-NLS-1$
        String value = columnDisplay(columns, schema, row, cells, "value", true); //$NON-NLS-1$

        StringBuilder builder = new StringBuilder();
        if ("object".equals(kind)) //$NON-NLS-1$
        {
            builder.append("object "); //$NON-NLS-1$
            builder.append(name == null ? "<object>" : name); //$NON-NLS-1$
            if (type != null && type.length() > 0)
                builder.append(": ").append(type); //$NON-NLS-1$
            if (address != null && address.length() > 0)
                builder.append(" @").append(address); //$NON-NLS-1$
            appendInspectorHeap(builder, columns, schema, cells);
            return builder.toString();
        }

        if ("field".equals(kind)) //$NON-NLS-1$
        {
            builder.append('.').append(name);
        }
        else if ("static".equals(kind)) //$NON-NLS-1$
        {
            builder.append(".static ").append(name); //$NON-NLS-1$
        }
        else if ("element".equals(kind)) //$NON-NLS-1$
        {
            builder.append(name);
        }
        else
        {
            builder.append(name == null ? "<node>" : name); //$NON-NLS-1$
        }

        if ("reference".equals(valueKind) || ("preview".equals(valueKind) && address != null && address.length() > 0)) //$NON-NLS-1$ //$NON-NLS-2$
        {
            builder.append(" -> "); //$NON-NLS-1$
            if (type != null && type.length() > 0)
                builder.append(type);
            else
                builder.append("<object>"); //$NON-NLS-1$
            if (address != null && address.length() > 0)
                builder.append(" @").append(address); //$NON-NLS-1$
            appendInspectorHeap(builder, columns, schema, cells);
            return builder.toString();
        }

        builder.append(" = "); //$NON-NLS-1$
        if ("null".equals(valueKind)) //$NON-NLS-1$
            builder.append("null"); //$NON-NLS-1$
        else
            builder.append(value == null ? "<value>" : value); //$NON-NLS-1$
        if (type != null && type.length() > 0 && !"null".equals(valueKind)) //$NON-NLS-1$
            builder.append(" : ").append(type); //$NON-NLS-1$
        return builder.toString();
    }

    private void appendInspectorHeap(StringBuilder builder, Column[] columns, ColumnSchema[] schema, CellValue[] cells)
    {
        int shallowIndex = columnIndex(schema, "shallow_heap"); //$NON-NLS-1$
        int retainedIndex = columnIndex(schema, "retained_heap"); //$NON-NLS-1$
        String shallow = shallowIndex < 0 ? null : bytesText(cellValue(cells[shallowIndex]));
        String retained = retainedIndex < 0 ? null : bytesText(cellValue(cells[retainedIndex]));
        if (shallow == null && retained == null)
            return;

        builder.append(" ["); //$NON-NLS-1$
        boolean wroteValue = false;
        if (shallow != null)
        {
            builder.append("shallow=").append(shallow); //$NON-NLS-1$
            wroteValue = true;
        }
        if (retained != null)
        {
            if (wroteValue)
                builder.append(' ');
            builder.append("retained=").append(retained); //$NON-NLS-1$
        }
        builder.append(']');
    }

    private String bytesText(Object value)
    {
        if (!(value instanceof Bytes))
            return null;
        return ((Bytes) value).getValue() + "B"; //$NON-NLS-1$
    }

    private List<Integer> primaryColumns(Column[] columns, ColumnSchema[] schema, Object row, CellValue[] cells)
    {
        List<Integer> primary = new ArrayList<Integer>(2);
        for (int ii = 0; ii < columns.length && primary.size() < 2; ii++)
        {
            String display = displayText(columns[ii], schema[ii], row, cells[ii], false);
            if (display == null || display.length() == 0)
                continue;
            if (isPrimaryTextColumn(columns[ii], cells[ii]))
                primary.add(Integer.valueOf(ii));
        }

        if (primary.isEmpty())
        {
            for (int ii = 0; ii < columns.length; ii++)
            {
                String display = displayText(columns[ii], schema[ii], row, cells[ii], true);
                if (display != null && display.length() > 0)
                {
                    primary.add(Integer.valueOf(ii));
                    break;
                }
            }
        }
        return primary;
    }

    private boolean isPrimaryTextColumn(Column column, CellValue cell)
    {
        if (cell == null || cellError(cell) != null || cellValue(cell) == null)
            return false;
        if (isAddressColumn(column) || Bytes.class.isAssignableFrom(column.getType()))
            return false;
        return cellValue(cell) instanceof CharSequence || displayMetadata(cellValue(cell)) != null;
    }

    private String displayText(Column column, ColumnSchema schema, Object row, CellValue cell, boolean renderNullLiteral)
    {
        return renderText(column, schema, row, cell, renderNullLiteral, true);
    }

    private String pathText(Column column, ColumnSchema schema, Object row, CellValue cell, boolean renderNullLiteral)
    {
        return renderText(column, schema, row, cell, renderNullLiteral, false);
    }

    private String renderText(Column column, ColumnSchema schema, Object row, CellValue cell, boolean renderNullLiteral,
                    boolean spacedDecorator)
    {
        if (cellError(cell) != null)
            return formatCellError(cellError(cell));
        if (cellValue(cell) == null)
            return renderNullLiteral && "value".equals(columnId(schema)) ? "null" : null; //$NON-NLS-1$ //$NON-NLS-2$
        String display = spacedDecorator ? displayValue(column, row, cellValue(cell))
                        : pathDisplayValue(column, row, cellValue(cell));
        return display == null ? null : display.trim();
    }

    private String columnDisplay(Column[] columns, ColumnSchema[] schema, Object row, CellValue[] cells, String id,
                    boolean renderNullLiteral)
    {
        int index = columnIndex(schema, id);
        if (index < 0)
            return null;
        return displayText(columns[index], schema[index], row, cells[index], renderNullLiteral);
    }

    private String pathColumnDisplay(Column[] columns, ColumnSchema[] schema, Object row, CellValue[] cells, String id,
                    boolean renderNullLiteral)
    {
        int index = columnIndex(schema, id);
        if (index < 0)
            return null;
        return pathText(columns[index], schema[index], row, cells[index], renderNullLiteral);
    }

    private int columnIndex(ColumnSchema[] schema, String id)
    {
        for (int ii = 0; ii < schema.length; ii++)
        {
            if (id.equals(columnId(schema[ii])))
                return ii;
        }
        return -1;
    }

    private String valueKind(Column[] columns, ColumnSchema[] schema, CellValue[] cells, Integer objectId)
    {
        int valueIndex = columnIndex(schema, "value"); //$NON-NLS-1$
        if (valueIndex >= 0 && cellError(cells[valueIndex]) == null && cellValue(cells[valueIndex]) == null)
            return "null"; //$NON-NLS-1$

        if (hasNonNullAddress(columns, cells))
            return "reference"; //$NON-NLS-1$

        if (hasDisplayMetadata(cells))
            return "preview"; //$NON-NLS-1$

        if (objectId != null)
            return "reference"; //$NON-NLS-1$

        return "primitive"; //$NON-NLS-1$
    }

    private boolean hasDisplayMetadata(CellValue[] cells)
    {
        for (CellValue cell : cells)
        {
            if (displayMetadata(cellValue(cell)) != null)
                return true;
        }
        return false;
    }

    private boolean hasNonNullAddress(Column[] columns, CellValue[] cells)
    {
        for (int ii = 0; ii < columns.length; ii++)
        {
            if (isAddressColumn(columns[ii]) && cellError(cells[ii]) == null && cellValue(cells[ii]) != null)
                return true;
        }
        return false;
    }

    private TreeTextStyle textStyle(IResultTree tree)
    {
        if (tree instanceof TreeTextStyleProvider)
            return ((TreeTextStyleProvider) tree).getTreeTextStyle();
        return TreeTextStyle.COMPACT;
    }

    private String pathFor(String parentPath, int siblingCount, int index, Column[] columns, ColumnSchema[] schema,
                    Object row, CellValue[] cells)
    {
        if (parentPath == null)
            return siblingCount <= 1 ? ROOT_PATH : ROOT_PATH + "[" + index + "]"; //$NON-NLS-1$ //$NON-NLS-2$

        String segment = pathSegment(columns, schema, row, cells, index);
        if (segment.startsWith("[") && segment.endsWith("]")) //$NON-NLS-1$ //$NON-NLS-2$
            return parentPath + segment;
        return parentPath + "." + segment; //$NON-NLS-1$
    }

    private String pathSegment(Column[] columns, ColumnSchema[] schema, Object row, CellValue[] cells, int index)
    {
        String name = pathColumnDisplay(columns, schema, row, cells, "name", false); //$NON-NLS-1$
        if (name == null || name.length() == 0)
        {
            for (int ii = 0; ii < columns.length; ii++)
            {
                if (cellError(cells[ii]) != null || cellValue(cells[ii]) == null)
                    continue;
                String display = pathDisplayValue(columns[ii], row, cellValue(cells[ii]));
                if (display != null && display.trim().length() > 0)
                {
                    name = display.trim();
                    break;
                }
            }
        }
        if (name == null || name.length() == 0)
            return "<index:" + index + ">"; //$NON-NLS-1$ //$NON-NLS-2$

        String segment = name.trim();
        if (segment.length() == 0)
            return "<index:" + index + ">"; //$NON-NLS-1$ //$NON-NLS-2$
        return segment;
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

    private static final class TextNode
    {
        private final Object row;
        private final CellValue[] cells;
        private final boolean hasChildren;
        private final Integer objectId;
        private final String path;
        private final String valueKind;

        private TextNode(Object row, CellValue[] cells, boolean hasChildren, Integer objectId, String path,
                        String valueKind)
        {
            this.row = row;
            this.cells = cells;
            this.hasChildren = hasChildren;
            this.objectId = objectId;
            this.path = path;
            this.valueKind = valueKind;
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
