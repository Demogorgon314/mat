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
        private final String format;
        private final String resultKind;
        private final String description;

        private OutputDefinition(String format, String resultKind, String description)
        {
            this.format = format;
            this.resultKind = resultKind;
            this.description = description;
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
    private static final List<OptionDefinition> FORMAT_LIMIT_CLASS_OPTIONS = Arrays.asList(
                    option("--class", "<fqcn>", false,
                                    "Exactly match one fully qualified class name. Cannot be combined with --class-regex or --class-contains."),
                    option("--class-regex", "<regex>", false,
                                    "Match class names with a full Java regex. Use .*String.* for contains matching. Cannot be combined with --class or --class-contains."),
                    option("--class-contains", "<text>", false,
                                    "Match class names containing plain text. Cannot be combined with --class or --class-regex."),
                    option("--include-subclasses", null, false, "Include subclasses of each matched class."),
                    option("--limit", "N", false, "Limit rows returned."),
                    option("--format", "text|json", false, "Select text or JSON output."));
    private static final List<OptionDefinition> FORMAT_LIMIT_AND_DEPTH_OPTIONS = Arrays.asList(
                    option("--limit", "N", false, "Limit rows, sections, or children per level."),
                    option("--depth", "N", false, "Limit nested tree or section depth."),
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
                        Arrays.asList(output("text", "summary", "Human-readable heap summary."),
                                        output("json", "summary", "Stable summary JSON envelope.")),
                        Arrays.asList("histogram <heap>", "top-consumers <heap>",
                                        "query <heap> --command \"thread_overview\""),
                        "summary object with snapshot-wide counters and heap metadata.",
                        Arrays.asList("summary.path", "summary.heapFormat", "summary.numberOfObjects",
                                        "summary.numberOfClasses", "summary.usedHeapSize")));
        definitions.put(CliCommand.THREADS, new CommandDefinition(CliCommand.THREADS,
                        "Show a best-effort thread report from the heap dump, including state, retained heap, and stack traces when available.",
                        "mat-cli threads <heap> [--limit N] [--format text|json]", true,
                        Collections.singletonList("heap"),
                        Arrays.asList(option("--limit", "N", false,
                                        "Limit threads returned. Defaults to all threads."),
                                        option("--format", "text|json", false, "Select text or JSON output.")),
                        Arrays.asList(output("text", "threads", "Thread report with overview plus per-thread stack sections."),
                                        output("json", "threads", "Stable thread report JSON.")),
                        Arrays.asList("histogram <heap>", "top-consumers <heap>"),
                        "thread report payload with a summary, best-effort notice, and one entry per returned thread.",
                        Arrays.asList("notice", "summary.totalThreads", "summary.returnedThreads",
                                        "summary.stackAvailableThreads", "summary.stateAvailableThreads", "threads[]",
                                        "threads[].name", "threads[].technicalName", "threads[].objectAddress",
                                        "threads[].state", "threads[].retainedBytes", "threads[].stackAvailable",
                                        "threads[].stackUnavailableReason", "threads[].stackFrames[]")));
        definitions.put(CliCommand.HISTOGRAM, new CommandDefinition(CliCommand.HISTOGRAM,
                        "Group objects by class and report shallow heap plus approximate retained heap.",
                        "mat-cli histogram <heap> [--limit N] [--format text|json]", true,
                        Collections.singletonList("heap"), FORMAT_AND_LIMIT_OPTIONS,
                        Arrays.asList(output("text", "table", "Text table with formatted columns."),
                                        output("json", "table", "Stable keyed table JSON.")),
                        Arrays.asList("top-consumers <heap>",
                                        "query <heap> --command \"histogram\""),
                        "table payload with stable column ids, normalized byte values, optional row addresses, and per-cell metadata for approximate retained sizes.",
                        Arrays.asList("items[]", "items[]._address when no address column",
                                        "items[]._meta.retained_heap.kind when approximate")));
        definitions.put(CliCommand.INSTANCES, new CommandDefinition(CliCommand.INSTANCES,
                        "List live objects for one class so you can pick a concrete instance to inspect.",
                        "mat-cli instances <heap> [--class <fqcn> | --class-regex <regex> | --class-contains <text>] [--include-subclasses] [--limit N] [--format text|json]",
                        true, Collections.singletonList("heap"), FORMAT_LIMIT_CLASS_OPTIONS,
                        Arrays.asList(output("text", "table", "Text table of matching objects."),
                                        output("json", "table", "Stable keyed table JSON for matching objects.")),
                        Arrays.asList("inspect-object <heap> --object 0x...",
                                        "path2gc <heap> --object 0x..."),
                        "table payload listing matching objects with addresses, previews, and heap sizes.",
                        Arrays.asList("items[]", "items[].object_address", "items[].class_name",
                                        "items[].preview")));
        definitions.put(CliCommand.INSPECT_OBJECT, new CommandDefinition(CliCommand.INSPECT_OBJECT,
                        "Inspect one object like MAT's object inspector, or jump directly to one field path for targeted state checks.",
                        "mat-cli inspect-object <heap> --object 0x... [--select-field FIELD | --field-path PATH] [--show-nulls] [--limit N] [--depth N] [--format text|json]",
                        true, Collections.singletonList("heap"), Arrays.asList(option("--object", "0x...", true,
                                        "Object address to inspect."),
                                        option("--select-field", "FIELD", false,
                                                        "Inspect one direct field from the root object. Cannot be combined with --field-path."),
                                        option("--field-path", "PATH", false,
                                                        "Inspect a dotted field path such as cleaner.offsetMap. Cannot be combined with --select-field."),
                                        option("--show-nulls", null, false,
                                                        "Show nested null fields and array slots in text output."),
                                        option("--limit", "N", false, "Limit children per level."),
                                        option("--depth", "N", false,
                                                        "Limit nested object expansion depth. Defaults to 3 for inspect-object when omitted."),
                                        option("--format", "text|json", false, "Select text or JSON output.")),
                        Arrays.asList(output("text", "tree", "Indented object-inspector tree."),
                                        output("json", "tree", "Stable keyed object-inspector tree JSON.")),
                        Arrays.asList("path2gc <heap> --object 0x...",
                                        "oql <heap> --query \"SELECT * FROM OBJECTS 0x...\""),
                        "tree payload with field and element nodes, stable paths, value kinds, concrete values, targeted field-path roots, preview metadata, sparse child arrays, and optional row addresses.",
                        Arrays.asList("items[]", "items[].path", "items[].valueKind",
                                        "items[].hasChildren", "items[].kind", "items[].name",
                                        "items[].value", "items[].object_address", "items[]._children[] when returned",
                                        "items[]._childrenTruncated when true", "items[]._address when no address column",
                                        "items[]._meta.value.kind when previewed",
                                        "items[]._meta.value.length when previewed",
                                        "items[]._meta.value.encoding when byte[] previewed")));
        definitions.put(CliCommand.TOP_CONSUMERS, new CommandDefinition(CliCommand.TOP_CONSUMERS,
                        "Show the largest dominators grouped the same way as MAT top consumers.",
                        "mat-cli top-consumers <heap> [--limit N] [--depth N] [--format text|json]", true,
                        Collections.singletonList("heap"), FORMAT_LIMIT_AND_DEPTH_OPTIONS,
                        Arrays.asList(output("text", "top-consumers", "Text report matching MAT top consumers."),
                                        output("json", "top-consumers",
                                                        "Stable aggregated JSON without display-only duplicates.")),
                        Arrays.asList("histogram <heap>",
                                        "path2gc <heap> --object 0x..."),
                        "aggregated payload with biggestObjects, classes, classLoaders, and packages.",
                        Arrays.asList("totalRetainedHeap", "biggestObjects[]",
                                        "biggestObjects[].objectAddress", "biggestObjectsTruncated",
                                        "classes[]", "classes[].objectAddress", "classesTruncated",
                                        "classLoaders[]", "classLoaders[].objectAddress",
                                        "classLoadersTruncated", "packages", "packagesTruncated")));
        definitions.put(CliCommand.PATH2GC, new CommandDefinition(CliCommand.PATH2GC,
                        "Find paths from an object to GC roots using MAT's native query.",
                        "mat-cli path2gc <heap> --object 0x... [--limit N] [--depth N] [--format text|json]", true,
                        Collections.singletonList("heap"), Arrays.asList(option("--object", "0x...", true,
                                        "Object address to resolve from the snapshot."),
                                        option("--limit", "N", false, "Limit children per level."),
                                        option("--depth", "N", false, "Limit nested tree depth."),
                                        option("--format", "text|json", false, "Select text or JSON output.")),
                        Arrays.asList(output("text", "tree", "Indented tree view."),
                                        output("json", "tree", "Stable keyed tree JSON.")),
                        Arrays.asList("inspect-object <heap> --object 0x...",
                                        "oql <heap> --query \"SELECT * FROM OBJECTS 0x...\""),
                        "tree payload with keyed nodes, stable paths, value kinds, sparse child arrays, and optional row addresses.",
                        Arrays.asList("items[]", "items[].path", "items[].valueKind",
                                        "items[].hasChildren", "items[]._children[] when returned",
                                        "items[]._childrenTruncated when true", "items[]._address when no address column")));
        definitions.put(CliCommand.OQL, new CommandDefinition(CliCommand.OQL,
                        "Run a MAT OQL query directly against the snapshot.",
                        "mat-cli oql <heap> --query \"...\" [--limit N] [--depth N] [--format text|json]", true,
                        Collections.singletonList("heap"), Arrays.asList(option("--query", "\"...\"", true,
                                        "OQL query string."),
                                        option("--query-file", "PATH", false,
                                                        "Read OQL text from a UTF-8 file to avoid shell quoting issues such as inner classes with '$'."),
                                        option("--query-stdin", null, false, "Read OQL text from stdin."),
                                        option("--limit", "N", false, "Limit rows, sections, or children per level."),
                                        option("--depth", "N", false, "Limit nested tree or section depth."),
                                        option("--format", "text|json", false, "Select text or JSON output.")),
                        Arrays.asList(output("text", "text|table|tree|pie", "Depends on the OQL result."),
                                        output("json", "text|table|tree|pie",
                                                        "Stable JSON envelope around the resolved result kind.")),
                        Arrays.asList("histogram <heap>",
                                        "query <heap> --command \"histogram\""),
                        "same payload contract as the resolved result kind returned by the OQL query.",
                        Arrays.asList("resultKind", "items[] when table/tree",
                                        "items[].path/valueKind/hasChildren when tree",
                                        "slices[] when pie", "content when text")));
        definitions.put(CliCommand.QUERY, new CommandDefinition(CliCommand.QUERY,
                        "Run a MAT query command string through SnapshotQuery.parse(...).",
                        "mat-cli query <heap> --command \"...\" [--limit N] [--depth N] [--format text|json]", true,
                        Collections.singletonList("heap"), Arrays.asList(option("--command", "\"...\"", true,
                                        "MAT query command string."),
                                        option("--command-file", "PATH", false,
                                                        "Read MAT query text from a UTF-8 file to avoid shell quoting issues such as inner classes with '$'."),
                                        option("--command-stdin", null, false, "Read MAT query text from stdin."),
                                        option("--limit", "N", false, "Limit rows, sections, or children per level."),
                                        option("--depth", "N", false, "Limit nested tree or section depth."),
                                        option("--format", "text|json", false, "Select text or JSON output.")),
                        Arrays.asList(output("text", "text|table|tree|section|pie|top-consumers",
                                        "Depends on the resolved query result."),
                                        output("json", "text|table|tree|section|pie|top-consumers",
                                                        "Stable JSON envelope around the resolved result kind.")),
                        Arrays.asList("schema histogram",
                                        "describe top-consumers"),
                        "same payload contract as the resolved result kind returned by the parsed query.",
                        Arrays.asList("resultKind", "items[] when table/tree",
                                        "items[].path/valueKind/hasChildren when tree",
                                        "slices[] when pie", "sections[] when section")));
        definitions.put(CliCommand.DESCRIBE, new CommandDefinition(CliCommand.DESCRIBE,
                        "Describe a CLI command, its options, and the result kinds it can return.",
                        "mat-cli describe <command> [--format text|json]", false,
                        Collections.singletonList("command"), FORMAT_OPTION,
                        Arrays.asList(output("text", "describe", "Human-readable command description."),
                                        output("json", "describe", "Stable command metadata JSON.")),
                        Arrays.asList("schema <command>"), "metadata payload for one command definition.",
                        Arrays.asList("name", "summary", "usage", "requiresSnapshot", "options[]", "outputs[]",
                                        "suggestedNextCommands via top-level envelope")));
        definitions.put(CliCommand.SCHEMA, new CommandDefinition(CliCommand.SCHEMA,
                        "Describe the stable JSON contract for a CLI command.",
                        "mat-cli schema <command> [--format text|json]", false,
                        Collections.singletonList("command"), FORMAT_OPTION,
                        Arrays.asList(output("text", "schema", "Readable contract summary."),
                                        output("json", "schema", "Stable contract metadata JSON.")),
                        Arrays.asList("describe <command>"), "JSON envelope and payload contract summary.",
                        Arrays.asList("jsonEnvelope", "payloadKind", "payloadFields[]", "outputs[]")));
        definitions.put(CliCommand.LIST_QUERIES, new CommandDefinition(CliCommand.LIST_QUERIES,
                        "List the registered MAT query commands available through SnapshotQuery/QueryRegistry.",
                        "mat-cli list-queries [--format text|json]", false, Collections.<String>emptyList(),
                        FORMAT_OPTION,
                        Arrays.asList(output("text", "query-list", "List MAT query identifiers and summaries."),
                                        output("json", "query-list", "Stable query registry metadata JSON.")),
                        Arrays.asList("describe-query histogram",
                                        "query <heap> --command \"histogram\""),
                        "array of MAT query descriptors including identifiers, usage, and arguments.",
                        Arrays.asList("queries[]", "queries[].identifier", "queries[].usage",
                                        "queries[].arguments[]")));
        definitions.put(CliCommand.DESCRIBE_QUERY, new CommandDefinition(CliCommand.DESCRIBE_QUERY,
                        "Describe one registered MAT query, including its arguments and help text.",
                        "mat-cli describe-query <query-id> [--format text|json]", false,
                        Collections.singletonList("query-id"), FORMAT_OPTION,
                        Arrays.asList(output("text", "query-description", "Human-readable MAT query metadata."),
                                        output("json", "query-description", "Stable MAT query metadata JSON.")),
                        Arrays.asList("list-queries",
                                        "query <heap> --command \"<query>\""),
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

    private static OutputDefinition output(String format, String resultKind, String description)
    {
        return new OutputDefinition(format, resultKind, description);
    }
}
