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
import java.util.List;

import org.eclipse.mat.cli.internal.CliArguments;
import org.eclipse.mat.cli.internal.CliCommandCatalog;
import org.eclipse.mat.cli.internal.CliExecution;
import org.eclipse.mat.cli.internal.CliException;
import org.eclipse.mat.cli.internal.CliExitCodes;
import org.eclipse.mat.cli.internal.CommandMetadataResult;
import org.eclipse.mat.cli.internal.SnapshotSummary;
import org.eclipse.mat.cli.internal.TopConsumersResult;
import org.eclipse.mat.query.IResult;
import org.eclipse.mat.query.IResultTable;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.query.results.TextResult;
import org.eclipse.mat.report.Spec;

public class ResultSerializer
{
    private static final String AGENT_SCHEMA_VERSION = "mat-cli/v1"; //$NON-NLS-1$

    private final TableResultSerializer tableSerializer = new TableResultSerializer();
    private final TreeResultSerializer treeSerializer = new TreeResultSerializer();
    private final TextResultSerializer textSerializer = new TextResultSerializer();
    private final SpecResultSerializer specSerializer = new SpecResultSerializer(tableSerializer, treeSerializer,
                    textSerializer);
    private final TopConsumersResultSerializer topConsumersSerializer = new TopConsumersResultSerializer();
    private final CommandMetadataSerializer metadataSerializer = new CommandMetadataSerializer();

    public void serialize(CliArguments arguments, CliExecution execution, PrintStream out) throws IOException, CliException
    {
        if (arguments.getFormat() == CliArguments.OutputFormat.JSON)
        {
            if (arguments.isAgentProfile())
                serializeAgentJson(arguments, execution, out);
            else
                serializeJson(arguments, execution, out);
        }
        else
            serializeText(arguments, execution, out);
    }

