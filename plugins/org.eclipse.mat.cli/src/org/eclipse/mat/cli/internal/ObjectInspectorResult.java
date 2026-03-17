/*******************************************************************************
 * Copyright (c) 2026 Eclipse Memory Analyzer Project.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.mat.cli.internal;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.IContextObject;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.query.ResultMetaData;
import org.eclipse.mat.snapshot.model.Field;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.snapshot.model.IInstance;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.model.IObjectArray;
import org.eclipse.mat.snapshot.model.IPrimitiveArray;
import org.eclipse.mat.snapshot.model.ObjectReference;
import org.eclipse.mat.cli.internal.serialization.TreeTextStyleProvider;

final class ObjectInspectorResult implements IResultTree, TreeTextStyleProvider
{
    static final class RootValue
    {
        private final String kind;
        private final String name;
        private final String declaredType;
        private final Object value;

        RootValue(String kind, String name, String declaredType, Object value)
        {
            this.kind = kind;
            this.name = name;
            this.declaredType = declaredType;
            this.value = value;
        }

        String getKind()
        {
            return kind;
        }

        String getName()
        {
            return name;
        }

        String getDeclaredType()
        {
            return declaredType;
        }

        Object getValue()
        {
            return value;
        }
    }

    private static final Column[] COLUMNS = new Column[] {
                    new Column("Kind", String.class).noTotals(), //$NON-NLS-1$
                    new Column("Name", String.class).noTotals(), //$NON-NLS-1$
                    new Column("Type", String.class).noTotals(), //$NON-NLS-1$
                    new Column("Value", String.class).noTotals(), //$NON-NLS-1$
                    new Column("Object Address", Long.class).noTotals(), //$NON-NLS-1$
                    new Column("Shallow Heap", Bytes.class).noTotals(), //$NON-NLS-1$
                    new Column("Retained Heap", Bytes.class).noTotals() //$NON-NLS-1$
    };

    private static final class Node
    {
        private final String kind;
        private final String name;
        private final String type;
        private final Object value;
        private final IObject object;

        private Node(String kind, String name, String type, Object value, IObject object)
        {
            this.kind = kind;
            this.name = name;
            this.type = type;
            this.value = value;
            this.object = object;
        }
    }

    private final List<Node> roots;
    private final boolean dump;

    ObjectInspectorResult(IObject object)
    {
        this(object, false);
    }

    ObjectInspectorResult(IObject object, boolean dump)
    {
        this.dump = dump;
        this.roots = Collections.singletonList(objectNode("object", "<object>", object)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    ObjectInspectorResult(RootValue value)
    {
        this(value, false);
    }

    ObjectInspectorResult(RootValue value, boolean dump)
    {
        this.dump = dump;
        this.roots = Collections.singletonList(rootNode(value));
    }

    ObjectInspectorResult(List<RootValue> values)
    {
        this(values, false);
    }

    ObjectInspectorResult(List<RootValue> values, boolean dump)
    {
        this.dump = dump;
        List<Node> resolvedRoots = new ArrayList<Node>(values.size());
        for (RootValue value : values)
        {
            resolvedRoots.add(rootNode(value));
        }
        this.roots = Collections.unmodifiableList(resolvedRoots);
    }

    public ResultMetaData getResultMetaData()
    {
        return null;
    }

    public Column[] getColumns()
    {
        return COLUMNS;
    }

    public List<?> getElements()
    {
        return roots;
    }

    public TreeTextStyle getTreeTextStyle()
    {
        return TreeTextStyle.INSPECTOR;
    }

    public boolean hasChildren(Object element)
    {
        IObject object = ((Node) element).object;
        if (object == null)
            return false;
        if (object instanceof IInstance)
            return !((IInstance) object).getFields().isEmpty();
        if (object instanceof IClass)
            return hasVisibleStaticFields((IClass) object);
        if (object instanceof IObjectArray)
            return ((IObjectArray) object).getLength() > 0;
        if (object instanceof IPrimitiveArray)
            return ((IPrimitiveArray) object).getLength() > 0;
        return false;
    }

    public List<?> getChildren(Object parent)
    {
        IObject object = ((Node) parent).object;
        if (object == null)
            return Collections.emptyList();
        if (object instanceof IInstance)
            return instanceChildren((IInstance) object);
        if (object instanceof IClass)
            return staticChildren((IClass) object);
        if (object instanceof IObjectArray)
            return objectArrayChildren((IObjectArray) object);
        if (object instanceof IPrimitiveArray)
            return primitiveArrayChildren((IPrimitiveArray) object);
        return Collections.emptyList();
    }

    public Object getColumnValue(Object row, int columnIndex)
    {
        Node node = (Node) row;
        switch (columnIndex)
        {
            case 0:
                return node.kind;
            case 1:
                return node.name;
            case 2:
                return node.type;
            case 3:
                return node.value;
            case 4:
                return node.object == null ? null : Long.valueOf(node.object.getObjectAddress());
            case 5:
                return node.object == null ? null : new Bytes(node.object.getUsedHeapSize());
            case 6:
                return node.object == null ? null : new Bytes(node.object.getRetainedHeapSize());
            default:
                return null;
        }
    }

    public IContextObject getContext(Object row)
    {
        IObject object = ((Node) row).object;
        if (object == null)
            return null;

        final int objectId = object.getObjectId();
        return new IContextObject()
        {
            public int getObjectId()
            {
                return objectId;
            }
        };
    }

    private boolean hasVisibleStaticFields(IClass object)
    {
        for (Field field : object.getStaticFields())
        {
            if (!ObjectDisplayHelper.isPseudoStatic(field))
                return true;
        }
        return false;
    }

    private List<Node> instanceChildren(IInstance object)
    {
        List<Node> children = new ArrayList<Node>();
        for (Field field : object.getFields())
        {
            children.add(fieldNode("field", field)); //$NON-NLS-1$
        }
        return children;
    }

    private List<Node> staticChildren(IClass object)
    {
        List<Node> children = new ArrayList<Node>();
        for (Field field : object.getStaticFields())
        {
            if (ObjectDisplayHelper.isPseudoStatic(field))
                continue;
            children.add(fieldNode("static", field)); //$NON-NLS-1$
        }
        return children;
    }

    private List<?> objectArrayChildren(final IObjectArray array)
    {
        return new AbstractList<Node>()
        {
            @Override
            public Node get(int index)
            {
                try
                {
                    long address = array.getReferenceArray(index, 1)[0];
                    if (address == 0)
                        return primitiveNode("element", "[" + index + "]", "ref", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    IObject child = new ObjectReference(array.getSnapshot(), address).getObject();
                    return objectNode("element", "[" + index + "]", child); //$NON-NLS-1$ //$NON-NLS-2$
                }
                catch (SnapshotException e)
                {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public int size()
            {
                return array.getLength();
            }
        };
    }

    private List<?> primitiveArrayChildren(final IPrimitiveArray array)
    {
        return new AbstractList<Node>()
        {
            @Override
            public Node get(int index)
            {
                return primitiveNode("element", "[" + index + "]", ObjectDisplayHelper.primitiveTypeName(array.getType()), //$NON-NLS-1$ //$NON-NLS-2$
                                array.getValueAt(index));
            }

            @Override
            public int size()
            {
                return array.getLength();
            }
        };
    }

    private Node fieldNode(String kind, Field field)
    {
        Object value = field.getValue();
        if (value instanceof ObjectReference)
        {
            ObjectReference reference = (ObjectReference) value;
            if (reference.getObjectAddress() == 0)
                return primitiveNode(kind, field.getName(), ObjectDisplayHelper.fieldType(field), null);
            try
            {
                IObject target = reference.getObject();
                return objectNode(kind, field.getName(), target);
            }
            catch (SnapshotException e)
            {
                throw new RuntimeException(e);
            }
        }

        return primitiveNode(kind, field.getName(), ObjectDisplayHelper.fieldType(field), value);
    }

    private Node rootNode(RootValue value)
    {
        if (value.getValue() instanceof IObject)
            return objectNode(value.getKind(), value.getName(), (IObject) value.getValue());
        return primitiveNode(value.getKind(), value.getName(), value.getDeclaredType(), value.getValue());
    }

    private Node objectNode(String kind, String name, IObject object)
    {
        Object inlineValue = ObjectInspectorDisplayHelper.inlineValue(object, dump);
        if (inlineValue != null)
            return new Node(kind, name, object.getClazz().getName(), inlineValue, null);

        return new Node(kind, name, object.getClazz().getName(), ObjectDisplayHelper.previewValue(object, dump), object);
    }

    private Node primitiveNode(String kind, String name, String type, Object value)
    {
        return new Node(kind, name, type, value == null ? null : ObjectDisplayHelper.primitiveValue(value), null);
    }
}
