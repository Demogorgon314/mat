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

public final class CliExitCodes
{
    public static final int OK = 0;
    public static final int USAGE = 2;
    public static final int EXECUTION_ERROR = 3;
    public static final int UNSUPPORTED_RESULT = 4;
    public static final int OUT_OF_MEMORY = 79;

    private CliExitCodes()
    {}
}
