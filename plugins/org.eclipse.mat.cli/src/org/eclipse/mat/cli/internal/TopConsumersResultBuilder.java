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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.snapshot.ClassHistogramRecord;
import org.eclipse.mat.snapshot.ClassLoaderHistogramRecord;
import org.eclipse.mat.snapshot.Histogram;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.model.ObjectComparators;
import org.eclipse.mat.util.IProgressListener;
import org.eclipse.mat.util.SimpleMonitor;

public final class TopConsumersResultBuilder
{
    private static final int THRESHOLD_PERCENT = 1;

    public TopConsumersResult build(ISnapshot snapshot, IProgressListener listener) throws SnapshotException
    {
        SimpleMonitor monitor = new SimpleMonitor("Top Consumers", listener, new int[] { 100, 100, 100 }); //$NON-NLS-1$
        IProgressListener phase = monitor.nextMonitor();

        long totalRetainedHeap = snapshot.getSnapshotInfo().getUsedHeapSize();
        int[] topDominators = snapshot.getImmediateDominatedIds(-1);
        long threshold = THRESHOLD_PERCENT * totalRetainedHeap / 100;

        List<TopConsumersResult.ObjectRow> biggestObjects = collectBiggestObjects(snapshot, topDominators,
                        totalRetainedHeap, threshold, phase);

        if (phase.isCanceled())
            throw new IProgressListener.OperationCanceledException();

        phase = monitor.nextMonitor();
        Histogram histogram = groupByClasses(snapshot, topDominators, phase);
        List<TopConsumersResult.DominatorRow> classes = collectClasses(histogram.getClassHistogramRecords(),
                        totalRetainedHeap, threshold);
        List<TopConsumersResult.DominatorRow> classLoaders = collectClassLoaders(
                        histogram.getClassLoaderHistogramRecords(), totalRetainedHeap, threshold);

        if (phase.isCanceled())
            throw new IProgressListener.OperationCanceledException();

        phase = monitor.nextMonitor();
        MutablePackageNode packageRoot = groupByPackage(snapshot, topDominators, phase);
        packageRoot.retainedBytes = totalRetainedHeap;
        packageRoot.topDominators = topDominators.length;

        listener.done();
        return new TopConsumersResult(totalRetainedHeap, biggestObjects, classes, classLoaders,
                        toPackageNode(packageRoot, totalRetainedHeap, threshold));
    }

    private List<TopConsumersResult.ObjectRow> collectBiggestObjects(ISnapshot snapshot, int[] topDominators,
                    long totalRetainedHeap, long threshold, IProgressListener listener) throws SnapshotException
    {
        List<IObject> suspects = new ArrayList<IObject>();
        for (int dominatorId : topDominators)
        {
            long retainedBytes = snapshot.getRetainedHeapSize(dominatorId);
            if (retainedBytes <= threshold)
                break;

            suspects.add(snapshot.getObject(dominatorId));
            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();
        }

        suspects.sort(ObjectComparators.getComparatorForRetainedHeapSizeDescending());

        List<TopConsumersResult.ObjectRow> rows = new ArrayList<TopConsumersResult.ObjectRow>(suspects.size());
        for (IObject object : suspects)
        {
            long retainedBytes = snapshot.getRetainedHeapSize(object.getObjectId());
            rows.add(new TopConsumersResult.ObjectRow(object.getObjectId(), formatObjectLabel(object), retainedBytes,
                            totalRetainedHeap == 0 ? 0d : retainedBytes / (double) totalRetainedHeap));
        }
        return rows;
    }

    private Histogram groupByClasses(ISnapshot snapshot, int[] dominated, IProgressListener listener)
                    throws SnapshotException
    {
        Histogram histogram = snapshot.getHistogram(dominated, listener);
        if (listener.isCanceled())
            throw new IProgressListener.OperationCanceledException();

        Collection<ClassHistogramRecord> classRecords = histogram.getClassHistogramRecords();
        for (ClassHistogramRecord record : classRecords)
        {
            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();
            record.setRetainedHeapSize(sumRetainedSize(record.getObjectIds(), snapshot));
        }

        Collection<ClassLoaderHistogramRecord> classLoaderRecords = histogram.getClassLoaderHistogramRecords();
        for (ClassLoaderHistogramRecord record : classLoaderRecords)
        {
            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();
            long retainedBytes = 0L;
            for (ClassHistogramRecord classRecord : record.getClassHistogramRecords())
            {
                retainedBytes += classRecord.getRetainedHeapSize();
            }
            record.setRetainedHeapSize(retainedBytes);
        }

        return histogram;
    }

