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

public final class CliArgumentParser
{
    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 10000;
    static final int DEFAULT_TREE_DEPTH = 8;
    static final int DEFAULT_AGENT_TREE_DEPTH = 4;

    public CliArguments parse(String[] args) throws CliException
    {
        if (args == null || args.length == 0)
            return new CliArguments(null, null, null, null, CliArguments.OutputProfile.DEFAULT,
                            CliArguments.OutputFormat.TEXT, false, true, DEFAULT_LIMIT, DEFAULT_TREE_DEPTH, null, null,
                            null);

        CliCommand command = null;
        CliCommand subjectCommand = null;
        String subjectName = null;
        File heapFile = null;
        CliArguments.OutputProfile profile = CliArguments.OutputProfile.DEFAULT;
        CliArguments.OutputFormat format = CliArguments.OutputFormat.TEXT;
        boolean formatExplicit = false;
        boolean verbose = false;
        boolean help = false;
        int limit = DEFAULT_LIMIT;
        boolean limitExplicit = false;
        int treeDepthLimit = DEFAULT_TREE_DEPTH;
        boolean treeDepthExplicit = false;
        String objectAddress = null;
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
                profile = CliArguments.OutputProfile.AGENT;
            }
            else if (arg.startsWith("--profile=")) //$NON-NLS-1$
            {
                profile = CliArguments.OutputProfile.parse(arg.substring("--profile=".length())); //$NON-NLS-1$
            }
            else if ("--profile".equals(arg)) //$NON-NLS-1$
            {
                profile = CliArguments.OutputProfile.parse(nextArg(args, ++ii, "--profile")); //$NON-NLS-1$
            }
            else if (arg.startsWith("--format=")) //$NON-NLS-1$
            {
                format = CliArguments.OutputFormat.parse(arg.substring("--format=".length())); //$NON-NLS-1$
                formatExplicit = true;
            }
            else if ("--format".equals(arg)) //$NON-NLS-1$
            {
                format = CliArguments.OutputFormat.parse(nextArg(args, ++ii, "--format")); //$NON-NLS-1$
                formatExplicit = true;
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
            else if (command.requiresQueryIdentifier() && subjectName == null)
            {
                subjectName = arg;
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

        if (profile == CliArguments.OutputProfile.AGENT && !formatExplicit)
            format = CliArguments.OutputFormat.JSON;
        if (!treeDepthExplicit)
            treeDepthLimit = defaultTreeDepth(profile);

        if (command == null)
        {
            if (help)
            {
                return new CliArguments(null, null, null, null, profile, format, verbose, true, limit,
                                treeDepthLimit, objectAddress, oqlQuery, queryCommand);
            }
            throw CliException.usage("Missing command"); //$NON-NLS-1$
        }

        if (command == CliCommand.OQL)
            oqlQuery = resolveExclusiveInput("oql", "--query", oqlQuery, "--query-file", oqlQueryFile, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            "--query-stdin", oqlQueryStdin); //$NON-NLS-1$
        else if (command == CliCommand.QUERY)
            queryCommand = resolveExclusiveInput("query", "--command", queryCommand, "--command-file", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            queryCommandFile, "--command-stdin", queryCommandStdin); //$NON-NLS-1$

        if (!limitExplicit)
            limit = defaultLimit(command, queryCommand);

        CliArguments parsed = new CliArguments(command, subjectCommand, subjectName, heapFile, profile, format, verbose,
                        help, limit, treeDepthLimit, objectAddress, oqlQuery, queryCommand);
        validate(parsed);
        return parsed;
    }

    public CliArguments.OutputProfile detectProfile(String[] args)
    {
        if (args == null)
            return CliArguments.OutputProfile.DEFAULT;

        CliArguments.OutputProfile profile = CliArguments.OutputProfile.DEFAULT;
        for (int ii = 0; ii < args.length; ii++)
        {
            String arg = args[ii];
            if ("--agent".equals(arg)) //$NON-NLS-1$
            {
                profile = CliArguments.OutputProfile.AGENT;
            }
            else if (arg.startsWith("--profile=")) //$NON-NLS-1$
            {
                try
                {
                    profile = CliArguments.OutputProfile.parse(arg.substring("--profile=".length())); //$NON-NLS-1$
                }
                catch (CliException ignore)
                {
                    return profile;
                }
            }
            else if ("--profile".equals(arg) && ii + 1 < args.length) //$NON-NLS-1$
            {
                try
                {
                    profile = CliArguments.OutputProfile.parse(args[++ii]);
                }
                catch (CliException ignore)
                {
                    return profile;
                }
            }
        }
        return profile;
    }

    public CliArguments.OutputFormat detectFormat(String[] args, CliArguments.OutputProfile profile)
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
                        return defaultFormat(profile);
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
                        return defaultFormat(profile);
                    }
                }
            }
        }
        return defaultFormat(profile);
    }

    private void validate(CliArguments arguments) throws CliException
    {
        if (arguments.isHelp())
            return;

        if (arguments.getCommand().requiresSubjectCommand() && arguments.getSubjectCommand() == null)
            throw CliException.usage(arguments.getCommand().getToken() + " requires a command name"); //$NON-NLS-1$

        if (arguments.getCommand().requiresQueryIdentifier() && isEmpty(arguments.getSubjectName()))
            throw CliException.usage(arguments.getCommand().getToken() + " requires a query identifier"); //$NON-NLS-1$

        if (arguments.getCommand().requiresSnapshot() && arguments.getHeapFile() == null)
            throw CliException.usage("Missing heap dump path"); //$NON-NLS-1$

        switch (arguments.getCommand())
        {
            case PATH2GC:
                if (isEmpty(arguments.getObjectAddress()))
                    throw CliException.usage("path2gc requires --object 0x..."); //$NON-NLS-1$
                break;
            case OQL:
                if (isEmpty(arguments.getOqlQuery()))
                    throw CliException.usage("oql requires --query"); //$NON-NLS-1$
                break;
            case QUERY:
                if (isEmpty(arguments.getQueryCommand()))
                    throw CliException.usage("query requires --command"); //$NON-NLS-1$
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

    private CliArguments.OutputFormat defaultFormat(CliArguments.OutputProfile profile)
    {
        return profile == CliArguments.OutputProfile.AGENT ? CliArguments.OutputFormat.JSON
                        : CliArguments.OutputFormat.TEXT;
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

    public CliArguments partialParse(String[] args, CliArguments.OutputProfile profile,
                    CliArguments.OutputFormat format)
    {
        CliCommand command = null;
        CliCommand subjectCommand = null;
        String subjectName = null;
        File heapFile = null;
        boolean help = args == null || args.length == 0;
        String objectAddress = null;
        String oqlQuery = null;
        String queryCommand = null;
        int treeDepthLimit = defaultTreeDepth(profile);

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
                        else if ("--depth".equals(arg)) //$NON-NLS-1$
                            treeDepthLimit = safePartialDepth(value, treeDepthLimit);
                        else if ("--query".equals(arg)) //$NON-NLS-1$
                            oqlQuery = value;
                        else if ("--command".equals(arg)) //$NON-NLS-1$
                            queryCommand = value;
                    }
                }
                else if (arg.startsWith("--object=")) //$NON-NLS-1$
                {
                    objectAddress = arg.substring("--object=".length()); //$NON-NLS-1$
                }
                else if (arg.startsWith("--query=")) //$NON-NLS-1$
                {
                    oqlQuery = arg.substring("--query=".length()); //$NON-NLS-1$
                }
                else if (arg.startsWith("--depth=")) //$NON-NLS-1$
                {
                    treeDepthLimit = safePartialDepth(arg.substring("--depth=".length()), treeDepthLimit); //$NON-NLS-1$
                }
                else if (arg.startsWith("--command=")) //$NON-NLS-1$
                {
                    queryCommand = arg.substring("--command=".length()); //$NON-NLS-1$
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
                else if (command.requiresQueryIdentifier() && subjectName == null)
                {
                    subjectName = arg;
                }
                else if (command.requiresSnapshot() && heapFile == null)
                {
                    heapFile = new File(arg).getAbsoluteFile();
                }
            }
        }

        return new CliArguments(command, subjectCommand, subjectName, heapFile, profile, format, false, help,
                        defaultLimit(command, queryCommand), treeDepthLimit, objectAddress, oqlQuery, queryCommand);
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
        return "--profile".equals(option) || "--format".equals(option) || "--limit".equals(option) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        || "--depth".equals(option) //$NON-NLS-1$
                        || "--object".equals(option) || "--query".equals(option) || "--query-file".equals(option) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        || "--command".equals(option) || "--command-file".equals(option); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private int defaultTreeDepth(CliArguments.OutputProfile profile)
    {
        return profile == CliArguments.OutputProfile.AGENT ? DEFAULT_AGENT_TREE_DEPTH : DEFAULT_TREE_DEPTH;
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
}
