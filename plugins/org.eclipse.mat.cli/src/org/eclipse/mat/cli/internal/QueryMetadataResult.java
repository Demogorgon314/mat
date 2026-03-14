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

import java.util.List;

import org.eclipse.mat.query.IResult;
import org.eclipse.mat.query.ResultMetaData;

public final class QueryMetadataResult implements IResult
{
    public enum Kind
    {
        LIST, DESCRIBE
    }

    public static final class QueryArgument
    {
        private final String name;
        private final String flag;
        private final String type;
        private final String advice;
        private final boolean mandatory;
        private final boolean multiple;
        private final boolean bool;
        private final boolean enumeration;
        private final String defaultValue;
        private final String help;

        public QueryArgument(String name, String flag, String type, String advice, boolean mandatory, boolean multiple,
                        boolean bool, boolean enumeration, String defaultValue, String help)
        {
            this.name = name;
            this.flag = flag;
            this.type = type;
            this.advice = advice;
            this.mandatory = mandatory;
            this.multiple = multiple;
            this.bool = bool;
            this.enumeration = enumeration;
            this.defaultValue = defaultValue;
            this.help = help;
        }

        public String getName()
        {
            return name;
        }

        public String getFlag()
        {
            return flag;
        }

        public String getType()
        {
            return type;
        }

        public String getAdvice()
        {
            return advice;
        }

        public boolean isMandatory()
        {
            return mandatory;
        }

        public boolean isMultiple()
        {
            return multiple;
        }

        public boolean isBoolean()
        {
            return bool;
        }

        public boolean isEnumeration()
        {
            return enumeration;
        }

        public String getDefaultValue()
        {
            return defaultValue;
        }

        public String getHelp()
        {
            return help;
        }
    }

    public static final class QueryDefinition
    {
        private final String identifier;
        private final String name;
        private final String category;
        private final String usage;
        private final String summary;
        private final String help;
        private final String helpUrl;
        private final String commandClass;
        private final boolean shallow;
        private final List<String> subjects;
        private final List<QueryArgument> arguments;

        public QueryDefinition(String identifier, String name, String category, String usage, String summary, String help,
                        String helpUrl, String commandClass, boolean shallow, List<String> subjects,
                        List<QueryArgument> arguments)
        {
            this.identifier = identifier;
            this.name = name;
            this.category = category;
            this.usage = usage;
            this.summary = summary;
            this.help = help;
            this.helpUrl = helpUrl;
            this.commandClass = commandClass;
            this.shallow = shallow;
            this.subjects = subjects;
            this.arguments = arguments;
        }

        public String getIdentifier()
        {
            return identifier;
        }

        public String getName()
        {
            return name;
        }

        public String getCategory()
        {
            return category;
        }

        public String getUsage()
        {
            return usage;
        }

        public String getSummary()
        {
            return summary;
        }

        public String getHelp()
        {
            return help;
        }

        public String getHelpUrl()
        {
            return helpUrl;
        }

        public String getCommandClass()
        {
            return commandClass;
        }

        public boolean isShallow()
        {
            return shallow;
        }

        public List<String> getSubjects()
        {
            return subjects;
        }

        public List<QueryArgument> getArguments()
        {
            return arguments;
        }
    }

    private final Kind kind;
    private final QueryDefinition query;
    private final List<QueryDefinition> queries;

    public QueryMetadataResult(Kind kind, QueryDefinition query, List<QueryDefinition> queries)
    {
        this.kind = kind;
        this.query = query;
        this.queries = queries;
    }

    public Kind getKind()
    {
        return kind;
    }

    public QueryDefinition getQuery()
    {
        return query;
    }

    public List<QueryDefinition> getQueries()
    {
        return queries;
    }

    public ResultMetaData getResultMetaData()
    {
        return null;
    }
}
