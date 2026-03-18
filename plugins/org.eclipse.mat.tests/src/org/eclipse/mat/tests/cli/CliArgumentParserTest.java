/*******************************************************************************
 * Copyright (c) 2026 Eclipse Memory Analyzer Project.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.mat.tests.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.eclipse.mat.cli.internal.CliArgumentParser;
import org.eclipse.mat.cli.internal.CliArguments;
import org.eclipse.mat.cli.internal.CliCommand;
import org.eclipse.mat.cli.internal.CliException;
import org.eclipse.mat.cli.internal.CliHelp;
import org.eclipse.mat.query.BytesDisplay;
import org.junit.Test;

public class CliArgumentParserTest
{
    @Test
    public void parsesObjectsArgumentsWithDefaultClassGrouping() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "objects", "sample.hprof", "--limit", "5", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertEquals(CliCommand.OBJECTS, arguments.getCommand());
        assertEquals(CliArguments.OutputFormat.JSON, arguments.getFormat());
        assertEquals(BytesDisplay.Smart, arguments.getBytesDisplay());
        assertEquals(CliArguments.ObjectsGrouping.CLASS, arguments.getObjectsGrouping());
        assertEquals(5, arguments.getLimit());
        assertEquals("sample.hprof", arguments.getHeapFile().getName()); //$NON-NLS-1$
    }

    @Test
    public void parsesDumpArguments() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser
                        .parse(new String[] { "objects", "sample.hprof", "--dump", "--limit", "5", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertTrue(arguments.isDump());
        assertEquals(5, arguments.getLimit());
        assertEquals(CliArguments.OutputFormat.JSON, arguments.getFormat());
    }

    @Test
    public void parsesExplicitBytesDisplay() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser
                        .parse(new String[] { "objects", "sample.hprof", "--bytes-display", "megabytes" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertEquals(BytesDisplay.Megabytes, arguments.getBytesDisplay());
    }

    @Test
    public void rejectsInvalidBytesDisplay() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        try
        {
            parser.parse(new String[] { "objects", "sample.hprof", "--bytes-display", "wat" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            fail("Expected invalid --bytes-display rejection"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertTrue(e.getMessage().contains("Invalid --bytes-display")); //$NON-NLS-1$
            assertTrue(e.getMessage().contains("bytes, kilobytes, megabytes, gigabytes, smart")); //$NON-NLS-1$
        }
    }

    @Test
    public void parsesObjectsPackageGroupingAndFilter() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(
                        new String[] { "objects", "sample.hprof", "--by", "package", "--package", "org.apache" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertEquals(CliCommand.OBJECTS, arguments.getCommand());
        assertEquals(CliArguments.ObjectsGrouping.PACKAGE, arguments.getObjectsGrouping());
        assertEquals("org.apache", arguments.getPackageName()); //$NON-NLS-1$
    }

    @Test
    public void parsesObjectsClassLoaderGroupingAndFilter() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "objects", "sample.hprof", "--by", "class-loader",
                        "--class-loader", "AppClassLoader" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertEquals(CliCommand.OBJECTS, arguments.getCommand());
        assertEquals(CliArguments.ObjectsGrouping.CLASS_LOADER, arguments.getObjectsGrouping());
        assertEquals("AppClassLoader", arguments.getClassLoaderName()); //$NON-NLS-1$
    }

    @Test
    public void parsesObjectsClassGroupingWithClassLoaderFilterByDefault() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser
                        .parse(new String[] { "objects", "sample.hprof", "--class-loader", "AppClassLoader" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertEquals(CliCommand.OBJECTS, arguments.getCommand());
        assertEquals(CliArguments.ObjectsGrouping.CLASS, arguments.getObjectsGrouping());
        assertEquals("AppClassLoader", arguments.getClassLoaderName()); //$NON-NLS-1$
    }

    @Test
    public void parsesPathToGcArguments() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "path2gc", "sample.hprof", "--object", "0x2a" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertEquals(CliCommand.PATH2GC, arguments.getCommand());
        assertEquals("0x2a", arguments.getObjectAddress()); //$NON-NLS-1$
    }

    @Test
    public void parsesInstancesArguments() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "instances", "sample.hprof", "--class",
                        "java.lang.String", "--include-subclasses", "--limit", "7", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

        assertEquals(CliCommand.INSTANCES, arguments.getCommand());
        assertEquals("java.lang.String", arguments.getClassName()); //$NON-NLS-1$
        assertNull(arguments.getClassRegex());
        assertNull(arguments.getClassContains());
        assertTrue(arguments.isIncludeSubclasses());
        assertEquals(7, arguments.getLimit());
        assertEquals(CliArguments.OutputFormat.JSON, arguments.getFormat());
    }

    @Test
    public void parsesInstancesRegexArguments() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "instances", "sample.hprof",
                        "--class-regex=java\\.lang\\.String", "--limit", "7", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertEquals(CliCommand.INSTANCES, arguments.getCommand());
        assertNull(arguments.getClassName());
        assertEquals("java\\.lang\\.String", arguments.getClassRegex()); //$NON-NLS-1$
        assertNull(arguments.getClassContains());
        assertEquals(7, arguments.getLimit());
        assertEquals(CliArguments.OutputFormat.JSON, arguments.getFormat());
    }

    @Test
    public void parsesInstancesContainsArguments() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "instances", "sample.hprof", "--class-contains",
                        "String", "--limit", "7", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertEquals(CliCommand.INSTANCES, arguments.getCommand());
        assertNull(arguments.getClassName());
        assertNull(arguments.getClassRegex());
        assertEquals("String", arguments.getClassContains()); //$NON-NLS-1$
        assertEquals(7, arguments.getLimit());
        assertEquals(CliArguments.OutputFormat.JSON, arguments.getFormat());
    }

    @Test
    public void parsesInspectObjectArguments() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(
                        new String[] { "inspect-object", "sample.hprof", "--object", "0x2a", "--field-paths",
                                        "value", "--depth", "3", "--show-nulls" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

        assertEquals(CliCommand.INSPECT_OBJECT, arguments.getCommand());
        assertEquals("0x2a", arguments.getObjectAddress()); //$NON-NLS-1$
        assertEquals(Arrays.asList("value"), arguments.getFieldPaths()); //$NON-NLS-1$
        assertEquals(3, arguments.getTreeDepthLimit());
        assertTrue(arguments.isShowNulls());
    }

    @Test
    public void parsesRepeatedInspectObjectFieldPathsArguments() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "inspect-object", "sample.hprof", "--object", "0x2a",
                        "--field-paths", "value", "--field-paths", "count" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertEquals(Arrays.asList("value", "count"), arguments.getFieldPaths()); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(arguments.getSelectFields().isEmpty());
    }

    @Test
    public void parsesRepeatedInspectObjectSelectFieldsArguments() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "inspect-object", "sample.hprof", "--object", "0x2a",
                        "--select-fields", "value", "--select-fields", "coder" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertEquals(Arrays.asList("value", "coder"), arguments.getSelectFields()); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(arguments.getFieldPaths().isEmpty());
    }

    @Test
    public void defaultsInspectObjectDepthToThree() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "inspect-object", "sample.hprof", "--object", "0x2a" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertEquals(3, arguments.getTreeDepthLimit());
    }

    @Test
    public void rejectsRemovedAgentOption() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        try
        {
            parser.parse(new String[] { "--agent", "inspect-object", "sample.hprof", "--object", "0x2a" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            fail("Expected removed --agent option to be rejected"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertEquals(2, e.getExitCode());
            assertTrue(e.getMessage().contains("Use --format json")); //$NON-NLS-1$
        }
    }

    @Test
    public void rejectsConflictingInspectObjectPathOptions() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();

        try
        {
            parser.parse(new String[] { "inspect-object", "sample.hprof", "--object", "0x2a", "--select-fields",
                            "value", "--field-paths", "value.count" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
            fail("Expected conflicting inspect-object options"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertTrue(e.getMessage().contains("only one of --select-fields or --field-paths")); //$NON-NLS-1$
        }
    }

    @Test
    public void rejectsInstancesWithoutClassSelector() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();

        try
        {
            parser.parse(new String[] { "instances", "sample.hprof" }); //$NON-NLS-1$ //$NON-NLS-2$
            fail("Expected missing instances class selector"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertEquals(2, e.getExitCode());
            assertTrue(e.getMessage().contains("requires one of --class <fqcn>, --class-regex <regex> or --class-contains <text>")); //$NON-NLS-1$
        }
    }

    @Test
    public void rejectsInstancesWithConflictingClassSelectors() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();

        try
        {
            parser.parse(new String[] { "instances", "sample.hprof", "--class", "java.lang.String", "--class-regex",
                            "java\\.lang\\..*" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            fail("Expected conflicting instances selectors"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertEquals(2, e.getExitCode());
            assertTrue(e.getMessage().contains("only one of --class, --class-regex or --class-contains")); //$NON-NLS-1$
        }
    }

    @Test
    public void rejectsInstancesWithContainsAndRegexSelectors() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();

        try
        {
            parser.parse(new String[] { "instances", "sample.hprof", "--class-contains", "String", "--class-regex",
                            "java\\.lang\\..*" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            fail("Expected conflicting instances selectors"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertEquals(2, e.getExitCode());
            assertTrue(e.getMessage().contains("only one of --class, --class-regex or --class-contains")); //$NON-NLS-1$
        }
    }

    @Test
    public void rejectsInvalidInstancesClassRegex() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();

        try
        {
            parser.parse(new String[] { "instances", "sample.hprof", "--class-regex", "[" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            fail("Expected invalid class regex"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertEquals(2, e.getExitCode());
            assertTrue(e.getMessage().contains("Invalid --class-regex")); //$NON-NLS-1$
        }
    }

    @Test
    public void rejectsDottedSelectField() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();

        try
        {
            parser.parse(new String[] { "inspect-object", "sample.hprof", "--object", "0x2a", "--select-fields",
                            "value.count" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            fail("Expected dotted select-fields rejection"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertTrue(e.getMessage().contains("Use --field-paths")); //$NON-NLS-1$
        }
    }

    @Test
    public void rejectsInvalidFieldPaths() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();

        try
        {
            parser.parse(new String[] { "inspect-object", "sample.hprof", "--object", "0x2a", "--field-paths",
                            "value..count" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            fail("Expected invalid field-paths rejection"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertTrue(e.getMessage().contains("Invalid --field-paths")); //$NON-NLS-1$
        }
    }

    @Test
    public void rejectsLegacyInspectObjectFlags() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();

        try
        {
            parser.parse(new String[] { "inspect-object", "sample.hprof", "--object", "0x2a", "--select-field",
                            "value" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            fail("Expected legacy select-field rejection"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertTrue(e.getMessage().contains("Unknown option: --select-field")); //$NON-NLS-1$
        }

        try
        {
            parser.parse(new String[] { "inspect-object", "sample.hprof", "--object", "0x2a", "--field-path",
                            "value" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            fail("Expected legacy field-path rejection"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertTrue(e.getMessage().contains("Unknown option: --field-path")); //$NON-NLS-1$
        }
    }

    @Test
    public void defaultsThreadsCommandToAllThreads()
                    throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "threads", "sample.hprof" }); //$NON-NLS-1$ //$NON-NLS-2$

        assertEquals(CliCommand.THREADS, arguments.getCommand());
        assertEquals(Integer.MAX_VALUE, arguments.getLimit());
    }

    @Test
    public void defaultsBiggestObjectsDepthToOne() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "biggest-objects", "sample.hprof" }); //$NON-NLS-1$ //$NON-NLS-2$

        assertEquals(CliCommand.BIGGEST_OBJECTS, arguments.getCommand());
        assertEquals(1, arguments.getTreeDepthLimit());
    }

    @Test
    public void rejectsObjectsClassLoaderFilterWithPackageGrouping() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();

        try
        {
            parser.parse(new String[] { "objects", "sample.hprof", "--by", "package", "--class-loader",
                            "AppClassLoader" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            fail("Expected --class-loader validation"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertTrue(e.getMessage().contains("--class-loader is not supported with --by package")); //$NON-NLS-1$
        }
    }

    @Test
    public void rejectsObjectsPackageFilterWithClassLoaderGrouping() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();

        try
        {
            parser.parse(new String[] { "objects", "sample.hprof", "--by", "class-loader", "--package", "java" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            fail("Expected --package validation"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertTrue(e.getMessage().contains("--package is not supported with --by class-loader")); //$NON-NLS-1$
        }
    }

    @Test
    public void rejectsLegacyCliCommandsWithMigrationHint() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();

        try
        {
            parser.parse(new String[] { "histogram", "sample.hprof" }); //$NON-NLS-1$ //$NON-NLS-2$
            fail("Expected histogram migration error"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertTrue(e.getMessage().contains("replaced by `objects`")); //$NON-NLS-1$
        }

        try
        {
            parser.parse(new String[] { "top-consumers", "sample.hprof" }); //$NON-NLS-1$ //$NON-NLS-2$
            fail("Expected top-consumers migration error"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertTrue(e.getMessage().contains("replaced by `biggest-objects`")); //$NON-NLS-1$
            assertTrue(e.getMessage().contains("objects <heap> --by package")); //$NON-NLS-1$
        }
    }

    @Test
    public void rejectsRemovedProfileOption() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        try
        {
            parser.parse(new String[] { "describe", "objects", "--profile", "agent" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            fail("Expected removed --profile option to be rejected"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertEquals(2, e.getExitCode());
            assertTrue(e.getMessage().contains("Use --format json")); //$NON-NLS-1$
        }
    }

    @Test
    public void parsesDescribeCommandWithoutHeap() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "describe", "objects", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertEquals(CliCommand.DESCRIBE, arguments.getCommand());
        assertEquals(CliCommand.OBJECTS, arguments.getSubjectCommand());
        assertEquals(CliArguments.OutputFormat.JSON, arguments.getFormat());
    }

    @Test
    public void parsesDescribeQueryCommandWithoutHeap() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "describe-query", "hash_entries", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertEquals(CliCommand.DESCRIBE_QUERY, arguments.getCommand());
        assertEquals("hash_entries", arguments.getSubjectName()); //$NON-NLS-1$
        assertEquals(CliArguments.OutputFormat.JSON, arguments.getFormat());
    }

    @Test
    public void parsesCompletionCommandForBash() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "completion", "bash" }); //$NON-NLS-1$ //$NON-NLS-2$

        assertEquals(CliCommand.COMPLETION, arguments.getCommand());
        assertEquals("bash", arguments.getCompletionShell()); //$NON-NLS-1$
        assertEquals("bash", arguments.getSubjectName()); //$NON-NLS-1$
    }

    @Test
    public void parsesCompletionCommandForZshInJsonMode() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "completion", "zsh", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertEquals(CliCommand.COMPLETION, arguments.getCommand());
        assertEquals("zsh", arguments.getCompletionShell()); //$NON-NLS-1$
        assertEquals(CliArguments.OutputFormat.JSON, arguments.getFormat());
    }

    @Test
    public void rejectsCompletionCommandWithoutShell() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();

        try
        {
            parser.parse(new String[] { "completion" }); //$NON-NLS-1$
            fail("Expected missing completion shell to be rejected"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertEquals(2, e.getExitCode());
            assertTrue(e.getMessage().contains("completion requires a shell name")); //$NON-NLS-1$
        }
    }

    @Test
    public void rejectsUnsupportedCompletionShell() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();

        try
        {
            parser.parse(new String[] { "completion", "fish" }); //$NON-NLS-1$ //$NON-NLS-2$
            fail("Expected unsupported completion shell to be rejected"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertEquals(2, e.getExitCode());
            assertTrue(e.getMessage().contains("Unsupported completion shell: fish")); //$NON-NLS-1$
        }
    }

    @Test
    public void parsesListQueriesCommandWithoutHeap() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "list-queries", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertEquals(CliCommand.LIST_QUERIES, arguments.getCommand());
        assertEquals(CliArguments.OutputFormat.JSON, arguments.getFormat());
    }

    @Test
    public void readsOqlFromFile() throws Exception
    {
        Path queryFile = Files.createTempFile("mat-cli-oql-", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        Files.write(queryFile, "SELECT * FROM java.lang.String".getBytes(StandardCharsets.UTF_8)); //$NON-NLS-1$

        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(
                        new String[] { "oql", "sample.hprof", "--query-file", queryFile.toString() }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertEquals("SELECT * FROM java.lang.String", arguments.getOqlQuery()); //$NON-NLS-1$
    }

    @Test
    public void readsQueryCommandFromStandardInput() throws Exception
    {
        InputStream original = System.in;
        try
        {
            System.setIn(new ByteArrayInputStream("hash_entries java.util.AbstractMap -include_subclasses" //$NON-NLS-1$
                            .getBytes(StandardCharsets.UTF_8)));
            CliArgumentParser parser = new CliArgumentParser();
            CliArguments arguments = parser.parse(
                            new String[] { "query", "sample.hprof", "--command-stdin" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            assertEquals("hash_entries java.util.AbstractMap -include_subclasses", arguments.getQueryCommand()); //$NON-NLS-1$
        }
        finally
        {
            System.setIn(original);
        }
    }

    @Test
    public void detectsRequestedFormatBeforeFullParse()
    {
        CliArgumentParser parser = new CliArgumentParser();

        assertEquals(CliArguments.OutputFormat.JSON,
                        parser.detectFormat(new String[] { "--format", "json", "summary", "sample.hprof" })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        assertEquals(CliArguments.OutputFormat.TEXT,
                        parser.detectFormat(new String[] { "--agent", "--format", "text", "summary", "sample.hprof" })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    }

    @Test
    public void capsExplicitLimitAtMaximum()
                    throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "objects", "sample.hprof", "--limit", "500000" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertEquals(10000, arguments.getLimit());
    }

    @Test
    public void appliesDefaultLimitToQueries()
                    throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "query", "sample.hprof", "--command", "thread_overview" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertEquals(20, arguments.getLimit());
    }

    @Test
    public void parsesExplicitDepth()
                    throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(
                        new String[] { "query", "sample.hprof", "--command", "gc_roots", "--depth", "3" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        assertEquals(3, arguments.getTreeDepthLimit());
    }

    @Test
    public void rejectsInvalidDepth()
                    throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();

        try
        {
            parser.parse(new String[] { "query", "sample.hprof", "--command", "gc_roots", "--depth", "0" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            fail("Expected invalid depth"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertEquals(2, e.getExitCode());
            assertTrue(e.getMessage().contains("Depth must be >= 1")); //$NON-NLS-1$
        }
    }

    @Test
    public void rejectsUnsupportedDumpCommand() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();

        try
        {
            parser.parse(new String[] { "summary", "sample.hprof", "--dump" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            fail("Expected unsupported --dump rejection"); //$NON-NLS-1$
        }
        catch (CliException e)
        {
            assertEquals(2, e.getExitCode());
            assertTrue(e.getMessage().contains("summary does not support --dump")); //$NON-NLS-1$
        }
    }

    @Test
    public void parsesHelpWithoutCommand() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[0]);

        assertTrue(arguments.isHelp());
    }

    @Test
    public void parsesCommandSpecificHelpAfterCommand() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "summary", "--help" }); //$NON-NLS-1$ //$NON-NLS-2$

        assertEquals(CliCommand.SUMMARY, arguments.getCommand());
        assertTrue(arguments.isHelp());
        assertNull(arguments.getHeapFile());
    }

    @Test
    public void parsesCommandSpecificHelpBeforeCommand() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "help", "objects" }); //$NON-NLS-1$ //$NON-NLS-2$

        assertEquals(CliCommand.OBJECTS, arguments.getCommand());
        assertTrue(arguments.isHelp());
        assertNull(arguments.getHeapFile());
    }

    @Test
    public void parsesCommandSpecificHelpKeywordAfterCommand() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "query", "help" }); //$NON-NLS-1$ //$NON-NLS-2$

        assertEquals(CliCommand.QUERY, arguments.getCommand());
        assertTrue(arguments.isHelp());
        assertNull(arguments.getHeapFile());
        assertNull(arguments.getQueryCommand());
    }

    @Test
    public void parsesDescribeQueryHelpWithoutSubject() throws Exception
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.parse(new String[] { "describe-query", "--help" }); //$NON-NLS-1$ //$NON-NLS-2$

        assertEquals(CliCommand.DESCRIBE_QUERY, arguments.getCommand());
        assertTrue(arguments.isHelp());
        assertNull(arguments.getSubjectName());
    }

    @Test
    public void partialParsePreservesCommandForPrefixedHelp()
    {
        CliArgumentParser parser = new CliArgumentParser();
        CliArguments arguments = parser.partialParse(new String[] { "help", "path2gc", "--format", "json" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                        CliArguments.OutputFormat.JSON);

        assertEquals(CliCommand.PATH2GC, arguments.getCommand());
        assertTrue(arguments.isHelp());
        assertEquals(CliArguments.OutputFormat.JSON, arguments.getFormat());
    }

    @Test
    public void helpIncludesObjectsAndBiggestObjectsCommands()
    {
        String help = CliHelp.generalHelp();

        assertTrue(help.contains("Usage:\n  mat-cli <command> <heap> [options]\n  mat-cli <command> [options]\n  mat-cli <command> --help\n  mat-cli --help")); //$NON-NLS-1$
        assertHelpContainsCommandSummary(help, "summary", "Read basic heap metadata such as object counts and used heap."); //$NON-NLS-1$ //$NON-NLS-2$
        assertHelpContainsCommandSummary(help, "objects", "Inspect objects grouped by class, package, or class loader."); //$NON-NLS-1$ //$NON-NLS-2$
        assertHelpContainsCommandSummary(help, "inspect-object", //$NON-NLS-1$
                        "Inspect one object like MAT's object inspector, or jump directly to one or more field paths for targeted state checks."); //$NON-NLS-1$
        assertHelpContainsCommandSummary(help, "describe", //$NON-NLS-1$
                        "Describe a CLI command, its options, and the result kinds it can return."); //$NON-NLS-1$
        assertHelpContainsCommandSummary(help, "completion", "Generate a bash or zsh shell completion script for mat-cli."); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(help.contains("--bytes-display MODE Byte display mode for text output: bytes|kilobytes|megabytes|gigabytes|smart (default: smart)")); //$NON-NLS-1$
        assertTrue(help.contains("--depth N            Maximum tree or section depth (default: 8, inspect-object: 3, biggest-objects: 1)")); //$NON-NLS-1$
        assertTrue(help.contains("--dump               Emit full structured values for supported commands, ignore --limit, and only honor --depth")); //$NON-NLS-1$
        assertTrue(help.contains("--field-paths PATH    Inspect dotted field paths such as cleaner.offsetMap; may be repeated")); //$NON-NLS-1$
        assertTrue(help.contains("--select-fields FIELD Inspect direct fields from the root object; may be repeated")); //$NON-NLS-1$
        assertTrue(help.contains("--show-nulls         Show nested null fields and array slots in text output")); //$NON-NLS-1$
        assertTrue(help.contains("Use 'mat-cli <command> --help' for command-specific help.")); //$NON-NLS-1$
        assertTrue(help.contains("Inner class names containing '$' must be quoted or escaped")); //$NON-NLS-1$
        assertFalse(help.contains("mat-cli describe <command> [options]")); //$NON-NLS-1$
        assertFalse(help.contains("mat-cli schema <command> [options]")); //$NON-NLS-1$
        assertFalse(help.contains("mat-cli list-queries [options]")); //$NON-NLS-1$
        assertFalse(help.contains("mat-cli describe-query <query-id> [options]")); //$NON-NLS-1$
        assertFalse(help.contains("mat-cli completion <bash|zsh> [options]")); //$NON-NLS-1$
        assertFalse(help.contains("objects <heap> [--by class|package|class-loader]")); //$NON-NLS-1$
        assertFalse(help.contains("--field-path PATH")); //$NON-NLS-1$
        assertFalse(help.contains("--select-field FIELD")); //$NON-NLS-1$
        assertFalse(help.contains("top-consumers <heap>")); //$NON-NLS-1$
        assertFalse(help.contains("histogram <heap>")); //$NON-NLS-1$
    }

    @Test
    public void commandHelpRendersSpecificCommandMetadata()
    {
        String help = CliHelp.commandHelp(CliCommand.PATH2GC);

        assertTrue(help.contains("Command: path2gc")); //$NON-NLS-1$
        assertTrue(help.contains("Usage: mat-cli path2gc <heap> --object 0x... [--limit N] [--depth N] [--dump] [--bytes-display bytes|kilobytes|megabytes|gigabytes|smart] [--format text|json]")); //$NON-NLS-1$
        assertTrue(help.contains("--bytes-display bytes|kilobytes|megabytes|gigabytes|smart: Select byte unit rendering for text output. Defaults to smart.")); //$NON-NLS-1$
        assertTrue(help.contains("--dump: Emit full structured values for file redirection, ignore --limit, and only honor --depth.")); //$NON-NLS-1$
        assertTrue(help.contains("--object 0x... (required): Object address to resolve from the snapshot.")); //$NON-NLS-1$
        assertTrue(help.contains("inspect-object <heap> --object 0x...")); //$NON-NLS-1$
        assertFalse(help.contains("inspect-object <heap> --object 0x... --format json")); //$NON-NLS-1$
        assertTrue(help.contains("Suggested next commands:")); //$NON-NLS-1$
    }

    @Test
    public void commandHelpForDescribeShowsDescribeUsage()
    {
        String help = CliHelp.commandHelp(CliCommand.DESCRIBE);

        assertTrue(help.contains("Command: describe")); //$NON-NLS-1$
        assertTrue(help.contains("Usage: mat-cli describe <command> [--format text|json]")); //$NON-NLS-1$
        assertTrue(help.contains("Positional arguments:")); //$NON-NLS-1$
    }

    @Test
    public void commandHelpForCompletionShowsShellUsage()
    {
        String help = CliHelp.commandHelp(CliCommand.COMPLETION);

        assertTrue(help.contains("Command: completion")); //$NON-NLS-1$
        assertTrue(help.contains("Usage: mat-cli completion <bash|zsh> [--format text|json]")); //$NON-NLS-1$
        assertTrue(help.contains("bash|zsh")); //$NON-NLS-1$
    }

    private void assertHelpContainsCommandSummary(String help, String command, String summary)
    {
        Pattern pattern = Pattern.compile("^\\s*" + Pattern.quote(command) + "\\s+" + Pattern.quote(summary) + "$", //$NON-NLS-1$ //$NON-NLS-2$
                        Pattern.MULTILINE);
        assertTrue(pattern.matcher(help).find());
    }
}
