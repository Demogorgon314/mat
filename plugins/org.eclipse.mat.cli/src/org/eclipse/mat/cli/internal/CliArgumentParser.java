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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.mat.query.BytesDisplay;

public final class CliArgumentParser
{
    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 10000;
    static final int DEFAULT_TREE_DEPTH = 8;
    static final int DEFAULT_INSPECT_OBJECT_TREE_DEPTH = 3;

    public CliArguments parse(String[] args) throws CliException
    {
        if (args == null || args.length == 0)
            return new CliArguments(null, null, null, null, BytesDisplay.Smart, CliArguments.OutputFormat.TEXT, false, true,
                            false, DEFAULT_LIMIT, DEFAULT_TREE_DEPTH, null, null, null, null, null, null, false, null,
                            null);

        CliCommand command = null;
        CliCommand subjectCommand = null;
        String subjectName = null;
        File heapFile = null;
        BytesDisplay bytesDisplay = BytesDisplay.Smart;
        CliArguments.OutputFormat format = CliArguments.OutputFormat.TEXT;
        boolean verbose = false;
        boolean help = false;
        boolean showNulls = false;
        int limit = DEFAULT_LIMIT;
        boolean limitExplicit = false;
        int treeDepthLimit = DEFAULT_TREE_DEPTH;
        boolean treeDepthExplicit = false;
        String objectAddress = null;
        String className = null;
        String classRegex = null;
        String classContains = null;
        List<String> selectFields = new ArrayList<String>();
        List<String> fieldPaths = new ArrayList<String>();
        boolean includeSubclasses = false;
        String oqlQuery = null;
        String oqlQueryFile = null;
        boolean oqlQueryStdin = false;
        String queryCommand = null;
        String queryCommandFile = null;
        boolean queryCommandStdin = false;

        for (int ii = 0; ii < args.length; ii++)
        {
            String arg = args[ii];
            if (isHelpToken(arg))
            {
                help = true;
            }
            else if ("--agent".equals(arg)) //$NON-NLS-1$
            {
                throw removedOption("--agent"); //$NON-NLS-1$
            }
            else if (arg.startsWith("--profile=")) //$NON-NLS-1$
            {
                throw removedOption("--profile"); //$NON-NLS-1$
            }
            else if ("--profile".equals(arg)) //$NON-NLS-1$
            {
                throw removedOption("--profile"); //$NON-NLS-1$
            }
            else if (arg.startsWith("--format=")) //$NON-NLS-1$
            {
                format = CliArguments.OutputFormat.parse(arg.substring("--format=".length())); //$NON-NLS-1$
            }
            else if ("--format".equals(arg)) //$NON-NLS-1$
            {
                format = CliArguments.OutputFormat.parse(nextArg(args, ++ii, "--format")); //$NON-NLS-1$
            }
            else if (arg.startsWith("--bytes-display=")) //$NON-NLS-1$
            {
                bytesDisplay = parseBytesDisplay(arg.substring("--bytes-display=".length())); //$NON-NLS-1$
            }
            else if ("--bytes-display".equals(arg)) //$NON-NLS-1$
            {
                bytesDisplay = parseBytesDisplay(nextArg(args, ++ii, "--bytes-display")); //$NON-NLS-1$
            }
            else if (arg.startsWith("--limit=")) //$NON-NLS-1$
            {
                limit = parseLimit(arg.substring("--limit=".length())); //$NON-NLS-1$
                limitExplicit = true;
            }
            else if ("--limit".equals(arg)) //$NON-NLS-1$
            {
                limit = parseLimit(nextArg(args, ++ii, "--limit")); //$NON-NLS-1$
                limitExplicit = true;
            }
            else if (arg.startsWith("--depth=")) //$NON-NLS-1$
            {
                treeDepthLimit = parseDepth(arg.substring("--depth=".length())); //$NON-NLS-1$
                treeDepthExplicit = true;
            }
            else if ("--depth".equals(arg)) //$NON-NLS-1$
            {
                treeDepthLimit = parseDepth(nextArg(args, ++ii, "--depth")); //$NON-NLS-1$
                treeDepthExplicit = true;
            }
            else if (arg.startsWith("--object=")) //$NON-NLS-1$
            {
                objectAddress = arg.substring("--object=".length()); //$NON-NLS-1$
            }
            else if ("--object".equals(arg)) //$NON-NLS-1$
            {
                objectAddress = nextArg(args, ++ii, "--object"); //$NON-NLS-1$
            }
            else if (arg.startsWith("--class=")) //$NON-NLS-1$
            {
                className = arg.substring("--class=".length()); //$NON-NLS-1$
            }
            else if ("--class".equals(arg)) //$NON-NLS-1$
            {
                className = nextArg(args, ++ii, "--class"); //$NON-NLS-1$
            }
            else if (arg.startsWith("--class-regex=")) //$NON-NLS-1$
            {
                classRegex = arg.substring("--class-regex=".length()); //$NON-NLS-1$
            }
            else if ("--class-regex".equals(arg)) //$NON-NLS-1$
            {
                classRegex = nextArg(args, ++ii, "--class-regex"); //$NON-NLS-1$
            }
            else if (arg.startsWith("--class-contains=")) //$NON-NLS-1$
            {
                classContains = arg.substring("--class-contains=".length()); //$NON-NLS-1$
            }
            else if ("--class-contains".equals(arg)) //$NON-NLS-1$
            {
                classContains = nextArg(args, ++ii, "--class-contains"); //$NON-NLS-1$
            }
            else if (arg.startsWith("--select-fields=")) //$NON-NLS-1$
            {
                selectFields.add(arg.substring("--select-fields=".length())); //$NON-NLS-1$
            }
            else if ("--select-fields".equals(arg)) //$NON-NLS-1$
            {
                selectFields.add(nextArg(args, ++ii, "--select-fields")); //$NON-NLS-1$
            }
            else if (arg.startsWith("--field-paths=")) //$NON-NLS-1$
            {
                fieldPaths.add(arg.substring("--field-paths=".length())); //$NON-NLS-1$
            }
            else if ("--field-paths".equals(arg)) //$NON-NLS-1$
            {
                fieldPaths.add(nextArg(args, ++ii, "--field-paths")); //$NON-NLS-1$
            }
            else if ("--include-subclasses".equals(arg)) //$NON-NLS-1$
            {
                includeSubclasses = true;
            }
            else if (arg.startsWith("--query=")) //$NON-NLS-1$
            {
                oqlQuery = arg.substring("--query=".length()); //$NON-NLS-1$
            }
            else if ("--query".equals(arg)) //$NON-NLS-1$
            {
                oqlQuery = nextArg(args, ++ii, "--query"); //$NON-NLS-1$
            }
            else if (arg.startsWith("--query-file=")) //$NON-NLS-1$
            {
                oqlQueryFile = arg.substring("--query-file=".length()); //$NON-NLS-1$
            }
            else if ("--query-file".equals(arg)) //$NON-NLS-1$
            {
                oqlQueryFile = nextArg(args, ++ii, "--query-file"); //$NON-NLS-1$
            }
            else if ("--query-stdin".equals(arg)) //$NON-NLS-1$
            {
                oqlQueryStdin = true;
            }
            else if (arg.startsWith("--command=")) //$NON-NLS-1$
            {
                queryCommand = arg.substring("--command=".length()); //$NON-NLS-1$
            }
            else if ("--command".equals(arg)) //$NON-NLS-1$
            {
                queryCommand = nextArg(args, ++ii, "--command"); //$NON-NLS-1$
            }
            else if (arg.startsWith("--command-file=")) //$NON-NLS-1$
            {
                queryCommandFile = arg.substring("--command-file=".length()); //$NON-NLS-1$
            }
            else if ("--command-file".equals(arg)) //$NON-NLS-1$
            {
                queryCommandFile = nextArg(args, ++ii, "--command-file"); //$NON-NLS-1$
            }
            else if ("--command-stdin".equals(arg)) //$NON-NLS-1$
            {
                queryCommandStdin = true;
            }
            else if ("--verbose".equals(arg)) //$NON-NLS-1$
            {
                verbose = true;
            }
            else if ("--show-nulls".equals(arg)) //$NON-NLS-1$
            {
                showNulls = true;
            }
            else if (arg.startsWith("--")) //$NON-NLS-1$
            {
                throw CliException.usage("Unknown option: " + arg); //$NON-NLS-1$
            }
            else if (command == null)
            {
                command = CliCommand.parse(arg);
            }
            else if (command.requiresSubjectCommand() && subjectCommand == null)
            {
                subjectName = arg;
                subjectCommand = CliCommand.parse(arg);
            }
            else if (command.requiresSubjectName() && subjectName == null)
            {
                subjectName = normalizeSubjectName(command, arg);
            }
            else if (command.requiresSnapshot() && heapFile == null)
            {
                heapFile = new File(arg).getAbsoluteFile();
            }
            else
            {
                throw CliException.usage("Unexpected argument: " + arg); //$NON-NLS-1$
            }
        }

        if (command == null)
        {
            if (help)
            {
                return new CliArguments(null, null, null, null, bytesDisplay, format, verbose, true, showNulls, limit,
                                treeDepthLimit, objectAddress, className, classRegex, classContains, selectFields,
                                fieldPaths, includeSubclasses, oqlQuery, queryCommand);
            }
            throw CliException.usage("Missing command"); //$NON-NLS-1$
        }

        if (!treeDepthExplicit)
            treeDepthLimit = defaultTreeDepth(command);

        if (command == CliCommand.OQL)
            oqlQuery = resolveExclusiveInput("oql", "--query", oqlQuery, "--query-file", oqlQueryFile, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            "--query-stdin", oqlQueryStdin); //$NON-NLS-1$
        else if (command == CliCommand.QUERY)
            queryCommand = resolveExclusiveInput("query", "--command", queryCommand, "--command-file", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            queryCommandFile, "--command-stdin", queryCommandStdin); //$NON-NLS-1$

        if (!limitExplicit)
            limit = defaultLimit(command, queryCommand);

        CliArguments parsed = new CliArguments(command, subjectCommand, subjectName, heapFile, bytesDisplay, format, verbose, help,
                        showNulls,
                        limit, treeDepthLimit, objectAddress, className, classRegex, classContains, selectFields,
                        fieldPaths, includeSubclasses, oqlQuery, queryCommand);
        validate(parsed);
        return parsed;
    }

