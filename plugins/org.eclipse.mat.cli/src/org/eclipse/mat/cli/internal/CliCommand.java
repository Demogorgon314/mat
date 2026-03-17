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

public enum CliCommand
{
    SUMMARY("summary", true, false, false, false), //$NON-NLS-1$
    THREADS("threads", true, false, false, false), //$NON-NLS-1$
    OBJECTS("objects", true, false, false, false), //$NON-NLS-1$
    INSTANCES("instances", true, false, false, false), //$NON-NLS-1$
    INSPECT_OBJECT("inspect-object", true, false, false, false), //$NON-NLS-1$
    BIGGEST_OBJECTS("biggest-objects", true, false, false, false), //$NON-NLS-1$
    PATH2GC("path2gc", true, false, false, false), //$NON-NLS-1$
    OQL("oql", true, false, false, false), //$NON-NLS-1$
    QUERY("query", true, false, false, false), //$NON-NLS-1$
    DESCRIBE("describe", false, true, false, false), //$NON-NLS-1$
    SCHEMA("schema", false, true, false, false), //$NON-NLS-1$
    LIST_QUERIES("list-queries", false, false, true, false), //$NON-NLS-1$
    DESCRIBE_QUERY("describe-query", false, false, true, true), //$NON-NLS-1$
    COMPLETION("completion", false, false, false, true); //$NON-NLS-1$

    private final String token;
    private final boolean requiresSnapshot;
    private final boolean requiresSubjectCommand;
    private final boolean requiresQueryRegistry;
    private final boolean requiresSubjectName;

    private CliCommand(String token, boolean requiresSnapshot, boolean requiresSubjectCommand,
                    boolean requiresQueryRegistry, boolean requiresSubjectName)
    {
        this.token = token;
        this.requiresSnapshot = requiresSnapshot;
        this.requiresSubjectCommand = requiresSubjectCommand;
        this.requiresQueryRegistry = requiresQueryRegistry;
        this.requiresSubjectName = requiresSubjectName;
    }

    public String getToken()
    {
        return token;
    }

    public boolean requiresSnapshot()
    {
        return requiresSnapshot;
    }

    public boolean requiresSubjectCommand()
    {
        return requiresSubjectCommand;
    }

    public boolean requiresQueryRegistry()
    {
        return requiresQueryRegistry;
    }

    public boolean requiresSubjectName()
    {
        return requiresSubjectName;
    }

    public boolean requiresRuntimeServices()
    {
        return requiresSnapshot || requiresQueryRegistry;
    }

    public static CliCommand parse(String token) throws CliException
    {
        if ("histogram".equals(token)) //$NON-NLS-1$
        {
            throw CliException.usage(
                            "`histogram` has been replaced by `objects`. Use `mat-cli objects <heap>`."); //$NON-NLS-1$
        }
        if ("top-consumers".equals(token)) //$NON-NLS-1$
        {
            throw CliException.usage(
                            "`top-consumers` has been replaced by `biggest-objects` and `objects --by package`. Use `mat-cli biggest-objects <heap>` or `mat-cli objects <heap> --by package`."); //$NON-NLS-1$
        }
        for (CliCommand command : values())
        {
            if (command.token.equals(token))
                return command;
        }
        throw CliException.usage("Unknown command: " + token); //$NON-NLS-1$
    }
}
