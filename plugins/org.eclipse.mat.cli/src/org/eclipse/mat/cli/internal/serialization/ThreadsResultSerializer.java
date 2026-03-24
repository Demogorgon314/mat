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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.mat.cli.internal.ThreadsResult;

public class ThreadsResultSerializer
{
    private static final String AVAILABLE = "available"; //$NON-NLS-1$
    private static final String UNAVAILABLE = "unavailable"; //$NON-NLS-1$
    private static final String EMPTY_STACK = "<empty stack>"; //$NON-NLS-1$

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
        return toText(result, new SerializationOptions(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    public String toText(ThreadsResult result, SerializationOptions options)
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
        appendOverview(builder, result.getThreads(), options);

        if (!result.getThreads().isEmpty())
            builder.append('\n');

        for (int ii = 0; ii < result.getThreads().size(); ii++)
        {
            if (ii > 0)
                builder.append('\n');
            appendThreadSection(builder, result.getThreads().get(ii), options);
        }

        return builder.toString();
    }

    public void appendMarkdown(MarkdownDocument document, ThreadsResult result, SerializationOptions options)
    {
        document.addSection("Result", result.getNotice()); //$NON-NLS-1$
        List<String> summary = new ArrayList<String>(4);
        summary.add("Total threads: " + result.getSummary().getTotalThreads()); //$NON-NLS-1$
        summary.add("Returned threads: " + result.getSummary().getReturnedThreads()); //$NON-NLS-1$
        summary.add("Stacks available: " + result.getSummary().getStackAvailableThreads()); //$NON-NLS-1$
        summary.add("States available: " + result.getSummary().getStateAvailableThreads()); //$NON-NLS-1$
        document.addSection("Summary", MarkdownDocument.bullets(summary)); //$NON-NLS-1$
        document.addSection("Overview", overviewMarkdown(result.getThreads(), options)); //$NON-NLS-1$

        for (ThreadsResult.ThreadEntry entry : result.getThreads())
        {
            StringBuilder builder = new StringBuilder();
            List<String> details = new ArrayList<String>(4);
            details.add("State: " + entry.getState()); //$NON-NLS-1$
            details.add("Retained Heap: " + formatBytes(entry.getRetainedBytes(), options)); //$NON-NLS-1$
            if (entry.getTechnicalName() != null && entry.getTechnicalName().length() > 0)
                details.add("Technical Name: " + entry.getTechnicalName()); //$NON-NLS-1$
            if (!entry.isStackAvailable())
            {
                details.add("Stack: unavailable"); //$NON-NLS-1$
                if (entry.getStackUnavailableReason() != null && entry.getStackUnavailableReason().length() > 0)
                    details.add("Reason: " + entry.getStackUnavailableReason()); //$NON-NLS-1$
            }
            builder.append(MarkdownDocument.bullets(details));
            if (entry.isStackAvailable())
            {
                builder.append('\n').append('\n');
                String stack = entry.getStackFrames().isEmpty() ? EMPTY_STACK
                                : MarkdownDocument.joinLines(entry.getStackFrames());
                builder.append(MarkdownDocument.fencedCode("text", stack)); //$NON-NLS-1$
            }
            document.addSection("Thread: \"" + entry.getName().replace("\"", "\\\"") + "\" @ " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            + entry.getObjectAddress(), builder.toString());
        }
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

    private void appendOverview(StringBuilder builder, List<ThreadsResult.ThreadEntry> threads, SerializationOptions options)
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
            retainedWidth = Math.max(retainedWidth, formatBytes(entry.getRetainedBytes(), options).length());
            addressWidth = Math.max(addressWidth, entry.getObjectAddress().length());
            stackWidth = Math.max(stackWidth, stackLabel(entry).length());
        }

        appendRow(builder, "Thread", "State", "Retained Heap", "Object Address", "Stack", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                        threadWidth, stateWidth, retainedWidth, addressWidth, stackWidth);
        for (ThreadsResult.ThreadEntry entry : threads)
        {
            appendRow(builder, entry.getName(), entry.getState(), formatBytes(entry.getRetainedBytes(), options),
                            entry.getObjectAddress(), stackLabel(entry), threadWidth, stateWidth, retainedWidth,
                            addressWidth, stackWidth);
        }
    }

    private String overviewMarkdown(List<ThreadsResult.ThreadEntry> threads, SerializationOptions options)
    {
        if (threads.isEmpty())
            return "- No threads found."; //$NON-NLS-1$

        List<List<String>> rows = new ArrayList<List<String>>(threads.size());
        for (ThreadsResult.ThreadEntry entry : threads)
        {
            List<String> row = new ArrayList<String>(5);
            row.add(entry.getName());
            row.add(entry.getState());
            row.add(formatBytes(entry.getRetainedBytes(), options));
            row.add(entry.getObjectAddress());
            row.add(stackLabel(entry));
            rows.add(row);
        }
        return MarkdownDocument.table(java.util.Arrays.asList("Thread", "State", "Retained Heap", "Object Address", "Stack"), rows); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    }

    private void appendThreadSection(StringBuilder builder, ThreadsResult.ThreadEntry entry, SerializationOptions options)
    {
        builder.append('"').append(entry.getName().replace("\"", "\\\"")).append('"'); //$NON-NLS-1$ //$NON-NLS-2$
        builder.append(" @ ").append(entry.getObjectAddress()).append('\n'); //$NON-NLS-1$
        builder.append("  State: ").append(entry.getState()).append('\n'); //$NON-NLS-1$
        builder.append("  Retained Heap: ").append(formatBytes(entry.getRetainedBytes(), options)).append('\n'); //$NON-NLS-1$
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

    private String formatBytes(long value, SerializationOptions options)
    {
        return options.getBytesFormatter().format(value);
    }
}
