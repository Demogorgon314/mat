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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.snapshot.model.IObject;

final class ObjectInspectorDisplayHelper
{
    private static final String JAVA_LANG_ENUM = "java.lang.Enum"; //$NON-NLS-1$

    private static final Set<String> INLINE_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                    "java.lang.String", //$NON-NLS-1$
                    "java.lang.StringBuilder", //$NON-NLS-1$
                    "java.lang.StringBuffer", //$NON-NLS-1$
                    "java.lang.Boolean", //$NON-NLS-1$
                    "java.lang.Byte", //$NON-NLS-1$
                    "java.lang.Character", //$NON-NLS-1$
                    "java.lang.Short", //$NON-NLS-1$
                    "java.lang.Integer", //$NON-NLS-1$
                    "java.lang.Long", //$NON-NLS-1$
                    "java.lang.Float", //$NON-NLS-1$
                    "java.lang.Double", //$NON-NLS-1$
                    "java.lang.Class", //$NON-NLS-1$
                    "java.util.concurrent.atomic.AtomicBoolean", //$NON-NLS-1$
                    "java.util.concurrent.atomic.AtomicInteger", //$NON-NLS-1$
                    "java.util.concurrent.atomic.AtomicLong", //$NON-NLS-1$
                    "java.util.concurrent.atomic.LongAdder", //$NON-NLS-1$
                    "java.util.concurrent.atomic.DoubleAdder", //$NON-NLS-1$
                    "java.math.BigInteger", //$NON-NLS-1$
                    "java.math.BigDecimal"))); //$NON-NLS-1$

    private static final Set<String> QUOTED_INLINE_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays
                    .asList("java.lang.String", "java.lang.StringBuilder", "java.lang.StringBuffer"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    private ObjectInspectorDisplayHelper()
    {}

    static Object inlineValue(IObject object)
    {
        if (object == null || !isInlineCandidate(object))
            return null;

        String resolved = object.getClassSpecificName();
        if (resolved == null)
            return null;

        if (QUOTED_INLINE_TYPES.contains(object.getClazz().getName()))
            return ObjectDisplayHelper.quotedTextValue(resolved);

        return resolved;
    }

    private static boolean isInlineCandidate(IObject object)
    {
        String className = object.getClazz().getName();
        if (INLINE_TYPES.contains(className))
            return true;

        try
        {
            return object.getClazz().doesExtend(JAVA_LANG_ENUM);
        }
        catch (SnapshotException e)
        {
            throw new RuntimeException(e);
        }
    }
}
