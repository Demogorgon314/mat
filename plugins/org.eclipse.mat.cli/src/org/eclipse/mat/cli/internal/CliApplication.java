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

import java.io.PrintStream;
import java.util.Locale;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.mat.cli.internal.serialization.ResultSerializer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class CliApplication implements IApplication
{
    private static final String[] REQUIRED_RUNTIME_BUNDLES = new String[] { "org.eclipse.core.runtime", //$NON-NLS-1$
                    "org.eclipse.equinox.registry", //$NON-NLS-1$
                    "org.eclipse.equinox.preferences", //$NON-NLS-1$
                    "org.apache.felix.scr", //$NON-NLS-1$
                    "org.eclipse.core.contenttype" //$NON-NLS-1$
    };

    private final CliArgumentParser parser = new CliArgumentParser();
    private final CliCommandExecutor executor = new CliCommandExecutor();
    private final ResultSerializer serializer = new ResultSerializer();

    public Object start(IApplicationContext context) throws Exception
    {
        String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
        CliArguments parsed = null;
        CliArguments.OutputFormat requestedFormat = parser.detectFormat(args);
        try
        {
            if (requestedFormat == CliArguments.OutputFormat.JSON)
                Locale.setDefault(Locale.ENGLISH);

            parsed = parser.parse(args);
            if (parsed.isHelp())
            {
                System.out.print(CliHelp.generalHelp());
                return IApplication.EXIT_OK;
            }

            if (parsed.getCommand().requiresRuntimeServices())
                ensurePlatformServices();

            if (parsed.getCommand().requiresSnapshot())
            {
                try (SnapshotSession session = executor.openSnapshot(parsed))
                {
                    CliExecution execution = executor.execute(parsed, session);
                    serializer.serialize(parsed, execution, System.out);
                }
            }
            else
            {
                CliExecution execution = executor.execute(parsed);
                serializer.serialize(parsed, execution, System.out);
            }
            return IApplication.EXIT_OK;
        }
        catch (CliException e)
        {
            if (parsed == null)
                parsed = parser.partialParse(args, requestedFormat);
            return exit(e.getExitCode(), parsed, requestedFormat, e, System.err, System.out);
        }
        catch (OutOfMemoryError e)
        {
            if (parsed == null)
                parsed = parser.partialParse(args, requestedFormat);
            return exit(CliExitCodes.OUT_OF_MEMORY, parsed, requestedFormat, e, System.err, System.out);
        }
        catch (Exception e)
        {
            if (parsed == null)
                parsed = parser.partialParse(args, requestedFormat);
            return exit(CliExitCodes.EXECUTION_ERROR, parsed, requestedFormat, e, System.err, System.out);
        }
    }

    private Object exit(int code, CliArguments parsed, CliArguments.OutputFormat requestedFormat, Throwable error,
                    PrintStream err, PrintStream out)
    {
        CliArguments.OutputFormat format = parsed == null ? requestedFormat : parsed.getFormat();
        boolean json = format == CliArguments.OutputFormat.JSON;
        if (json)
        {
            serializer.serializeError(parsed, format, code, error, out);
        }
        else
        {
            err.println(error.getMessage() == null ? error.getClass().getName() : error.getMessage());
            if (parsed != null && parsed.isVerbose() && error.getCause() != null)
                error.getCause().printStackTrace(err);
            else if (parsed != null && parsed.isVerbose())
                error.printStackTrace(err);
            if (code == CliExitCodes.USAGE)
                err.print(CliHelp.generalHelp());
        }
        return Integer.valueOf(code);
    }

    public void stop()
    {}

    private void ensurePlatformServices() throws CliException
    {
        for (String bundleId : REQUIRED_RUNTIME_BUNDLES)
            startBundle(bundleId);

        if (Platform.getExtensionRegistry() == null)
            throw CliException.execution("Eclipse extension registry is not available in the CLI runtime.", null); //$NON-NLS-1$

        long deadline = System.currentTimeMillis() + 5000L;
        while (Platform.getContentTypeManager() == null && System.currentTimeMillis() < deadline)
        {
            try
            {
                Thread.sleep(50L);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw CliException.execution("Interrupted while starting Eclipse content type services.", e); //$NON-NLS-1$
            }
        }

        if (Platform.getContentTypeManager() == null)
            throw CliException.execution("Eclipse content type manager is not available in the CLI runtime.", null); //$NON-NLS-1$
    }

    private void startBundle(String bundleId) throws CliException
    {
        Bundle bundle = Platform.getBundle(bundleId);
        if (bundle == null)
            throw CliException.execution("Required Eclipse runtime bundle is missing: " + bundleId, null); //$NON-NLS-1$

        int state = bundle.getState();
        if ((state & (Bundle.ACTIVE | Bundle.STARTING)) != 0)
            return;

        try
        {
            bundle.start(Bundle.START_TRANSIENT);
        }
        catch (BundleException e)
        {
            throw CliException.execution("Unable to start required Eclipse runtime bundle: " + bundleId, e); //$NON-NLS-1$
        }
    }
}
