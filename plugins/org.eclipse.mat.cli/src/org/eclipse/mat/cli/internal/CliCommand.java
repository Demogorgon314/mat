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
    HISTOGRAM("histogram", true, false, false, false), //$NON-NLS-1$
    INSTANCES("instances", true, false, false, false), //$NON-NLS-1$
    INSPECT_OBJECT("inspect-object", true, false, false, false), //$NON-NLS-1$
    TOP_CONSUMERS("top-consumers", true, false, false, false), //$NON-NLS-1$
    PATH2GC("path2gc", true, false, false, false), //$NON-NLS-1$
    OQL("oql", true, false, false, false), //$NON-NLS-1$
    QUERY("query", true, false, false, false), //$NON-NLS-1$
    DESCRIBE("describe", false, true, false, false), //$NON-NLS-1$
    SCHEMA("schema", false, true, false, false), //$NON-NLS-1$
    LIST_QUERIES("list-queries", false, false, true, false), //$NON-NLS-1$
    DESCRIBE_QUERY("describe-query", false, false, true, true); //$NON-NLS-1$

    private final String token;
    private final boolean requiresSnapshot;
    private final boolean requiresSubjectCommand;
    private final boolean requiresQueryRegistry;
    private final boolean requiresQueryIdentifier;

    private CliCommand(String token, boolean requiresSnapshot, boolean requiresSubjectCommand,
                    boolean requiresQueryRegistry, boolean requiresQueryIdentifier)
    {
        this.token = token;
        this.requiresSnapshot = requiresSnapshot;
        this.requiresSubjectCommand = requiresSubjectCommand;
        this.requiresQueryRegistry = requiresQueryRegistry;
        this.requiresQueryIdentifier = requiresQueryIdentifier;
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

    public boolean requiresQueryIdentifier()
    {
        return requiresQueryIdentifier;
    }

    public boolean requiresRuntimeServices()
    {
        return requiresSnapshot || requiresQueryRegistry;
    }

    public static CliCommand parse(String token) throws CliException
    {
        for (CliCommand command : values())
        {
            if (command.token.equals(token))
                return command;
        }
        throw CliException.usage("Unknown command: " + token); //$NON-NLS-1$
    }
}
