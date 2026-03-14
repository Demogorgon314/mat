/*******************************************************************************
 * Copyright (c) 2026 Eclipse Memory Analyzer Project.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.mat.tests.cli;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.mat.cli.internal.serialization.JsonWriter;
import org.eclipse.mat.cli.internal.serialization.SerializationOptions;
import org.eclipse.mat.cli.internal.serialization.TableResultSerializer;
import org.eclipse.mat.cli.internal.serialization.TreeResultSerializer;
import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.IContextObject;
import org.eclipse.mat.query.IResultTable;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.query.ResultMetaData;
import org.junit.Test;

public class ResultSerializerTest
{
    @Test
    public void serializesTableResultToJson()
    {
        TableResultSerializer serializer = new TableResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, new SampleTable(), new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"columns\":[{\"label\":\"Name\",\"type\":\"java.lang.String\"},{\"label\":\"Count\",\"type\":\"int\"}]")); //$NON-NLS-1$
        assertTrue(json.contains("\"values\":[\"Alpha\",7]")); //$NON-NLS-1$
        assertTrue(json.contains("\"context\":{\"objectId\":42}")); //$NON-NLS-1$
    }

    @Test
    public void serializesTreeResultToJson()
    {
        TreeResultSerializer serializer = new TreeResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, new SampleTree(), new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"rows\":[{\"values\":[\"Root\",1]")); //$NON-NLS-1$
        assertTrue(json.contains("\"children\":[{\"values\":[\"Leaf\",2]")); //$NON-NLS-1$
        assertTrue(json.contains("\"context\":{\"objectId\":77}")); //$NON-NLS-1$
    }

    private static final class SampleTable implements IResultTable
    {
        private final List<Row> rows = Collections.singletonList(new Row("Alpha", Integer.valueOf(7), 42)); //$NON-NLS-1$
        private final Column[] columns = new Column[] { new Column("Name", String.class), new Column("Count", int.class) }; //$NON-NLS-1$ //$NON-NLS-2$

        public ResultMetaData getResultMetaData()
        {
            return null;
        }

        public Column[] getColumns()
        {
            return columns;
        }

        public Object getColumnValue(Object row, int columnIndex)
        {
            Row value = (Row) row;
            return columnIndex == 0 ? value.name : value.count;
        }

        public IContextObject getContext(Object row)
        {
            final Row value = (Row) row;
            return new IContextObject()
            {
                public int getObjectId()
                {
                    return value.objectId;
                }
            };
        }

        public int getRowCount()
        {
            return rows.size();
        }

        public Object getRow(int rowId)
        {
            return rows.get(rowId);
        }
    }

    private static final class SampleTree implements IResultTree
    {
        private final Node root = new Node("Root", Integer.valueOf(1), 77, Collections.singletonList(new Node("Leaf", Integer.valueOf(2), 78, Collections.emptyList()))); //$NON-NLS-1$ //$NON-NLS-2$
        private final Column[] columns = new Column[] { new Column("Name", String.class), new Column("Depth", int.class) }; //$NON-NLS-1$ //$NON-NLS-2$

        public ResultMetaData getResultMetaData()
        {
            return null;
        }

        public Column[] getColumns()
        {
            return columns;
        }

        public Object getColumnValue(Object row, int columnIndex)
        {
            Node value = (Node) row;
            return columnIndex == 0 ? value.name : value.depth;
        }

        public IContextObject getContext(Object row)
        {
            final Node value = (Node) row;
            return new IContextObject()
            {
                public int getObjectId()
                {
                    return value.objectId;
                }
            };
        }

        public List<?> getElements()
        {
            return Arrays.asList(root);
        }

        public boolean hasChildren(Object element)
        {
            return !((Node) element).children.isEmpty();
        }

        public List<?> getChildren(Object parent)
        {
            return ((Node) parent).children;
        }
    }

    private static final class Row
    {
        private final String name;
        private final Integer count;
        private final int objectId;

        private Row(String name, Integer count, int objectId)
        {
            this.name = name;
            this.count = count;
            this.objectId = objectId;
        }
    }

    private static final class Node
    {
        private final String name;
        private final Integer depth;
        private final int objectId;
        private final List<Node> children;

        private Node(String name, Integer depth, int objectId, List<Node> children)
        {
            this.name = name;
            this.depth = depth;
            this.objectId = objectId;
            this.children = children;
        }
    }
}
