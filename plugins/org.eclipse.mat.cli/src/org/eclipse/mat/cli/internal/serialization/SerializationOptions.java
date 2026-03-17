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

import org.eclipse.mat.cli.internal.CliExecution.ObjectAddressResolver;
import org.eclipse.mat.query.BytesDisplay;
import org.eclipse.mat.query.BytesFormat;

public final class SerializationOptions
{
    private final int limit;
    private final int treeDepthLimit;
    private final int treeNodeLimit;
    private final boolean agentProfile;
    private final boolean dump;
    private final ObjectAddressResolver objectAddressResolver;
    private final BytesDisplay bytesDisplay;
    private final BytesFormat bytesFormatter;

    public SerializationOptions(int limit, int treeDepthLimit)
    {
        this(limit, treeDepthLimit, defaultTreeNodeLimit(limit), false, false, null, BytesDisplay.Smart);
    }

    public SerializationOptions(int limit, int treeDepthLimit, boolean agentProfile)
    {
        this(limit, treeDepthLimit, defaultTreeNodeLimit(limit), agentProfile, false, null, BytesDisplay.Smart);
    }

    public SerializationOptions(int limit, int treeDepthLimit, ObjectAddressResolver objectAddressResolver)
    {
        this(limit, treeDepthLimit, defaultTreeNodeLimit(limit), false, false, objectAddressResolver,
                        BytesDisplay.Smart);
    }

    public SerializationOptions(int limit, int treeDepthLimit, boolean agentProfile,
                    ObjectAddressResolver objectAddressResolver)
    {
        this(limit, treeDepthLimit, defaultTreeNodeLimit(limit), agentProfile, false, objectAddressResolver,
                        BytesDisplay.Smart);
    }

    public SerializationOptions(int limit, int treeDepthLimit, int treeNodeLimit)
    {
        this(limit, treeDepthLimit, treeNodeLimit, false, false, null, BytesDisplay.Smart);
    }

    public SerializationOptions(int limit, int treeDepthLimit, int treeNodeLimit,
                    ObjectAddressResolver objectAddressResolver)
    {
        this(limit, treeDepthLimit, treeNodeLimit, false, false, objectAddressResolver, BytesDisplay.Smart);
    }

    public SerializationOptions(int limit, int treeDepthLimit, int treeNodeLimit, boolean agentProfile)
    {
        this(limit, treeDepthLimit, treeNodeLimit, agentProfile, false, null, BytesDisplay.Smart);
    }

    public SerializationOptions(int limit, int treeDepthLimit, int treeNodeLimit, boolean agentProfile,
                    ObjectAddressResolver objectAddressResolver)
    {
        this(limit, treeDepthLimit, treeNodeLimit, agentProfile, false, objectAddressResolver, BytesDisplay.Smart);
    }

    public SerializationOptions(int limit, int treeDepthLimit, BytesDisplay bytesDisplay)
    {
        this(limit, treeDepthLimit, defaultTreeNodeLimit(limit), false, false, null, bytesDisplay);
    }

    public SerializationOptions(int limit, int treeDepthLimit, BytesDisplay bytesDisplay, boolean dump)
    {
        this(limit, treeDepthLimit, defaultTreeNodeLimit(limit), false, dump, null, bytesDisplay);
    }

    public SerializationOptions(int limit, int treeDepthLimit, boolean agentProfile,
                    ObjectAddressResolver objectAddressResolver, BytesDisplay bytesDisplay)
    {
        this(limit, treeDepthLimit, defaultTreeNodeLimit(limit), agentProfile, false, objectAddressResolver,
                        bytesDisplay);
    }

    public SerializationOptions(int limit, int treeDepthLimit, boolean agentProfile, boolean dump,
                    ObjectAddressResolver objectAddressResolver, BytesDisplay bytesDisplay)
    {
        this(limit, treeDepthLimit, defaultTreeNodeLimit(limit), agentProfile, dump, objectAddressResolver,
                        bytesDisplay);
    }

    public SerializationOptions(int limit, int treeDepthLimit, int treeNodeLimit, boolean agentProfile,
                    ObjectAddressResolver objectAddressResolver, BytesDisplay bytesDisplay)
    {
        this(limit, treeDepthLimit, treeNodeLimit, agentProfile, false, objectAddressResolver, bytesDisplay);
    }

    public SerializationOptions(int limit, int treeDepthLimit, int treeNodeLimit, boolean agentProfile, boolean dump,
                    ObjectAddressResolver objectAddressResolver, BytesDisplay bytesDisplay)
    {
        this.limit = limit;
        this.treeDepthLimit = treeDepthLimit;
        this.treeNodeLimit = treeNodeLimit;
        this.agentProfile = agentProfile;
        this.dump = dump;
        this.objectAddressResolver = objectAddressResolver;
        this.bytesDisplay = bytesDisplay == null ? BytesDisplay.Smart : bytesDisplay;
        this.bytesFormatter = new BytesFormat(this.bytesDisplay);
    }

    public int getLimit()
    {
        return limit;
    }

    public int getEffectiveLimit()
    {
        return dump ? Integer.MAX_VALUE : limit;
    }

    public int getTreeDepthLimit()
    {
        return treeDepthLimit;
    }

    public int getTreeNodeLimit()
    {
        return treeNodeLimit;
    }

    public int getEffectiveTreeNodeLimit()
    {
        return dump ? Integer.MAX_VALUE : treeNodeLimit;
    }

    public boolean isAgentProfile()
    {
        return agentProfile;
    }

    public boolean isDump()
    {
        return dump;
    }

    public BytesDisplay getBytesDisplay()
    {
        return bytesDisplay;
    }

    public BytesFormat getBytesFormatter()
    {
        return bytesFormatter;
    }

    public String resolveObjectAddress(int objectId)
    {
        return objectAddressResolver == null ? null : objectAddressResolver.resolveObjectAddress(objectId);
    }

    private static int defaultTreeNodeLimit(int limit)
    {
        if (limit >= Integer.MAX_VALUE / 10)
            return Integer.MAX_VALUE;
        return Math.max(100, limit * 10);
    }
}
