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

import java.util.Collections;
import java.util.List;

import org.eclipse.mat.query.IResult;
import org.eclipse.mat.query.ResultMetaData;

public final class TopConsumersResult implements IResult
{
    public static final class ObjectRow
    {
        private final int objectId;
        private final String label;
        private final long retainedBytes;
        private final double retainedPercent;

        public ObjectRow(int objectId, String label, long retainedBytes, double retainedPercent)
        {
            this.objectId = objectId;
            this.label = label;
            this.retainedBytes = retainedBytes;
            this.retainedPercent = retainedPercent;
        }

        public int getObjectId()
        {
            return objectId;
        }

        public String getLabel()
        {
            return label;
        }

        public long getRetainedBytes()
        {
            return retainedBytes;
        }

        public double getRetainedPercent()
        {
            return retainedPercent;
        }
    }

    public static final class DominatorRow
    {
        private final int objectId;
        private final String label;
        private final long count;
        private final long retainedBytes;
        private final double retainedPercent;

        public DominatorRow(int objectId, String label, long count, long retainedBytes, double retainedPercent)
        {
            this.objectId = objectId;
            this.label = label;
            this.count = count;
            this.retainedBytes = retainedBytes;
            this.retainedPercent = retainedPercent;
        }

        public int getObjectId()
        {
            return objectId;
        }

        public String getLabel()
        {
            return label;
        }

        public long getCount()
        {
            return count;
        }

        public long getRetainedBytes()
        {
            return retainedBytes;
        }

        public double getRetainedPercent()
        {
            return retainedPercent;
        }
    }

    public static final class PackageNode
    {
        private final String name;
        private final long retainedBytes;
        private final double retainedPercent;
        private final long topDominators;
        private final List<PackageNode> children;

        public PackageNode(String name, long retainedBytes, double retainedPercent, long topDominators,
                        List<PackageNode> children)
        {
            this.name = name;
            this.retainedBytes = retainedBytes;
            this.retainedPercent = retainedPercent;
            this.topDominators = topDominators;
            this.children = children == null ? Collections.<PackageNode>emptyList() : children;
        }

        public String getName()
        {
            return name;
        }

        public long getRetainedBytes()
        {
            return retainedBytes;
        }

        public double getRetainedPercent()
        {
            return retainedPercent;
        }

        public long getTopDominators()
        {
            return topDominators;
        }

        public List<PackageNode> getChildren()
        {
            return children;
        }
    }

    private final long totalRetainedHeap;
    private final List<ObjectRow> biggestObjects;
    private final List<DominatorRow> classes;
    private final List<DominatorRow> classLoaders;
    private final PackageNode packages;

    public TopConsumersResult(long totalRetainedHeap, List<ObjectRow> biggestObjects, List<DominatorRow> classes,
                    List<DominatorRow> classLoaders, PackageNode packages)
    {
        this.totalRetainedHeap = totalRetainedHeap;
        this.biggestObjects = biggestObjects == null ? Collections.<ObjectRow>emptyList() : biggestObjects;
        this.classes = classes == null ? Collections.<DominatorRow>emptyList() : classes;
        this.classLoaders = classLoaders == null ? Collections.<DominatorRow>emptyList() : classLoaders;
        this.packages = packages;
    }

    public long getTotalRetainedHeap()
    {
        return totalRetainedHeap;
    }

    public List<ObjectRow> getBiggestObjects()
    {
        return biggestObjects;
    }

    public List<DominatorRow> getClasses()
    {
        return classes;
    }

    public List<DominatorRow> getClassLoaders()
    {
        return classLoaders;
    }

    public PackageNode getPackages()
    {
        return packages;
    }

    public ResultMetaData getResultMetaData()
    {
        return null;
    }
}
