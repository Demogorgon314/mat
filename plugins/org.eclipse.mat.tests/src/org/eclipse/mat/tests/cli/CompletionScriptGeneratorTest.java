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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.mat.cli.internal.CompletionScriptGenerator;
import org.junit.Test;

public class CompletionScriptGeneratorTest
{
    @Test
    public void generatesBashCompletionWithStaticCommandAndValueMetadata() throws Exception
    {
        String script = new CompletionScriptGenerator().generate("bash"); //$NON-NLS-1$

        assertTrue(script.contains("# bash completion for mat-cli")); //$NON-NLS-1$
        assertTrue(script.contains("--format")); //$NON-NLS-1$
        assertTrue(script.contains("enum:text json markdown")); //$NON-NLS-1$
        assertTrue(script.contains("summary:0")); //$NON-NLS-1$
        assertTrue(script.contains("objects:0")); //$NON-NLS-1$
        assertTrue(script.contains("biggest-objects:0")); //$NON-NLS-1$
        assertTrue(script.contains("describe:0")); //$NON-NLS-1$
        assertTrue(script.contains("completion:0")); //$NON-NLS-1$
        assertTrue(script.contains("objects:--by")); //$NON-NLS-1$
        assertTrue(script.contains("enum:class package class-loader")); //$NON-NLS-1$
        assertTrue(script.contains("file")); //$NON-NLS-1$
        assertTrue(script.contains("--query-file")); //$NON-NLS-1$
        assertTrue(script.contains("--command-file")); //$NON-NLS-1$
        assertTrue(script.contains("free-text")); //$NON-NLS-1$
    }

    @Test
    public void generatesZshCompletionWithStaticCommandAndValueMetadata() throws Exception
    {
        String script = new CompletionScriptGenerator().generate("zsh"); //$NON-NLS-1$

        assertTrue(script.contains("#compdef mat-cli")); //$NON-NLS-1$
        assertTrue(script.contains("compdef _mat-cli mat-cli")); //$NON-NLS-1$
        assertTrue(script.contains("--format")); //$NON-NLS-1$
        assertTrue(script.contains("enum:text json markdown")); //$NON-NLS-1$
        assertTrue(script.contains("objects:0")); //$NON-NLS-1$
        assertTrue(script.contains("biggest-objects:0")); //$NON-NLS-1$
        assertTrue(script.contains("completion:0")); //$NON-NLS-1$
        assertTrue(script.contains("_files")); //$NON-NLS-1$
    }

    @Test
    public void generatedScriptsMatchShippedRootfiles() throws Exception
    {
        CompletionScriptGenerator generator = new CompletionScriptGenerator();
        Path repositoryRoot = locateRepositoryRoot();

        assertEquals(generator.generate("bash"), //$NON-NLS-1$
                        readUtf8(repositoryRoot.resolve(
                                        "features/org.eclipse.mat.cli.feature/rootfiles/completion/bash/mat-cli"))); //$NON-NLS-1$
        assertEquals(generator.generate("zsh"), //$NON-NLS-1$
                        readUtf8(repositoryRoot.resolve(
                                        "features/org.eclipse.mat.cli.feature/rootfiles/completion/zsh/_mat-cli"))); //$NON-NLS-1$
    }

    @Test
    public void featureBuildPropertiesIncludeCompletionRootfiles() throws Exception
    {
        Path repositoryRoot = locateRepositoryRoot();
        String buildProperties = readUtf8(
                        repositoryRoot.resolve("features/org.eclipse.mat.cli.feature/build.properties")); //$NON-NLS-1$

        assertTrue(buildProperties.contains("file:rootfiles/completion/bash/mat-cli")); //$NON-NLS-1$
        assertTrue(buildProperties.contains("file:rootfiles/completion/zsh/_mat-cli")); //$NON-NLS-1$
    }

    private Path locateRepositoryRoot()
    {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath(); //$NON-NLS-1$
        while (current != null)
        {
            if (Files.isDirectory(current.resolve("features/org.eclipse.mat.cli.feature")) //$NON-NLS-1$
                            && Files.isDirectory(current.resolve("plugins/org.eclipse.mat.cli"))) //$NON-NLS-1$
                return current;
            current = current.getParent();
        }

        fail("Unable to locate repository root from " + System.getProperty("user.dir")); //$NON-NLS-1$ //$NON-NLS-2$
        return null;
    }

    private String readUtf8(Path path) throws Exception
    {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
