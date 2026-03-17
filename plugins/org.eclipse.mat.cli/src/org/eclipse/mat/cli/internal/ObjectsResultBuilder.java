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
import org.eclipse.mat.util.IProgressListener;

final class ObjectsResultBuilder
{
    private static final Comparator<ClassHistogramRecord> CLASS_ORDER = Comparator
                    .comparingLong((ClassHistogramRecord record) -> retainedSortValue(record)).reversed()
                    .thenComparing(Comparator.comparingLong(ClassHistogramRecord::getNumberOfObjects).reversed())
                    .thenComparing(ClassHistogramRecord::getLabel, Comparator.nullsFirst(String::compareTo));
    private static final Comparator<ObjectsByClassLoaderResult.Row> CLASS_LOADER_ORDER = Comparator
                    .comparingLong((ObjectsByClassLoaderResult.Row row) -> retainedSortValue(row.getRecord())).reversed()
                    .thenComparing(Comparator.comparingLong(
                                    (ObjectsByClassLoaderResult.Row row) -> row.getRecord().getNumberOfObjects())
                                    .reversed())
                    .thenComparing(row -> row.getRecord().getLabel(), Comparator.nullsFirst(String::compareTo));
    private static final Comparator<MutableNode> PACKAGE_ORDER = Comparator
                    .comparingLong(MutableNode::getRetainedBytes).reversed()
                    .thenComparing(Comparator.comparingLong(MutableNode::getTopDominators).reversed())
                    .thenComparing(MutableNode::getName);

    public ObjectsByClassResult buildClass(ISnapshot snapshot, String packageName, String classLoaderName,
                    IProgressListener listener)
                    throws SnapshotException
    {
        Histogram histogram = snapshot.getHistogram(listener);
        List<ClassHistogramRecord> rows = new ArrayList<ClassHistogramRecord>();
        if (classLoaderName == null || classLoaderName.length() == 0)
        {
            collectClassRows(snapshot, histogram.getClassHistogramRecords(), packageName, rows, listener);
        }
        else
        {
            for (ClassLoaderHistogramRecord record : histogram.getClassLoaderHistogramRecords())
            {
                if (listener.isCanceled())
                    throw new IProgressListener.OperationCanceledException();
                if (!matchesClassLoader(record.getLabel(), classLoaderName))
                    continue;
                collectClassRows(snapshot, record.getClassHistogramRecords(), packageName, rows, listener);
            }
        }
        rows.sort(CLASS_ORDER);
        return new ObjectsByClassResult(rows);
    }

    public ObjectsByClassLoaderResult buildClassLoader(ISnapshot snapshot, String classLoaderName,
                    IProgressListener listener) throws SnapshotException
    {
        Histogram histogram = snapshot.getHistogram(listener);
        List<ObjectsByClassLoaderResult.Row> rows = new ArrayList<ObjectsByClassLoaderResult.Row>();
        for (ClassLoaderHistogramRecord record : histogram.getClassLoaderHistogramRecords())
        {
            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();
            if (!matchesClassLoader(record.getLabel(), classLoaderName))
                continue;
            record.calculateRetainedSize(snapshot, true, true, listener);
            int classes = 0;
            int classesWithoutInstances = 0;
            for (ClassHistogramRecord classRecord : record.getClassHistogramRecords())
            {
                classes++;
                if (classRecord.getNumberOfObjects() == 0)
                    classesWithoutInstances++;
            }
            rows.add(new ObjectsByClassLoaderResult.Row(record, classes, classesWithoutInstances));
        }
        rows.sort(CLASS_LOADER_ORDER);
        return new ObjectsByClassLoaderResult(rows);
    }

    public PackageTreeResult buildPackage(ISnapshot snapshot, String packageName, IProgressListener listener)
                    throws SnapshotException
    {
        int[] topDominators = snapshot.getImmediateDominatedIds(-1);
        MutableNode root = new MutableNode("<all>"); //$NON-NLS-1$
        root.retainedBytes = snapshot.getSnapshotInfo().getUsedHeapSize();
        root.topDominators = topDominators.length;

        listener.beginTask("Grouping by package", (topDominators.length + 99) / 100); //$NON-NLS-1$
        int index = 0;
        for (int dominatorId : topDominators)
        {
            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();

            long retainedBytes = snapshot.getRetainedHeapSize(dominatorId);
            MutableNode current = root;
            for (String token : packageTokens(snapshot, dominatorId))
            {
                MutableNode child = current.children.get(token);
                if (child == null)
                {
                    child = new MutableNode(token);
                    current.children.put(token, child);
                }
                child.retainedBytes += retainedBytes;
                child.topDominators++;
                current = child;
            }

            if (++index % 100 == 0)
                listener.worked(1);
        }
        listener.done();

        MutableNode selected = selectRoot(root, packageName);
        if (selected == null)
            return new PackageTreeResult(null);

        long totalRetainedHeap = root.retainedBytes;
        return new PackageTreeResult(toNode(selected, totalRetainedHeap));
    }

