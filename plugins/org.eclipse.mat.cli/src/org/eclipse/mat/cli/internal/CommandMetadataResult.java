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

import org.eclipse.mat.query.IResult;
import org.eclipse.mat.query.ResultMetaData;

public final class CommandMetadataResult implements IResult
{
    public enum Kind
    {
        DESCRIBE, SCHEMA
    }

    private final Kind kind;
    private final CliCommandCatalog.CommandDefinition definition;

    public CommandMetadataResult(Kind kind, CliCommandCatalog.CommandDefinition definition)
    {
        this.kind = kind;
        this.definition = definition;
    }

    public Kind getKind()
    {
        return kind;
    }

    public CliCommandCatalog.CommandDefinition getDefinition()
    {
        return definition;
    }

    public ResultMetaData getResultMetaData()
    {
        return null;
    }
}