    public CliArguments.OutputFormat detectFormat(String[] args)
    {
        if (args != null)
        {
            for (int ii = 0; ii < args.length; ii++)
            {
                String arg = args[ii];
                if (arg.startsWith("--format=")) //$NON-NLS-1$
                {
                    try
                    {
                        return CliArguments.OutputFormat.parse(arg.substring("--format=".length())); //$NON-NLS-1$
                    }
                    catch (CliException ignore)
                    {
                        return defaultFormat();
                    }
                }
                else if ("--format".equals(arg) && ii + 1 < args.length) //$NON-NLS-1$
                {
                    try
                    {
                        return CliArguments.OutputFormat.parse(args[++ii]);
                    }
                    catch (CliException ignore)
                    {
                        return defaultFormat();
                    }
                }
            }
        }
        return defaultFormat();
    }

    private void validate(CliArguments arguments) throws CliException
    {
        if (arguments.isHelp())
            return;

        if (arguments.getCommand().requiresSubjectCommand() && arguments.getSubjectCommand() == null)
            throw CliException.usage(arguments.getCommand().getToken() + " requires a command name"); //$NON-NLS-1$

        if (arguments.getCommand().requiresSubjectName() && isEmpty(arguments.getSubjectName()))
        {
            if (arguments.getCommand() == CliCommand.COMPLETION)
                throw CliException.usage("completion requires a shell name (bash or zsh)"); //$NON-NLS-1$
            if (arguments.getCommand() == CliCommand.DESCRIBE_QUERY)
                throw CliException.usage("describe-query requires a query identifier"); //$NON-NLS-1$
            throw CliException.usage(arguments.getCommand().getToken() + " requires a subject name"); //$NON-NLS-1$
        }

        if (arguments.getCommand().requiresSnapshot() && arguments.getHeapFile() == null)
            throw CliException.usage("Missing heap dump path"); //$NON-NLS-1$

        switch (arguments.getCommand())
        {
            case PATH2GC:
            case INSPECT_OBJECT:
                if (isEmpty(arguments.getObjectAddress()))
                    throw CliException.usage(arguments.getCommand().getToken() + " requires --object 0x..."); //$NON-NLS-1$
                if (!arguments.getSelectFields().isEmpty() && !arguments.getFieldPaths().isEmpty())
                    throw CliException.usage("inspect-object accepts only one of --select-fields or --field-paths"); //$NON-NLS-1$
                validateSelectFields(arguments.getSelectFields());
                validateFieldPaths(arguments.getFieldPaths());
                break;
            case INSTANCES:
                boolean hasClassName = !isEmpty(arguments.getClassName());
                boolean hasClassRegex = !isEmpty(arguments.getClassRegex());
                boolean hasClassContains = !isEmpty(arguments.getClassContains());
                int selectorCount = (hasClassName ? 1 : 0) + (hasClassRegex ? 1 : 0) + (hasClassContains ? 1 : 0);
                if (selectorCount == 0)
                    throw CliException.usage(
                                    "instances requires one of --class <fqcn>, --class-regex <regex> or --class-contains <text>"); //$NON-NLS-1$
                if (selectorCount > 1)
                    throw CliException.usage(
                                    "instances accepts only one of --class, --class-regex or --class-contains"); //$NON-NLS-1$
                if (hasClassRegex)
                    validateClassRegex(arguments.getClassRegex());
                break;
            case OQL:
                if (isEmpty(arguments.getOqlQuery()))
                    throw CliException.usage("oql requires --query"); //$NON-NLS-1$
                break;
            case QUERY:
                if (isEmpty(arguments.getQueryCommand()))
                    throw CliException.usage("query requires --command"); //$NON-NLS-1$
                break;
            case COMPLETION:
                validateCompletionShell(arguments.getCompletionShell());
                break;
            case DESCRIBE:
            case SCHEMA:
            default:
                break;
        }
    }

