/*******************************************************************************
 * Copyright (c) 2026 Eclipse Memory Analyzer Project.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.mat.cli.internal.serialization;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.eclipse.mat.cli.internal.CliArguments;
import org.eclipse.mat.cli.internal.CliCommand;
import org.eclipse.mat.cli.internal.CliCommandCatalog;
import org.eclipse.mat.cli.internal.CliExecution;
import org.eclipse.mat.cli.internal.CliException;
import org.eclipse.mat.cli.internal.CliExitCodes;
import org.eclipse.mat.cli.internal.PackageTreeResult;
import org.eclipse.mat.cli.internal.CommandMetadataResult;
import org.eclipse.mat.cli.internal.QueryMetadataResult;
import org.eclipse.mat.cli.internal.SnapshotSummary;
import org.eclipse.mat.cli.internal.ThreadsResult;
import org.eclipse.mat.query.IResult;
import org.eclipse.mat.query.IResultPie;
import org.eclipse.mat.query.IResultTable;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.query.results.CompositeResult;
import org.eclipse.mat.query.results.TextResult;
import org.eclipse.mat.report.Spec;

public class ResultSerializer
{
    private static final String JSON_SCHEMA_VERSION = "mat-cli/v1"; //$NON-NLS-1$

    private final TableResultSerializer tableSerializer = new TableResultSerializer();
    private final TreeResultSerializer treeSerializer = new TreeResultSerializer();
    private final TextResultSerializer textSerializer = new TextResultSerializer();
    private final PieResultSerializer pieSerializer = new PieResultSerializer();
    private final SpecResultSerializer specSerializer = new SpecResultSerializer(tableSerializer, treeSerializer,
                    textSerializer, pieSerializer);
    private final ThreadsResultSerializer threadsSerializer = new ThreadsResultSerializer();
    private final PackageTreeResultSerializer packageTreeSerializer = new PackageTreeResultSerializer();
    private final CommandMetadataSerializer metadataSerializer = new CommandMetadataSerializer();
    private final QueryMetadataSerializer queryMetadataSerializer = new QueryMetadataSerializer();

    public void serialize(CliArguments arguments, CliExecution execution, PrintStream out) throws IOException, CliException
    {
        if (arguments.getFormat() == CliArguments.OutputFormat.JSON)
            serializeJson(arguments, execution, out);
        else
            serializeText(arguments, execution, out);
    }

    public void serializeError(CliArguments arguments, int exitCode, Throwable error, PrintStream out)
    {
        if (arguments != null && arguments.getFormat() == CliArguments.OutputFormat.JSON)
        {
            serializeJsonError(arguments, exitCode, error, out);
            return;
        }

        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        writer.name("command").value(arguments == null || arguments.getCommand() == null ? null //$NON-NLS-1$
                        : arguments.getCommand().getToken());
        writer.name("heap").value(arguments == null || arguments.getHeapFile() == null ? null //$NON-NLS-1$
                        : arguments.getHeapFile().getAbsolutePath());
        writer.name("resultType").value("error"); //$NON-NLS-1$ //$NON-NLS-2$
        writer.name("truncated").value(false); //$NON-NLS-1$
        writer.name("error").beginObject(); //$NON-NLS-1$
        writer.name("code").value(exitCode); //$NON-NLS-1$
        writer.name("message").value(error.getMessage() == null ? error.getClass().getName() : error.getMessage()); //$NON-NLS-1$
        writer.endObject();
        writer.endObject();
        out.println(writer.toString());
    }

    public void serializeError(CliArguments arguments, CliArguments.OutputFormat format, int exitCode, Throwable error,
                    PrintStream out)
    {
        if (format == CliArguments.OutputFormat.JSON)
        {
            serializeJsonError(arguments, exitCode, error, out);
            return;
        }
        serializeError(arguments, exitCode, error, out);
    }

    private void serializeText(CliArguments arguments, CliExecution execution, PrintStream out) throws CliException
    {
        if (execution.isSummary())
        {
            out.print(execution.getSummary().asText(arguments.getBytesDisplay()));
            return;
        }

        IResult result = execution.getResult();
        SerializationOptions options = new SerializationOptions(arguments.getLimit(), arguments.getTreeDepthLimit(),
                        false, null, arguments.getBytesDisplay());
        if (result instanceof TextResult)
        {
            out.println(textSerializer.toText((TextResult) result));
        }
        else if (result instanceof CommandMetadataResult)
        {
            out.print(metadataSerializer.toText((CommandMetadataResult) result));
        }
        else if (result instanceof QueryMetadataResult)
        {
            out.print(queryMetadataSerializer.toText((QueryMetadataResult) result));
        }
        else if (result instanceof ThreadsResult)
        {
            out.print(threadsSerializer.toText((ThreadsResult) result, options));
        }
        else if (result instanceof PackageTreeResult)
        {
            out.print(packageTreeSerializer.toText((PackageTreeResult) result, options));
        }
        else if (result instanceof IResultTable)
        {
            out.print(tableSerializer.toText((IResultTable) result, options));
        }
        else if (result instanceof IResultTree)
        {
            out.print(treeSerializer.toText((IResultTree) result, options, arguments.isShowNulls()));
        }
        else if (result instanceof IResultPie)
        {
            out.print(pieSerializer.toText((IResultPie) result, options));
        }
        else if (result instanceof CompositeResult)
        {
            out.print(specSerializer.compositeToText((CompositeResult) result, options));
        }
        else if (result instanceof Spec)
        {
            out.print(specSerializer.toText((Spec) result, options));
        }
        else
        {
            throw unsupported(result);
        }
    }

    private void serializeJson(CliArguments arguments, CliExecution execution, PrintStream out) throws IOException, CliException
    {
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        writer.name("schemaVersion").value(JSON_SCHEMA_VERSION); //$NON-NLS-1$

        boolean truncated;
        if (execution.isSummary())
        {
            writer.name("resultKind").value("summary"); //$NON-NLS-1$ //$NON-NLS-2$
            writeSummary(writer, execution.getSummary());
            truncated = false;
        }
        else
        {
            SerializationOptions options = new SerializationOptions(arguments.getLimit(),
                            arguments.getTreeDepthLimit(), true, execution.getObjectAddressResolver(),
                            arguments.getBytesDisplay());
            IResult result = execution.getResult();
            if (result instanceof TextResult)
            {
                writer.name("resultKind").value("text"); //$NON-NLS-1$ //$NON-NLS-2$
                writer.name("content").value(textSerializer.toText((TextResult) result)); //$NON-NLS-1$
                truncated = false;
            }
            else if (result instanceof CommandMetadataResult)
            {
                writer.name("resultKind").value(metadataSerializer.resultKind((CommandMetadataResult) result)); //$NON-NLS-1$
                truncated = metadataSerializer.writeJson(writer, (CommandMetadataResult) result);
            }
            else if (result instanceof QueryMetadataResult)
            {
                writer.name("resultKind").value(queryMetadataSerializer.resultKind((QueryMetadataResult) result)); //$NON-NLS-1$
                truncated = queryMetadataSerializer.writeJson(writer, (QueryMetadataResult) result);
            }
            else if (result instanceof ThreadsResult)
            {
                writer.name("resultKind").value("threads"); //$NON-NLS-1$ //$NON-NLS-2$
                truncated = threadsSerializer.writeJson(writer, (ThreadsResult) result);
            }
            else if (result instanceof PackageTreeResult)
            {
                writer.name("resultKind").value("tree"); //$NON-NLS-1$ //$NON-NLS-2$
                truncated = packageTreeSerializer.writeJson(writer, (PackageTreeResult) result, options);
            }
            else if (result instanceof IResultTable)
            {
                writer.name("resultKind").value("table"); //$NON-NLS-1$ //$NON-NLS-2$
                truncated = tableSerializer.writeAgentJson(writer, (IResultTable) result, options);
            }
            else if (result instanceof IResultTree)
            {
                writer.name("resultKind").value("tree"); //$NON-NLS-1$ //$NON-NLS-2$
                truncated = treeSerializer.writeAgentJson(writer, (IResultTree) result, options);
            }
            else if (result instanceof IResultPie)
            {
                writer.name("resultKind").value("pie"); //$NON-NLS-1$ //$NON-NLS-2$
                truncated = pieSerializer.writeAgentJson(writer, (IResultPie) result, options);
            }
            else if (result instanceof CompositeResult)
            {
                writer.name("resultKind").value("section"); //$NON-NLS-1$ //$NON-NLS-2$
                truncated = specSerializer.writeCompositeResult(writer, (CompositeResult) result, options);
            }
            else if (result instanceof Spec)
            {
                writer.name("resultKind").value(specSerializer.rootResultType((Spec) result)); //$NON-NLS-1$
                truncated = specSerializer.writeJson(writer, (Spec) result, options);
            }
            else
            {
                throw unsupported(result);
            }
        }

        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writeStringField(writer, "note", execution == null ? null : execution.getNote()); //$NON-NLS-1$
        if (shouldWriteSuggestedNextCommands(arguments, execution))
            writeSuggestedNextCommands(writer, suggestedNextCommands(arguments, execution));
        writer.endObject();
        out.println(writer.toString());
    }

    private void serializeJsonError(CliArguments arguments, int exitCode, Throwable error, PrintStream out)
    {
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        writer.name("schemaVersion").value(JSON_SCHEMA_VERSION); //$NON-NLS-1$
        writer.name("resultKind").value("error"); //$NON-NLS-1$ //$NON-NLS-2$
        writer.name("error").beginObject(); //$NON-NLS-1$
        writer.name("code").value(exitCode); //$NON-NLS-1$
        writer.name("message").value(error.getMessage() == null ? error.getClass().getName() : error.getMessage()); //$NON-NLS-1$
        writer.name("kind").value(errorKind(arguments, exitCode, error)); //$NON-NLS-1$
        writeStringField(writer, "hint", errorHint(arguments, exitCode, error)); //$NON-NLS-1$
        writer.name("retryable").value(isRetryable(arguments, exitCode, error)); //$NON-NLS-1$
        if (arguments != null && arguments.isVerbose())
        {
            writeStringField(writer, "exceptionClass", error == null ? null : error.getClass().getName()); //$NON-NLS-1$
            writeStringField(writer, "rootCauseClass", rootCause(error).getClass().getName()); //$NON-NLS-1$
            writeStringField(writer, "rootCauseMessage", rootCauseMessage(error)); //$NON-NLS-1$
            writeStringField(writer, "stackTrace", stackTrace(error)); //$NON-NLS-1$
        }
        writer.endObject();
        writer.name("truncated").value(false); //$NON-NLS-1$
        writeSuggestedNextCommands(writer, suggestedNextCommands(arguments, exitCode, error));
        writer.endObject();
        out.println(writer.toString());
    }

    private void writeSummary(JsonWriter writer, SnapshotSummary summary)
    {
        writer.name("summary").beginObject(); //$NON-NLS-1$
        writeStringField(writer, "path", summary.getPath()); //$NON-NLS-1$
        writeStringField(writer, "heapFormat", summary.getHeapFormat()); //$NON-NLS-1$
        writeStringField(writer, "jvmInfo", summary.getJvmInfo()); //$NON-NLS-1$
        writeStringField(writer, "creationDate", summary.getCreationDate()); //$NON-NLS-1$
        writer.name("identifierSize").value(summary.getIdentifierSize()); //$NON-NLS-1$
        writer.name("numberOfObjects").value(summary.getNumberOfObjects()); //$NON-NLS-1$
        writer.name("numberOfClasses").value(summary.getNumberOfClasses()); //$NON-NLS-1$
        writer.name("numberOfClassLoaders").value(summary.getNumberOfClassLoaders()); //$NON-NLS-1$
        writer.name("numberOfGCRoots").value(summary.getNumberOfGCRoots()); //$NON-NLS-1$
        writer.name("usedHeapSize").value(summary.getUsedHeapSize()); //$NON-NLS-1$
        writer.endObject();
    }

    private CliException unsupported(IResult result)
    {
        String resultName = result == null ? "null" : result.getClass().getName(); //$NON-NLS-1$
        return CliException.unsupported("Unsupported result type: " + resultName); //$NON-NLS-1$
    }

    private void writeSuggestedNextCommands(JsonWriter writer, List<String> commands)
    {
        if (commands == null || commands.isEmpty())
            return;
        writer.name("suggestedNextCommands").beginArray(); //$NON-NLS-1$
        for (String command : commands)
        {
            writer.value(command);
        }
        writer.endArray();
    }

    private void writeStringField(JsonWriter writer, String name, String value)
    {
        if (value == null || value.length() == 0)
            return;
        writer.name(name).value(value);
    }

    private boolean shouldWriteSuggestedNextCommands(CliArguments arguments, CliExecution execution)
    {
        if (execution == null)
            return false;
        if (execution.isSummary())
            return true;
        IResult result = execution.getResult();
        if (result instanceof CommandMetadataResult || result instanceof QueryMetadataResult)
            return true;
        return isEmptyResult(result);
    }

    private boolean isEmptyResult(IResult result)
    {
        if (result == null)
            return true;
        if (result instanceof IResultTable)
            return ((IResultTable) result).getRowCount() == 0;
        if (result instanceof IResultTree)
            return ((IResultTree) result).getElements().isEmpty();
        if (result instanceof IResultPie)
            return ((IResultPie) result).getSlices().isEmpty();
        if (result instanceof ThreadsResult)
            return ((ThreadsResult) result).getThreads().isEmpty();
        if (result instanceof PackageTreeResult)
            return ((PackageTreeResult) result).getRoot() == null;
        if (result instanceof CompositeResult)
            return ((CompositeResult) result).getResultEntries().isEmpty();
        return false;
    }

    private List<String> suggestedNextCommands(CliArguments arguments, CliExecution execution)
    {
        if (execution != null && execution.getResult() instanceof CommandMetadataResult)
        {
            return renderSuggestions(arguments,
                            ((CommandMetadataResult) execution.getResult()).getDefinition().getSuggestedNextCommands(),
                            null);
        }

        CliCommandCatalog.CommandDefinition definition = null;
        if (arguments != null && arguments.getCommand() != null)
        {
            definition = CliCommandCatalog.lookup(arguments.getCommand().requiresSubjectCommand()
                            ? arguments.getSubjectCommand()
                            : arguments.getCommand());
        }
        if (definition == null)
            return renderSuggestions(arguments, Arrays.asList("mat-cli --help", "mat-cli describe summary"), //$NON-NLS-1$ //$NON-NLS-2$
                            execution == null ? null : execution.getPrimaryObjectAddress());
        return renderSuggestions(arguments, definition.getSuggestedNextCommands(),
                        execution == null ? null : execution.getPrimaryObjectAddress());
    }

    private List<String> suggestedNextCommands(CliArguments arguments, int exitCode, Throwable error)
    {
        if (arguments == null || arguments.getCommand() == null)
            return renderSuggestions(arguments, Arrays.asList("mat-cli --help", "mat-cli describe summary"), null); //$NON-NLS-1$ //$NON-NLS-2$

        String kind = errorKind(arguments, exitCode, error);
        if ("query_not_found".equals(kind)) //$NON-NLS-1$
            return renderSuggestions(arguments, Arrays.asList("mat-cli list-queries"), null); //$NON-NLS-1$
        if ("missing_file".equals(kind)) //$NON-NLS-1$
            return renderSuggestions(arguments, Arrays.asList("mat-cli --help", "mat-cli list-queries"), null); //$NON-NLS-1$ //$NON-NLS-2$
        if ("invalid_argument".equals(kind) && arguments.getCommand() == CliCommand.PATH2GC) //$NON-NLS-1$
        {
            return renderSuggestions(arguments,
                            Arrays.asList("oql <heap> --query \"SELECT * FROM OBJECTS 0x...\"", //$NON-NLS-1$
                                            "objects <heap>", "biggest-objects <heap>"), //$NON-NLS-1$ //$NON-NLS-2$
                            arguments.getObjectAddress());
        }
        if ("invalid_argument".equals(kind) && arguments.getCommand() == CliCommand.INSTANCES) //$NON-NLS-1$
            return renderSuggestions(arguments, Arrays.asList("objects <heap>", "oql <heap> --query \"SELECT * FROM java.lang.String s\""), null); //$NON-NLS-1$ //$NON-NLS-2$

        return suggestedNextCommands(arguments, (CliExecution) null);
    }

    private List<String> renderSuggestions(CliArguments arguments, List<String> suggestions, String objectAddress)
    {
        java.util.ArrayList<String> rendered = new java.util.ArrayList<String>(suggestions.size());
        for (String suggestion : suggestions)
        {
            rendered.add(renderSuggestion(arguments, suggestion, objectAddress));
        }
        return rendered;
    }

    private String renderSuggestion(CliArguments arguments, String suggestion, String objectAddress)
    {
        String rendered = suggestion.startsWith("mat-cli ") ? suggestion : "mat-cli " + suggestion; //$NON-NLS-1$ //$NON-NLS-2$
        if (arguments == null)
            return rendered;

        if (arguments.getHeapFile() != null)
            rendered = rendered.replace("<heap>", "\"" + arguments.getHeapFile().getAbsolutePath() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
        if (arguments.getSubjectCommand() != null)
            rendered = rendered.replace("<command>", arguments.getSubjectCommand().getToken()); //$NON-NLS-1$
        if (arguments.getSubjectName() != null)
        {
            rendered = rendered.replace("<command>", arguments.getSubjectName()); //$NON-NLS-1$
            rendered = rendered.replace("<query>", arguments.getSubjectName()); //$NON-NLS-1$
            rendered = rendered.replace("<subject>", arguments.getSubjectName()); //$NON-NLS-1$
        }
        if (arguments.getClassName() != null || arguments.getClassRegex() != null || arguments.getClassContains() != null)
            rendered = rendered.replace("<class>",
                            arguments.getClassName() != null ? arguments.getClassName()
                                            : arguments.getClassRegex() != null ? arguments.getClassRegex()
                                                            : arguments.getClassContains()); //$NON-NLS-1$
        String resolvedObjectAddress = arguments.getObjectAddress() != null ? arguments.getObjectAddress() : objectAddress;
        if (resolvedObjectAddress != null)
            rendered = rendered.replace("0x...", resolvedObjectAddress); //$NON-NLS-1$
        return rendered;
    }

    private String errorKind(CliArguments arguments, int exitCode, Throwable error)
    {
        if (exitCode == CliExitCodes.USAGE)
            return "usage"; //$NON-NLS-1$
        if (exitCode == CliExitCodes.OUT_OF_MEMORY)
            return "out_of_memory"; //$NON-NLS-1$

        Throwable root = rootCause(error);
        String rootClass = root.getClass().getName();
        String message = rootCauseMessage(error);
        String lower = message == null ? "" : message.toLowerCase(java.util.Locale.ENGLISH); //$NON-NLS-1$

        if (root instanceof java.io.FileNotFoundException || lower.contains("heap dump not found")) //$NON-NLS-1$
            return "missing_file"; //$NON-NLS-1$
        if (root instanceof java.nio.file.AccessDeniedException || lower.contains("not writable") //$NON-NLS-1$
                        || lower.contains("permission denied") || lower.contains("could not be written")) //$NON-NLS-1$ //$NON-NLS-2$
            return "permission"; //$NON-NLS-1$
        if (lower.contains("no object found at address") || lower.contains("invalid object address") //$NON-NLS-1$ //$NON-NLS-2$
                        || lower.contains("no classes found matching")) //$NON-NLS-1$
            return "invalid_argument"; //$NON-NLS-1$
        if (lower.contains("unknown mat query") || (lower.contains("command") && lower.contains("not found"))) //$NON-NLS-1$ //$NON-NLS-2$
            return "query_not_found"; //$NON-NLS-1$
        if (rootClass.endsWith("ParseException") || lower.contains("encountered ") || lower.contains("syntax")) //$NON-NLS-1$ //$NON-NLS-2$
            return "query_syntax"; //$NON-NLS-1$
        if (arguments != null && arguments.getCommand() == CliCommand.OQL && lower.contains("problem reported:")) //$NON-NLS-1$
            return "query_error"; //$NON-NLS-1$
        if (lower.contains("o2address() is null") || lower.contains("outbound() is null") //$NON-NLS-1$ //$NON-NLS-2$
                        || lower.contains("this.in is null") || lower.contains("snapshot has been disposed") //$NON-NLS-1$ //$NON-NLS-2$
                        || lower.contains("reader is closed")) //$NON-NLS-1$
            return "snapshot_lifecycle"; //$NON-NLS-1$
        if (root instanceof java.io.IOException)
            return "io"; //$NON-NLS-1$
        return "internal"; //$NON-NLS-1$
    }

    private String errorHint(CliArguments arguments, int exitCode, Throwable error)
    {
        String kind = errorKind(arguments, exitCode, error);
        if (exitCode == CliExitCodes.OUT_OF_MEMORY)
            return "Retry with a larger heap, for example MAT_CLI_VMARGS=\"-Xmx2g\"."; //$NON-NLS-1$
        if (exitCode == CliExitCodes.USAGE)
            return usageHint(arguments);
        if ("query_not_found".equals(kind)) //$NON-NLS-1$
            return "Run `mat-cli list-queries --format json` to discover valid MAT query identifiers."; //$NON-NLS-1$
        if ("missing_file".equals(kind)) //$NON-NLS-1$
            return "Verify that the heap path exists and is readable."; //$NON-NLS-1$
        if ("permission".equals(kind)) //$NON-NLS-1$
            return "Use writable runtime directories, for example MAT_CLI_CONFIG_DIR=/tmp/mat-cli/config and MAT_CLI_DATA_DIR=/tmp/mat-cli/workspace."; //$NON-NLS-1$
        if ("invalid_argument".equals(kind) && arguments != null && arguments.getCommand() == CliCommand.PATH2GC) //$NON-NLS-1$
            return "Use `mat-cli oql <heap> --query \"SELECT * FROM OBJECTS " //$NON-NLS-1$
                            + (arguments.getObjectAddress() == null ? "0x..." : arguments.getObjectAddress()) //$NON-NLS-1$
                            + "\" --format json` to verify the address before retrying path2gc."; //$NON-NLS-1$
        if ("invalid_argument".equals(kind) && arguments != null && arguments.getCommand() == CliCommand.INSTANCES) //$NON-NLS-1$
            return "Verify the fully qualified class name, class regex, or contains text, or use `mat-cli objects <heap> --format json` to discover matching classes first."; //$NON-NLS-1$
        if ("snapshot_lifecycle".equals(kind)) //$NON-NLS-1$
            return "The snapshot or its index files were closed before result materialization finished; rerun with a fixed CLI build or use --verbose for diagnostics."; //$NON-NLS-1$
        if ("query_syntax".equals(kind) && arguments != null
                        && arguments.getCommand() == org.eclipse.mat.cli.internal.CliCommand.OQL)
            return "Check the OQL query syntax, or use --query-file/--query-stdin for complex expressions."; //$NON-NLS-1$
        if ("query_error".equals(kind) && arguments != null && arguments.getCommand() == CliCommand.OQL)
            return "The OQL query was parsed, but MAT reported an execution error; inspect the reported query and narrow the expression."; //$NON-NLS-1$
        if ("query_syntax".equals(kind) && arguments != null
                        && arguments.getCommand() == org.eclipse.mat.cli.internal.CliCommand.QUERY)
            return "Check the MAT query command syntax, or use list-queries/describe-query to inspect registered queries first."; //$NON-NLS-1$
        if (arguments != null && arguments.getCommand() == org.eclipse.mat.cli.internal.CliCommand.DESCRIBE_QUERY)
            return "Run `mat-cli list-queries --format json` to discover valid MAT query identifiers."; //$NON-NLS-1$
        return "Retry with --verbose for diagnostics or narrow the query scope."; //$NON-NLS-1$
    }

    private String usageHint(CliArguments arguments)
    {
        if (arguments == null || arguments.getCommand() == null)
            return "Run `mat-cli --help` or `mat-cli describe <command> --format json` for the expected arguments."; //$NON-NLS-1$

        String command = arguments.getCommand().getToken();
        return "Run `mat-cli " + command + " --help` or `mat-cli describe " + command //$NON-NLS-1$ //$NON-NLS-2$
                        + " --format json` for the expected arguments."; //$NON-NLS-1$
    }

    private boolean isRetryable(CliArguments arguments, int exitCode, Throwable error)
    {
        String kind = errorKind(arguments, exitCode, error);
        return exitCode == CliExitCodes.OUT_OF_MEMORY || "io".equals(kind) || "permission".equals(kind); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private Throwable rootCause(Throwable error)
    {
        Throwable current = error == null ? new IllegalStateException("Unknown error") : error; //$NON-NLS-1$
        while (current.getCause() != null && current.getCause() != current)
        {
            current = current.getCause();
        }
        return current;
    }

    private String rootCauseMessage(Throwable error)
    {
        Throwable root = rootCause(error);
        return root.getMessage() == null ? root.getClass().getName() : root.getMessage();
    }

    private String stackTrace(Throwable error)
    {
        java.io.StringWriter writer = new java.io.StringWriter();
        java.io.PrintWriter printWriter = new java.io.PrintWriter(writer);
        error.printStackTrace(printWriter);
        printWriter.flush();
        return writer.toString();
    }
}
