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

import java.util.List;

import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.IContextObject;
import org.eclipse.mat.query.IResultTable;
import org.eclipse.mat.query.ResultMetaData;
import org.eclipse.mat.snapshot.ClassHistogramRecord;

final class ObjectsByClassResult implements IResultTable
{
    private static final Column[] COLUMNS = new Column[] {
                    new Column("Class", String.class).noTotals(), //$NON-NLS-1$
                    new Column("Objects", long.class).noTotals(), //$NON-NLS-1$
                    new Column("Shallow Size", Bytes.class).noTotals(), //$NON-NLS-1$
                    new Column("Retained Size", Bytes.class).noTotals() //$NON-NLS-1$
    };

    private final List<ClassHistogramRecord> rows;

    ObjectsByClassResult(List<ClassHistogramRecord> rows)
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
        return rows.size();
    }

    public Object getRow(int rowId)
    {
        return rows.get(rowId);
    }

    public Object getColumnValue(Object row, int columnIndex)
    {
        ClassHistogramRecord record = (ClassHistogramRecord) row;
        switch (columnIndex)
        {
            case 0:
                return record.getLabel();
            case 1:
                return Long.valueOf(record.getNumberOfObjects());
            case 2:
                return new Bytes(record.getUsedHeapSize());
            case 3:
                return new Bytes(record.getRetainedHeapSize());
            default:
                return null;
        }
    }

    public IContextObject getContext(Object row)
    {
        final int classId = ((ClassHistogramRecord) row).getClassId();
        if (classId < 0)
            return null;

        return new IContextObject()
        {
            public int getObjectId()
            {
                return classId;
            }
        };
    }
}