    public void serializeError(CliArguments arguments, int exitCode, Throwable error, PrintStream out)
    {
        if (arguments != null && arguments.isAgentProfile() && arguments.getFormat() == CliArguments.OutputFormat.JSON)
        {
            serializeAgentError(arguments, exitCode, error, out);
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

    public void serializeError(CliArguments arguments, CliArguments.OutputProfile profile, int exitCode, Throwable error,
                    PrintStream out)
    {
        if (profile == CliArguments.OutputProfile.AGENT)
        {
            serializeAgentError(arguments, exitCode, error, out);
            return;
        }
        serializeError(arguments, exitCode, error, out);
    }

    private void serializeText(CliArguments arguments, CliExecution execution, PrintStream out) throws CliException
    {
        if (execution.isSummary())
        {
            out.print(execution.getSummary().asText());
            return;
        }

        IResult result = execution.getResult();
        SerializationOptions options = new SerializationOptions(arguments.getLimit(), arguments.getTreeDepthLimit());
        if (result instanceof TextResult)
        {
            out.println(textSerializer.toText((TextResult) result));
        }
        else if (result instanceof CommandMetadataResult)
        {
            out.print(metadataSerializer.toText((CommandMetadataResult) result));
        }
        else if (result instanceof TopConsumersResult)
        {
            out.print(topConsumersSerializer.toText((TopConsumersResult) result, options));
        }
        else if (result instanceof IResultTable)
        {
            out.print(tableSerializer.toText((IResultTable) result, options));
        }
        else if (result instanceof IResultTree)
        {
            out.print(treeSerializer.toText((IResultTree) result, options));
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
        writer.name("command").value(arguments.getCommand().getToken()); //$NON-NLS-1$
        writer.name("heap").value(arguments.getHeapFile() == null ? null : arguments.getHeapFile().getAbsolutePath()); //$NON-NLS-1$

        if (execution.isSummary())
        {
            writer.name("resultType").value("summary"); //$NON-NLS-1$ //$NON-NLS-2$
            writer.name("truncated").value(false); //$NON-NLS-1$
            writeSummary(writer, execution.getSummary());
        }
        else
        {
            SerializationOptions options = new SerializationOptions(arguments.getLimit(), arguments.getTreeDepthLimit());
            IResult result = execution.getResult();
            boolean truncated;
            if (result instanceof TextResult)
            {
                writer.name("resultType").value("text"); //$NON-NLS-1$ //$NON-NLS-2$
                truncated = textSerializer.writeJson(writer, (TextResult) result);
            }
            else if (result instanceof CommandMetadataResult)
            {
                truncated = metadataSerializer.writeJson(writer, (CommandMetadataResult) result, false);
            }
            else if (result instanceof TopConsumersResult)
            {
                writer.name("resultType").value("top-consumers"); //$NON-NLS-1$ //$NON-NLS-2$
                truncated = topConsumersSerializer.writeJson(writer, (TopConsumersResult) result, options);
            }
            else if (result instanceof IResultTable)
            {
                writer.name("resultType").value("table"); //$NON-NLS-1$ //$NON-NLS-2$
                truncated = tableSerializer.writeJson(writer, (IResultTable) result, options);
            }
            else if (result instanceof IResultTree)
            {
                writer.name("resultType").value("tree"); //$NON-NLS-1$ //$NON-NLS-2$
                truncated = treeSerializer.writeJson(writer, (IResultTree) result, options);
            }
            else if (result instanceof Spec)
            {
                writer.name("resultType").value(specSerializer.rootResultType((Spec) result)); //$NON-NLS-1$
                truncated = specSerializer.writeJson(writer, (Spec) result, options);
            }
            else
            {
                throw unsupported(result);
            }

            writer.name("truncated").value(truncated); //$NON-NLS-1$
        }

        writer.endObject();
        out.println(writer.toString());
    }

    private void serializeAgentJson(CliArguments arguments, CliExecution execution, PrintStream out)
                    throws IOException, CliException
    {
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        writer.name("schemaVersion").value(AGENT_SCHEMA_VERSION); //$NON-NLS-1$
        writer.name("profile").value("agent"); //$NON-NLS-1$ //$NON-NLS-2$
        writer.name("command").value(arguments.getCommand().getToken()); //$NON-NLS-1$
        if (arguments.getHeapFile() != null)
            writer.name("heap").value(arguments.getHeapFile().getAbsolutePath()); //$NON-NLS-1$
        if (arguments.getSubjectCommand() != null)
            writer.name("subject").value(arguments.getSubjectCommand().getToken()); //$NON-NLS-1$

        boolean truncated;
        if (execution.isSummary())
        {
            writer.name("resultKind").value("summary"); //$NON-NLS-1$ //$NON-NLS-2$
            writeSummary(writer, execution.getSummary());
            truncated = false;
        }
        else
        {
            SerializationOptions options = new SerializationOptions(arguments.getLimit(), arguments.getTreeDepthLimit(),
                            true);
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
                truncated = metadataSerializer.writeJson(writer, (CommandMetadataResult) result, true);
            }
            else if (result instanceof TopConsumersResult)
            {
                writer.name("resultKind").value("top-consumers"); //$NON-NLS-1$ //$NON-NLS-2$
                truncated = topConsumersSerializer.writeAgentJson(writer, (TopConsumersResult) result, options);
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
        writeSuggestedNextCommands(writer, suggestedNextCommands(arguments, execution));
        writer.endObject();
        out.println(writer.toString());
    }

    private void serializeAgentError(CliArguments arguments, int exitCode, Throwable error, PrintStream out)
    {
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        writer.name("schemaVersion").value(AGENT_SCHEMA_VERSION); //$NON-NLS-1$
        writer.name("profile").value("agent"); //$NON-NLS-1$ //$NON-NLS-2$
        writer.name("command").value(arguments == null || arguments.getCommand() == null ? null //$NON-NLS-1$
                        : arguments.getCommand().getToken());
        if (arguments != null && arguments.getHeapFile() != null)
            writer.name("heap").value(arguments.getHeapFile().getAbsolutePath()); //$NON-NLS-1$
        if (arguments != null && arguments.getSubjectCommand() != null)
            writer.name("subject").value(arguments.getSubjectCommand().getToken()); //$NON-NLS-1$
        writer.name("resultKind").value("error"); //$NON-NLS-1$ //$NON-NLS-2$
        writer.name("error").beginObject(); //$NON-NLS-1$
        writer.name("code").value(exitCode); //$NON-NLS-1$
        writer.name("message").value(error.getMessage() == null ? error.getClass().getName() : error.getMessage()); //$NON-NLS-1$
        writer.name("hint").value(errorHint(arguments, exitCode, error)); //$NON-NLS-1$
        writer.name("retryable").value(isRetryable(exitCode)); //$NON-NLS-1$
        writer.endObject();
        writer.name("truncated").value(false); //$NON-NLS-1$
        writeSuggestedNextCommands(writer, suggestedNextCommands(arguments, null));
        writer.endObject();
        out.println(writer.toString());
    }

    private void writeSummary(JsonWriter writer, SnapshotSummary summary)
    {
        writer.name("summary").beginObject(); //$NON-NLS-1$
        writer.name("path").value(summary.getPath()); //$NON-NLS-1$
        writer.name("heapFormat").value(summary.getHeapFormat()); //$NON-NLS-1$
        writer.name("jvmInfo").value(summary.getJvmInfo()); //$NON-NLS-1$
        writer.name("creationDate").value(summary.getCreationDate()); //$NON-NLS-1$
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
        writer.name("suggestedNextCommands").beginArray(); //$NON-NLS-1$
        for (String command : commands)
        {
            writer.value(command);
        }
        writer.endArray();
    }

    private List<String> suggestedNextCommands(CliArguments arguments, CliExecution execution)
    {
        if (execution != null && execution.getResult() instanceof CommandMetadataResult)
        {
            return renderSuggestions(arguments,
                            ((CommandMetadataResult) execution.getResult()).getDefinition().getSuggestedNextCommands());
        }

        CliCommandCatalog.CommandDefinition definition = null;
        if (arguments != null && arguments.getCommand() != null)
        {
            definition = CliCommandCatalog.lookup(arguments.getCommand().requiresSubjectCommand()
                            ? arguments.getSubjectCommand()
                            : arguments.getCommand());
        }
        if (definition == null)
            return renderSuggestions(arguments, java.util.Arrays.asList("mat-cli --help", "mat-cli describe summary --agent")); //$NON-NLS-1$ //$NON-NLS-2$
        return renderSuggestions(arguments, definition.getSuggestedNextCommands());
    }

    private List<String> renderSuggestions(CliArguments arguments, List<String> suggestions)
    {
        java.util.ArrayList<String> rendered = new java.util.ArrayList<String>(suggestions.size());
        for (String suggestion : suggestions)
        {
            rendered.add(renderSuggestion(arguments, suggestion));
        }
        return rendered;
    }

    private String renderSuggestion(CliArguments arguments, String suggestion)
    {
        String rendered = suggestion.startsWith("mat-cli ") ? suggestion : "mat-cli " + suggestion; //$NON-NLS-1$ //$NON-NLS-2$
        if (arguments == null)
            return rendered;

        if (arguments.getHeapFile() != null)
            rendered = rendered.replace("<heap>", "\"" + arguments.getHeapFile().getAbsolutePath() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
        if (arguments.getSubjectCommand() != null)
            rendered = rendered.replace("<command>", arguments.getSubjectCommand().getToken()); //$NON-NLS-1$
        if (arguments.getObjectAddress() != null)
            rendered = rendered.replace("0x...", arguments.getObjectAddress()); //$NON-NLS-1$
        return rendered;
    }

    private String errorHint(CliArguments arguments, int exitCode, Throwable error)
    {
        if (exitCode == CliExitCodes.OUT_OF_MEMORY)
            return "Retry with a larger heap, for example MAT_CLI_VMARGS=\"-Xmx2g\"."; //$NON-NLS-1$
        if (exitCode == CliExitCodes.USAGE)
            return "Run `mat-cli --help` or `mat-cli describe <command> --agent` for the expected arguments."; //$NON-NLS-1$
        String message = error == null || error.getMessage() == null ? "" : error.getMessage(); //$NON-NLS-1$
        if (message.contains("Heap dump not found")) //$NON-NLS-1$
            return "Verify that the heap path exists and is readable."; //$NON-NLS-1$
        if (arguments != null && arguments.getCommand() == org.eclipse.mat.cli.internal.CliCommand.OQL)
            return "Check the OQL query syntax and try a smaller LIMIT or narrower SELECT."; //$NON-NLS-1$
        if (arguments != null && arguments.getCommand() == org.eclipse.mat.cli.internal.CliCommand.QUERY)
            return "Check the MAT query command syntax or try `describe`/`schema` on a built-in command first."; //$NON-NLS-1$
        return "Retry with --verbose for diagnostics or narrow the query scope."; //$NON-NLS-1$
    }

    private boolean isRetryable(int exitCode)
    {
        return exitCode == CliExitCodes.OUT_OF_MEMORY || exitCode == CliExitCodes.EXECUTION_ERROR;
    }
}
