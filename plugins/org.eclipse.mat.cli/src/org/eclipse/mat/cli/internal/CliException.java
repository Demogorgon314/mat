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

public class CliException extends Exception
{
    private static final long serialVersionUID = 1L;

    private final int exitCode;

    private CliException(int exitCode, String message, Throwable cause)
    {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public static CliException usage(String message)
    {
        return new CliException(CliExitCodes.USAGE, message, null);
    }

    public static CliException execution(String message, Throwable cause)
    {
        return new CliException(CliExitCodes.EXECUTION_ERROR, message, cause);
    }

    public static CliException unsupported(String message)
    {
        return new CliException(CliExitCodes.UNSUPPORTED_RESULT, message, null);
    }

    public int getExitCode()
    {
        return exitCode;
    }
}
