/*******************************************************************************
 * Copyright (c) 2026 Eclipse Memory Analyzer Project.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.mat.cli.internal.serialization;

import java.util.List;

import org.eclipse.mat.cli.internal.ThreadsResult;
import org.eclipse.mat.query.BytesFormat;

public class ThreadsResultSerializer
{
    private static final String AVAILABLE = "available"; //$NON-NLS-1$
    private static final String UNAVAILABLE = "unavailable"; //$NON-NLS-1$
    private static final String EMPTY_STACK = "<empty stack>"; //$NON-NLS-1$

    private final BytesFormat bytesFormatter = BytesFormat.getInstance();

    public boolean writeJson(JsonWriter writer, ThreadsResult result)
    {
        writeStringField(writer, "notice", result.getNotice()); //$NON-NLS-1$
        writeSummary(writer, result.getSummary());
        writer.name("threads").beginArray(); //$NON-NLS-1$
        for (ThreadsResult.ThreadEntry entry : result.getThreads())
        {
            writer.beginObject();
            writer.name("name").value(entry.getName()); //$NON-NLS-1$
            writeStringField(writer, "technicalName", entry.getTechnicalName()); //$NON-NLS-1$
            writeStringField(writer, "objectAddress", entry.getObjectAddress()); //$NON-NLS-1$
            writeStringField(writer, "state", entry.getState()); //$NON-NLS-1$
            writer.name("retainedBytes").value(entry.getRetainedBytes()); //$NON-NLS-1$
            writer.name("stackAvailable").value(entry.isStackAvailable()); //$NON-NLS-1$
            if (!entry.isStackAvailable())
            {
                writeStringField(writer, "stackUnavailableReason", entry.getStackUnavailableReason()); //$NON-NLS-1$
            }
            if (!entry.getStackFrames().isEmpty())
            {
                writer.name("stackFrames").beginArray(); //$NON-NLS-1$
                for (String frame : entry.getStackFrames())
                {
                    writer.value(frame);
                }
                writer.endArray();
            }
            writer.endObject();
        }
        writer.endArray();
        return result.isTruncated();
    }

    public String toText(ThreadsResult result)
    {
        StringBuilder builder = new StringBuilder(4096);
        builder.append("Threads\n"); //$NON-NLS-1$
        builder.append(result.getNotice()).append('\n').append('\n');

        builder.append("Summary\n"); //$NON-NLS-1$
        builder.append("  Total threads: ").append(result.getSummary().getTotalThreads()).append('\n'); //$NON-NLS-1$
        builder.append("  Returned threads: ").append(result.getSummary().getReturnedThreads()).append('\n'); //$NON-NLS-1$
        builder.append("  Stacks available: ").append(result.getSummary().getStackAvailableThreads()).append('\n'); //$NON-NLS-1$
        builder.append("  States available: ").append(result.getSummary().getStateAvailableThreads()).append('\n'); //$NON-NLS-1$
        builder.append('\n');

        builder.append("Overview\n"); //$NON-NLS-1$
        appendOverview(builder, result.getThreads());

        if (!result.getThreads().isEmpty())
            builder.append('\n');

        for (int ii = 0; ii < result.getThreads().size(); ii++)
        {
            if (ii > 0)
                builder.append('\n');
            appendThreadSection(builder, result.getThreads().get(ii));
        }

        return builder.toString();
    }

    private void writeSummary(JsonWriter writer, ThreadsResult.Summary summary)
    {
        writer.name("summary").beginObject(); //$NON-NLS-1$
        writer.name("totalThreads").value(summary.getTotalThreads()); //$NON-NLS-1$
        writer.name("returnedThreads").value(summary.getReturnedThreads()); //$NON-NLS-1$
        writer.name("stackAvailableThreads").value(summary.getStackAvailableThreads()); //$NON-NLS-1$
        writer.name("stateAvailableThreads").value(summary.getStateAvailableThreads()); //$NON-NLS-1$
        writer.endObject();
    }

    private void writeStringField(JsonWriter writer, String name, String value)
    {
        if (value == null || value.length() == 0)
            return;
        writer.name(name).value(value);
    }

    private void appendOverview(StringBuilder builder, List<ThreadsResult.ThreadEntry> threads)
    {
        if (threads.isEmpty())
        {
            builder.append("  No threads found.\n"); //$NON-NLS-1$
            return;
        }

        int threadWidth = "Thread".length(); //$NON-NLS-1$
        int stateWidth = "State".length(); //$NON-NLS-1$
        int retainedWidth = "Retained Heap".length(); //$NON-NLS-1$
        int addressWidth = "Object Address".length(); //$NON-NLS-1$
        int stackWidth = "Stack".length(); //$NON-NLS-1$

        for (ThreadsResult.ThreadEntry entry : threads)
        {
            threadWidth = Math.max(threadWidth, entry.getName().length());
            stateWidth = Math.max(stateWidth, entry.getState().length());
            retainedWidth = Math.max(retainedWidth, formatBytes(entry.getRetainedBytes()).length());
            addressWidth = Math.max(addressWidth, entry.getObjectAddress().length());
            stackWidth = Math.max(stackWidth, stackLabel(entry).length());
        }

        appendRow(builder, "Thread", "State", "Retained Heap", "Object Address", "Stack", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                        threadWidth, stateWidth, retainedWidth, addressWidth, stackWidth);
        for (ThreadsResult.ThreadEntry entry : threads)
        {
            appendRow(builder, entry.getName(), entry.getState(), formatBytes(entry.getRetainedBytes()),
                            entry.getObjectAddress(), stackLabel(entry), threadWidth, stateWidth, retainedWidth,
                            addressWidth, stackWidth);
        }
    }

    private void appendThreadSection(StringBuilder builder, ThreadsResult.ThreadEntry entry)
    {
        builder.append('"').append(entry.getName().replace("\"", "\\\"")).append('"'); //$NON-NLS-1$ //$NON-NLS-2$
        builder.append(" @ ").append(entry.getObjectAddress()).append('\n'); //$NON-NLS-1$
        builder.append("  State: ").append(entry.getState()).append('\n'); //$NON-NLS-1$
        builder.append("  Retained Heap: ").append(formatBytes(entry.getRetainedBytes())).append('\n'); //$NON-NLS-1$
        builder.append("  Stack:"); //$NON-NLS-1$
        if (!entry.isStackAvailable())
        {
            builder.append(' ').append(UNAVAILABLE).append('\n');
            return;
        }

        builder.append('\n');
        if (entry.getStackFrames().isEmpty())
        {
            builder.append("    ").append(EMPTY_STACK).append('\n'); //$NON-NLS-1$
            return;
        }

        for (String frame : entry.getStackFrames())
        {
            builder.append("    ").append(frame).append('\n'); //$NON-NLS-1$
        }
    }

    private void appendRow(StringBuilder builder, String thread, String state, String retained, String address,
                    String stack, int threadWidth, int stateWidth, int retainedWidth, int addressWidth, int stackWidth)
    {
        builder.append(padRight(thread, threadWidth)).append(" | "); //$NON-NLS-1$
        builder.append(padRight(state, stateWidth)).append(" | "); //$NON-NLS-1$
        builder.append(padRight(retained, retainedWidth)).append(" | "); //$NON-NLS-1$
        builder.append(padRight(address, addressWidth)).append(" | "); //$NON-NLS-1$
        builder.append(padRight(stack, stackWidth)).append('\n');
    }

    private String stackLabel(ThreadsResult.ThreadEntry entry)
    {
        return entry.isStackAvailable() ? AVAILABLE : UNAVAILABLE;
    }

    private String padRight(String value, int width)
    {
        StringBuilder builder = new StringBuilder(width);
        builder.append(value);
        while (builder.length() < width)
        {
            builder.append(' ');
        }
        return builder.toString();
    }

    private String formatBytes(long value)
    {
        return bytesFormatter.format(value);
    }
}
