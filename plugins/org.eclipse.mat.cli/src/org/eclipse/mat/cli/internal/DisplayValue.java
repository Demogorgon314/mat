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

public final class DisplayValue
{
    public static final class Metadata
    {
        private final String kind;
        private final Integer length;
        private final Boolean truncated;
        private final String encoding;

        public Metadata(String kind, Integer length, Boolean truncated, String encoding)
        {
            this.kind = kind;
            this.length = length;
            this.truncated = truncated;
            this.encoding = encoding;
        }

        public String getKind()
        {
            return kind;
        }

        public Integer getLength()
        {
            return length;
        }

        public Boolean isTruncated()
        {
            return truncated;
        }

        public String getEncoding()
        {
            return encoding;
        }
    }

    private final String text;
    private final Metadata metadata;

    public DisplayValue(String text)
    {
        this(text, null);
    }

    public DisplayValue(String text, Metadata metadata)
    {
        this.text = text;
        this.metadata = metadata;
    }

    public String getText()
    {
        return text;
    }

    public Metadata getMetadata()
    {
        return metadata;
    }

    @Override
    public String toString()
    {
        return text;
    }
}
