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
import java.util.Collections;
import java.util.List;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.IContextObject;
import org.eclipse.mat.query.IResult;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.model.IStackFrame;
import org.eclipse.mat.snapshot.model.IThreadStack;
import org.eclipse.mat.snapshot.query.SnapshotQuery;
import org.eclipse.mat.util.IProgressListener;

final class ThreadsResultBuilder
{
    private static final String STATE_FALLBACK_LABEL = "State"; //$NON-NLS-1$

    public ThreadsResult build(ISnapshot snapshot, IProgressListener listener, int limit) throws Exception
    {
        IResult result = SnapshotQuery.lookup("thread_overview", snapshot).execute(listener); //$NON-NLS-1$
        if (result == null)
        {
            return new ThreadsResult(ThreadsResult.NOTICE, new ThreadsResult.Summary(0, 0, 0, 0),
                            Collections.<ThreadsResult.ThreadEntry>emptyList(), false);
        }
        if (!(result instanceof IResultTree))
            throw new SnapshotException("thread_overview returned unsupported result type: " + result.getClass().getName()); //$NON-NLS-1$

        IResultTree tree = (IResultTree) result;
        List<?> elements = tree.getElements();
        List<ThreadsResult.ThreadEntry> rows = new ArrayList<ThreadsResult.ThreadEntry>();
        int stateColumn = findStateColumn(tree.getColumns());
        int limitToApply = Math.max(0, limit);
        boolean truncated = false;
        int stackAvailableThreads = 0;
        int stateAvailableThreads = 0;

        for (Object element : elements)
        {
            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();

            if (rows.size() >= limitToApply)
            {
                truncated = true;
                break;
            }

            IContextObject context = tree.getContext(element);
            if (context == null || context.getObjectId() < 0)
                continue;

            int objectId = context.getObjectId();
            IObject threadObject = snapshot.getObject(objectId);
            String state = stateValue(tree, element, stateColumn);
            ThreadsResult.ThreadEntry entry = toEntry(snapshot, threadObject, state);
            rows.add(entry);
            if (!ThreadsResult.UNAVAILABLE.equals(state))
                stateAvailableThreads++;
            if (entry.isStackAvailable())
                stackAvailableThreads++;
        }

        ThreadsResult.Summary summary = new ThreadsResult.Summary(elements.size(), rows.size(), stackAvailableThreads,
                        stateAvailableThreads);
        return new ThreadsResult(ThreadsResult.NOTICE, summary, rows, truncated);
    }

    private ThreadsResult.ThreadEntry toEntry(ISnapshot snapshot, IObject threadObject, String state)
    {
        String name = threadObject.getClassSpecificName();
        if (name == null || name.length() == 0)
            name = threadObject.getTechnicalName();

        List<String> stackFrames = new ArrayList<String>();
        boolean stackAvailable = false;
        String stackUnavailableReason = ThreadsResult.STACK_UNAVAILABLE_REASON;
        try
        {
            IThreadStack stack = snapshot.getThreadStack(threadObject.getObjectId());
            if (stack != null)
            {
                for (IStackFrame frame : stack.getStackFrames())
                {
                    stackFrames.add(frame.getText());
                }
                stackAvailable = true;
                stackUnavailableReason = null;
            }
        }
        catch (SnapshotException e)
        {
            if (e.getLocalizedMessage() != null && e.getLocalizedMessage().length() > 0)
                stackUnavailableReason = e.getLocalizedMessage();
        }
        catch (RuntimeException e)
        {
            if (e.getLocalizedMessage() != null && e.getLocalizedMessage().length() > 0)
                stackUnavailableReason = e.getLocalizedMessage();
        }

        return new ThreadsResult.ThreadEntry(threadObject.getObjectId(), name, threadObject.getTechnicalName(),
                        "0x" + Long.toHexString(threadObject.getObjectAddress()), state, //$NON-NLS-1$
                        threadObject.getRetainedHeapSize(), stackAvailable, stackUnavailableReason, stackFrames);
    }

    private int findStateColumn(Column[] columns)
    {
        if (columns == null)
            return -1;

        for (int ii = 0; ii < columns.length; ii++)
        {
            String label = columns[ii].getLabel();
            if (STATE_FALLBACK_LABEL.equalsIgnoreCase(label))
                return ii;
        }
        return -1;
    }

    private String stateValue(IResultTree tree, Object row, int stateColumn)
    {
        if (stateColumn < 0)
            return ThreadsResult.UNAVAILABLE;

        Object value = tree.getColumnValue(row, stateColumn);
        if (value == null)
            return ThreadsResult.UNAVAILABLE;

        String text = String.valueOf(value).trim();
        return text.length() == 0 ? ThreadsResult.UNAVAILABLE : text;
    }
}
