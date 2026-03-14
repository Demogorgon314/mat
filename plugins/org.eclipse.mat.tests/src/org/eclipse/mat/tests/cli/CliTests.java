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

import junit.framework.JUnit4TestAdapter;

import org.eclipse.mat.cli.internal.CliApplicationErrorContextTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses( { CliArgumentParserTest.class, ResultSerializerTest.class, CliCommandExecutorTest.class,
                CliApplicationErrorContextTest.class })
public class CliTests
{
    public static junit.framework.Test suite()
    {
        return new JUnit4TestAdapter(CliTests.class);
    }
}
