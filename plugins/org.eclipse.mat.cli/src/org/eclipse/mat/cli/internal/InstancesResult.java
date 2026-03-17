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

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.IContextObject;
import org.eclipse.mat.query.IResultTable;
import org.eclipse.mat.query.ResultMetaData;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IObject;

final class InstancesResult implements IResultTable
{
    private static final Column[] COLUMNS = new Column[] {
                    new Column("Object Address", Long.class).noTotals(), //$NON-NLS-1$
                    new Column("Class Name", String.class).noTotals(), //$NON-NLS-1$
                    new Column("Preview", String.class).noTotals(), //$NON-NLS-1$
                    new Column("Shallow Heap", Bytes.class).noTotals(), //$NON-NLS-1$
                    new Column("Retained Heap", Bytes.class).noTotals() //$NON-NLS-1$
    };

    static final class Row
    {
        private final ISnapshot snapshot;
        private final int objectId;
        private final boolean dump;

        private IObject object;
        private String className;
        private Object preview;
        private Long objectAddress;
        private Bytes shallowHeap;
        private Bytes retainedHeap;

        Row(ISnapshot snapshot, int objectId, boolean dump)
        {
            this.snapshot = snapshot;
            this.objectId = objectId;
            this.dump = dump;
        }

        private void resolve()
        {
            if (object != null)
                return;

            try
            {
                object = snapshot.getObject(objectId);
                className = object.getClazz().getName();
                preview = ObjectDisplayHelper.previewValue(object, dump);
                objectAddress = Long.valueOf(object.getObjectAddress());
                shallowHeap = new Bytes(object.getUsedHeapSize());
                retainedHeap = new Bytes(object.getRetainedHeapSize());
            }
            catch (SnapshotException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private final Row[] rows;

    InstancesResult(Row[] rows)
    {
        this.rows = rows;
    }

    public ResultMetaData getResultMetaData()
    {
        return null;
    }

    public Column[] getColumns()
    {
        return COLUMNS;
    }

    public int getRowCount()
    {
        return rows.length;
    }

    public Row getRow(int rowId)
    {
        return rows[rowId];
    }

    public Object getColumnValue(Object row, int columnIndex)
    {
        Row instance = (Row) row;
        instance.resolve();

        switch (columnIndex)
        {
            case 0:
                return instance.objectAddress;
            case 1:
                return instance.className;
            case 2:
                return instance.preview;
            case 3:
                return instance.shallowHeap;
            case 4:
                return instance.retainedHeap;
            default:
                return null;
        }
    }

    public IContextObject getContext(Object row)
    {
        final int objectId = ((Row) row).objectId;
        return new IContextObject()
        {
            public int getObjectId()
            {
                return objectId;
            }
        };
    }
}
