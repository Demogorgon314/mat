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
import org.eclipse.mat.snapshot.ClassLoaderHistogramRecord;

final class ObjectsByClassLoaderResult implements IResultTable
{
    static final class Row
    {
        private final ClassLoaderHistogramRecord record;
        private final int classes;
        private final int classesWithoutInstances;

        Row(ClassLoaderHistogramRecord record, int classes, int classesWithoutInstances)
        {
            this.record = record;
            this.classes = classes;
            this.classesWithoutInstances = classesWithoutInstances;
        }

        ClassLoaderHistogramRecord getRecord()
        {
            return record;
        }

        int getClasses()
        {
            return classes;
        }

        int getClassesWithoutInstances()
        {
            return classesWithoutInstances;
        }
    }

    private static final Column[] COLUMNS = new Column[] {
                    new Column("Class Loader", String.class).noTotals(), //$NON-NLS-1$
                    new Column("Objects", long.class).noTotals(), //$NON-NLS-1$
                    new Column("Shallow Size", Bytes.class).noTotals(), //$NON-NLS-1$
                    new Column("Retained Size", Bytes.class).noTotals(), //$NON-NLS-1$
                    new Column("Classes", int.class).noTotals(), //$NON-NLS-1$
                    new Column("Classes w/o Instances", int.class).noTotals() //$NON-NLS-1$
    };

    private final List<Row> rows;

    ObjectsByClassLoaderResult(List<Row> rows)
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
        Row classLoader = (Row) row;
        switch (columnIndex)
        {
            case 0:
                return classLoader.getRecord().getLabel();
            case 1:
                return Long.valueOf(classLoader.getRecord().getNumberOfObjects());
            case 2:
                return new Bytes(classLoader.getRecord().getUsedHeapSize());
            case 3:
                return new Bytes(classLoader.getRecord().getRetainedHeapSize());
            case 4:
                return Integer.valueOf(classLoader.getClasses());
            case 5:
                return Integer.valueOf(classLoader.getClassesWithoutInstances());
            default:
                return null;
        }
    }

    public IContextObject getContext(Object row)
    {
        final int classLoaderId = ((Row) row).getRecord().getClassLoaderId();
        if (classLoaderId < 0)
            return null;

        return new IContextObject()
        {
            public int getObjectId()
            {
                return classLoaderId;
            }
        };
    }
}
