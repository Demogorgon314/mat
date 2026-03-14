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

public final class CliArgumentParser
{
    static final int DEFAULT_LIMIT = 20;
    static final int DEFAULT_TREE_DEPTH = 8;

    public CliArguments parse(String[] args) throws CliException
    {
        if (args == null || args.length == 0)
            return new CliArguments(null, null, null, CliArguments.OutputProfile.DEFAULT, CliArguments.OutputFormat.TEXT,
                            false, true, DEFAULT_LIMIT, DEFAULT_TREE_DEPTH, null, null, null);

        CliCommand command = null;
        CliCommand subjectCommand = null;
        File heapFile = null;
        CliArguments.OutputProfile profile = CliArguments.OutputProfile.DEFAULT;
        CliArguments.OutputFormat format = CliArguments.OutputFormat.TEXT;
        boolean formatExplicit = false;
        boolean verbose = false;
        boolean help = false;
        int limit = DEFAULT_LIMIT;
        String objectAddress = null;
        String oqlQuery = null;
        String queryCommand = null;

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
            }
            else if ("--limit".equals(arg)) //$NON-NLS-1$
            {
                limit = parseLimit(nextArg(args, ++ii, "--limit")); //$NON-NLS-1$
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
            else if (arg.startsWith("--command=")) //$NON-NLS-1$
            {
                queryCommand = arg.substring("--command=".length()); //$NON-NLS-1$
            }
            else if ("--command".equals(arg)) //$NON-NLS-1$
            {
                queryCommand = nextArg(args, ++ii, "--command"); //$NON-NLS-1$
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
                subjectCommand = CliCommand.parse(arg);
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

        if (command == null)
        {
            if (help)
            {
                return new CliArguments(null, null, null, profile, format, verbose, true, limit, DEFAULT_TREE_DEPTH,
                                objectAddress, oqlQuery, queryCommand);
            }
            throw CliException.usage("Missing command"); //$NON-NLS-1$
        }

        CliArguments parsed = new CliArguments(command, subjectCommand, heapFile, profile, format, verbose, help,
                        limit, DEFAULT_TREE_DEPTH, objectAddress, oqlQuery, queryCommand);
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
            return limit;
        }
        catch (NumberFormatException e)
        {
            throw CliException.usage("Invalid limit: " + value); //$NON-NLS-1$
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
}
