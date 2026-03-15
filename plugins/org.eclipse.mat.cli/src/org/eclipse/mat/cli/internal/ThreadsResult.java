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

public final class ThreadsResult implements IResult
{
    public static final String NOTICE = "best-effort from heap dump, not a full jstack equivalent"; //$NON-NLS-1$
    public static final String UNAVAILABLE = "unavailable"; //$NON-NLS-1$
    public static final String STACK_UNAVAILABLE_REASON = "No thread stack information present in heap dump"; //$NON-NLS-1$

    public static final class Summary
    {
        private final int totalThreads;
        private final int returnedThreads;
        private final int stackAvailableThreads;
        private final int stateAvailableThreads;

        public Summary(int totalThreads, int returnedThreads, int stackAvailableThreads, int stateAvailableThreads)
        {
            this.totalThreads = totalThreads;
            this.returnedThreads = returnedThreads;
            this.stackAvailableThreads = stackAvailableThreads;
            this.stateAvailableThreads = stateAvailableThreads;
        }

        public int getTotalThreads()
        {
            return totalThreads;
        }

        public int getReturnedThreads()
        {
            return returnedThreads;
        }

        public int getStackAvailableThreads()
        {
            return stackAvailableThreads;
        }

        public int getStateAvailableThreads()
        {
            return stateAvailableThreads;
        }
    }

    public static final class ThreadEntry
    {
        private final int objectId;
        private final String name;
        private final String technicalName;
        private final String objectAddress;
        private final String state;
        private final long retainedBytes;
        private final boolean stackAvailable;
        private final String stackUnavailableReason;
        private final List<String> stackFrames;

        public ThreadEntry(int objectId, String name, String technicalName, String objectAddress, String state,
                        long retainedBytes, boolean stackAvailable, String stackUnavailableReason,
                        List<String> stackFrames)
        {
            this.objectId = objectId;
            this.name = name;
            this.technicalName = technicalName;
            this.objectAddress = objectAddress;
            this.state = state;
            this.retainedBytes = retainedBytes;
            this.stackAvailable = stackAvailable;
            this.stackUnavailableReason = stackUnavailableReason;
            this.stackFrames = stackFrames == null ? Collections.<String>emptyList()
                            : Collections.unmodifiableList(stackFrames);
        }

        public int getObjectId()
        {
            return objectId;
        }

        public String getName()
        {
            return name;
        }

        public String getTechnicalName()
        {
            return technicalName;
        }

        public String getObjectAddress()
        {
            return objectAddress;
        }

        public String getState()
        {
            return state;
        }

        public long getRetainedBytes()
        {
            return retainedBytes;
        }

        public boolean isStackAvailable()
        {
            return stackAvailable;
        }

        public String getStackUnavailableReason()
        {
            return stackUnavailableReason;
        }

        public List<String> getStackFrames()
        {
            return stackFrames;
        }
    }

    private final String notice;
    private final Summary summary;
    private final List<ThreadEntry> threads;
    private final boolean truncated;

    public ThreadsResult(String notice, Summary summary, List<ThreadEntry> threads, boolean truncated)
    {
        this.notice = notice;
        this.summary = summary;
        this.threads = threads == null ? Collections.<ThreadEntry>emptyList() : Collections.unmodifiableList(threads);
        this.truncated = truncated;
    }

    public String getNotice()
    {
        return notice;
    }

    public Summary getSummary()
    {
        return summary;
    }

    public List<ThreadEntry> getThreads()
    {
        return threads;
    }

    public boolean isTruncated()
    {
        return truncated;
    }

    public ResultMetaData getResultMetaData()
    {
        return null;
    }
}