    private List<TopConsumersResult.DominatorRow> collectClasses(Collection<ClassHistogramRecord> records,
                    long totalRetainedHeap, long threshold)
    {
        ClassHistogramRecord[] sorted = records.toArray(new ClassHistogramRecord[0]);
        Arrays.sort(sorted, Histogram.reverseComparator(Histogram.COMPARATOR_FOR_RETAINEDHEAPSIZE));

        List<TopConsumersResult.DominatorRow> rows = new ArrayList<TopConsumersResult.DominatorRow>();
        for (ClassHistogramRecord record : sorted)
        {
            long retainedBytes = record.getRetainedHeapSize();
            if (retainedBytes <= threshold)
                break;

            rows.add(new TopConsumersResult.DominatorRow(record.getClassId(), record.getLabel(),
                            record.getNumberOfObjects(), retainedBytes,
                            totalRetainedHeap == 0 ? 0d : retainedBytes / (double) totalRetainedHeap));
        }
        return rows;
    }

    private List<TopConsumersResult.DominatorRow> collectClassLoaders(Collection<ClassLoaderHistogramRecord> records,
                    long totalRetainedHeap, long threshold)
    {
        ClassLoaderHistogramRecord[] sorted = records.toArray(new ClassLoaderHistogramRecord[0]);
        Arrays.sort(sorted, Histogram.reverseComparator(Histogram.COMPARATOR_FOR_RETAINEDHEAPSIZE));

        List<TopConsumersResult.DominatorRow> rows = new ArrayList<TopConsumersResult.DominatorRow>();
        for (ClassLoaderHistogramRecord record : sorted)
        {
            long retainedBytes = record.getRetainedHeapSize();
            if (retainedBytes <= threshold)
                break;

            rows.add(new TopConsumersResult.DominatorRow(record.getClassLoaderId(), record.getLabel(),
                            record.getNumberOfObjects(), retainedBytes,
                            totalRetainedHeap == 0 ? 0d : retainedBytes / (double) totalRetainedHeap));
        }
        return rows;
    }

    private long sumRetainedSize(int[] objectIds, ISnapshot snapshot) throws SnapshotException
    {
        long sum = 0L;
        for (int objectId : objectIds)
        {
            sum += snapshot.getRetainedHeapSize(objectId);
        }
        return sum;
    }

    private MutablePackageNode groupByPackage(ISnapshot snapshot, int[] dominators, IProgressListener listener)
                    throws SnapshotException
    {
        MutablePackageNode root = new MutablePackageNode("<all>"); //$NON-NLS-1$
        listener.beginTask("Grouping by package", (dominators.length + 999) / 1000); //$NON-NLS-1$

        int counter = 0;
        for (int dominatorId : dominators)
        {
            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();

            long retainedBytes = snapshot.getRetainedHeapSize(dominatorId);
            MutablePackageNode current = root;

            IClass objectClass = snapshot.getClassOf(dominatorId);
            if (IClass.JAVA_LANG_CLASS.equals(objectClass.getName()))
            {
                IObject dominatorObject = snapshot.getObject(dominatorId);
                if (dominatorObject instanceof IClass)
                    objectClass = (IClass) dominatorObject;
            }

            StringTokenizer tokenizer = new StringTokenizer(objectClass.getName(), "."); //$NON-NLS-1$
            while (tokenizer.hasMoreTokens())
            {
                String segment = tokenizer.nextToken();
                MutablePackageNode child = current.children.get(segment);
                if (child == null)
                {
                    child = new MutablePackageNode(segment);
                    current.children.put(segment, child);
                }
                child.topDominators++;
                child.retainedBytes += retainedBytes;
                current = child;
            }

            if (++counter % 1000 == 0)
                listener.worked(1);
        }

        listener.done();
        return root;
    }

    private TopConsumersResult.PackageNode toPackageNode(MutablePackageNode source, long totalRetainedHeap, long threshold)
    {
        List<MutablePackageNode> children = new ArrayList<MutablePackageNode>(source.children.values());
        children.sort(Comparator.comparingLong(MutablePackageNode::getRetainedBytes).reversed()
                        .thenComparing(MutablePackageNode::getName));

        List<TopConsumersResult.PackageNode> filteredChildren = new ArrayList<TopConsumersResult.PackageNode>();
        for (MutablePackageNode child : children)
        {
            if (child.retainedBytes < threshold)
                break;
            filteredChildren.add(toPackageNode(child, totalRetainedHeap, threshold));
        }

        return new TopConsumersResult.PackageNode(source.name, source.retainedBytes,
                        totalRetainedHeap == 0 ? 0d : source.retainedBytes / (double) totalRetainedHeap,
                        source.topDominators, filteredChildren);
    }

    private String formatObjectLabel(IObject object)
    {
        StringBuilder builder = new StringBuilder(object.getTechnicalName());
        String details = object.getClassSpecificName();
        if (details != null)
            builder.append("  ").append(details); //$NON-NLS-1$
        return builder.toString();
    }

    private static final class MutablePackageNode
    {
        private final String name;
        private final Map<String, MutablePackageNode> children = new HashMap<String, MutablePackageNode>();
        private long retainedBytes;
        private long topDominators;

        private MutablePackageNode(String name)
        {
            this.name = name;
        }

        private String getName()
        {
            return name;
        }

        private long getRetainedBytes()
        {
            return retainedBytes;
        }
    }
}
