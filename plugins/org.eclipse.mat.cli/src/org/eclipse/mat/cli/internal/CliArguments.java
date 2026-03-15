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
    private final CliCommand subjectCommand;
    private final String subjectName;
    private final File heapFile;
    private final OutputFormat format;
    private final boolean verbose;
    private final boolean help;
    private final int limit;
    private final int treeDepthLimit;
    private final String objectAddress;
    private final String className;
    private final String classRegex;
    private final String classContains;
    private final String selectField;
    private final String fieldPath;
    private final boolean includeSubclasses;
    private final String oqlQuery;
    private final String queryCommand;

    CliArguments(CliCommand command, CliCommand subjectCommand, String subjectName, File heapFile,
                    OutputFormat format, boolean verbose, boolean help, int limit, int treeDepthLimit,
                    String objectAddress, String className, String classRegex, String classContains,
                    String selectField, String fieldPath, boolean includeSubclasses, String oqlQuery,
                    String queryCommand)
    {
        this.command = command;
        this.subjectCommand = subjectCommand;
        this.subjectName = subjectName;
        this.heapFile = heapFile;
        this.format = format;
        this.verbose = verbose;
        this.help = help;
        this.limit = limit;
        this.treeDepthLimit = treeDepthLimit;
        this.objectAddress = objectAddress;
        this.className = className;
        this.classRegex = classRegex;
        this.classContains = classContains;
        this.selectField = selectField;
        this.fieldPath = fieldPath;
        this.includeSubclasses = includeSubclasses;
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

    public String getSubjectName()
    {
        return subjectName;
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

    public String getClassName()
    {
        return className;
    }

    public String getClassRegex()
    {
        return classRegex;
    }

    public String getClassContains()
    {
        return classContains;
    }

    public String getSelectField()
    {
        return selectField;
    }

    public String getFieldPath()
    {
        return fieldPath;
    }

    public String getInspectionFieldPath()
    {
        return fieldPath != null ? fieldPath : selectField;
    }

    public boolean isIncludeSubclasses()
    {
        return includeSubclasses;
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
