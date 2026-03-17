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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.mat.query.BytesDisplay;

public final class CliArguments
{
    public enum ObjectsGrouping
    {
        CLASS, PACKAGE, CLASS_LOADER;

        public static ObjectsGrouping parse(String value) throws CliException
        {
            if ("class".equalsIgnoreCase(value)) //$NON-NLS-1$
                return CLASS;
            if ("package".equalsIgnoreCase(value)) //$NON-NLS-1$
                return PACKAGE;
            if ("class-loader".equalsIgnoreCase(value)) //$NON-NLS-1$
                return CLASS_LOADER;
            throw CliException.usage("Unsupported --by value: " + value); //$NON-NLS-1$
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
    private final String subjectName;
    private final File heapFile;
    private final BytesDisplay bytesDisplay;
    private final OutputFormat format;
    private final boolean verbose;
    private final boolean help;
    private final boolean showNulls;
    private final boolean dump;
    private final int limit;
    private final int treeDepthLimit;
    private final String objectAddress;
    private final String className;
    private final String classRegex;
    private final String classContains;
    private final ObjectsGrouping objectsGrouping;
    private final String packageName;
    private final String classLoaderName;
    private final List<String> selectFields;
    private final List<String> fieldPaths;
    private final boolean includeSubclasses;
    private final String oqlQuery;
    private final String queryCommand;

    CliArguments(CliCommand command, CliCommand subjectCommand, String subjectName, File heapFile,
                    BytesDisplay bytesDisplay, OutputFormat format, boolean verbose, boolean help, boolean showNulls,
                    boolean dump, int limit,
                    int treeDepthLimit,
                    String objectAddress, String className, String classRegex, String classContains,
                    ObjectsGrouping objectsGrouping, String packageName, String classLoaderName,
                    List<String> selectFields, List<String> fieldPaths, boolean includeSubclasses, String oqlQuery,
                    String queryCommand)
    {
        this.command = command;
        this.subjectCommand = subjectCommand;
        this.subjectName = subjectName;
        this.heapFile = heapFile;
        this.bytesDisplay = bytesDisplay;
        this.format = format;
        this.verbose = verbose;
        this.help = help;
        this.showNulls = showNulls;
        this.dump = dump;
        this.limit = limit;
        this.treeDepthLimit = treeDepthLimit;
        this.objectAddress = objectAddress;
        this.className = className;
        this.classRegex = classRegex;
        this.classContains = classContains;
        this.objectsGrouping = objectsGrouping == null ? ObjectsGrouping.CLASS : objectsGrouping;
        this.packageName = packageName;
        this.classLoaderName = classLoaderName;
        this.selectFields = immutableCopy(selectFields);
        this.fieldPaths = immutableCopy(fieldPaths);
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

    public String getCompletionShell()
    {
        return command == CliCommand.COMPLETION ? subjectName : null;
    }

    public File getHeapFile()
    {
        return heapFile;
    }

    public OutputFormat getFormat()
    {
        return format;
    }

    public BytesDisplay getBytesDisplay()
    {
        return bytesDisplay;
    }

    public boolean isVerbose()
    {
        return verbose;
    }

    public boolean isHelp()
    {
        return help;
    }

    public boolean isShowNulls()
    {
        return showNulls;
    }

    public boolean isDump()
    {
        return dump;
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

    public ObjectsGrouping getObjectsGrouping()
    {
        return objectsGrouping;
    }

    public String getPackageName()
    {
        return packageName;
    }

    public String getClassLoaderName()
    {
        return classLoaderName;
    }

    public List<String> getSelectFields()
    {
        return selectFields;
    }

    public List<String> getFieldPaths()
    {
        return fieldPaths;
    }

    public List<String> getInspectionFieldPaths()
    {
        return !fieldPaths.isEmpty() ? fieldPaths : selectFields;
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

    private static List<String> immutableCopy(List<String> values)
    {
        if (values == null || values.isEmpty())
            return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<String>(values));
    }
}
