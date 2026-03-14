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
    public enum OutputProfile
    {
        DEFAULT, AGENT;

        public static OutputProfile parse(String value) throws CliException
        {
            if ("default".equalsIgnoreCase(value)) //$NON-NLS-1$
                return DEFAULT;
            if ("agent".equalsIgnoreCase(value)) //$NON-NLS-1$
                return AGENT;
            throw CliException.usage("Unsupported profile: " + value); //$NON-NLS-1$
        }
    }

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
    private final CliCommand subjectCommand;
    private final File heapFile;
    private final OutputProfile profile;
    private final OutputFormat format;
    private final boolean verbose;
    private final boolean help;
    private final int limit;
    private final int treeDepthLimit;
    private final String objectAddress;
    private final String oqlQuery;
    private final String queryCommand;

    CliArguments(CliCommand command, CliCommand subjectCommand, File heapFile, OutputProfile profile,
                    OutputFormat format, boolean verbose, boolean help, int limit, int treeDepthLimit,
                    String objectAddress, String oqlQuery, String queryCommand)
    {
        this.command = command;
        this.subjectCommand = subjectCommand;
        this.heapFile = heapFile;
        this.profile = profile;
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

    public CliCommand getSubjectCommand()
    {
        return subjectCommand;
    }

    public File getHeapFile()
    {
        return heapFile;
    }

    public OutputProfile getProfile()
    {
        return profile;
    }

    public boolean isAgentProfile()
    {
        return profile == OutputProfile.AGENT;
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
