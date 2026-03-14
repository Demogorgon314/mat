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

import org.eclipse.mat.cli.internal.TopConsumersResult;
import org.eclipse.mat.cli.internal.serialization.JsonWriter;
import org.eclipse.mat.cli.internal.serialization.SerializationOptions;
import org.eclipse.mat.cli.internal.serialization.SpecResultSerializer;
import org.eclipse.mat.cli.internal.serialization.TableResultSerializer;
import org.eclipse.mat.cli.internal.serialization.TextResultSerializer;
import org.eclipse.mat.cli.internal.serialization.TopConsumersResultSerializer;
import org.eclipse.mat.cli.internal.serialization.TreeResultSerializer;
import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.IContextObject;
import org.eclipse.mat.query.IResult;
import org.eclipse.mat.query.IResultTable;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.query.ResultMetaData;
import org.eclipse.mat.query.results.TextResult;
import org.eclipse.mat.report.QuerySpec;
import org.eclipse.mat.report.SectionSpec;
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

    @Test
    public void truncatesTreeJsonByTotalNodeBudget()
    {
        TreeResultSerializer serializer = new TreeResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, new BudgetTree(), new SerializationOptions(10, 8, 2));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertTrue(truncated);
        assertTrue(json.contains("\"values\":[\"Root\",0]")); //$NON-NLS-1$
        assertTrue(json.contains("\"values\":[\"Child A\",1]")); //$NON-NLS-1$
        assertFalse(json.contains("\"values\":[\"Child B\",1]")); //$NON-NLS-1$
    }

    @Test
    public void serializesCompositeResultToJson() throws Exception
    {
        SpecResultSerializer serializer = new SpecResultSerializer(new TableResultSerializer(), new TreeResultSerializer(),
                        new TextResultSerializer());
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, sampleSection(), new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"sections\":[{\"kind\":\"query\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"Sample Table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultType\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"Sample Tree\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultType\":\"tree\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"Overview\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultType\":\"text\"")); //$NON-NLS-1$
    }

    @Test
    public void serializesTopConsumersSectionToCompactJson() throws Exception
    {
        TopConsumersResultSerializer serializer = new TopConsumersResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, sampleTopConsumersResult(), new SerializationOptions(2, 8, 20));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertTrue(truncated);
        assertTrue(json.contains("\"totalRetainedHeap\":1000")); //$NON-NLS-1$
        assertTrue(json.contains("\"biggestObjects\":{")); //$NON-NLS-1$
        assertTrue(json.contains("\"biggestObjects\":{\"name\":\"Biggest Objects\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"label\":\"Largest\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"retainedBytes\":500")); //$NON-NLS-1$
        assertTrue(json.contains("\"classes\":{\"name\":\"Biggest Top-Level Dominator Classes\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"classLoaders\":{\"name\":\"Biggest Top-Level Dominator Class Loaders\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"packages\":{\"name\":\"Biggest Top-Level Dominator Packages\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"root\":{\"name\":\"<all>\"")); //$NON-NLS-1$
    }

    @Test
    public void serializesTopConsumersJsonWithoutNullPackageNodes() throws Exception
    {
        TopConsumersResultSerializer serializer = new TopConsumersResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, sampleTopConsumersResult(), new SerializationOptions(2, 8, 2));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertTrue(truncated);
        assertFalse(json.contains("\"children\":[null")); //$NON-NLS-1$
        assertFalse(json.contains(",null")); //$NON-NLS-1$
    }

    @Test
    public void serializesTopConsumersToText() throws Exception
    {
        TopConsumersResultSerializer serializer = new TopConsumersResultSerializer();

        String text = serializer.toText(sampleTopConsumersResult(), new SerializationOptions(2, 8, 20));

        assertTrue(text.contains("Biggest Objects:")); //$NON-NLS-1$
        assertTrue(text.contains("50.00%  500  Largest")); //$NON-NLS-1$
        assertFalse(text.contains("Third")); //$NON-NLS-1$
        assertTrue(text.contains("Biggest Top-Level Dominator Classes:")); //$NON-NLS-1$
        assertTrue(text.contains("60.00%  600  4  Alpha")); //$NON-NLS-1$
        assertFalse(text.contains("Gamma")); //$NON-NLS-1$
        assertTrue(text.contains("Biggest Top-Level Dominator Packages:")); //$NON-NLS-1$
        assertTrue(text.contains("<all>  (100.00%)  1,000  4")); //$NON-NLS-1$
        assertFalse(text.contains("more rows")); //$NON-NLS-1$
        assertFalse(text.contains("more nodes")); //$NON-NLS-1$
    }

    @Test
    public void marksUnsupportedNestedCompositeResult() throws Exception
    {
        SpecResultSerializer serializer = new SpecResultSerializer(new TableResultSerializer(), new TreeResultSerializer(),
                        new TextResultSerializer());
        QuerySpec query = new QuerySpec("Unsupported", new UnsupportedResult()); //$NON-NLS-1$
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, query, new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"resultType\":\"unsupported\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"unsupportedType\":\"org.eclipse.mat.tests.cli.ResultSerializerTest$UnsupportedResult\"")); //$NON-NLS-1$
    }

    private SectionSpec sampleSection()
    {
        SectionSpec section = new SectionSpec("Top Consumers"); //$NON-NLS-1$
        section.add(new QuerySpec("Overview", new TextResult("Summary", true))); //$NON-NLS-1$ //$NON-NLS-2$
        section.add(new QuerySpec("Sample Table", new SampleTable())); //$NON-NLS-1$
        section.add(new QuerySpec("Sample Tree", new SampleTree())); //$NON-NLS-1$
        return section;
    }

    private TopConsumersResult sampleTopConsumersResult()
    {
        List<TopConsumersResult.ObjectRow> biggestObjects = Arrays.asList(
                        new TopConsumersResult.ObjectRow(91, "Largest", 500, 0.5d), //$NON-NLS-1$
                        new TopConsumersResult.ObjectRow(92, "Second", 300, 0.3d), //$NON-NLS-1$
                        new TopConsumersResult.ObjectRow(93, "Third", 150, 0.15d)); //$NON-NLS-1$
        List<TopConsumersResult.DominatorRow> classes = Arrays.asList(
                        new TopConsumersResult.DominatorRow(41, "Alpha", 4, 600, 0.6d), //$NON-NLS-1$
                        new TopConsumersResult.DominatorRow(42, "Beta", 2, 250, 0.25d), //$NON-NLS-1$
                        new TopConsumersResult.DominatorRow(43, "Gamma", 1, 120, 0.12d)); //$NON-NLS-1$
        List<TopConsumersResult.DominatorRow> classLoaders = Arrays.asList(
                        new TopConsumersResult.DominatorRow(2, "Loader A", 6, 700, 0.7d), //$NON-NLS-1$
                        new TopConsumersResult.DominatorRow(0, "<system class loader>", 3, 200, 0.2d)); //$NON-NLS-1$
        TopConsumersResult.PackageNode root = new TopConsumersResult.PackageNode("<all>", 1000, 1.0d, 4, Arrays.asList( //$NON-NLS-1$
                        new TopConsumersResult.PackageNode("org", 700, 0.7d, 3, Arrays.asList( //$NON-NLS-1$
                                        new TopConsumersResult.PackageNode("example", 650, 0.65d, 2, Collections.<TopConsumersResult.PackageNode>emptyList()))), //$NON-NLS-1$
                        new TopConsumersResult.PackageNode("com", 200, 0.2d, 1, Collections.<TopConsumersResult.PackageNode>emptyList()))); //$NON-NLS-1$
        return new TopConsumersResult(1000, biggestObjects, classes, classLoaders, root);
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

    private static final class BudgetTree implements IResultTree
    {
        private final Node root = new Node("Root", Integer.valueOf(0), 80, Arrays.asList( //$NON-NLS-1$
                        new Node("Child A", Integer.valueOf(1), 81, Collections.emptyList()), //$NON-NLS-1$
                        new Node("Child B", Integer.valueOf(1), 82, Collections.emptyList()))); //$NON-NLS-1$
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

    private static final class BiggestObjectsTree implements IResultTree
    {
        private final BiggestObject root = new BiggestObject("Largest", Integer.valueOf(64), new Bytes(500), 91, //$NON-NLS-1$
                        Collections.singletonList(new BiggestObject("Child", Integer.valueOf(32), new Bytes(250), 92, //$NON-NLS-1$
                                        Collections.<BiggestObject>emptyList())));
        private final Column[] columns = new Column[] { new Column("Object", String.class), //$NON-NLS-1$
                        new Column("Shallow Heap", int.class), new Column("Retained Heap", Bytes.class) }; //$NON-NLS-1$ //$NON-NLS-2$

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
            BiggestObject value = (BiggestObject) row;
            switch (columnIndex)
            {
                case 0:
                    return value.label;
                case 1:
                    return value.shallowHeap;
                case 2:
                    return value.retainedHeap;
                default:
                    return null;
            }
        }

        public IContextObject getContext(Object row)
        {
            final BiggestObject value = (BiggestObject) row;
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
            return Collections.singletonList(root);
        }

        public boolean hasChildren(Object element)
        {
            return !((BiggestObject) element).children.isEmpty();
        }

        public List<?> getChildren(Object parent)
        {
            return ((BiggestObject) parent).children;
        }
    }

    private static final class PackageTree implements IResultTree
    {
        private final PackageNode root = new PackageNode("<all>", new Bytes(1000), Double.valueOf(1.0d), 4, -1, //$NON-NLS-1$
                        Collections.singletonList(new PackageNode("org", new Bytes(500), Double.valueOf(0.5d), 2, -1, //$NON-NLS-1$
                                        Collections.<PackageNode>emptyList())));
        private final Column[] columns = new Column[] { new Column("Package", String.class), //$NON-NLS-1$
                        new Column("Retained Heap", Bytes.class), new Column("Retained Heap, %", double.class), //$NON-NLS-1$ //$NON-NLS-2$
                        new Column("Top Dominators", int.class) }; //$NON-NLS-1$

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
            PackageNode value = (PackageNode) row;
            switch (columnIndex)
            {
                case 0:
                    return value.name;
                case 1:
                    return value.retainedHeap;
                case 2:
                    return value.retainedPercent;
                case 3:
                    return Integer.valueOf(value.topDominators);
                default:
                    return null;
            }
        }

        public IContextObject getContext(Object row)
        {
            final PackageNode value = (PackageNode) row;
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
            return Collections.singletonList(root);
        }

        public boolean hasChildren(Object element)
        {
            return !((PackageNode) element).children.isEmpty();
        }

        public List<?> getChildren(Object parent)
        {
            return ((PackageNode) parent).children;
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

    private static final class BiggestObject
    {
        private final String label;
        private final Integer shallowHeap;
        private final Bytes retainedHeap;
        private final int objectId;
        private final List<BiggestObject> children;

        private BiggestObject(String label, Integer shallowHeap, Bytes retainedHeap, int objectId,
                        List<BiggestObject> children)
        {
            this.label = label;
            this.shallowHeap = shallowHeap;
            this.retainedHeap = retainedHeap;
            this.objectId = objectId;
            this.children = children;
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

    private static final class PackageNode
    {
        private final String name;
        private final Bytes retainedHeap;
        private final Double retainedPercent;
        private final int topDominators;
        private final int objectId;
        private final List<PackageNode> children;

        private PackageNode(String name, Bytes retainedHeap, Double retainedPercent, int topDominators, int objectId,
                        List<PackageNode> children)
        {
            this.name = name;
            this.retainedHeap = retainedHeap;
            this.retainedPercent = retainedPercent;
            this.topDominators = topDominators;
            this.objectId = objectId;
            this.children = children;
        }
    }

    private static final class UnsupportedResult implements IResult
    {
        public ResultMetaData getResultMetaData()
        {
            return null;
        }
    }
}
