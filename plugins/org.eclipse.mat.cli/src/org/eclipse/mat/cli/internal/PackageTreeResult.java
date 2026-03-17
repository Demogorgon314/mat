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

import org.eclipse.mat.query.IResult;
import org.eclipse.mat.query.ResultMetaData;

public final class PackageTreeResult implements IResult
{
    public static final class Node
    {
        private final String name;
        private final long retainedBytes;
        private final double retainedPercent;
        private final long topDominators;
        private final List<Node> children;

        public Node(String name, long retainedBytes, double retainedPercent, long topDominators, List<Node> children)
        {
            this.name = name;
            this.retainedBytes = retainedBytes;
            this.retainedPercent = retainedPercent;
            this.topDominators = topDominators;
            this.children = children == null ? Collections.<Node>emptyList()
                            : Collections.unmodifiableList(new ArrayList<Node>(children));
        }

        public String getName()
        {
            return name;
        }

        public long getRetainedBytes()
        {
            return retainedBytes;
        }

        public double getRetainedPercent()
        {
            return retainedPercent;
        }

        public long getTopDominators()
        {
            return topDominators;
        }

        public List<Node> getChildren()
        {
            return children;
        }
    }

    private final Node root;

    public PackageTreeResult(Node root)
    {
        this.root = root;
    }

    public Node getRoot()
    {
        return root;
    }

    public ResultMetaData getResultMetaData()
    {
        return null;
    }
}
