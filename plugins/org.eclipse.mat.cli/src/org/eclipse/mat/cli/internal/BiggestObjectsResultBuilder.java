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
import java.util.Comparator;
import java.util.List;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.util.IProgressListener;

final class BiggestObjectsResultBuilder
{
    private static final Comparator<BiggestObjectsResult.Row> ORDER = Comparator
                    .comparingLong(BiggestObjectsResult.Row::getRetainedBytes).reversed()
                    .thenComparingInt(BiggestObjectsResult.Row::getLevel)
                    .thenComparing(BiggestObjectsResult.Row::getName, Comparator.nullsFirst(String::compareTo));

    public BiggestObjectsResult build(ISnapshot snapshot, int depth, IProgressListener listener) throws SnapshotException
    {
        List<BiggestObjectsResult.Row> rows = new ArrayList<BiggestObjectsResult.Row>();
        long totalUsedHeap = snapshot.getSnapshotInfo().getUsedHeapSize();
        collect(snapshot, snapshot.getImmediateDominatedIds(-1), 1, depth, totalUsedHeap, rows, listener);
        rows.sort(ORDER);
        return new BiggestObjectsResult(rows);
    }

    private void collect(ISnapshot snapshot, int[] objectIds, int level, int maxDepth, long totalUsedHeap,
                    List<BiggestObjectsResult.Row> rows, IProgressListener listener) throws SnapshotException
    {
        if (objectIds == null || level > maxDepth)
            return;

        for (int objectId : objectIds)
        {
            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();

            long retainedBytes = snapshot.getRetainedHeapSize(objectId);
            IObject object = snapshot.getObject(objectId);
            rows.add(new BiggestObjectsResult.Row(objectId, object.getDisplayName(), retainedBytes,
                            object.getUsedHeapSize(), totalUsedHeap == 0 ? 0d : retainedBytes / (double) totalUsedHeap,
                            level));

            if (level < maxDepth)
                collect(snapshot, snapshot.getImmediateDominatedIds(objectId), level + 1, maxDepth, totalUsedHeap, rows,
                                listener);
        }
    }
}
