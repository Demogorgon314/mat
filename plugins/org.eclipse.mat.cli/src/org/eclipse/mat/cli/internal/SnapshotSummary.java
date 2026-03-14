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

import java.io.Serializable;
import java.time.Instant;

import org.eclipse.mat.snapshot.SnapshotInfo;
import org.eclipse.mat.util.Units;

public final class SnapshotSummary
{
    private final String path;
    private final String heapFormat;
    private final String jvmInfo;
    private final String creationDate;
    private final int identifierSize;
    private final int numberOfObjects;
    private final int numberOfClasses;
    private final int numberOfClassLoaders;
    private final int numberOfGCRoots;
    private final long usedHeapSize;

    private SnapshotSummary(String path, String heapFormat, String jvmInfo, String creationDate, int identifierSize,
                    int numberOfObjects, int numberOfClasses, int numberOfClassLoaders, int numberOfGCRoots,
                    long usedHeapSize)
    {
        this.path = path;
        this.heapFormat = heapFormat;
        this.jvmInfo = jvmInfo;
        this.creationDate = creationDate;
        this.identifierSize = identifierSize;
        this.numberOfObjects = numberOfObjects;
        this.numberOfClasses = numberOfClasses;
        this.numberOfClassLoaders = numberOfClassLoaders;
        this.numberOfGCRoots = numberOfGCRoots;
        this.usedHeapSize = usedHeapSize;
    }

    public static SnapshotSummary from(SnapshotInfo info)
    {
        Serializable format = info.getProperty("$heapFormat"); //$NON-NLS-1$
        String creationDate = info.getCreationDate() == null ? null
                        : Instant.ofEpochMilli(info.getCreationDate().getTime()).toString();
        return new SnapshotSummary(info.getPath(), format == null ? null : format.toString(), info.getJvmInfo(),
                        creationDate, info.getIdentifierSize(), info.getNumberOfObjects(), info.getNumberOfClasses(),
                        info.getNumberOfClassLoaders(), info.getNumberOfGCRoots(), info.getUsedHeapSize());
    }

    public String asText()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Path: ").append(path).append('\n'); //$NON-NLS-1$
        builder.append("Format: ").append(valueOrUnknown(heapFormat)).append('\n'); //$NON-NLS-1$
        builder.append("Objects: ").append(numberOfObjects).append('\n'); //$NON-NLS-1$
        builder.append("Classes: ").append(numberOfClasses).append('\n'); //$NON-NLS-1$
        builder.append("Class Loaders: ").append(numberOfClassLoaders).append('\n'); //$NON-NLS-1$
        builder.append("GC Roots: ").append(numberOfGCRoots).append('\n'); //$NON-NLS-1$
        builder.append("Identifier Size: ").append(identifierSize).append('\n'); //$NON-NLS-1$
        builder.append("Used Heap: ").append(usedHeapSize).append(" bytes"); //$NON-NLS-1$ //$NON-NLS-2$
        builder.append(" (").append(Units.Storage.of(usedHeapSize).format(usedHeapSize)).append(")\n"); //$NON-NLS-1$ //$NON-NLS-2$
        if (jvmInfo != null)
            builder.append("JVM Info: ").append(jvmInfo).append('\n'); //$NON-NLS-1$
        if (creationDate != null)
            builder.append("Creation Date: ").append(creationDate).append('\n'); //$NON-NLS-1$
        return builder.toString();
    }

    private String valueOrUnknown(String value)
    {
        return value == null ? "unknown" : value; //$NON-NLS-1$
    }

    public String getPath()
    {
        return path;
    }

    public String getHeapFormat()
    {
        return heapFormat;
    }

    public String getJvmInfo()
    {
        return jvmInfo;
    }

    public String getCreationDate()
    {
        return creationDate;
    }

    public int getIdentifierSize()
    {
        return identifierSize;
    }

    public int getNumberOfObjects()
    {
        return numberOfObjects;
    }

    public int getNumberOfClasses()
    {
        return numberOfClasses;
    }

    public int getNumberOfClassLoaders()
    {
        return numberOfClassLoaders;
    }

    public int getNumberOfGCRoots()
    {
        return numberOfGCRoots;
    }

    public long getUsedHeapSize()
    {
        return usedHeapSize;
    }
}
