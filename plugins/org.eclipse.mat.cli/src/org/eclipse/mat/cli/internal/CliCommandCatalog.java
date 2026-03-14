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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CliCommandCatalog
{
    public static final class OptionDefinition
    {
        private final String name;
        private final String valueHint;
        private final boolean required;
        private final String description;

        private OptionDefinition(String name, String valueHint, boolean required, String description)
        {
            this.name = name;
            this.valueHint = valueHint;
            this.required = required;
            this.description = description;
        }

        public String getName()
        {
            return name;
        }

        public String getValueHint()
        {
            return valueHint;
        }

        public boolean isRequired()
        {
            return required;
        }

        public String getDescription()
        {
            return description;
        }
    }

    public static final class OutputDefinition
    {
        private final String profile;
        private final String format;
        private final String resultKind;
        private final String description;

        private OutputDefinition(String profile, String format, String resultKind, String description)
        {
            this.profile = profile;
            this.format = format;
            this.resultKind = resultKind;
            this.description = description;
        }

        public String getProfile()
        {
            return profile;
        }

        public String getFormat()
        {
            return format;
        }

        public String getResultKind()
        {
            return resultKind;
        }

        public String getDescription()
        {
            return description;
        }
    }

    public static final class CommandDefinition
    {
        private final CliCommand command;
        private final String summary;
        private final String usage;
        private final boolean requiresSnapshot;
        private final List<String> positionalArguments;
        private final List<OptionDefinition> options;
        private final List<OutputDefinition> outputs;
        private final List<String> suggestedNextCommands;
        private final List<String> agentPayloadFields;
        private final String agentPayloadDescription;

        private CommandDefinition(CliCommand command, String summary, String usage, boolean requiresSnapshot,
                        List<String> positionalArguments, List<OptionDefinition> options, List<OutputDefinition> outputs,
                        List<String> suggestedNextCommands, String agentPayloadDescription,
                        List<String> agentPayloadFields)
        {
            this.command = command;
            this.summary = summary;
            this.usage = usage;
            this.requiresSnapshot = requiresSnapshot;
            this.positionalArguments = positionalArguments;
            this.options = options;
            this.outputs = outputs;
            this.suggestedNextCommands = suggestedNextCommands;
            this.agentPayloadDescription = agentPayloadDescription;
            this.agentPayloadFields = agentPayloadFields;
        }

        public CliCommand getCommand()
        {
            return command;
        }

        public String getSummary()
        {
            return summary;
        }

        public String getUsage()
        {
            return usage;
        }

        public boolean requiresSnapshot()
        {
            return requiresSnapshot;
        }

        public List<String> getPositionalArguments()
        {
            return positionalArguments;
        }

        public List<OptionDefinition> getOptions()
        {
            return options;
        }

        public List<OutputDefinition> getOutputs()
        {
            return outputs;
        }

        public List<String> getSuggestedNextCommands()
        {
            return suggestedNextCommands;
        }

        public List<String> getAgentPayloadFields()
        {
            return agentPayloadFields;
        }

        public String getAgentPayloadDescription()
        {
            return agentPayloadDescription;
        }
    }

    private static final List<OptionDefinition> FORMAT_OPTION = Collections.singletonList(
                    option("--format", "text|json", false, "Select text or JSON output."));
    private static final List<OptionDefinition> FORMAT_AND_LIMIT_OPTIONS = Arrays.asList(
                    option("--limit", "N", false, "Limit rows or children per level."),
                    option("--format", "text|json", false, "Select text or JSON output."));
    private static final Map<String, Integer> QUERY_LIMIT_OVERRIDES = queryLimitOverrides();

    private static final Map<CliCommand, CommandDefinition> DEFINITIONS = definitions();

    private CliCommandCatalog()
    {}

    public static CommandDefinition lookup(CliCommand command)
    {
        return DEFINITIONS.get(command);
    }

    public static Integer queryDefaultLimit(String queryIdentifier)
    {
        return queryIdentifier == null ? null : QUERY_LIMIT_OVERRIDES.get(queryIdentifier);
    }

    private static Map<CliCommand, CommandDefinition> definitions()
    {
        EnumMap<CliCommand, CommandDefinition> definitions = new EnumMap<CliCommand, CommandDefinition>(CliCommand.class);
        definitions.put(CliCommand.SUMMARY, new CommandDefinition(CliCommand.SUMMARY,
                        "Read basic heap metadata such as object counts and used heap.",
                        "mat-cli summary <heap> [--format text|json]", true, Collections.singletonList("heap"), FORMAT_OPTION,
                        Arrays.asList(output("default", "text", "summary", "Human-readable heap summary."),
                                        output("default", "json", "summary", "Summary JSON envelope."),
                                        output("agent", "json", "summary", "Stable summary envelope for agents.")),
                        Arrays.asList("histogram <heap> --agent", "top-consumers <heap> --agent"),
                        "summary object with snapshot-wide counters and heap metadata.",
                        Arrays.asList("summary.path", "summary.heapFormat", "summary.numberOfObjects",
                                        "summary.numberOfClasses", "summary.usedHeapSize")));
        definitions.put(CliCommand.HISTOGRAM, new CommandDefinition(CliCommand.HISTOGRAM,
                        "Group objects by class and report retained and shallow heap.",
                        "mat-cli histogram <heap> [--limit N] [--format text|json]", true,
                        Collections.singletonList("heap"), FORMAT_AND_LIMIT_OPTIONS,
                        Arrays.asList(output("default", "text", "table", "Text table with formatted columns."),
                                        output("default", "json", "table", "MAT table JSON with columns and rows."),
                                        output("agent", "json", "table", "Stable table schema plus keyed items.")),
                        Arrays.asList("top-consumers <heap> --agent", "query <heap> --command \"histogram\" --agent"),
                        "table payload with stable column ids, raw values, and row context.",
                        Arrays.asList("schema.columns[].id", "schema.columns[].jsonType", "items[]",
                                        "items[]._context.objectId")));
        definitions.put(CliCommand.TOP_CONSUMERS, new CommandDefinition(CliCommand.TOP_CONSUMERS,
                        "Show the largest dominators grouped the same way as MAT top consumers.",
                        "mat-cli top-consumers <heap> [--limit N] [--format text|json]", true,
                        Collections.singletonList("heap"), FORMAT_AND_LIMIT_OPTIONS,
                        Arrays.asList(output("default", "text", "top-consumers", "Text report matching MAT top consumers."),
                                        output("default", "json", "top-consumers", "Compact aggregated JSON."),
                                        output("agent", "json", "top-consumers",
                                                        "Stable aggregated JSON without display-only duplicates.")),
                        Arrays.asList("histogram <heap> --agent", "path2gc <heap> --object 0x... --agent"),
                        "aggregated payload with biggestObjects, classes, classLoaders, and packages.",
                        Arrays.asList("totalRetainedHeap", "biggestObjects[]", "biggestObjectsTruncated", "classes[]",
                                        "classesTruncated", "classLoaders[]", "classLoadersTruncated", "packages",
                                        "packagesTruncated")));
        definitions.put(CliCommand.PATH2GC, new CommandDefinition(CliCommand.PATH2GC,
                        "Find paths from an object to GC roots using MAT's native query.",
                        "mat-cli path2gc <heap> --object 0x... [--format text|json]", true,
                        Collections.singletonList("heap"), Arrays.asList(option("--object", "0x...", true,
                                        "Object address to resolve from the snapshot."),
                                        option("--format", "text|json", false, "Select text or JSON output."),
                                        option("--limit", "N", false, "Limit children per level.")),
                        Arrays.asList(output("default", "text", "tree", "Indented tree view."),
                                        output("default", "json", "tree", "MAT tree JSON with columns and rows."),
                                        output("agent", "json", "tree", "Stable tree schema and keyed nodes.")),
                        Arrays.asList("oql <heap> --query \"SELECT * FROM OBJECTS 0x...\" --agent",
                                        "summary <heap> --agent"),
                        "tree payload with stable column ids, keyed nodes, and explicit child truncation flags.",
                        Arrays.asList("schema.columns[].id", "items[]", "items[]._children[]",
                                        "items[]._childrenTruncated", "items[]._context.objectId")));
        definitions.put(CliCommand.OQL, new CommandDefinition(CliCommand.OQL,
                        "Run a MAT OQL query directly against the snapshot.",
                        "mat-cli oql <heap> --query \"...\" [--format text|json]", true,
                        Collections.singletonList("heap"), Arrays.asList(option("--query", "\"...\"", true,
                                        "OQL query string."),
                                        option("--query-file", "PATH", false,
                                                        "Read OQL text from a UTF-8 file."),
                                        option("--query-stdin", null, false, "Read OQL text from stdin."),
                                        option("--format", "text|json", false, "Select text or JSON output."),
                                        option("--limit", "N", false, "Limit rows or children per level.")),
                        Arrays.asList(output("default", "text", "text|table|tree|pie", "Depends on the OQL result."),
                                        output("default", "json", "text|table|tree|pie", "Depends on the OQL result."),
                                        output("agent", "json", "text|table|tree|pie",
                                                        "Stable JSON envelope around the resolved result kind.")),
                        Arrays.asList("histogram <heap> --agent", "query <heap> --command \"histogram\" --agent"),
                        "same payload contract as the resolved result kind returned by the OQL query.",
                        Arrays.asList("resultKind", "schema.columns[] when table/tree", "items[] when table/tree",
                                        "slices[] when pie", "content when text")));
        definitions.put(CliCommand.QUERY, new CommandDefinition(CliCommand.QUERY,
                        "Run a MAT query command string through SnapshotQuery.parse(...).",
                        "mat-cli query <heap> --command \"...\" [--format text|json]", true,
                        Collections.singletonList("heap"), Arrays.asList(option("--command", "\"...\"", true,
                                        "MAT query command string."),
                                        option("--command-file", "PATH", false,
                                                        "Read MAT query text from a UTF-8 file."),
                                        option("--command-stdin", null, false, "Read MAT query text from stdin."),
                                        option("--format", "text|json", false, "Select text or JSON output."),
                                        option("--limit", "N", false, "Limit rows or children per level.")),
                        Arrays.asList(output("default", "text", "text|table|tree|section|pie|top-consumers",
                                        "Depends on the resolved query result."),
                                        output("default", "json", "text|table|tree|section|pie|top-consumers",
                                                        "Depends on the resolved query result."),
                                        output("agent", "json", "text|table|tree|section|pie|top-consumers",
                                                        "Stable JSON envelope around the resolved result kind.")),
                        Arrays.asList("schema histogram --agent", "describe top-consumers --agent"),
                        "same payload contract as the resolved result kind returned by the parsed query.",
                        Arrays.asList("resultKind", "schema.columns[] when table/tree", "items[] when table/tree",
                                        "slices[] when pie", "sections[] when section")));
        definitions.put(CliCommand.DESCRIBE, new CommandDefinition(CliCommand.DESCRIBE,
                        "Describe a CLI command, its options, and the result kinds it can return.",
                        "mat-cli describe <command> [--format text|json]", false,
                        Collections.singletonList("command"), FORMAT_OPTION,
                        Arrays.asList(output("default", "text", "describe", "Human-readable command description."),
                                        output("default", "json", "describe", "Command metadata as JSON."),
                                        output("agent", "json", "describe", "Stable metadata envelope for agents.")),
                        Arrays.asList("schema <command> --agent"), "metadata payload for one command definition.",
                        Arrays.asList("name", "summary", "usage", "requiresSnapshot", "options[]", "outputs[]",
                                        "suggestedNextCommands[]")));
        definitions.put(CliCommand.SCHEMA, new CommandDefinition(CliCommand.SCHEMA,
                        "Describe the stable agent JSON contract for a CLI command.",
                        "mat-cli schema <command> [--format text|json]", false,
                        Collections.singletonList("command"), FORMAT_OPTION,
                        Arrays.asList(output("default", "text", "schema", "Readable contract summary."),
                                        output("default", "json", "schema", "Contract metadata as JSON."),
                                        output("agent", "json", "schema", "Stable schema envelope for agents.")),
                        Arrays.asList("describe <command> --agent"), "agent envelope and payload contract summary.",
                        Arrays.asList("agentEnvelope", "payloadKind", "payloadFields[]", "outputs[]")));
        definitions.put(CliCommand.LIST_QUERIES, new CommandDefinition(CliCommand.LIST_QUERIES,
                        "List the registered MAT query commands available through SnapshotQuery/QueryRegistry.",
                        "mat-cli list-queries [--format text|json]", false, Collections.<String>emptyList(),
                        FORMAT_OPTION,
                        Arrays.asList(output("default", "text", "query-list", "List MAT query identifiers and summaries."),
                                        output("default", "json", "query-list", "Query registry metadata as JSON."),
                                        output("agent", "json", "query-list",
                                                        "Stable query discovery payload for agents.")),
                        Arrays.asList("describe-query histogram --agent", "query <heap> --command \"histogram\" --agent"),
                        "array of MAT query descriptors including identifiers, usage, and arguments.",
                        Arrays.asList("queries[]", "queries[].identifier", "queries[].usage",
                                        "queries[].arguments[]")));
        definitions.put(CliCommand.DESCRIBE_QUERY, new CommandDefinition(CliCommand.DESCRIBE_QUERY,
                        "Describe one registered MAT query, including its arguments and help text.",
                        "mat-cli describe-query <query-id> [--format text|json]", false,
                        Collections.singletonList("query-id"), FORMAT_OPTION,
                        Arrays.asList(output("default", "text", "query-description",
                                        "Human-readable MAT query metadata."),
                                        output("default", "json", "query-description", "MAT query metadata as JSON."),
                                        output("agent", "json", "query-description",
                                                        "Stable MAT query metadata envelope for agents.")),
                        Arrays.asList("list-queries --agent", "query <heap> --command \"<query>\" --agent"),
                        "single MAT query descriptor with argument metadata, help, and subjects.",
                        Arrays.asList("query.identifier", "query.usage", "query.arguments[]", "query.subjects[]")));
        return definitions;
    }

    private static OptionDefinition option(String name, String valueHint, boolean required, String description)
    {
        return new OptionDefinition(name, valueHint, required, description);
    }

    private static Map<String, Integer> queryLimitOverrides()
    {
        Map<String, Integer> limits = new HashMap<String, Integer>();
        limits.put("gc_roots", Integer.valueOf(100)); //$NON-NLS-1$
        limits.put("thread_overview", Integer.valueOf(100)); //$NON-NLS-1$
        return Collections.unmodifiableMap(limits);
    }

    private static OutputDefinition output(String profile, String format, String resultKind, String description)
    {
        return new OutputDefinition(profile, format, resultKind, description);
    }
}
