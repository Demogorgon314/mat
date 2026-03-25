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

import org.eclipse.mat.query.results.DisplayFileResult;

public class DisplayFileResultSerializer
{
    public String toText(DisplayFileResult result)
    {
        return "Generated report file: " + path(result); //$NON-NLS-1$
    }

    public String toMarkdown(DisplayFileResult result)
    {
        return "Generated report file: " + MarkdownDocument.inlineCode(path(result)); //$NON-NLS-1$
    }

    private String path(DisplayFileResult result)
    {
        return result == null ? "<no file>" : result.toString(); //$NON-NLS-1$
    }
}
