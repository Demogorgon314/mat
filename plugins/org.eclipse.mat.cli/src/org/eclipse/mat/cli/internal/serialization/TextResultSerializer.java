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

import org.eclipse.mat.query.results.TextResult;

public class TextResultSerializer
{
    public boolean writeJson(JsonWriter writer, TextResult result)
    {
        writer.name("text").value(toText(result)); //$NON-NLS-1$
        return false;
    }

    public String toText(TextResult result)
    {
        if (!result.isHtml())
            return result.getText();

        String text = result.getText();
        text = text.replaceAll("<b>", "").replace("</b>", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        text = text.replaceAll("<strong>", "").replace("</strong>", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        text = text.replaceAll("<q>", "\"").replace("</q>", "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        text = text.replaceAll("<br>", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
        text = text.replaceAll("<br/>", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
        text = text.replaceAll("<p>", "\n").replaceAll("</p>", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        text = text.replaceAll("<a [^>]+>", "").replaceAll("</a>", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        text = text.replaceAll("<ul [^>]+>", "\n").replaceAll("</ul>", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        text = text.replaceAll("<li>", "").replace("</li>", "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        text = text.replaceAll("&quot;", "\""); //$NON-NLS-1$ //$NON-NLS-2$
        text = text.replaceAll("&apos;", "'"); //$NON-NLS-1$ //$NON-NLS-2$
        text = text.replaceAll("&lt;", "<"); //$NON-NLS-1$ //$NON-NLS-2$
        text = text.replaceAll("&gt;", ">"); //$NON-NLS-1$ //$NON-NLS-2$
        text = text.replaceAll("&amp;", "&"); //$NON-NLS-1$ //$NON-NLS-2$
        return text;
    }
}
