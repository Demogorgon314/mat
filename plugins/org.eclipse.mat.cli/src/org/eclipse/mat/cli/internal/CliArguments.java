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

import java.io.File;

public final class CliArguments
{
    public enum OutputFormat
    {
        TEXT, JSON;

        public static OutputFormat parse(String value) throws CliException
        {
            if ("text".equalsIgnoreCase(value)) //$NON-NLS-1$
                return TEXT;
            if ("json".equalsIgnoreCase(value)) //$NON-NLS-1$
                return JSON;
            throw CliException.usage("Unsupported format: " + value); //$NON-NLS-1$
        }
    }

    private final CliCommand command;
    private final File heapFile;
    private final OutputFormat format;
    private final boolean verbose;
    private final boolean help;
    private final int limit;
    private final int treeDepthLimit;
    private final String objectAddress;
    private final String oqlQuery;
    private final String queryCommand;

    CliArguments(CliCommand command, File heapFile, OutputFormat format, boolean verbose, boolean help, int limit,
                    int treeDepthLimit, String objectAddress, String oqlQuery, String queryCommand)
    {
        this.command = command;
        this.heapFile = heapFile;
        this.format = format;
        this.verbose = verbose;
        this.help = help;
        this.limit = limit;
        this.treeDepthLimit = treeDepthLimit;
        this.objectAddress = objectAddress;
        this.oqlQuery = oqlQuery;
        this.queryCommand = queryCommand;
    }

    public CliCommand getCommand()
    {
        return command;
    }

    public File getHeapFile()
    {
        return heapFile;
    }

    public OutputFormat getFormat()
    {
        return format;
    }

    public boolean isVerbose()
    {
        return verbose;
    }

    public boolean isHelp()
    {
        return help;
    }

    public int getLimit()
    {
        return limit;
    }

    public int getTreeDepthLimit()
    {
        return treeDepthLimit;
    }

    public String getObjectAddress()
    {
        return objectAddress;
    }

    public String getOqlQuery()
    {
        return oqlQuery;
    }

    public String getQueryCommand()
    {
        return queryCommand;
    }
}
