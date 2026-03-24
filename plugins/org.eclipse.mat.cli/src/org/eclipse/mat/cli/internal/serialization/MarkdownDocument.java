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
import java.util.Arrays;
import java.util.List;

public final class MarkdownDocument
{
    private static final class Section
    {
        private final String title;
        private final String content;

        private Section(String title, String content)
        {
            this.title = title;
            this.content = content;
        }
    }

    private final List<Section> sections = new ArrayList<Section>();

    public void addSection(String title, String content)
    {
        if (title == null || title.length() == 0 || content == null)
            return;

        String normalized = trimBlankLines(content);
        if (normalized.length() == 0)
            return;

        sections.add(new Section(title, normalized));
    }

    public void addSection(String title, List<String> lines)
    {
        addSection(title, joinLines(lines));
    }

    public boolean isEmpty()
    {
        return sections.isEmpty();
    }

    public String render()
    {
        StringBuilder builder = new StringBuilder();
        for (Section section : sections)
        {
            if (builder.length() > 0)
                builder.append('\n');
            builder.append("### ").append(section.title).append('\n'); //$NON-NLS-1$
            builder.append(section.content).append('\n');
        }
        return builder.toString();
    }

    public static String bullets(String... items)
    {
        return bullets(Arrays.asList(items), false);
    }

    public static String bullets(List<String> items)
    {
        return bullets(items, false);
    }

    public static String bullets(List<String> items, boolean codeWrap)
    {
        StringBuilder builder = new StringBuilder();
        for (String item : items)
        {
            if (item == null || item.length() == 0)
                continue;
            builder.append("- "); //$NON-NLS-1$
            builder.append(codeWrap ? inlineCode(item) : item);
            builder.append('\n');
        }
        return trimTrailingNewline(builder.toString());
    }

    public static String fencedCode(String info, String content)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("```"); //$NON-NLS-1$
        if (info != null && info.length() > 0)
            builder.append(info);
        builder.append('\n');
        builder.append(content == null ? "" : trimTrailingNewline(content)); //$NON-NLS-1$
        builder.append('\n').append("```"); //$NON-NLS-1$
        return builder.toString();
    }

    public static String table(List<String> headers, List<List<String>> rows)
    {
        StringBuilder builder = new StringBuilder();
        builder.append('|');
        for (String header : headers)
        {
            builder.append(' ').append(escapeTableCell(header)).append(' ').append('|');
        }
        builder.append('\n').append('|');
        for (int ii = 0; ii < headers.size(); ii++)
        {
            builder.append(" --- |"); //$NON-NLS-1$
        }
        if (rows != null)
        {
            for (List<String> row : rows)
            {
                builder.append('\n').append('|');
                for (int ii = 0; ii < headers.size(); ii++)
                {
                    String value = ii < row.size() ? row.get(ii) : ""; //$NON-NLS-1$
                    builder.append(' ').append(escapeTableCell(value)).append(' ').append('|');
                }
            }
        }
        return builder.toString();
    }

    public static String escapeTableCell(String value)
    {
        if (value == null)
            return ""; //$NON-NLS-1$
        return value.replace("\\", "\\\\").replace("|", "\\|").replace("\r", "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                        .replace("\n", "<br>"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static String inlineCode(String value)
    {
        if (value == null)
            return "``"; //$NON-NLS-1$
        return "`" + value.replace("`", "\\`") + "`"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    public static String joinLines(List<String> lines)
    {
        StringBuilder builder = new StringBuilder();
        if (lines != null)
        {
            for (String line : lines)
            {
                if (line == null)
                    continue;
                builder.append(line).append('\n');
            }
        }
        return trimTrailingNewline(builder.toString());
    }

    private static String trimBlankLines(String value)
    {
        return value.replaceFirst("^(\\s*\\n)+", "").replaceFirst("(\\n\\s*)+$", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    private static String trimTrailingNewline(String value)
    {
        if (value == null)
            return ""; //$NON-NLS-1$
        while (value.endsWith("\n")) //$NON-NLS-1$
        {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
