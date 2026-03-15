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

import org.eclipse.mat.query.IResult;

public final class CliExecution
{
    @FunctionalInterface
    public interface ObjectAddressResolver
    {
        String resolveObjectAddress(int objectId);
    }

    private final SnapshotSummary summary;
    private final IResult result;
    private final String primaryObjectAddress;
    private final String note;
    private final ObjectAddressResolver objectAddressResolver;

    private CliExecution(SnapshotSummary summary, IResult result, String primaryObjectAddress, String note,
                    ObjectAddressResolver objectAddressResolver)
    {
        this.summary = summary;
        this.result = result;
        this.primaryObjectAddress = primaryObjectAddress;
        this.note = note;
        this.objectAddressResolver = objectAddressResolver;
    }

    public static CliExecution summary(SnapshotSummary summary)
    {
        return new CliExecution(summary, null, null, null, null);
    }

    public static CliExecution result(IResult result)
    {
        return new CliExecution(null, result, null, null, null);
    }

    public static CliExecution result(IResult result, String primaryObjectAddress, String note)
    {
        return new CliExecution(null, result, primaryObjectAddress, note, null);
    }

    public static CliExecution result(IResult result, String primaryObjectAddress, String note,
                    ObjectAddressResolver objectAddressResolver)
    {
        return new CliExecution(null, result, primaryObjectAddress, note, objectAddressResolver);
    }

    public boolean isSummary()
    {
        return summary != null;
    }

    public SnapshotSummary getSummary()
    {
        return summary;
    }

    public IResult getResult()
    {
        return result;
    }

    public String getPrimaryObjectAddress()
    {
        return primaryObjectAddress;
    }

    public String getNote()
    {
        return note;
    }

    public ObjectAddressResolver getObjectAddressResolver()
    {
        return objectAddressResolver;
    }
}
