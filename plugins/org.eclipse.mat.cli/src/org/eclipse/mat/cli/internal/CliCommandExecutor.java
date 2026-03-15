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
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.query.IContextObject;
import org.eclipse.mat.query.IResult;
import org.eclipse.mat.query.IResultPie;
import org.eclipse.mat.query.IResultTable;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.query.refined.RefinedResultBuilder;
import org.eclipse.mat.query.results.CompositeResult;
import org.eclipse.mat.query.results.TextResult;
import org.eclipse.mat.report.QuerySpec;
import org.eclipse.mat.report.SectionSpec;
import org.eclipse.mat.report.Spec;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.SnapshotFactory;
import org.eclipse.mat.snapshot.model.GCRootInfo;
import org.eclipse.mat.snapshot.query.RetainedSizeDerivedData;
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
            case LIST_QUERIES:
                return CliExecution.result(new QueryMetadataCollector().listQueries());
            case DESCRIBE_QUERY:
                return CliExecution.result(new QueryMetadataCollector().describeQuery(arguments.getSubjectName()));
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
                RefinedResultBuilder histogram = SnapshotQuery.lookup("histogram", snapshot).refine(listener); //$NON-NLS-1$
                histogram.addDefaultContextDerivedColumn(RetainedSizeDerivedData.APPROXIMATE);
                return CliExecution.result(histogram.build());
            case TOP_CONSUMERS:
                return CliExecution.result(new TopConsumersResultBuilder().build(snapshot, listener));
            case PATH2GC:
                return executePath2Gc(arguments, snapshot, listener);
            case OQL:
                return executeOql(arguments, snapshot, listener);
            case QUERY:
                return executeQuery(arguments, snapshot, listener);
            case DESCRIBE:
            case SCHEMA:
            case LIST_QUERIES:
            case DESCRIBE_QUERY:
                return execute(arguments);
            default:
                throw CliException.usage("Unsupported command: " + arguments.getCommand().getToken()); //$NON-NLS-1$
        }
    }

    private CliExecution executePath2Gc(CliArguments arguments, ISnapshot snapshot, IProgressListener listener)
                    throws Exception
    {
        int objectId = resolveObjectId(arguments.getObjectAddress(), snapshot);
        IResult result = SnapshotQuery.lookup("path2gc", snapshot) //$NON-NLS-1$
                        .setArgument("object", Integer.valueOf(objectId)).execute(listener); //$NON-NLS-1$
        validateResult(result);
        return CliExecution.result(result, arguments.getObjectAddress(), path2gcNote(snapshot, result));
    }

    private CliExecution executeOql(CliArguments arguments, ISnapshot snapshot, IProgressListener listener)
                    throws Exception
    {
        IResult result = SnapshotQuery.lookup("oql", snapshot) //$NON-NLS-1$
                        .setArgument("queryString", arguments.getOqlQuery()).execute(listener); //$NON-NLS-1$
        validateResult(result);
        if (result instanceof TextResult && isOqlFailureText(((TextResult) result).getText()))
            throw CliException.execution(extractOqlFailureMessage(((TextResult) result).getText()), null);
        return CliExecution.result(result, derivePrimaryObjectAddress(snapshot, result), null);
    }

    private CliExecution executeQuery(CliArguments arguments, ISnapshot snapshot, IProgressListener listener)
                    throws Exception
    {
        if ("help".equalsIgnoreCase(arguments.getQueryCommand().trim())) //$NON-NLS-1$
            return CliExecution.result(new QueryMetadataCollector().listQueries());

        IResult result = SnapshotQuery.parse(arguments.getQueryCommand(), snapshot).execute(listener);
        validateResult(result);
        return CliExecution.result(result, derivePrimaryObjectAddress(snapshot, result), null);
    }

    private int resolveObjectId(String objectAddress, ISnapshot snapshot) throws CliException
    {
        if (objectAddress == null || !objectAddress.startsWith("0x")) //$NON-NLS-1$
            throw CliException.usage("path2gc requires --object 0x..."); //$NON-NLS-1$

        try
        {
            long address = new BigInteger(objectAddress.substring(2), 16).longValue();
            return snapshot.mapAddressToId(address);
        }
        catch (NumberFormatException e)
        {
            throw CliException.usage("Invalid object address: " + objectAddress); //$NON-NLS-1$
        }
        catch (SnapshotException e)
        {
            throw invalidObjectAddress(objectAddress);
        }
        catch (RuntimeException e)
        {
            throw invalidObjectAddress(objectAddress);
        }
    }

    private CliException invalidObjectAddress(String objectAddress)
    {
        return CliException.execution("No object found at address " + objectAddress //$NON-NLS-1$
                        + ". Use 'mat-cli oql <heap> --query \"SELECT * FROM OBJECTS " + objectAddress //$NON-NLS-1$
                        + "\"' to verify the address.", null); //$NON-NLS-1$
    }

    private boolean isOqlFailureText(String text)
    {
        if (text == null)
            return false;

        String lower = text.toLowerCase(Locale.ENGLISH);
        return lower.contains("encountered \"") || lower.contains("problem reported:"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String extractOqlFailureMessage(String text)
    {
        if (text == null)
            return null;

        int marker = text.indexOf("Problem reported:"); //$NON-NLS-1$
        if (marker >= 0)
            text = text.substring(marker).trim();

        int stackTrace = text.indexOf("\n\n"); //$NON-NLS-1$
        if (stackTrace >= 0)
            return text.substring(0, stackTrace).trim();
        return text.trim();
    }

    private String path2gcNote(ISnapshot snapshot, IResult result)
    {
        if (!(result instanceof IResultTree))
            return null;

        IResultTree tree = (IResultTree) result;
        List<?> elements = tree.getElements();
        if (elements.size() != 1)
            return null;

        Object root = elements.get(0);
        if (tree.hasChildren(root))
            return null;

        IContextObject context = contextFromTree(tree);
        if (context == null || context.getObjectId() < 0)
            return null;

        try
        {
            if (!snapshot.isGCRoot(context.getObjectId()))
                return null;

            GCRootInfo[] info = snapshot.getGCRootInfo(context.getObjectId());
            String type = info == null || info.length == 0 ? null : GCRootInfo.getTypeSetAsString(info);
            return type == null || type.length() == 0 ? "Object is already a GC root. No path needed." //$NON-NLS-1$
                            : "Object is already a GC root (" + type + "). No path needed."; //$NON-NLS-1$ //$NON-NLS-2$
        }
        catch (SnapshotException e)
        {
            return "Object is already a GC root. No path needed."; //$NON-NLS-1$
        }
    }

    private String derivePrimaryObjectAddress(ISnapshot snapshot, IResult result)
    {
        if (result instanceof IResultTable)
            return addressForContext(snapshot, contextFromTable((IResultTable) result));
        if (result instanceof IResultTree)
            return addressForContext(snapshot, contextFromTree((IResultTree) result));
        if (result instanceof IResultPie)
            return addressForContext(snapshot, contextFromPie((IResultPie) result));
        if (result instanceof QuerySpec)
            return derivePrimaryObjectAddress(snapshot, ((QuerySpec) result).getResult());
        if (result instanceof SectionSpec)
        {
            for (Spec child : ((SectionSpec) result).getChildren())
            {
                String address = derivePrimaryObjectAddress(snapshot, child);
                if (address != null)
                    return address;
            }
        }
        if (result instanceof CompositeResult)
        {
            for (CompositeResult.Entry entry : ((CompositeResult) result).getResultEntries())
            {
                String address = derivePrimaryObjectAddress(snapshot, entry.getResult());
                if (address != null)
                    return address;
            }
        }
        return null;
    }

    private IContextObject contextFromTable(IResultTable table)
    {
        if (table.getRowCount() <= 0)
            return null;

        try
        {
            return table.getContext(table.getRow(0));
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    private IContextObject contextFromTree(IResultTree tree)
    {
        List<?> elements = tree.getElements();
        if (elements.isEmpty())
            return null;

        try
        {
            return tree.getContext(elements.get(0));
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    private IContextObject contextFromPie(IResultPie pie)
    {
        List<? extends IResultPie.Slice> slices = pie.getSlices();
        if (slices.isEmpty())
            return null;
        return slices.get(0).getContext();
    }

    private String addressForContext(ISnapshot snapshot, IContextObject context)
    {
        if (context == null || context.getObjectId() < 0)
            return null;

        try
        {
            return "0x" + Long.toHexString(snapshot.mapIdToAddress(context.getObjectId())); //$NON-NLS-1$
        }
        catch (SnapshotException e)
        {
            return null;
        }
    }

    private void validateResult(IResult result) throws CliException
    {
        if (result == null)
            throw CliException.execution("Query returned no result", null); //$NON-NLS-1$

        if (!(result instanceof TextResult || result instanceof IResultTable || result instanceof IResultTree
                        || result instanceof IResultPie || result instanceof CompositeResult || result instanceof Spec))
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
