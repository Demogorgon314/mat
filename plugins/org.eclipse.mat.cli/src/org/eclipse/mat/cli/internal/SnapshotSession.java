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

import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.SnapshotFactory;
import org.eclipse.mat.util.IProgressListener;

public final class SnapshotSession implements AutoCloseable
{
    private final ISnapshot snapshot;
    private final IProgressListener listener;

    SnapshotSession(ISnapshot snapshot, IProgressListener listener)
    {
        this.snapshot = snapshot;
        this.listener = listener;
    }

    public ISnapshot getSnapshot()
    {
        return snapshot;
    }

    public IProgressListener getProgressListener()
    {
        return listener;
    }

    public void close()
    {
        try
        {
            SnapshotFactory.dispose(snapshot);
        }
        finally
        {
            listener.done();
        }
    }
}
