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
import java.util.Collection;
import java.util.List;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.util.IProgressListener;

final class InstancesResultBuilder
{
    public InstancesResult build(ISnapshot snapshot, String className, boolean includeSubclasses,
                    IProgressListener listener) throws SnapshotException, CliException
    {
        Collection<IClass> classes = snapshot.getClassesByName(className, includeSubclasses);
        if (classes == null || classes.isEmpty())
            throw CliException.execution("No classes found matching " + className, null); //$NON-NLS-1$

        List<InstancesResult.Row> rows = new ArrayList<InstancesResult.Row>();
        for (IClass clazz : classes)
        {
            for (int objectId : clazz.getObjectIds())
            {
                if (listener.isCanceled())
                    throw new IProgressListener.OperationCanceledException();
                rows.add(new InstancesResult.Row(snapshot, objectId));
            }
        }

        return new InstancesResult(rows.toArray(new InstancesResult.Row[0]));
    }
}