    private boolean isEmpty(String value)
    {
        return value == null || value.length() == 0;
    }

    private void validateCompletionShell(String shell) throws CliException
    {
        for (String supportedShell : CliCommandCatalog.supportedCompletionShells())
        {
            if (supportedShell.equals(shell))
                return;
        }
        throw CliException.usage("Unsupported completion shell: " + shell + " (expected bash or zsh)"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void validateSelectFields(List<String> selectFields) throws CliException
    {
        for (String selectField : selectFields)
        {
            if (isEmpty(selectField))
                throw CliException.usage("inspect-object requires a non-empty --select-fields"); //$NON-NLS-1$
            if (selectField.indexOf('.') >= 0)
                throw CliException.usage("--select-fields accepts direct field names. Use --field-paths for dotted paths."); //$NON-NLS-1$
        }
    }

    private void validateFieldPaths(List<String> fieldPaths) throws CliException
    {
        for (String fieldPath : fieldPaths)
        {
            if (isEmpty(fieldPath))
                throw CliException.usage("inspect-object requires a non-empty --field-paths"); //$NON-NLS-1$
            if (hasEmptyPathSegment(fieldPath))
                throw CliException.usage("Invalid --field-paths: " + fieldPath); //$NON-NLS-1$
        }
    }

    private String resolveExclusiveInput(String commandName, String inlineOption, String inlineValue, String fileOption,
                    String fileValue, String stdinOption, boolean stdin) throws CliException
    {
        int sources = (isEmpty(inlineValue) ? 0 : 1) + (isEmpty(fileValue) ? 0 : 1) + (stdin ? 1 : 0);
        if (sources == 0)
            return inlineValue;
        if (sources > 1)
        {
            throw CliException.usage(commandName + " accepts only one of " + inlineOption + ", " + fileOption + " or " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            + stdinOption);
        }
        if (!isEmpty(inlineValue))
            return inlineValue;
        if (!isEmpty(fileValue))
            return readFile(fileValue, fileOption);
        return readStandardInput(stdinOption);
    }

    private String readFile(String path, String optionName) throws CliException
    {
        try
        {
            return Files.readString(new File(path).toPath(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw CliException.execution("Unable to read " + optionName + " from " + path + ": " + e.getMessage(), e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

    private String readStandardInput(String optionName) throws CliException
    {
        try
        {
            return new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw CliException.execution("Unable to read " + optionName + " from stdin: " + e.getMessage(), e); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private CliArguments.OutputFormat defaultFormat()
    {
        return CliArguments.OutputFormat.TEXT;
    }

    private int parseLimit(String value) throws CliException
    {
        try
        {
            int limit = Integer.parseInt(value);
            if (limit < 1)
                throw CliException.usage("Limit must be >= 1"); //$NON-NLS-1$
            return Math.min(limit, MAX_LIMIT);
        }
        catch (NumberFormatException e)
        {
            throw CliException.usage("Invalid limit: " + value); //$NON-NLS-1$
        }
    }

    private int parseDepth(String value) throws CliException
    {
        try
        {
            int depth = Integer.parseInt(value);
            if (depth < 1)
                throw CliException.usage("Depth must be >= 1"); //$NON-NLS-1$
            return depth;
        }
        catch (NumberFormatException e)
        {
            throw CliException.usage("Invalid depth: " + value); //$NON-NLS-1$
        }
    }

    private String nextArg(String[] args, int index, String option) throws CliException
    {
        if (index >= args.length)
            throw CliException.usage("Missing value for " + option); //$NON-NLS-1$
        return args[index];
    }

    private boolean isHelpToken(String token)
    {
        return "--help".equals(token) || "-h".equals(token) || "help".equals(token); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    public CliArguments partialParse(String[] args, CliArguments.OutputFormat format)
    {
        CliCommand command = null;
        CliCommand subjectCommand = null;
        String subjectName = null;
        File heapFile = null;
        BytesDisplay bytesDisplay = BytesDisplay.Smart;
        boolean help = args == null || args.length == 0;
        String objectAddress = null;
        String className = null;
        String classRegex = null;
        String classContains = null;
        List<String> selectFields = new ArrayList<String>();
        List<String> fieldPaths = new ArrayList<String>();
        boolean includeSubclasses = false;
        String oqlQuery = null;
        String queryCommand = null;
        int treeDepthLimit = defaultTreeDepth(null);
        boolean treeDepthExplicit = false;
        boolean showNulls = false;

        if (args != null)
        {
            for (int ii = 0; ii < args.length; ii++)
            {
                String arg = args[ii];
                if (isHelpToken(arg))
                {
                    help = true;
                }
                else if (expectsValue(arg))
                {
                    if (ii + 1 < args.length)
                    {
                        String value = args[++ii];
                        if ("--object".equals(arg)) //$NON-NLS-1$
                            objectAddress = value;
                        else if ("--class".equals(arg)) //$NON-NLS-1$
                            className = value;
                        else if ("--class-regex".equals(arg)) //$NON-NLS-1$
                            classRegex = value;
                        else if ("--class-contains".equals(arg)) //$NON-NLS-1$
                            classContains = value;
                        else if ("--select-fields".equals(arg)) //$NON-NLS-1$
                            selectFields.add(value);
                        else if ("--field-paths".equals(arg)) //$NON-NLS-1$
                            fieldPaths.add(value);
                        else if ("--depth".equals(arg)) //$NON-NLS-1$
                        {
                            treeDepthLimit = safePartialDepth(value, treeDepthLimit);
                            treeDepthExplicit = true;
                        }
                        else if ("--query".equals(arg)) //$NON-NLS-1$
                            oqlQuery = value;
                        else if ("--command".equals(arg)) //$NON-NLS-1$
                            queryCommand = value;
                        else if ("--bytes-display".equals(arg)) //$NON-NLS-1$
                            bytesDisplay = safePartialBytesDisplay(value, bytesDisplay);
                    }
                }
                else if (arg.startsWith("--bytes-display=")) //$NON-NLS-1$
                {
                    bytesDisplay = safePartialBytesDisplay(arg.substring("--bytes-display=".length()), bytesDisplay); //$NON-NLS-1$
                }
                else if (arg.startsWith("--object=")) //$NON-NLS-1$
                {
                    objectAddress = arg.substring("--object=".length()); //$NON-NLS-1$
                }
                else if (arg.startsWith("--class=")) //$NON-NLS-1$
                {
                    className = arg.substring("--class=".length()); //$NON-NLS-1$
                }
                else if (arg.startsWith("--class-regex=")) //$NON-NLS-1$
                {
                    classRegex = arg.substring("--class-regex=".length()); //$NON-NLS-1$
                }
                else if (arg.startsWith("--class-contains=")) //$NON-NLS-1$
                {
                    classContains = arg.substring("--class-contains=".length()); //$NON-NLS-1$
                }
                else if (arg.startsWith("--query=")) //$NON-NLS-1$
                {
                    oqlQuery = arg.substring("--query=".length()); //$NON-NLS-1$
                }
                else if (arg.startsWith("--select-fields=")) //$NON-NLS-1$
                {
                    selectFields.add(arg.substring("--select-fields=".length())); //$NON-NLS-1$
                }
                else if (arg.startsWith("--field-paths=")) //$NON-NLS-1$
                {
                    fieldPaths.add(arg.substring("--field-paths=".length())); //$NON-NLS-1$
                }
                else if (arg.startsWith("--depth=")) //$NON-NLS-1$
                {
                    treeDepthLimit = safePartialDepth(arg.substring("--depth=".length()), treeDepthLimit); //$NON-NLS-1$
                    treeDepthExplicit = true;
                }
                else if (arg.startsWith("--command=")) //$NON-NLS-1$
                {
                    queryCommand = arg.substring("--command=".length()); //$NON-NLS-1$
                }
                else if ("--include-subclasses".equals(arg)) //$NON-NLS-1$
                {
                    includeSubclasses = true;
                }
                else if ("--show-nulls".equals(arg)) //$NON-NLS-1$
                {
                    showNulls = true;
                }
                else if (arg.startsWith("--")) //$NON-NLS-1$
                {
                    continue;
                }
                else if (command == null)
                {
                    try
                    {
                        command = CliCommand.parse(arg);
                    }
                    catch (CliException ignore)
                    {
                        break;
                    }
                }
                else if (command.requiresSubjectCommand() && subjectCommand == null)
                {
                    subjectName = arg;
                    try
                    {
                        subjectCommand = CliCommand.parse(arg);
                    }
                    catch (CliException ignore)
                    {
                        subjectCommand = null;
                    }
                }
                else if (command.requiresSubjectName() && subjectName == null)
                {
                    subjectName = normalizeSubjectName(command, arg);
                }
                else if (command.requiresSnapshot() && heapFile == null)
                {
                    heapFile = new File(arg).getAbsoluteFile();
                }
            }
        }

        if (!treeDepthExplicit)
            treeDepthLimit = defaultTreeDepth(command);

        return new CliArguments(command, subjectCommand, subjectName, heapFile, bytesDisplay, format, false, help, showNulls,
                        defaultLimit(command, queryCommand), treeDepthLimit, objectAddress, className, classRegex,
                        classContains, selectFields, fieldPaths, includeSubclasses, oqlQuery, queryCommand);
    }

    private int defaultLimit(CliCommand command, String queryCommand)
    {
        if (command == CliCommand.THREADS)
            return Integer.MAX_VALUE;

        if (command == CliCommand.QUERY)
        {
            String queryIdentifier = queryIdentifier(queryCommand);
            Integer override = CliCommandCatalog.queryDefaultLimit(queryIdentifier);
            if (override != null)
                return override.intValue();
        }
        return DEFAULT_LIMIT;
    }

    private String queryIdentifier(String queryCommand)
    {
        if (isEmpty(queryCommand))
            return null;

        String trimmed = queryCommand.trim();
        int separator = trimmed.indexOf(' ');
        return separator < 0 ? trimmed : trimmed.substring(0, separator);
    }

    private boolean expectsValue(String option)
    {
        return "--format".equals(option) || "--bytes-display".equals(option) || "--limit".equals(option) || "--depth".equals(option) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        || "--object".equals(option) || "--class".equals(option) //$NON-NLS-1$ //$NON-NLS-2$
                        || "--class-regex".equals(option) //$NON-NLS-1$
                        || "--class-contains".equals(option) //$NON-NLS-1$
                        || "--select-fields".equals(option) || "--field-paths".equals(option) //$NON-NLS-1$ //$NON-NLS-2$
                        || "--query".equals(option) //$NON-NLS-1$
                        || "--query-file".equals(option) //$NON-NLS-1$
                        || "--command".equals(option) || "--command-file".equals(option); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void validateClassRegex(String classRegex) throws CliException
    {
        try
        {
            Pattern.compile(classRegex);
        }
        catch (PatternSyntaxException e)
        {
            throw CliException.usage("Invalid --class-regex: " + classRegex + " (" + e.getDescription() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private int defaultTreeDepth(CliCommand command)
    {
        if (command == CliCommand.INSPECT_OBJECT)
            return DEFAULT_INSPECT_OBJECT_TREE_DEPTH;
        return DEFAULT_TREE_DEPTH;
    }

    private boolean hasEmptyPathSegment(String fieldPath)
    {
        return fieldPath.startsWith(".") || fieldPath.endsWith(".") || fieldPath.indexOf("..") >= 0; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    private int safePartialDepth(String value, int fallback)
    {
        try
        {
            return parseDepth(value);
        }
        catch (CliException e)
        {
            return fallback;
        }
    }

    private BytesDisplay parseBytesDisplay(String value) throws CliException
    {
        BytesDisplay bytesDisplay = BytesDisplay.parse(value);
        if (!bytesDisplay.toString().equalsIgnoreCase(value))
            throw CliException.usage("Invalid --bytes-display: " + value //$NON-NLS-1$
                            + " (expected one of bytes, kilobytes, megabytes, gigabytes, smart)"); //$NON-NLS-1$
        return bytesDisplay;
    }

    private BytesDisplay safePartialBytesDisplay(String value, BytesDisplay fallback)
    {
        try
        {
            return parseBytesDisplay(value);
        }
        catch (CliException e)
        {
            return fallback;
        }
    }

    private CliException removedOption(String option)
    {
        return CliException.usage(option + " has been removed. Use --format json."); //$NON-NLS-1$
    }

    private String normalizeSubjectName(CliCommand command, String value)
    {
        if (command == CliCommand.COMPLETION && value != null)
            return value.toLowerCase(java.util.Locale.ENGLISH);
        return value;
    }
}
