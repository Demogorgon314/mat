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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CliCommandCatalog
{
    public static final class PositionalDefinition
    {
        private final String name;
        private final CompletionValueType completionValueType;
        private final List<String> completionCandidates;

        private PositionalDefinition(String name, CompletionValueType completionValueType,
                        List<String> completionCandidates)
        {
            this.name = name;
            this.completionValueType = completionValueType;
            this.completionCandidates = immutableCopy(completionCandidates);
        }

        public String getName()
        {
            return name;
        }

        public CompletionValueType getCompletionValueType()
        {
            return completionValueType;
        }

        public List<String> getCompletionCandidates()
        {
            return completionCandidates;
        }
    }

    public static final class OptionDefinition
    {
        private final String name;
        private final String valueHint;
        private final boolean required;
        private final String description;
        private final CompletionValueType completionValueType;
        private final List<String> completionCandidates;

        private OptionDefinition(String name, String valueHint, boolean required, String description,
                        CompletionValueType completionValueType, List<String> completionCandidates)
        {
            this.name = name;
            this.valueHint = valueHint;
            this.required = required;
            this.description = description;
            this.completionValueType = completionValueType;
            this.completionCandidates = immutableCopy(completionCandidates);
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

        public CompletionValueType getCompletionValueType()
        {
            return completionValueType;
        }

        public List<String> getCompletionCandidates()
        {
            return completionCandidates;
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
        private final List<PositionalDefinition> positionalArguments;
        private final List<OptionDefinition> options;
        private final List<OutputDefinition> outputs;
        private final List<String> suggestedNextCommands;
        private final List<String> agentPayloadFields;
        private final String agentPayloadDescription;

        private CommandDefinition(CliCommand command, String summary, String usage, boolean requiresSnapshot,
                        List<PositionalDefinition> positionalArguments, List<OptionDefinition> options,
                        List<OutputDefinition> outputs, List<String> suggestedNextCommands,
                        String agentPayloadDescription, List<String> agentPayloadFields)
        {
            this.command = command;
            this.summary = summary;
            this.usage = usage;
            this.requiresSnapshot = requiresSnapshot;
            this.positionalArguments = immutableCopy(positionalArguments);
            this.options = immutableCopy(options);
            this.outputs = immutableCopy(outputs);
            this.suggestedNextCommands = immutableCopy(suggestedNextCommands);
            this.agentPayloadDescription = agentPayloadDescription;
            this.agentPayloadFields = immutableCopy(agentPayloadFields);
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
            List<String> names = new ArrayList<String>(positionalArguments.size());
            for (PositionalDefinition positionalArgument : positionalArguments)
            {
                names.add(positionalArgument.getName());
            }
            return Collections.unmodifiableList(names);
        }

        public List<PositionalDefinition> getPositionalDefinitions()
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

    private static final List<String> FORMAT_VALUES = immutableCopy(Arrays.asList("text", "json")); //$NON-NLS-1$ //$NON-NLS-2$
    private static final List<String> BYTES_DISPLAY_VALUES = immutableCopy(
                    Arrays.asList("bytes", "kilobytes", "megabytes", "gigabytes", "smart")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    private static final List<String> COMPLETION_SHELLS = immutableCopy(Arrays.asList("bash", "zsh")); //$NON-NLS-1$ //$NON-NLS-2$
    private static final List<String> COMMAND_TOKENS = buildCommandTokens();
    private static final List<OptionDefinition> GLOBAL_OPTIONS = immutableCopy(Arrays.asList(
                    flagOption("--help", "Show general or command-specific help."), //$NON-NLS-1$ //$NON-NLS-2$
                    flagOption("--verbose", "Print detailed diagnostics on failure."), //$NON-NLS-1$ //$NON-NLS-2$
                    enumOption("--format", "text|json", false, "Select text or JSON output.", FORMAT_VALUES))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    private static final OptionDefinition BYTES_DISPLAY_OPTION = enumOption("--bytes-display",
                    "bytes|kilobytes|megabytes|gigabytes|smart", false, //$NON-NLS-1$ //$NON-NLS-2$
                    "Select byte unit rendering for text output. Defaults to smart.", BYTES_DISPLAY_VALUES); //$NON-NLS-1$
    private static final List<OptionDefinition> FORMAT_OPTION = Collections.singletonList(
                    enumOption("--format", "text|json", false, "Select text or JSON output.", FORMAT_VALUES)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    private static final List<OptionDefinition> RESULT_FORMAT_OPTION = Arrays.asList(BYTES_DISPLAY_OPTION,
                    enumOption("--format", "text|json", false, "Select text or JSON output.", FORMAT_VALUES)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    private static final List<OptionDefinition> FORMAT_AND_LIMIT_OPTIONS = Arrays.asList(
                    freeTextOption("--limit", "N", false, "Limit rows or children per level."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    BYTES_DISPLAY_OPTION,
                    enumOption("--format", "text|json", false, "Select text or JSON output.", FORMAT_VALUES)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    private static final List<OptionDefinition> FORMAT_LIMIT_CLASS_OPTIONS = Arrays.asList(
                    freeTextOption("--class", "<fqcn>", false, //$NON-NLS-1$ //$NON-NLS-2$
                                    "Exactly match one fully qualified class name. Cannot be combined with --class-regex or --class-contains."), //$NON-NLS-1$
                    freeTextOption("--class-regex", "<regex>", false, //$NON-NLS-1$ //$NON-NLS-2$
                                    "Match class names with a full Java regex. Use .*String.* for contains matching. Cannot be combined with --class or --class-contains."), //$NON-NLS-1$
                    freeTextOption("--class-contains", "<text>", false, //$NON-NLS-1$ //$NON-NLS-2$
                                    "Match class names containing plain text. Cannot be combined with --class or --class-regex."), //$NON-NLS-1$
                    flagOption("--include-subclasses", "Include subclasses of each matched class."), //$NON-NLS-1$ //$NON-NLS-2$
                    freeTextOption("--limit", "N", false, "Limit rows returned."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    BYTES_DISPLAY_OPTION,
                    enumOption("--format", "text|json", false, "Select text or JSON output.", FORMAT_VALUES)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    private static final List<OptionDefinition> FORMAT_LIMIT_AND_DEPTH_OPTIONS = Arrays.asList(
                    freeTextOption("--limit", "N", false, "Limit rows, sections, or children per level."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    freeTextOption("--depth", "N", false, "Limit nested tree or section depth."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    BYTES_DISPLAY_OPTION,
                    enumOption("--format", "text|json", false, "Select text or JSON output.", FORMAT_VALUES)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

    public static List<OptionDefinition> globalOptions()
    {
        return GLOBAL_OPTIONS;
    }

    public static List<String> supportedCompletionShells()
    {
        return COMPLETION_SHELLS;
    }

    public static List<String> commandTokens()
    {
        return COMMAND_TOKENS;
    }

    public static List<OptionDefinition> completionOptions(CliCommand command)
    {
        CommandDefinition definition = lookup(command);
        java.util.LinkedHashMap<String, OptionDefinition> options = new java.util.LinkedHashMap<String, OptionDefinition>();
        if (definition != null)
        {
            for (OptionDefinition option : definition.getOptions())
            {
                options.put(option.getName(), option);
            }
        }
        for (OptionDefinition globalOption : GLOBAL_OPTIONS)
        {
            if (!"--format".equals(globalOption.getName())) //$NON-NLS-1$
                options.put(globalOption.getName(), globalOption);
        }
        return immutableCopy(new ArrayList<OptionDefinition>(options.values()));
    }

    private static Map<CliCommand, CommandDefinition> definitions()
    {
        EnumMap<CliCommand, CommandDefinition> definitions = new EnumMap<CliCommand, CommandDefinition>(CliCommand.class);
        List<PositionalDefinition> heapArgument = Collections.singletonList(filePositional("heap")); //$NON-NLS-1$
        List<PositionalDefinition> commandArgument = Collections.singletonList(enumPositional("command", COMMAND_TOKENS)); //$NON-NLS-1$
        List<PositionalDefinition> queryIdArgument = Collections.singletonList(freeTextPositional("query-id")); //$NON-NLS-1$
        List<PositionalDefinition> completionShellArgument = Collections.singletonList(
                        enumPositional("bash|zsh", COMPLETION_SHELLS)); //$NON-NLS-1$

        definitions.put(CliCommand.SUMMARY, new CommandDefinition(CliCommand.SUMMARY,
                        "Read basic heap metadata such as object counts and used heap.", //$NON-NLS-1$
                        "mat-cli summary <heap> [--bytes-display bytes|kilobytes|megabytes|gigabytes|smart] [--format text|json]", true, heapArgument, RESULT_FORMAT_OPTION, //$NON-NLS-1$
                        Arrays.asList(output("text", "summary", "Human-readable heap summary."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        output("json", "summary", "Compact mat-cli/v1 summary JSON envelope.")), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        Arrays.asList("histogram <heap>", "top-consumers <heap>", //$NON-NLS-1$ //$NON-NLS-2$
                                        "query <heap> --command \"thread_overview\""), //$NON-NLS-1$
                        "compact summary object with snapshot-wide counters and heap metadata, omitting absent strings.", //$NON-NLS-1$
                        Arrays.asList("summary.path", "summary.heapFormat", "summary.numberOfObjects", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        "summary.numberOfClasses", "summary.usedHeapSize"))); //$NON-NLS-1$ //$NON-NLS-2$
        definitions.put(CliCommand.THREADS, new CommandDefinition(CliCommand.THREADS,
                        "Show a best-effort thread report from the heap dump, including state, retained heap, and stack traces when available.", //$NON-NLS-1$
                        "mat-cli threads <heap> [--limit N] [--bytes-display bytes|kilobytes|megabytes|gigabytes|smart] [--format text|json]", true, heapArgument, //$NON-NLS-1$
                        Arrays.asList(freeTextOption("--limit", "N", false, "Limit threads returned. Defaults to all threads."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        BYTES_DISPLAY_OPTION,
                                        enumOption("--format", "text|json", false, "Select text or JSON output.", FORMAT_VALUES)), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        Arrays.asList(output("text", "threads", "Thread report with overview plus per-thread stack sections."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        output("json", "threads", "Compact mat-cli/v1 thread report JSON.")), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        Arrays.asList("histogram <heap>", "top-consumers <heap>"), //$NON-NLS-1$ //$NON-NLS-2$
                        "thread report payload with a summary, optional notice, and one compact entry per returned thread.", //$NON-NLS-1$
                        Arrays.asList("notice", "summary.totalThreads", "summary.returnedThreads", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        "summary.stackAvailableThreads", "summary.stateAvailableThreads", "threads[]", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        "threads[].name", "threads[].technicalName", "threads[].objectAddress", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        "threads[].state", "threads[].retainedBytes", "threads[].stackAvailable", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        "threads[].stackUnavailableReason when unavailable", "threads[].stackFrames[] when non-empty"))); //$NON-NLS-1$ //$NON-NLS-2$
        definitions.put(CliCommand.HISTOGRAM, new CommandDefinition(CliCommand.HISTOGRAM,
                        "Group objects by class and report shallow heap plus approximate retained heap.", //$NON-NLS-1$
                        "mat-cli histogram <heap> [--limit N] [--bytes-display bytes|kilobytes|megabytes|gigabytes|smart] [--format text|json]", true, heapArgument, //$NON-NLS-1$
                        FORMAT_AND_LIMIT_OPTIONS,
                        Arrays.asList(output("text", "table", "Text table with formatted columns."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        output("json", "table", "Compact keyed table JSON.")), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        Arrays.asList("top-consumers <heap>", "query <heap> --command \"histogram\""), //$NON-NLS-1$ //$NON-NLS-2$
                        "table payload with normalized column ids, non-null cell values only, optional row addresses, and per-cell metadata for approximate retained sizes.", //$NON-NLS-1$
                        Arrays.asList("items[]", "items[]._address when no address column", //$NON-NLS-1$ //$NON-NLS-2$
                                        "items[]._meta.retained_heap.kind when approximate"))); //$NON-NLS-1$
        definitions.put(CliCommand.INSTANCES, new CommandDefinition(CliCommand.INSTANCES,
                        "List live objects for one class so you can pick a concrete instance to inspect.", //$NON-NLS-1$
                        "mat-cli instances <heap> [--class <fqcn> | --class-regex <regex> | --class-contains <text>] [--include-subclasses] [--limit N] [--bytes-display bytes|kilobytes|megabytes|gigabytes|smart] [--format text|json]", //$NON-NLS-1$
                        true, heapArgument, FORMAT_LIMIT_CLASS_OPTIONS,
                        Arrays.asList(output("text", "table", "Text table of matching objects."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        output("json", "table", "Compact keyed table JSON for matching objects.")), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        Arrays.asList("inspect-object <heap> --object 0x...", "path2gc <heap> --object 0x..."), //$NON-NLS-1$ //$NON-NLS-2$
                        "table payload listing matching objects with non-null fields, addresses, previews, and heap sizes.", //$NON-NLS-1$
                        Arrays.asList("items[]", "items[].object_address", "items[].class_name", "items[].preview"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        definitions.put(CliCommand.INSPECT_OBJECT, new CommandDefinition(CliCommand.INSPECT_OBJECT,
                        "Inspect one object like MAT's object inspector, or jump directly to one or more field paths for targeted state checks.", //$NON-NLS-1$
                        "mat-cli inspect-object <heap> --object 0x... [--select-fields FIELD | --field-paths PATH] [--show-nulls] [--limit N] [--depth N] [--bytes-display bytes|kilobytes|megabytes|gigabytes|smart] [--format text|json]", //$NON-NLS-1$
                        true, heapArgument,
                        Arrays.asList(freeTextOption("--object", "0x...", true, "Object address to inspect."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        freeTextOption("--select-fields", "FIELD", false, //$NON-NLS-1$ //$NON-NLS-2$
                                                        "Inspect one or more direct fields from the root object. May be repeated. Cannot be combined with --field-paths."), //$NON-NLS-1$
                                        freeTextOption("--field-paths", "PATH", false, //$NON-NLS-1$ //$NON-NLS-2$
                                                        "Inspect one or more dotted field paths such as cleaner.offsetMap. May be repeated. Cannot be combined with --select-fields."), //$NON-NLS-1$
                                        flagOption("--show-nulls", "Show nested null fields and array slots in text output."), //$NON-NLS-1$ //$NON-NLS-2$
                                        freeTextOption("--limit", "N", false, "Limit children per level."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        freeTextOption("--depth", "N", false, //$NON-NLS-1$ //$NON-NLS-2$
                                                        "Limit nested object expansion depth. Defaults to 3 for inspect-object when omitted."), //$NON-NLS-1$
                                        BYTES_DISPLAY_OPTION,
                                        enumOption("--format", "text|json", false, "Select text or JSON output.", FORMAT_VALUES)), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        Arrays.asList(output("text", "tree", "Indented object-inspector tree."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        output("json", "tree", "Compact keyed object-inspector tree JSON.")), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        Arrays.asList("path2gc <heap> --object 0x...", "oql <heap> --query \"SELECT * FROM OBJECTS 0x...\""), //$NON-NLS-1$ //$NON-NLS-2$
                        "tree payload with field and element nodes, concrete values, preview metadata, compact child arrays, sparse slot preservation, and optional row addresses.", //$NON-NLS-1$
                        Arrays.asList("items[]", "items[].kind", "items[].name", "items[].value", "items[].object_address", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                                        "items[]._children[] when returned", "items[]._childrenTruncated when true", //$NON-NLS-1$ //$NON-NLS-2$
                                        "items[].slot when null siblings were compacted", //$NON-NLS-1$
                                        "items[]._address when no address column", //$NON-NLS-1$
                                        "items[]._meta.value.kind when previewed", //$NON-NLS-1$
                                        "items[]._meta.value.length when previewed", //$NON-NLS-1$
                                        "items[]._meta.value.encoding when byte[] previewed"))); //$NON-NLS-1$
        definitions.put(CliCommand.TOP_CONSUMERS, new CommandDefinition(CliCommand.TOP_CONSUMERS,
                        "Show the largest dominators grouped the same way as MAT top consumers.", //$NON-NLS-1$
                        "mat-cli top-consumers <heap> [--limit N] [--depth N] [--bytes-display bytes|kilobytes|megabytes|gigabytes|smart] [--format text|json]", true, //$NON-NLS-1$
                        heapArgument, FORMAT_LIMIT_AND_DEPTH_OPTIONS,
                        Arrays.asList(output("text", "top-consumers", "Text report matching MAT top consumers."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        output("json", "top-consumers", //$NON-NLS-1$ //$NON-NLS-2$
                                                        "Compact aggregated JSON without display-only duplicates.")), //$NON-NLS-1$
                        Arrays.asList("histogram <heap>", "path2gc <heap> --object 0x..."), //$NON-NLS-1$ //$NON-NLS-2$
                        "aggregated payload with biggestObjects, classes, classLoaders, and optional packages.", //$NON-NLS-1$
                        Arrays.asList("totalRetainedHeap", "biggestObjects[]", "biggestObjects[].objectAddress", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        "biggestObjectsTruncated when true", "classes[]", "classes[].objectAddress", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        "classesTruncated when true", "classLoaders[]", "classLoaders[].objectAddress", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        "classLoadersTruncated when true", "packages when available", "packagesTruncated when true"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        definitions.put(CliCommand.PATH2GC, new CommandDefinition(CliCommand.PATH2GC,
                        "Find paths from an object to GC roots using MAT's native query.", //$NON-NLS-1$
                        "mat-cli path2gc <heap> --object 0x... [--limit N] [--depth N] [--bytes-display bytes|kilobytes|megabytes|gigabytes|smart] [--format text|json]", true, //$NON-NLS-1$
                        heapArgument,
                        Arrays.asList(freeTextOption("--object", "0x...", true, "Object address to resolve from the snapshot."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        freeTextOption("--limit", "N", false, "Limit children per level."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        freeTextOption("--depth", "N", false, "Limit nested tree depth."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        BYTES_DISPLAY_OPTION,
                                        enumOption("--format", "text|json", false, "Select text or JSON output.", FORMAT_VALUES)), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        Arrays.asList(output("text", "tree", "Indented tree view."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        output("json", "tree", "Compact keyed tree JSON.")), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        Arrays.asList("inspect-object <heap> --object 0x...", "oql <heap> --query \"SELECT * FROM OBJECTS 0x...\""), //$NON-NLS-1$ //$NON-NLS-2$
                        "tree payload with compact nodes, sparse child arrays, optional slot preservation, and optional row addresses.", //$NON-NLS-1$
                        Arrays.asList("items[]", "items[]._children[] when returned", "items[]._childrenTruncated when true", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        "items[].slot when null siblings were compacted", "items[]._address when no address column"))); //$NON-NLS-1$ //$NON-NLS-2$
        definitions.put(CliCommand.OQL, new CommandDefinition(CliCommand.OQL,
                        "Run a MAT OQL query directly against the snapshot.", //$NON-NLS-1$
                        "mat-cli oql <heap> --query \"...\" [--limit N] [--depth N] [--bytes-display bytes|kilobytes|megabytes|gigabytes|smart] [--format text|json]", true, //$NON-NLS-1$
                        heapArgument,
                        Arrays.asList(freeTextOption("--query", "\"...\"", true, "OQL query string."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        fileOption("--query-file", "PATH", false, //$NON-NLS-1$ //$NON-NLS-2$
                                                        "Read OQL text from a UTF-8 file to avoid shell quoting issues such as inner classes with '$'."), //$NON-NLS-1$
                                        flagOption("--query-stdin", "Read OQL text from stdin."), //$NON-NLS-1$ //$NON-NLS-2$
                                        freeTextOption("--limit", "N", false, "Limit rows, sections, or children per level."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        freeTextOption("--depth", "N", false, "Limit nested tree or section depth."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        BYTES_DISPLAY_OPTION,
                                        enumOption("--format", "text|json", false, "Select text or JSON output.", FORMAT_VALUES)), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        Arrays.asList(output("text", "text|table|tree|pie", "Depends on the OQL result."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        output("json", "text|table|tree|pie", //$NON-NLS-1$ //$NON-NLS-2$
                                                        "Compact mat-cli/v1 JSON envelope around the resolved result kind.")), //$NON-NLS-1$
                        Arrays.asList("histogram <heap>", "query <heap> --command \"histogram\""), //$NON-NLS-1$ //$NON-NLS-2$
                        "same payload contract as the resolved result kind returned by the OQL query.", //$NON-NLS-1$
                        Arrays.asList("resultKind", "items[] when table/tree/pie", "items[].slot when tree siblings were compacted", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        "content when text"))); //$NON-NLS-1$
        definitions.put(CliCommand.QUERY, new CommandDefinition(CliCommand.QUERY,
                        "Run a MAT query command string through SnapshotQuery.parse(...).", //$NON-NLS-1$
                        "mat-cli query <heap> --command \"...\" [--limit N] [--depth N] [--bytes-display bytes|kilobytes|megabytes|gigabytes|smart] [--format text|json]", true, //$NON-NLS-1$
                        heapArgument,
                        Arrays.asList(freeTextOption("--command", "\"...\"", true, "MAT query command string."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        fileOption("--command-file", "PATH", false, //$NON-NLS-1$ //$NON-NLS-2$
                                                        "Read MAT query text from a UTF-8 file to avoid shell quoting issues such as inner classes with '$'."), //$NON-NLS-1$
                                        flagOption("--command-stdin", "Read MAT query text from stdin."), //$NON-NLS-1$ //$NON-NLS-2$
                                        freeTextOption("--limit", "N", false, "Limit rows, sections, or children per level."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        freeTextOption("--depth", "N", false, "Limit nested tree or section depth."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        BYTES_DISPLAY_OPTION,
                                        enumOption("--format", "text|json", false, "Select text or JSON output.", FORMAT_VALUES)), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        Arrays.asList(output("text", "text|table|tree|section|pie|top-consumers", //$NON-NLS-1$ //$NON-NLS-2$
                                        "Depends on the resolved query result."),
                                        output("json", "text|table|tree|section|pie|top-consumers", //$NON-NLS-1$ //$NON-NLS-2$
                                                        "Compact mat-cli/v1 JSON envelope around the resolved result kind.")), //$NON-NLS-1$
                        Arrays.asList("schema histogram", "describe top-consumers"), //$NON-NLS-1$ //$NON-NLS-2$
                        "same payload contract as the resolved result kind returned by the parsed query.", //$NON-NLS-1$
                        Arrays.asList("resultKind", "items[] when table/tree/pie", "items[].slot when tree siblings were compacted", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        "sections[] when section"))); //$NON-NLS-1$ //$NON-NLS-2$
        definitions.put(CliCommand.DESCRIBE, new CommandDefinition(CliCommand.DESCRIBE,
                        "Describe a CLI command, its options, and the result kinds it can return.", //$NON-NLS-1$
                        "mat-cli describe <command> [--format text|json]", false, commandArgument, FORMAT_OPTION, //$NON-NLS-1$
                        Arrays.asList(output("text", "describe", "Human-readable command description."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        output("json", "describe", "Compact command metadata JSON.")), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        Arrays.asList("schema <command>"), "metadata payload for one command definition.", //$NON-NLS-1$ //$NON-NLS-2$
                        Arrays.asList("name", "summary", "usage", "requiresSnapshot", "options[]", "outputs[]", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
                                        "suggestedNextCommands via top-level envelope"))); //$NON-NLS-1$
        definitions.put(CliCommand.SCHEMA, new CommandDefinition(CliCommand.SCHEMA,
                        "Describe the compact mat-cli/v1 JSON contract for a CLI command.", //$NON-NLS-1$
                        "mat-cli schema <command> [--format text|json]", false, commandArgument, FORMAT_OPTION, //$NON-NLS-1$
                        Arrays.asList(output("text", "schema", "Readable contract summary."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        output("json", "schema", "Compact contract metadata JSON.")), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        Arrays.asList("describe <command>"), "JSON envelope and payload contract summary.", //$NON-NLS-1$ //$NON-NLS-2$
                        Arrays.asList("jsonEnvelope", "payloadKind", "payloadFields[]", "outputs[]"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        definitions.put(CliCommand.LIST_QUERIES, new CommandDefinition(CliCommand.LIST_QUERIES,
                        "List the registered MAT query commands available through SnapshotQuery/QueryRegistry.", //$NON-NLS-1$
                        "mat-cli list-queries [--format text|json]", false, Collections.<PositionalDefinition>emptyList(), //$NON-NLS-1$
                        FORMAT_OPTION,
                        Arrays.asList(output("text", "query-list", "List MAT query identifiers and summaries."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        output("json", "query-list", "Compact query registry metadata JSON.")), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        Arrays.asList("describe-query histogram", "query <heap> --command \"histogram\""), //$NON-NLS-1$ //$NON-NLS-2$
                        "array of MAT query descriptors including identifiers, usage, and arguments.", //$NON-NLS-1$
                        Arrays.asList("queries[]", "queries[].identifier", "queries[].usage", "queries[].arguments[]"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        definitions.put(CliCommand.DESCRIBE_QUERY, new CommandDefinition(CliCommand.DESCRIBE_QUERY,
                        "Describe one registered MAT query, including its arguments and help text.", //$NON-NLS-1$
                        "mat-cli describe-query <query-id> [--format text|json]", false, queryIdArgument, //$NON-NLS-1$
                        FORMAT_OPTION,
                        Arrays.asList(output("text", "query-description", "Human-readable MAT query metadata."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        output("json", "query-description", "Compact MAT query metadata JSON.")), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        Arrays.asList("list-queries", "query <heap> --command \"<query>\""), //$NON-NLS-1$ //$NON-NLS-2$
                        "single MAT query descriptor with argument metadata, help, and subjects.", //$NON-NLS-1$
                        Arrays.asList("query.identifier", "query.usage", "query.arguments[]", "query.subjects[]"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        definitions.put(CliCommand.COMPLETION, new CommandDefinition(CliCommand.COMPLETION,
                        "Generate a bash or zsh shell completion script for mat-cli.", //$NON-NLS-1$
                        "mat-cli completion <bash|zsh> [--format text|json]", false, completionShellArgument, //$NON-NLS-1$
                        FORMAT_OPTION,
                        Arrays.asList(output("text", "text", "Shell completion script."), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        output("json", "text", "Shell completion script in the compact text envelope.")), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        Arrays.asList("completion bash", "completion zsh"), //$NON-NLS-1$ //$NON-NLS-2$
                        "plain-text shell completion script.", //$NON-NLS-1$
                        Arrays.asList("content"))); //$NON-NLS-1$
        return Collections.unmodifiableMap(definitions);
    }

    private static OptionDefinition enumOption(String name, String valueHint, boolean required, String description,
                    List<String> completionCandidates)
    {
        return new OptionDefinition(name, valueHint, required, description, CompletionValueType.ENUM,
                        completionCandidates);
    }

    private static OptionDefinition freeTextOption(String name, String valueHint, boolean required, String description)
    {
        return new OptionDefinition(name, valueHint, required, description, CompletionValueType.FREE_TEXT, null);
    }

    private static OptionDefinition fileOption(String name, String valueHint, boolean required, String description)
    {
        return new OptionDefinition(name, valueHint, required, description, CompletionValueType.FILE, null);
    }

    private static OptionDefinition flagOption(String name, String description)
    {
        return new OptionDefinition(name, null, false, description, CompletionValueType.NONE, null);
    }

    private static PositionalDefinition enumPositional(String name, List<String> completionCandidates)
    {
        return new PositionalDefinition(name, CompletionValueType.ENUM, completionCandidates);
    }

    private static PositionalDefinition freeTextPositional(String name)
    {
        return new PositionalDefinition(name, CompletionValueType.FREE_TEXT, null);
    }

    private static PositionalDefinition filePositional(String name)
    {
        return new PositionalDefinition(name, CompletionValueType.FILE, null);
    }

    private static Map<String, Integer> queryLimitOverrides()
    {
        Map<String, Integer> limits = new HashMap<String, Integer>();
        limits.put("gc_roots", Integer.valueOf(100)); //$NON-NLS-1$
        limits.put("thread_overview", Integer.valueOf(100)); //$NON-NLS-1$
        return Collections.unmodifiableMap(limits);
    }

    private static List<String> buildCommandTokens()
    {
        List<String> tokens = new ArrayList<String>(CliCommand.values().length);
        for (CliCommand command : CliCommand.values())
        {
            tokens.add(command.getToken());
        }
        return Collections.unmodifiableList(tokens);
    }

    private static OutputDefinition output(String format, String resultKind, String description)
    {
        return new OutputDefinition(format, resultKind, description);
    }

    private static <T> List<T> immutableCopy(List<T> values)
    {
        if (values == null || values.isEmpty())
            return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<T>(values));
    }
}
