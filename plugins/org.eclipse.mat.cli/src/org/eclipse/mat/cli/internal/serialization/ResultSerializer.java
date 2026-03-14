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

import org.eclipse.mat.cli.internal.CliArguments;
import org.eclipse.mat.cli.internal.CliExecution;
import org.eclipse.mat.cli.internal.CliException;
import org.eclipse.mat.cli.internal.SnapshotSummary;
import org.eclipse.mat.cli.internal.TopConsumersResult;
import org.eclipse.mat.query.IResult;
import org.eclipse.mat.query.IResultTable;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.query.results.TextResult;
import org.eclipse.mat.report.Spec;

public class ResultSerializer
{
    private final TableResultSerializer tableSerializer = new TableResultSerializer();
    private final TreeResultSerializer treeSerializer = new TreeResultSerializer();
    private final TextResultSerializer textSerializer = new TextResultSerializer();
    private final SpecResultSerializer specSerializer = new SpecResultSerializer(tableSerializer, treeSerializer,
                    textSerializer);
    private final TopConsumersResultSerializer topConsumersSerializer = new TopConsumersResultSerializer();

    public void serialize(CliArguments arguments, CliExecution execution, PrintStream out) throws IOException, CliException
    {
        if (arguments.getFormat() == CliArguments.OutputFormat.JSON)
            serializeJson(arguments, execution, out);
        else
            serializeText(arguments, execution, out);
    }

    public void serializeError(CliArguments arguments, int exitCode, Throwable error, PrintStream out)
    {
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
        writer.name("heap").value(arguments.getHeapFile().getAbsolutePath()); //$NON-NLS-1$

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
}