    private String[] packageTokens(ISnapshot snapshot, int dominatorId) throws SnapshotException
    {
        IClass objectClass = snapshot.getClassOf(dominatorId);
        if (IClass.JAVA_LANG_CLASS.equals(objectClass.getName()))
        {
            IObject dominatorObject = snapshot.getObject(dominatorId);
            if (dominatorObject instanceof IClass)
                objectClass = (IClass) dominatorObject;
        }

        StringTokenizer tokenizer = new StringTokenizer(objectClass.getName(), "."); //$NON-NLS-1$
        List<String> tokens = new ArrayList<String>();
        while (tokenizer.hasMoreTokens())
        {
            tokens.add(tokenizer.nextToken());
        }
        return tokens.toArray(new String[0]);
    }

    private MutableNode selectRoot(MutableNode root, String packageName)
    {
        if (packageName == null || packageName.length() == 0)
            return root;

        String[] segments = packageName.split("\\."); //$NON-NLS-1$
        MutableNode current = root;
        for (String segment : segments)
        {
            current = current.children.get(segment);
            if (current == null)
                return null;
        }
        return current.copyWithName(packageName);
    }

    private PackageTreeResult.Node toNode(MutableNode source, long totalRetainedHeap)
    {
        List<MutableNode> children = new ArrayList<MutableNode>(source.children.values());
        children.sort(PACKAGE_ORDER);

        List<PackageTreeResult.Node> builtChildren = new ArrayList<PackageTreeResult.Node>(children.size());
        for (MutableNode child : children)
        {
            builtChildren.add(toNode(child, totalRetainedHeap));
        }
        return new PackageTreeResult.Node(source.name, source.retainedBytes,
                        totalRetainedHeap == 0 ? 0d : source.retainedBytes / (double) totalRetainedHeap,
                        source.topDominators, builtChildren);
    }

    private boolean matchesClassPackage(String className, String packageName)
    {
        if (packageName == null || packageName.length() == 0)
            return true;
        int lastDot = className == null ? -1 : className.lastIndexOf('.');
        if (lastDot < 0)
            return false;
        String classPackage = className.substring(0, lastDot);
        return classPackage.equals(packageName) || classPackage.startsWith(packageName + '.');
    }

    private boolean matchesClassLoader(String label, String filter)
    {
        return filter == null || filter.length() == 0 || (label != null && label.contains(filter));
    }

    private void collectClassRows(ISnapshot snapshot, Collection<ClassHistogramRecord> source, String packageName,
                    List<ClassHistogramRecord> rows, IProgressListener listener) throws SnapshotException
    {
        for (ClassHistogramRecord record : source)
        {
            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();
            if (record.getNumberOfObjects() <= 0)
                continue;
            if (!matchesClassPackage(record.getLabel(), packageName))
                continue;
            record.calculateRetainedSize(snapshot, true, true, listener);
            rows.add(record);
        }
    }

    private static long retainedSortValue(ClassHistogramRecord record)
    {
        return retainedSortValue(record.getRetainedHeapSize());
    }

    private static long retainedSortValue(ClassLoaderHistogramRecord record)
    {
        return retainedSortValue(record.getRetainedHeapSize());
    }

    private static long retainedSortValue(long retainedBytes)
    {
        return retainedBytes < 0 ? -retainedBytes : retainedBytes;
    }

    private static final class MutableNode
    {
        private final String name;
        private final Map<String, MutableNode> children = new HashMap<String, MutableNode>();
        private long retainedBytes;
        private long topDominators;

        private MutableNode(String name)
        {
            this.name = name;
        }

        private MutableNode copyWithName(String renamed)
        {
            MutableNode copy = new MutableNode(renamed);
            copy.retainedBytes = retainedBytes;
            copy.topDominators = topDominators;
            copy.children.putAll(children);
            return copy;
        }

        private String getName()
        {
            return name;
        }

        private long getRetainedBytes()
        {
            return retainedBytes;
        }

        private long getTopDominators()
        {
            return topDominators;
        }
    }
}
