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

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.IContextObject;
import org.eclipse.mat.query.IDecorator;
import org.eclipse.mat.query.IResultTable;
import org.eclipse.mat.query.ResultMetaData;

final class BiggestObjectsResult implements IResultTable
{
    private static final class LevelIndentDecorator implements IDecorator
    {
        public String prefix(Object row)
        {
            Row biggest = (Row) row;
            int indent = (biggest.getLevel() - 1) * 2;
            if (indent <= 0)
                return null;

            int prefixLength = indent - 1;
            if (prefixLength <= 0)
                return " "; //$NON-NLS-1$

            StringBuilder builder = new StringBuilder(prefixLength);
            for (int ii = 0; ii < prefixLength; ii++)
            {
                builder.append(' ');
            }
            return builder.toString();
        }

        public String suffix(Object row)
        {
            return null;
        }
    }

    static final class Row
    {
        private final int objectId;
        private final String name;
        private final long retainedBytes;
        private final long shallowBytes;
        private final double retainedPercent;
        private final int level;

        Row(int objectId, String name, long retainedBytes, long shallowBytes, double retainedPercent, int level)
        {
            this.objectId = objectId;
            this.name = name;
            this.retainedBytes = retainedBytes;
            this.shallowBytes = shallowBytes;
            this.retainedPercent = retainedPercent;
            this.level = level;
        }

        int getObjectId()
        {
            return objectId;
        }

        String getName()
        {
            return name;
        }

        long getRetainedBytes()
        {
            return retainedBytes;
        }

        long getShallowBytes()
        {
            return shallowBytes;
        }

        double getRetainedPercent()
        {
            return retainedPercent;
        }

        int getLevel()
        {
            return level;
        }
    }

    private static NumberFormat percentFormatter()
    {
        NumberFormat formatter = NumberFormat.getPercentInstance(Locale.ENGLISH);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return formatter;
    }

    private static final Column[] COLUMNS = new Column[] {
                    new Column("Name", String.class).decorator(new LevelIndentDecorator()).noTotals(), //$NON-NLS-1$
                    new Column("Retained Size", Bytes.class).noTotals(), //$NON-NLS-1$
                    new Column("Shallow Size", Bytes.class).noTotals(), //$NON-NLS-1$
                    new Column("Retained %", double.class).formatting(percentFormatter()).noTotals(), //$NON-NLS-1$
                    new Column("Level", int.class).noTotals() //$NON-NLS-1$
    };

    private final List<Row> rows;

    BiggestObjectsResult(List<Row> rows)
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
        Row biggest = (Row) row;
        switch (columnIndex)
        {
            case 0:
                return biggest.getName();
            case 1:
                return new Bytes(biggest.getRetainedBytes());
            case 2:
                return new Bytes(biggest.getShallowBytes());
            case 3:
                return Double.valueOf(biggest.getRetainedPercent());
            case 4:
                return Integer.valueOf(biggest.getLevel());
            default:
                return null;
        }
    }

    public IContextObject getContext(Object row)
    {
        final int objectId = ((Row) row).getObjectId();
        if (objectId < 0)
            return null;

        return new IContextObject()
        {
            public int getObjectId()
            {
                return objectId;
            }
        };
    }
}
