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
import java.util.Collections;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.query.IResult;
import org.eclipse.mat.query.IResultTable;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.query.results.TextResult;
import org.eclipse.mat.report.Spec;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.SnapshotFactory;
import org.eclipse.mat.snapshot.query.SnapshotQuery;
import org.eclipse.mat.util.ConsoleProgressListener;
import org.eclipse.mat.util.IProgressListener;
import org.eclipse.mat.util.VoidProgressListener;

public class CliCommandExecutor
{
    public CliExecution execute(CliArguments arguments) throws Exception
    {
        switch (arguments.getCommand())
        {
            case DESCRIBE:
                return CliExecution.result(new CommandMetadataResult(CommandMetadataResult.Kind.DESCRIBE,
                                lookupDefinition(arguments.getSubjectCommand())));
            case SCHEMA:
                return CliExecution.result(new CommandMetadataResult(CommandMetadataResult.Kind.SCHEMA,
                                lookupDefinition(arguments.getSubjectCommand())));
            default:
                throw CliException.execution("Command requires a snapshot session: " + arguments.getCommand().getToken(), //$NON-NLS-1$
                                null);
        }
    }

    public SnapshotSession openSnapshot(CliArguments arguments) throws CliException
    {
        File heapFile = arguments.getHeapFile();
        if (!heapFile.exists())
            throw CliException.execution("Heap dump not found: " + heapFile.getAbsolutePath(), null); //$NON-NLS-1$

        IProgressListener listener = arguments.isVerbose() ? new ConsoleProgressListener(System.err)
                        : new VoidProgressListener();

        try
        {
            ISnapshot snapshot = SnapshotFactory.openSnapshot(heapFile, Collections.<String, String>emptyMap(), listener);
            return new SnapshotSession(snapshot, listener);
        }
        catch (SnapshotException e)
        {
            listener.done();
            throw CliException.execution(e.getMessage(), e);
        }
        catch (RuntimeException e)
        {
            listener.done();
            throw CliException.execution(e.getMessage(), e);
        }
    }

    public CliExecution execute(CliArguments arguments, SnapshotSession session) throws Exception
    {
        if (!arguments.getCommand().requiresSnapshot())
            return execute(arguments);
        return execute(arguments, session.getSnapshot(), session.getProgressListener());
    }

    private CliExecution execute(CliArguments arguments, ISnapshot snapshot, IProgressListener listener)
                    throws Exception
    {
        switch (arguments.getCommand())
        {
            case SUMMARY:
                return CliExecution.summary(SnapshotSummary.from(snapshot.getSnapshotInfo()));
            case HISTOGRAM:
                return CliExecution.result(SnapshotQuery.lookup("histogram", snapshot).refine(listener).build()); //$NON-NLS-1$
            case TOP_CONSUMERS:
                return CliExecution.result(new TopConsumersResultBuilder().build(snapshot, listener));
            case PATH2GC:
                return CliExecution.result(SnapshotQuery
                                .parse("path2gc " + arguments.getObjectAddress(), snapshot).execute(listener)); //$NON-NLS-1$
            case OQL:
                return CliExecution.result(SnapshotQuery.lookup("oql", snapshot) //$NON-NLS-1$
                                .setArgument("queryString", arguments.getOqlQuery()).execute(listener)); //$NON-NLS-1$
            case QUERY:
                IResult result = SnapshotQuery.parse(arguments.getQueryCommand(), snapshot).execute(listener);
                validateResult(result);
                return CliExecution.result(result);
            case DESCRIBE:
            case SCHEMA:
                return execute(arguments);
            default:
                throw CliException.usage("Unsupported command: " + arguments.getCommand().getToken()); //$NON-NLS-1$
        }
    }

    private void validateResult(IResult result) throws CliException
    {
        if (result == null)
            throw CliException.execution("Query returned no result", null); //$NON-NLS-1$

        if (!(result instanceof TextResult || result instanceof IResultTable || result instanceof IResultTree
                        || result instanceof Spec))
        {
            throw CliException.unsupported("Unsupported result type: " + result.getClass().getName()); //$NON-NLS-1$
        }
    }

    private CliCommandCatalog.CommandDefinition lookupDefinition(CliCommand command) throws CliException
    {
        CliCommandCatalog.CommandDefinition definition = CliCommandCatalog.lookup(command);
        if (definition == null)
            throw CliException.execution("No command metadata found for " + command.getToken(), null); //$NON-NLS-1$
        return definition;
    }
}
