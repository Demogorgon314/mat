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

public final class SerializationOptions
{
    private final int limit;
    private final int treeDepthLimit;
    private final int treeNodeLimit;

    public SerializationOptions(int limit, int treeDepthLimit)
    {
        this(limit, treeDepthLimit, Math.max(100, limit * 10));
    }

    public SerializationOptions(int limit, int treeDepthLimit, int treeNodeLimit)
    {
        this.limit = limit;
        this.treeDepthLimit = treeDepthLimit;
        this.treeNodeLimit = treeNodeLimit;
    }

    public int getLimit()
    {
        return limit;
    }

    public int getTreeDepthLimit()
    {
        return treeDepthLimit;
    }

    public int getTreeNodeLimit()
    {
        return treeNodeLimit;
    }
}
