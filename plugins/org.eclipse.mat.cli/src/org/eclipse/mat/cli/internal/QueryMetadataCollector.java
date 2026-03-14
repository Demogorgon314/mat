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
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.query.ContextDerivedData;
import org.eclipse.mat.query.annotations.Argument.Advice;
import org.eclipse.mat.query.registry.ArgumentDescriptor;
import org.eclipse.mat.query.registry.QueryContextImpl;
import org.eclipse.mat.query.registry.QueryDescriptor;
import org.eclipse.mat.query.registry.QueryRegistry;
import org.eclipse.mat.snapshot.extension.Subject;
import org.eclipse.mat.snapshot.extension.Subjects;

final class QueryMetadataCollector
{
    private static final EmptyQueryContext EMPTY_CONTEXT = new EmptyQueryContext();

    public QueryMetadataResult listQueries()
    {
        Collection<QueryDescriptor> queries = QueryRegistry.instance().getQueries();
        List<QueryMetadataResult.QueryDefinition> definitions = new ArrayList<QueryMetadataResult.QueryDefinition>(
                        queries.size());
        for (QueryDescriptor descriptor : queries)
        {
            definitions.add(toDefinition(descriptor));
        }
        Collections.sort(definitions, Comparator.comparing(QueryMetadataResult.QueryDefinition::getIdentifier));
        return new QueryMetadataResult(QueryMetadataResult.Kind.LIST, null, definitions);
    }

    public QueryMetadataResult describeQuery(String identifier) throws CliException
    {
        QueryDescriptor descriptor = QueryRegistry.instance().getQuery(identifier.toLowerCase(java.util.Locale.ENGLISH));
        if (descriptor == null)
            throw CliException.execution("Unknown MAT query: " + identifier, null); //$NON-NLS-1$
        return new QueryMetadataResult(QueryMetadataResult.Kind.DESCRIBE, toDefinition(descriptor), null);
    }

    private QueryMetadataResult.QueryDefinition toDefinition(QueryDescriptor descriptor)
    {
        List<QueryMetadataResult.QueryArgument> arguments = new ArrayList<QueryMetadataResult.QueryArgument>();
        for (ArgumentDescriptor argument : descriptor.getArguments())
        {
            arguments.add(new QueryMetadataResult.QueryArgument(argument.getName(), argument.getFlag(),
                            argument.getType() == null ? null : argument.getType().getName(),
                            argument.getAdvice() == null ? null : argument.getAdvice().name(), argument.isMandatory(),
                            argument.isMultiple(), argument.isBoolean(), argument.isEnum(),
                            argument.getDefaultValue() == null ? null : String.valueOf(argument.getDefaultValue()),
                            argument.getHelp()));
        }

        return new QueryMetadataResult.QueryDefinition(descriptor.getIdentifier(), descriptor.getName(),
                        descriptor.getCategory(), descriptor.getUsage(EMPTY_CONTEXT), descriptor.getShortDescription(),
                        descriptor.getHelp(), descriptor.getHelpUrl(), descriptor.getCommandType().getName(),
                        descriptor.isShallow(), extractSubjects(descriptor), arguments);
    }

    private List<String> extractSubjects(QueryDescriptor descriptor)
    {
        Subjects subjects = descriptor.getCommandType().getAnnotation(Subjects.class);
        if (subjects != null)
        {
            List<String> values = new ArrayList<String>(subjects.value().length);
            Collections.addAll(values, subjects.value());
            return values;
        }

        Subject subject = descriptor.getCommandType().getAnnotation(Subject.class);
        if (subject != null)
            return Collections.singletonList(subject.value());

        return Collections.emptyList();
    }

    private static final class EmptyQueryContext extends QueryContextImpl
    {
        public File getPrimaryFile()
        {
            return null;
        }

        public String getPrefix()
        {
            return ""; //$NON-NLS-1$
        }

        public String mapToExternalIdentifier(int objectId) throws SnapshotException
        {
            throw new SnapshotException("No snapshot is available"); //$NON-NLS-1$
        }

        public int mapToObjectId(String externalIdentifier) throws SnapshotException
        {
            throw new SnapshotException("No snapshot is available"); //$NON-NLS-1$
        }

        public boolean parses(Class<?> type, Advice advice)
        {
            return false;
        }

        public Object parse(Class<?> type, Advice advice, String[] args, ParsePosition pos) throws SnapshotException
        {
            throw new SnapshotException("No snapshot is available"); //$NON-NLS-1$
        }

        public ContextDerivedData getContextDerivedData()
        {
            return null;
        }
    }
}
