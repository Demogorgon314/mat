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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.mat.cli.internal.CliArgumentParser;
import org.eclipse.mat.cli.internal.CliArguments;
import org.eclipse.mat.cli.internal.CliExitCodes;
import org.eclipse.mat.cli.internal.CliException;
import org.eclipse.mat.cli.internal.CliExecution;
import org.eclipse.mat.cli.internal.DisplayValue;
import org.eclipse.mat.cli.internal.QueryMetadataResult;
import org.eclipse.mat.cli.internal.ThreadsResult;
import org.eclipse.mat.cli.internal.TopConsumersResult;
import org.eclipse.mat.cli.internal.serialization.JsonWriter;
import org.eclipse.mat.cli.internal.serialization.PieResultSerializer;
import org.eclipse.mat.cli.internal.serialization.QueryMetadataSerializer;
import org.eclipse.mat.cli.internal.serialization.ResultSerializer;
import org.eclipse.mat.cli.internal.serialization.SerializationOptions;
import org.eclipse.mat.cli.internal.serialization.SpecResultSerializer;
import org.eclipse.mat.cli.internal.serialization.TableResultSerializer;
import org.eclipse.mat.cli.internal.serialization.TextResultSerializer;
import org.eclipse.mat.cli.internal.serialization.TopConsumersResultSerializer;
import org.eclipse.mat.cli.internal.serialization.TreeResultSerializer;
import org.eclipse.mat.cli.internal.serialization.TreeTextStyleProvider;
import org.eclipse.mat.cli.internal.serialization.TreeTextStyleProvider.TreeTextStyle;
import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.IContextObject;
import org.eclipse.mat.query.IDecorator;
import org.eclipse.mat.query.IResult;
import org.eclipse.mat.query.IResultPie;
import org.eclipse.mat.query.IResultTable;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.query.ResultMetaData;
import org.eclipse.mat.query.results.TextResult;
import org.eclipse.mat.report.QuerySpec;
import org.eclipse.mat.report.SectionSpec;
import org.junit.Test;

public class ResultSerializerTest
{
    @Test
    public void serializesTableResultToJson()
    {
        TableResultSerializer serializer = new TableResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, new SampleTable(), new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"columns\":[{\"label\":\"Name\",\"type\":\"java.lang.String\"},{\"label\":\"Count\",\"type\":\"int\"}]")); //$NON-NLS-1$
        assertTrue(json.contains("\"values\":[\"Alpha\",7]")); //$NON-NLS-1$
        assertTrue(json.contains("\"context\":{\"objectId\":42}")); //$NON-NLS-1$
    }

    @Test
    public void serializesTableResultToAgentJson()
    {
        TableResultSerializer serializer = new TableResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeAgentJson(writer, new SampleTable(), new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"items\":[{\"name\":\"Alpha\",\"count\":7}]")); //$NON-NLS-1$
        assertFalse(json.contains("\"schema\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"_context\":")); //$NON-NLS-1$
        assertFalse(json.contains("displayValues")); //$NON-NLS-1$
    }

    @Test
    public void serializesTableResultToAgentJsonWithContextObjectAddressWhenResolverAvailable()
    {
        TableResultSerializer serializer = new TableResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeAgentJson(writer, new SampleTable(),
                        new SerializationOptions(10, 8, objectId -> "0x" + Integer.toHexString(objectId))); //$NON-NLS-1$
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"_address\":\"0x2a\"")); //$NON-NLS-1$
        assertFalse(json.contains("\"_context\":")); //$NON-NLS-1$
    }

    @Test
    public void serializesStructuredCellErrorsWithoutInliningFakeStrings() throws Exception
    {
        TableResultSerializer serializer = new TableResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, new ErrorTable(), new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"values\":[\"Alpha\",null]")); //$NON-NLS-1$
        assertTrue(json.contains("\"displayValues\":[\"Alpha\",null]")); //$NON-NLS-1$
        assertTrue(json.contains("\"cellErrors\":[{\"columnIndex\":1,\"columnLabel\":\"Count\",\"class\":\"java.lang.IllegalStateException\",\"message\":\"boom\"}]")); //$NON-NLS-1$
        assertFalse(json.contains("<error:")); //$NON-NLS-1$
    }

    @Test
    public void serializesAgentStructuredCellErrorsAsMetadata() throws Exception
    {
        TableResultSerializer serializer = new TableResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeAgentJson(writer, new ErrorTable(), new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"items\":[{\"name\":\"Alpha\",\"count\":null,\"_errors\":{\"count\":{\"class\":\"java.lang.IllegalStateException\",\"message\":\"boom\"}}}]")); //$NON-NLS-1$
        assertFalse(json.contains("\"_context\":")); //$NON-NLS-1$
    }

    @Test
    public void serializesTreeResultToJson()
    {
        TreeResultSerializer serializer = new TreeResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, new SampleTree(), new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"rows\":[{\"path\":\"<root>\",\"valueKind\":\"reference\",\"hasChildren\":true,\"values\":[\"Root\",1]")); //$NON-NLS-1$
        assertTrue(json.contains("\"children\":[{\"path\":\"<root>.Leaf\",\"valueKind\":\"reference\",\"hasChildren\":false,\"values\":[\"Leaf\",2]")); //$NON-NLS-1$
        assertTrue(json.contains("\"context\":{\"objectId\":77}")); //$NON-NLS-1$
    }

    @Test
    public void serializesTreeResultToAgentJson()
    {
        TreeResultSerializer serializer = new TreeResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeAgentJson(writer, new SampleTree(), new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"items\":[{\"path\":\"<root>\",\"valueKind\":\"reference\",\"hasChildren\":true,\"name\":\"Root\",\"depth\":1")); //$NON-NLS-1$
        assertTrue(json.contains("\"_children\":[{\"path\":\"<root>.Leaf\",\"valueKind\":\"reference\",\"hasChildren\":false,\"name\":\"Leaf\",\"depth\":2")); //$NON-NLS-1$
        assertFalse(json.contains("\"schema\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"_childrenTruncated\":false")); //$NON-NLS-1$
    }

    @Test
    public void serializesAddressColumnsAsHexStringsInAgentJson()
    {
        TableResultSerializer serializer = new TableResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeAgentJson(writer, new AddressTable(), new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"threadaddress\":\"0x2a\"")); //$NON-NLS-1$
        assertFalse(json.contains("\"schema\":")); //$NON-NLS-1$
    }

    @Test
    public void serializesAddressColumnsAsHexStringsInText()
    {
        TableResultSerializer serializer = new TableResultSerializer();

        String text = serializer.toText(new AddressTable(), new SerializationOptions(10, 8));

        assertTrue(text.contains("threadAddress")); //$NON-NLS-1$
        assertTrue(text.contains("0x2a")); //$NON-NLS-1$
        assertFalse(text.contains(" | 42")); //$NON-NLS-1$
    }

    @Test
    public void normalizesApproximateBytesInAgentJson()
    {
        TableResultSerializer serializer = new TableResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeAgentJson(writer, new ApproximateBytesTable(),
                        new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"retained_heap\":7")); //$NON-NLS-1$
        assertTrue(json.contains("\"_meta\":{\"retained_heap\":{\"kind\":\"approximate_lower_bound\"}}")); //$NON-NLS-1$
        assertFalse(json.contains("\"retained_heap\":-7")); //$NON-NLS-1$
    }

    @Test
    public void serializesDisplayValueMetadataInAgentTreeJson()
    {
        TreeResultSerializer serializer = new TreeResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeAgentJson(writer, new PreviewTree(), new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"path\":\"<root>\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"valueKind\":\"preview\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"hasChildren\":false")); //$NON-NLS-1$
        assertTrue(json.contains("\"value\":\"61 62 63 ...\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"_meta\":{\"value\":{\"kind\":\"hex_preview\",\"length\":64,\"truncated\":true,\"encoding\":\"hex\"}}")); //$NON-NLS-1$
    }

    @Test
    public void rendersTreeTextAsCompactTree()
    {
        TreeResultSerializer serializer = new TreeResultSerializer();

        String text = serializer.toText(new SampleTree(), new SerializationOptions(10, 8));

        assertFalse(text.startsWith("Name | Depth\n")); //$NON-NLS-1$
        assertTrue(text.contains("- Root [Depth=1]")); //$NON-NLS-1$
        assertTrue(text.contains("  - Leaf [Depth=2]")); //$NON-NLS-1$
    }

    @Test
    public void rendersDecoratorSpacingInTableTextAndJsonDisplayValues()
    {
        TableResultSerializer serializer = new TableResultSerializer();

        String text = serializer.toText(new DecoratedTable(), new SerializationOptions(10, 8));

        assertTrue(text.contains("pre Value post")); //$NON-NLS-1$
        assertFalse(text.contains("preValuepost")); //$NON-NLS-1$

        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, new DecoratedTable(), new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"values\":[\"Value\",7]")); //$NON-NLS-1$
        assertTrue(json.contains("\"displayValues\":[\"pre Value post\",\"7\"]")); //$NON-NLS-1$
        assertFalse(json.contains("preValuepost")); //$NON-NLS-1$
    }

    @Test
    public void rendersDecoratorSpacingInTreeTextAndPreservesPathValues()
    {
        TreeResultSerializer serializer = new TreeResultSerializer();

        String text = serializer.toText(new DecoratedTree(), new SerializationOptions(10, 8));

        assertTrue(text.contains("- Root [Depth=1]")); //$NON-NLS-1$
        assertTrue(text.contains("  - pre Leaf post [Depth=2]")); //$NON-NLS-1$
        assertFalse(text.contains("preLeafpost [Depth=2]")); //$NON-NLS-1$

        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, new DecoratedTree(), new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"path\":\"<root>.preLeafpost\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"values\":[\"Leaf\",2]")); //$NON-NLS-1$
        assertTrue(json.contains("\"displayValues\":[\"pre Leaf post\",\"2\"]")); //$NON-NLS-1$
    }

    @Test
    public void hidesNestedInspectorNullsByDefaultAndShowsThemWhenEnabled() throws Exception
    {
        ResultSerializer serializer = new ResultSerializer();
        CliArgumentParser parser = new CliArgumentParser();
        ByteArrayOutputStream hiddenOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream shownOutput = new ByteArrayOutputStream();

        CliArguments hiddenArguments = parser.parse(
                        new String[] { "inspect-object", "sample.hprof", "--object", "0x2a", "--format", "text" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        CliArguments shownArguments = parser.parse(new String[] { "inspect-object", "sample.hprof", "--object", "0x2a",
                        "--format", "text", "--show-nulls" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

        try (PrintStream stream = new PrintStream(hiddenOutput, true, StandardCharsets.UTF_8.name()))
        {
            serializer.serialize(hiddenArguments, CliExecution.result(new InspectorNullTree()), stream);
        }
        try (PrintStream stream = new PrintStream(shownOutput, true, StandardCharsets.UTF_8.name()))
        {
            serializer.serialize(shownArguments, CliExecution.result(new InspectorNullTree()), stream);
        }

        String hiddenText = hiddenOutput.toString(StandardCharsets.UTF_8.name());
        String shownText = shownOutput.toString(StandardCharsets.UTF_8.name());

        assertTrue(hiddenText.contains("object <object>: sample.Type")); //$NON-NLS-1$
        assertTrue(hiddenText.contains(".size = 3 : int")); //$NON-NLS-1$
        assertFalse(hiddenText.contains(".next = null")); //$NON-NLS-1$

        assertTrue(shownText.contains(".size = 3 : int")); //$NON-NLS-1$
        assertTrue(shownText.contains(".next = null")); //$NON-NLS-1$
    }

    @Test
    public void keepsRootNullInspectorRowVisibleWhenNullsAreHidden() throws Exception
    {
        ResultSerializer serializer = new ResultSerializer();
        CliArguments arguments = new CliArgumentParser().parse(
                        new String[] { "inspect-object", "sample.hprof", "--object", "0x2a", "--format", "text" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8.name()))
        {
            serializer.serialize(arguments, CliExecution.result(new InspectorRootNullTree()), stream);
        }

        String text = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(text.contains(".next = null")); //$NON-NLS-1$
    }

    @Test
    public void keepsHasChildrenWhenAgentTreeDepthTruncatesChildren()
    {
        TreeResultSerializer serializer = new TreeResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeAgentJson(writer, new SampleTree(), new SerializationOptions(10, 1));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertTrue(truncated);
        assertTrue(json.contains("\"path\":\"<root>\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"hasChildren\":true")); //$NON-NLS-1$
        assertFalse(json.contains("\"_children\":")); //$NON-NLS-1$
    }

    @Test
    public void serializesNullTreeValuesAsNullValueKind()
    {
        TreeResultSerializer serializer = new TreeResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeAgentJson(writer, new NullValueTree(), new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"path\":\"<root>\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"valueKind\":\"null\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"value\":null")); //$NON-NLS-1$
    }

    @Test
    public void truncatesTreeJsonByTotalNodeBudget()
    {
        TreeResultSerializer serializer = new TreeResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, new BudgetTree(), new SerializationOptions(10, 8, 2));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertTrue(truncated);
        assertTrue(json.contains("\"values\":[\"Root\",0]")); //$NON-NLS-1$
        assertTrue(json.contains("\"values\":[\"Child A\",1]")); //$NON-NLS-1$
        assertFalse(json.contains("\"values\":[\"Child B\",1]")); //$NON-NLS-1$
    }

    @Test
    public void serializesCompositeResultToJson() throws Exception
    {
        SpecResultSerializer serializer = new SpecResultSerializer(new TableResultSerializer(), new TreeResultSerializer(),
                        new TextResultSerializer(), new PieResultSerializer());
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, sampleSection(), new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"sections\":[{\"kind\":\"query\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"Sample Table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultType\":\"table\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"Sample Tree\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultType\":\"tree\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"Overview\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultType\":\"text\"")); //$NON-NLS-1$
    }

    @Test
    public void truncatesSectionChildrenByLimit() throws Exception
    {
        SpecResultSerializer serializer = new SpecResultSerializer(new TableResultSerializer(), new TreeResultSerializer(),
                        new TextResultSerializer(), new PieResultSerializer());
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, sampleSection(), new SerializationOptions(2, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertTrue(truncated);
        assertTrue(json.contains("\"name\":\"Overview\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"Sample Table\"")); //$NON-NLS-1$
        assertFalse(json.contains("\"name\":\"Sample Tree\"")); //$NON-NLS-1$
    }

    @Test
    public void truncatesNestedSectionsByDepth() throws Exception
    {
        SpecResultSerializer serializer = new SpecResultSerializer(new TableResultSerializer(), new TreeResultSerializer(),
                        new TextResultSerializer(), new PieResultSerializer());
        SectionSpec outer = new SectionSpec("Outer"); //$NON-NLS-1$
        SectionSpec inner = new SectionSpec("Inner"); //$NON-NLS-1$
        inner.add(new QuerySpec("Leaf", new TextResult("value", false))); //$NON-NLS-1$ //$NON-NLS-2$
        outer.add(new QuerySpec("Nested", inner)); //$NON-NLS-1$

        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, outer, new SerializationOptions(10, 1));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertTrue(truncated);
        assertTrue(json.contains("\"name\":\"Nested\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"sections\":[]")); //$NON-NLS-1$
    }

    @Test
    public void truncatesDirectSectionChildrenByDepth() throws Exception
    {
        SpecResultSerializer serializer = new SpecResultSerializer(new TableResultSerializer(), new TreeResultSerializer(),
                        new TextResultSerializer(), new PieResultSerializer());
        SectionSpec outer = new SectionSpec("Outer"); //$NON-NLS-1$
        SectionSpec inner = new SectionSpec("Inner"); //$NON-NLS-1$
        inner.add(new QuerySpec("Leaf", new TextResult("value", false))); //$NON-NLS-1$ //$NON-NLS-2$
        outer.add(inner);

        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, outer, new SerializationOptions(10, 1));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertTrue(truncated);
        assertTrue(json.contains("\"name\":\"Inner\"")); //$NON-NLS-1$
        assertFalse(json.contains("\"name\":\"Leaf\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"sections\":[]")); //$NON-NLS-1$
    }

    @Test
    public void serializesPieResultInsideSection() throws Exception
    {
        SpecResultSerializer serializer = new SpecResultSerializer(new TableResultSerializer(), new TreeResultSerializer(),
                        new TextResultSerializer(), new PieResultSerializer());
        SectionSpec section = new SectionSpec("LeakHunter"); //$NON-NLS-1$
        section.add(new QuerySpec("Overview", new SamplePie())); //$NON-NLS-1$
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, section, new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"resultType\":\"pie\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"label\":\"Suspect 1\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"color\":\"#ff0000\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"context\":{\"objectId\":101}")); //$NON-NLS-1$
    }

    @Test
    public void marksTreeCyclesInAgentJson()
    {
        TreeResultSerializer serializer = new TreeResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeAgentJson(writer, new CyclicTree(), new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"_cycle\":true")); //$NON-NLS-1$
        assertTrue(json.contains("\"name\":\"Repeat\"")); //$NON-NLS-1$
    }

    @Test
    public void marksTreeCyclesInJson()
    {
        TreeResultSerializer serializer = new TreeResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, new CyclicTree(), new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"_cycle\":true")); //$NON-NLS-1$
    }

    @Test
    public void serializesTopConsumersSectionToCompactJson() throws Exception
    {
        TopConsumersResultSerializer serializer = new TopConsumersResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, sampleTopConsumersResult(), new SerializationOptions(2, 8, 20));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertTrue(truncated);
        assertTrue(json.contains("\"totalRetainedHeap\":1000")); //$NON-NLS-1$
        assertTrue(json.contains("\"biggestObjects\":{")); //$NON-NLS-1$
        assertTrue(json.contains("\"biggestObjects\":{\"name\":\"Biggest Objects\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"label\":\"Largest\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"retainedBytes\":500")); //$NON-NLS-1$
        assertTrue(json.contains("\"classes\":{\"name\":\"Biggest Top-Level Dominator Classes\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"classLoaders\":{\"name\":\"Biggest Top-Level Dominator Class Loaders\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"packages\":{\"name\":\"Biggest Top-Level Dominator Packages\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"root\":{\"name\":\"<all>\"")); //$NON-NLS-1$
    }

    @Test
    public void serializesTopConsumersSectionToAgentJson() throws Exception
    {
        TopConsumersResultSerializer serializer = new TopConsumersResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeAgentJson(writer, sampleTopConsumersResult(), new SerializationOptions(2, 8, 20));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertTrue(truncated);
        assertTrue(json.contains("\"biggestObjects\":[{\"label\":\"Largest\",\"retainedBytes\":500,\"retainedPercent\":0.5}")); //$NON-NLS-1$
        assertTrue(json.contains("\"classes\":[{\"label\":\"Alpha\",\"count\":4,\"retainedBytes\":600,\"retainedPercent\":0.6}")); //$NON-NLS-1$
        assertTrue(json.contains("\"packages\":{\"name\":\"<all>\",\"retainedBytes\":1000,\"retainedPercent\":1.0,\"topDominators\":4")); //$NON-NLS-1$
        assertFalse(json.contains("retainedBytesText")); //$NON-NLS-1$
        assertFalse(json.contains("retainedPercentText")); //$NON-NLS-1$
        assertFalse(json.contains("\"objectId\":")); //$NON-NLS-1$
        assertFalse(json.contains("\"_context\":")); //$NON-NLS-1$
    }

    @Test
    public void truncatesTopConsumersPackagesByDepth() throws Exception
    {
        TopConsumersResultSerializer serializer = new TopConsumersResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeAgentJson(writer, sampleTopConsumersResult(), new SerializationOptions(2, 1, 20));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertTrue(truncated);
        assertTrue(json.contains("\"packages\":{\"name\":\"<all>\"")); //$NON-NLS-1$
        assertFalse(json.contains("\"_children\":[]")); //$NON-NLS-1$
        assertTrue(json.contains("\"_childrenTruncated\":true")); //$NON-NLS-1$
    }

    @Test
    public void serializesTopConsumersJsonWithoutNullPackageNodes() throws Exception
    {
        TopConsumersResultSerializer serializer = new TopConsumersResultSerializer();
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, sampleTopConsumersResult(), new SerializationOptions(2, 8, 2));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertTrue(truncated);
        assertFalse(json.contains("\"children\":[null")); //$NON-NLS-1$
        assertFalse(json.contains(",null")); //$NON-NLS-1$
    }

    @Test
    public void serializesTopConsumersToText() throws Exception
    {
        TopConsumersResultSerializer serializer = new TopConsumersResultSerializer();

        String text = serializer.toText(sampleTopConsumersResult(), new SerializationOptions(2, 8, 20));

        assertTrue(text.contains("Biggest Objects:")); //$NON-NLS-1$
        assertTrue(text.contains("50.00%  500  Largest")); //$NON-NLS-1$
        assertFalse(text.contains("Third")); //$NON-NLS-1$
        assertTrue(text.contains("Biggest Top-Level Dominator Classes:")); //$NON-NLS-1$
        assertTrue(text.contains("60.00%  600  4  Alpha")); //$NON-NLS-1$
        assertFalse(text.contains("Gamma")); //$NON-NLS-1$
        assertTrue(text.contains("Biggest Top-Level Dominator Packages:")); //$NON-NLS-1$
        assertTrue(text.contains("<all>  (100.00%)  1,000  4")); //$NON-NLS-1$
        assertFalse(text.contains("more rows")); //$NON-NLS-1$
        assertFalse(text.contains("more nodes")); //$NON-NLS-1$
    }

    @Test
    public void marksUnsupportedNestedCompositeResult() throws Exception
    {
        SpecResultSerializer serializer = new SpecResultSerializer(new TableResultSerializer(), new TreeResultSerializer(),
                        new TextResultSerializer(), new PieResultSerializer());
        QuerySpec query = new QuerySpec("Unsupported", new UnsupportedResult()); //$NON-NLS-1$
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        boolean truncated = serializer.writeJson(writer, query, new SerializationOptions(10, 8));
        writer.name("truncated").value(truncated); //$NON-NLS-1$
        writer.endObject();

        String json = writer.toString();
        assertFalse(truncated);
        assertTrue(json.contains("\"resultType\":\"unsupported\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"unsupportedType\":\"org.eclipse.mat.tests.cli.ResultSerializerTest$UnsupportedResult\"")); //$NON-NLS-1$
    }

    @Test
    public void serializesAgentErrorEnvelope() throws Exception
    {
        ResultSerializer serializer = new ResultSerializer();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CliArguments arguments = new CliArgumentParser()
                        .parse(new String[] { "oql", "sample.hprof", "--query", "SELECT * FROM java.lang.String", "--format",
                                        "json", "--verbose" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8.name()))
        {
            serializer.serializeError(arguments, CliArguments.OutputFormat.JSON, 3, new IllegalStateException( //$NON-NLS-1$
                            "wrapper", new NullPointerException("reader is closed")), stream); //$NON-NLS-1$ //$NON-NLS-2$
        }

        String json = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(json.contains("\"schemaVersion\":\"mat-cli/v2\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"resultKind\":\"error\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"exceptionClass\":\"java.lang.IllegalStateException\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"rootCauseClass\":\"java.lang.NullPointerException\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"rootCauseMessage\":\"reader is closed\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"kind\":\"snapshot_lifecycle\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"retryable\":false")); //$NON-NLS-1$
    }

    @Test
    public void serializesThreadsJsonEnvelope() throws Exception
    {
        ResultSerializer serializer = new ResultSerializer();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CliArguments arguments = new CliArgumentParser()
                        .parse(new String[] { "threads", "sample.hprof", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8.name()))
        {
            serializer.serialize(arguments, CliExecution.result(sampleThreadsResult()), stream);
        }

        String json = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(json.contains("\"resultKind\":\"threads\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"notice\":\"best-effort from heap dump, not a full jstack equivalent\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"summary\":{\"totalThreads\":2")); //$NON-NLS-1$
        assertTrue(json.contains("\"threads\":[{")); //$NON-NLS-1$
        assertTrue(json.contains("\"stackAvailable\":false")); //$NON-NLS-1$
        assertTrue(json.contains("\"objectAddress\":\"0x2a\"")); //$NON-NLS-1$
        assertFalse(json.contains("\"_context\":")); //$NON-NLS-1$
    }

    @Test
    public void classifiesProblemReportedOqlErrors() throws Exception
    {
        ResultSerializer serializer = new ResultSerializer();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CliArguments arguments = new CliArgumentParser()
                        .parse(new String[] { "oql", "sample.hprof", "--query", "SELECT * FROM java.lang.String", "--format",
                                        "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8.name()))
        {
            serializer.serializeError(arguments, CliArguments.OutputFormat.JSON, 3,
                            new IllegalStateException("Problem reported: boom"), stream); //$NON-NLS-1$
        }

        String json = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(json.contains("\"kind\":\"query_error\"")); //$NON-NLS-1$
    }

    @Test
    public void usageErrorsPreferCommandSpecificHelpHint() throws Exception
    {
        ResultSerializer serializer = new ResultSerializer();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CliArguments arguments = new CliArgumentParser().partialParse(
                        new String[] { "path2gc", "sample.hprof", "--format", "json" }, CliArguments.OutputFormat.JSON); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8.name()))
        {
            serializer.serializeError(arguments, CliArguments.OutputFormat.JSON, CliExitCodes.USAGE,
                            CliException.usage("path2gc requires --object 0x..."), stream); //$NON-NLS-1$
        }

        String json = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(json.contains("mat-cli path2gc --help")); //$NON-NLS-1$
        assertTrue(json.contains("mat-cli describe path2gc --format json")); //$NON-NLS-1$
    }

    @Test
    public void classifiesQueryNotFoundErrors() throws Exception
    {
        ResultSerializer serializer = new ResultSerializer();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CliArguments arguments = new CliArgumentParser()
                        .parse(new String[] { "query", "sample.hprof", "--command", "leak_suspects", "--format",
                                        "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8.name()))
        {
            serializer.serializeError(arguments, CliArguments.OutputFormat.JSON, 3,
                            new IllegalStateException("Command leak_suspects not found."), stream); //$NON-NLS-1$
        }

        String json = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(json.contains("\"kind\":\"query_not_found\"")); //$NON-NLS-1$
        assertTrue(json.contains("mat-cli list-queries --format json")); //$NON-NLS-1$
    }

    @Test
    public void omitsMissingHeapPathFromErrorSuggestions() throws Exception
    {
        ResultSerializer serializer = new ResultSerializer();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CliArguments arguments = new CliArgumentParser()
                        .parse(new String[] { "summary", "/does/not/exist.hprof", "--format", "json" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8.name()))
        {
            serializer.serializeError(arguments, CliArguments.OutputFormat.JSON, 3,
                            new IllegalStateException("Heap dump not found: /does/not/exist.hprof"), stream); //$NON-NLS-1$
        }

        String json = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(json.contains("\"kind\":\"missing_file\"")); //$NON-NLS-1$
        assertTrue(json.contains("mat-cli --help")); //$NON-NLS-1$
        assertFalse(json.contains("mat-cli histogram \"/does/not/exist.hprof\"")); //$NON-NLS-1$
    }

    @Test
    public void serializesQueryMetadataList() throws Exception
    {
        QueryMetadataSerializer serializer = new QueryMetadataSerializer();
        QueryMetadataResult result = new QueryMetadataResult(QueryMetadataResult.Kind.LIST, null,
                        Collections.singletonList(new QueryMetadataResult.QueryDefinition("hash_entries", //$NON-NLS-1$
                                        "Hash Entries", "Collections", "hash_entries <subject>", "Inspect map entries", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                                        "Reads the key/value entries from a hash collection.", null, "example.Query", //$NON-NLS-1$ //$NON-NLS-2$
                                        false, Collections.singletonList("java.util.AbstractMap"), //$NON-NLS-1$
                                        Collections.singletonList(new QueryMetadataResult.QueryArgument("subject", null, //$NON-NLS-1$
                                                        "int", "HEAP_OBJECT", true, false, false, false, null, //$NON-NLS-1$ //$NON-NLS-2$
                                                        "Heap object or class name"))))); //$NON-NLS-1$
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        writer.name("resultKind").value(serializer.resultKind(result)); //$NON-NLS-1$
        serializer.writeJson(writer, result);
        writer.endObject();

        String json = writer.toString();
        assertTrue(json.contains("\"resultKind\":\"query-list\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"queries\":[{\"identifier\":\"hash_entries\"")); //$NON-NLS-1$
        assertTrue(json.contains("\"arguments\":[{\"name\":\"subject\"")); //$NON-NLS-1$
    }

    private SectionSpec sampleSection()
    {
        SectionSpec section = new SectionSpec("Top Consumers"); //$NON-NLS-1$
        section.add(new QuerySpec("Overview", new TextResult("Summary", true))); //$NON-NLS-1$ //$NON-NLS-2$
        section.add(new QuerySpec("Sample Table", new SampleTable())); //$NON-NLS-1$
        section.add(new QuerySpec("Sample Tree", new SampleTree())); //$NON-NLS-1$
        return section;
    }

    private TopConsumersResult sampleTopConsumersResult()
    {
        List<TopConsumersResult.ObjectRow> biggestObjects = Arrays.asList(
                        new TopConsumersResult.ObjectRow(91, "Largest", 500, 0.5d), //$NON-NLS-1$
                        new TopConsumersResult.ObjectRow(92, "Second", 300, 0.3d), //$NON-NLS-1$
                        new TopConsumersResult.ObjectRow(93, "Third", 150, 0.15d)); //$NON-NLS-1$
        List<TopConsumersResult.DominatorRow> classes = Arrays.asList(
                        new TopConsumersResult.DominatorRow(41, "Alpha", 4, 600, 0.6d), //$NON-NLS-1$
                        new TopConsumersResult.DominatorRow(42, "Beta", 2, 250, 0.25d), //$NON-NLS-1$
                        new TopConsumersResult.DominatorRow(43, "Gamma", 1, 120, 0.12d)); //$NON-NLS-1$
        List<TopConsumersResult.DominatorRow> classLoaders = Arrays.asList(
                        new TopConsumersResult.DominatorRow(2, "Loader A", 6, 700, 0.7d), //$NON-NLS-1$
                        new TopConsumersResult.DominatorRow(0, "<system class loader>", 3, 200, 0.2d)); //$NON-NLS-1$
        TopConsumersResult.PackageNode root = new TopConsumersResult.PackageNode("<all>", 1000, 1.0d, 4, Arrays.asList( //$NON-NLS-1$
                        new TopConsumersResult.PackageNode("org", 700, 0.7d, 3, Arrays.asList( //$NON-NLS-1$
                                        new TopConsumersResult.PackageNode("example", 650, 0.65d, 2, Collections.<TopConsumersResult.PackageNode>emptyList()))), //$NON-NLS-1$
                        new TopConsumersResult.PackageNode("com", 200, 0.2d, 1, Collections.<TopConsumersResult.PackageNode>emptyList()))); //$NON-NLS-1$
        return new TopConsumersResult(1000, biggestObjects, classes, classLoaders, root);
    }

    private ThreadsResult sampleThreadsResult()
    {
        List<ThreadsResult.ThreadEntry> threads = Arrays.asList(
                        new ThreadsResult.ThreadEntry(42, "main", "java.lang.Thread @ 0x2a", "0x2a", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        "[alive, runnable]", 2048L, true, null, //$NON-NLS-1$
                                        Arrays.asList("at example.Main.run(Main.java:10)")), //$NON-NLS-1$
                        new ThreadsResult.ThreadEntry(43, "worker", "java.lang.Thread @ 0x2b", "0x2b", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        ThreadsResult.UNAVAILABLE, 1024L, false,
                                        ThreadsResult.STACK_UNAVAILABLE_REASON, Collections.<String>emptyList()));
        return new ThreadsResult(ThreadsResult.NOTICE, new ThreadsResult.Summary(2, 2, 1, 1), threads, false);
    }

    private static final class SampleTable implements IResultTable
    {
        private final List<Row> rows = Collections.singletonList(new Row("Alpha", Integer.valueOf(7), 42)); //$NON-NLS-1$
        private final Column[] columns = new Column[] { new Column("Name", String.class), new Column("Count", int.class) }; //$NON-NLS-1$ //$NON-NLS-2$

        public ResultMetaData getResultMetaData()
        {
            return null;
        }

        public Column[] getColumns()
        {
            return columns;
        }

        public Object getColumnValue(Object row, int columnIndex)
        {
            Row value = (Row) row;
            return columnIndex == 0 ? value.name : value.count;
        }

        public IContextObject getContext(Object row)
        {
            final Row value = (Row) row;
            return new IContextObject()
            {
                public int getObjectId()
                {
                    return value.objectId;
                }
            };
        }

        public int getRowCount()
        {
            return rows.size();
        }

        public Object getRow(int rowId)
        {
            return rows.get(rowId);
        }
    }

    private static final class SampleTree implements IResultTree
    {
        private final Node root = new Node("Root", Integer.valueOf(1), 77, Collections.singletonList(new Node("Leaf", Integer.valueOf(2), 78, Collections.emptyList()))); //$NON-NLS-1$ //$NON-NLS-2$
        private final Column[] columns = new Column[] { new Column("Name", String.class), new Column("Depth", int.class) }; //$NON-NLS-1$ //$NON-NLS-2$

        public ResultMetaData getResultMetaData()
        {
            return null;
        }

        public Column[] getColumns()
        {
            return columns;
        }

        public Object getColumnValue(Object row, int columnIndex)
        {
            Node value = (Node) row;
            return columnIndex == 0 ? value.name : value.depth;
        }

        public IContextObject getContext(Object row)
        {
            final Node value = (Node) row;
            return new IContextObject()
            {
                public int getObjectId()
                {
                    return value.objectId;
                }
            };
        }

        public List<?> getElements()
        {
            return Arrays.asList(root);
        }

        public boolean hasChildren(Object element)
        {
            return !((Node) element).children.isEmpty();
        }

        public List<?> getChildren(Object parent)
        {
            return ((Node) parent).children;
        }
    }

    private static final class DecoratedTable implements IResultTable
    {
        private final List<DecoratedRow> rows = Collections.singletonList(new DecoratedRow("Value", Integer.valueOf(7), //$NON-NLS-1$
                        "pre", "post")); //$NON-NLS-1$ //$NON-NLS-2$
        private final Column[] columns = new Column[] { new Column("Name", String.class).decorator(new RowDecorator()), //$NON-NLS-1$
                        new Column("Count", int.class) }; //$NON-NLS-1$

        public ResultMetaData getResultMetaData()
        {
            return null;
        }

        public Column[] getColumns()
        {
            return columns;
        }

        public Object getColumnValue(Object row, int columnIndex)
        {
            DecoratedRow value = (DecoratedRow) row;
            return columnIndex == 0 ? value.name : value.count;
        }

        public IContextObject getContext(Object row)
        {
            return null;
        }

        public int getRowCount()
        {
            return rows.size();
        }

        public Object getRow(int rowId)
        {
            return rows.get(rowId);
        }
    }

    private static final class DecoratedTree implements IResultTree
    {
        private final DecoratedNode root = new DecoratedNode("Root", Integer.valueOf(1), 77, null, null, //$NON-NLS-1$
                        Collections.singletonList(new DecoratedNode("Leaf", Integer.valueOf(2), 78, "pre", "post", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        Collections.<DecoratedNode>emptyList())));
        private final Column[] columns = new Column[] { new Column("Name", String.class).decorator(new RowDecorator()), //$NON-NLS-1$
                        new Column("Depth", int.class) }; //$NON-NLS-1$

        public ResultMetaData getResultMetaData()
        {
            return null;
        }

        public Column[] getColumns()
        {
            return columns;
        }

        public Object getColumnValue(Object row, int columnIndex)
        {
            DecoratedNode value = (DecoratedNode) row;
            return columnIndex == 0 ? value.name : value.depth;
        }

        public IContextObject getContext(Object row)
        {
            final DecoratedNode value = (DecoratedNode) row;
            return new IContextObject()
            {
                public int getObjectId()
                {
                    return value.objectId;
                }
            };
        }

        public List<?> getElements()
        {
            return Collections.singletonList(root);
        }

        public boolean hasChildren(Object element)
        {
            return !((DecoratedNode) element).children.isEmpty();
        }

        public List<?> getChildren(Object parent)
        {
            return ((DecoratedNode) parent).children;
        }
    }

    private static final class PreviewTree implements IResultTree
    {
        private final PreviewNode root = new PreviewNode("Payload", //$NON-NLS-1$
                        new DisplayValue("61 62 63 ...", new DisplayValue.Metadata("hex_preview", Integer.valueOf(64), //$NON-NLS-1$ //$NON-NLS-2$
                                        Boolean.TRUE, "hex")), 91); //$NON-NLS-1$
        private final Column[] columns = new Column[] { new Column("Name", String.class), new Column("Value", String.class) }; //$NON-NLS-1$ //$NON-NLS-2$

        public ResultMetaData getResultMetaData()
        {
            return null;
        }

        public Column[] getColumns()
        {
            return columns;
        }

        public Object getColumnValue(Object row, int columnIndex)
        {
            PreviewNode value = (PreviewNode) row;
            return columnIndex == 0 ? value.name : value.value;
        }

        public IContextObject getContext(Object row)
        {
            final PreviewNode value = (PreviewNode) row;
            return new IContextObject()
            {
                public int getObjectId()
                {
                    return value.objectId;
                }
            };
        }

        public List<?> getElements()
        {
            return Collections.singletonList(root);
        }

        public boolean hasChildren(Object element)
        {
            return false;
        }

        public List<?> getChildren(Object parent)
        {
            return Collections.emptyList();
        }
    }

    private static final class NullValueTree implements IResultTree
    {
        private final Column[] columns = new Column[] { new Column("Name", String.class), new Column("Value", String.class) }; //$NON-NLS-1$ //$NON-NLS-2$

        public ResultMetaData getResultMetaData()
        {
            return null;
        }

        public Column[] getColumns()
        {
            return columns;
        }

        public Object getColumnValue(Object row, int columnIndex)
        {
            return columnIndex == 0 ? "Slot" : null; //$NON-NLS-1$
        }

        public IContextObject getContext(Object row)
        {
            return null;
        }

        public List<?> getElements()
        {
            return Collections.singletonList("row"); //$NON-NLS-1$
        }

        public boolean hasChildren(Object element)
        {
            return false;
        }

        public List<?> getChildren(Object parent)
        {
            return Collections.emptyList();
        }
    }

    private static final class InspectorNullTree implements IResultTree, TreeTextStyleProvider
    {
        private final InspectorNode root = new InspectorNode("object", "<object>", "sample.Type", "sample.Type", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                        Integer.valueOf(1), Arrays.asList(
                                        new InspectorNode("field", "next", "java.lang.Object", null, null, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                                        Collections.<InspectorNode>emptyList()),
                                        new InspectorNode("field", "size", "int", Integer.valueOf(3), null, //$NON-NLS-1$ //$NON-NLS-2$
                                                        Collections.<InspectorNode>emptyList())));
        private final Column[] columns = new Column[] { new Column("Kind", String.class), new Column("Name", String.class), //$NON-NLS-1$ //$NON-NLS-2$
                        new Column("Type", String.class), new Column("Value", String.class) }; //$NON-NLS-1$ //$NON-NLS-2$

        public ResultMetaData getResultMetaData()
        {
            return null;
        }

        public Column[] getColumns()
        {
            return columns;
        }

        public Object getColumnValue(Object row, int columnIndex)
        {
            InspectorNode value = (InspectorNode) row;
            switch (columnIndex)
            {
                case 0:
                    return value.kind;
                case 1:
                    return value.name;
                case 2:
                    return value.type;
                case 3:
                    return value.value;
                default:
                    return null;
            }
        }

        public IContextObject getContext(Object row)
        {
            final InspectorNode value = (InspectorNode) row;
            if (value.objectId == null)
                return null;
            return new IContextObject()
            {
                public int getObjectId()
                {
                    return value.objectId.intValue();
                }
            };
        }

        public List<?> getElements()
        {
            return Collections.singletonList(root);
        }

        public boolean hasChildren(Object element)
        {
            return !((InspectorNode) element).children.isEmpty();
        }

        public List<?> getChildren(Object parent)
        {
            return ((InspectorNode) parent).children;
        }

        public TreeTextStyle getTreeTextStyle()
        {
            return TreeTextStyle.INSPECTOR;
        }
    }

    private static final class InspectorRootNullTree implements IResultTree, TreeTextStyleProvider
    {
        private final InspectorNode root = new InspectorNode("field", "next", "java.lang.Object", null, null, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        Collections.<InspectorNode>emptyList());
        private final Column[] columns = new Column[] { new Column("Kind", String.class), new Column("Name", String.class), //$NON-NLS-1$ //$NON-NLS-2$
                        new Column("Type", String.class), new Column("Value", String.class) }; //$NON-NLS-1$ //$NON-NLS-2$

        public ResultMetaData getResultMetaData()
        {
            return null;
        }

        public Column[] getColumns()
        {
            return columns;
        }

        public Object getColumnValue(Object row, int columnIndex)
        {
            InspectorNode value = (InspectorNode) row;
            switch (columnIndex)
            {
                case 0:
                    return value.kind;
                case 1:
                    return value.name;
                case 2:
                    return value.type;
                case 3:
                    return value.value;
                default:
                    return null;
            }
        }

        public IContextObject getContext(Object row)
        {
            return null;
        }

        public List<?> getElements()
        {
            return Collections.singletonList(root);
        }

        public boolean hasChildren(Object element)
        {
            return false;
        }

        public List<?> getChildren(Object parent)
        {
            return Collections.emptyList();
        }

        public TreeTextStyle getTreeTextStyle()
        {
            return TreeTextStyle.INSPECTOR;
        }
    }

    private static final class AddressTable implements IResultTable
    {
        private final Column[] columns = new Column[] { new Column("threadAddress", Long.class) }; //$NON-NLS-1$

        public ResultMetaData getResultMetaData()
        {
            return null;
        }

        public Column[] getColumns()
        {
            return columns;
        }

        public Object getColumnValue(Object row, int columnIndex)
        {
            return Long.valueOf(42L);
        }

        public IContextObject getContext(Object row)
        {
            return null;
        }

        public int getRowCount()
        {
            return 1;
        }

        public Object getRow(int rowId)
        {
            return Integer.valueOf(rowId);
        }
    }

    private static final class PreviewNode
    {
        private final String name;
        private final DisplayValue value;
        private final int objectId;

        private PreviewNode(String name, DisplayValue value, int objectId)
        {
            this.name = name;
            this.value = value;
            this.objectId = objectId;
        }
    }

    private static final class DecoratedRow
    {
        private final String name;
        private final Integer count;
        private final String prefix;
        private final String suffix;

        private DecoratedRow(String name, Integer count, String prefix, String suffix)
        {
            this.name = name;
            this.count = count;
            this.prefix = prefix;
            this.suffix = suffix;
        }
    }

    private static final class DecoratedNode
    {
        private final String name;
        private final Integer depth;
        private final int objectId;
        private final String prefix;
        private final String suffix;
        private final List<DecoratedNode> children;

        private DecoratedNode(String name, Integer depth, int objectId, String prefix, String suffix,
                        List<DecoratedNode> children)
        {
            this.name = name;
            this.depth = depth;
            this.objectId = objectId;
            this.prefix = prefix;
            this.suffix = suffix;
            this.children = children;
        }
    }

    private static final class RowDecorator implements IDecorator
    {
        public String prefix(Object row)
        {
            if (row instanceof DecoratedRow)
                return ((DecoratedRow) row).prefix;
            if (row instanceof DecoratedNode)
                return ((DecoratedNode) row).prefix;
            return null;
        }

        public String suffix(Object row)
        {
            if (row instanceof DecoratedRow)
                return ((DecoratedRow) row).suffix;
            if (row instanceof DecoratedNode)
                return ((DecoratedNode) row).suffix;
            return null;
        }
    }

    private static final class InspectorNode
    {
        private final String kind;
        private final String name;
        private final String type;
        private final Object value;
        private final Integer objectId;
        private final List<InspectorNode> children;

        private InspectorNode(String kind, String name, String type, Object value, Integer objectId,
                        List<InspectorNode> children)
        {
            this.kind = kind;
            this.name = name;
            this.type = type;
            this.value = value;
            this.objectId = objectId;
            this.children = children;
        }
    }

    private static final class ApproximateBytesTable implements IResultTable
    {
        private final Column[] columns = new Column[] { new Column("Retained Heap", Bytes.class) }; //$NON-NLS-1$

        public ResultMetaData getResultMetaData()
        {
            return null;
        }

        public Column[] getColumns()
        {
            return columns;
        }

        public Object getColumnValue(Object row, int columnIndex)
        {
            return new Bytes(-7L);
        }

        public IContextObject getContext(Object row)
        {
            return null;
        }

        public int getRowCount()
        {
            return 1;
        }

        public Object getRow(int rowId)
        {
            return Integer.valueOf(rowId);
        }
    }

    private static final class BudgetTree implements IResultTree
    {
        private final Node root = new Node("Root", Integer.valueOf(0), 80, Arrays.asList( //$NON-NLS-1$
                        new Node("Child A", Integer.valueOf(1), 81, Collections.emptyList()), //$NON-NLS-1$
                        new Node("Child B", Integer.valueOf(1), 82, Collections.emptyList()))); //$NON-NLS-1$
        private final Column[] columns = new Column[] { new Column("Name", String.class), new Column("Depth", int.class) }; //$NON-NLS-1$ //$NON-NLS-2$

        public ResultMetaData getResultMetaData()
        {
            return null;
        }

        public Column[] getColumns()
        {
            return columns;
        }

        public Object getColumnValue(Object row, int columnIndex)
        {
            Node value = (Node) row;
            return columnIndex == 0 ? value.name : value.depth;
        }

        public IContextObject getContext(Object row)
        {
            final Node value = (Node) row;
            return new IContextObject()
            {
                public int getObjectId()
                {
                    return value.objectId;
                }
            };
        }

        public List<?> getElements()
        {
            return Arrays.asList(root);
        }

        public boolean hasChildren(Object element)
        {
            return !((Node) element).children.isEmpty();
        }

        public List<?> getChildren(Object parent)
        {
            return ((Node) parent).children;
        }
    }

    private static final class BiggestObjectsTree implements IResultTree
    {
        private final BiggestObject root = new BiggestObject("Largest", Integer.valueOf(64), new Bytes(500), 91, //$NON-NLS-1$
                        Collections.singletonList(new BiggestObject("Child", Integer.valueOf(32), new Bytes(250), 92, //$NON-NLS-1$
                                        Collections.<BiggestObject>emptyList())));
        private final Column[] columns = new Column[] { new Column("Object", String.class), //$NON-NLS-1$
                        new Column("Shallow Heap", int.class), new Column("Retained Heap", Bytes.class) }; //$NON-NLS-1$ //$NON-NLS-2$

        public ResultMetaData getResultMetaData()
        {
            return null;
        }

        public Column[] getColumns()
        {
            return columns;
        }

        public Object getColumnValue(Object row, int columnIndex)
        {
            BiggestObject value = (BiggestObject) row;
            switch (columnIndex)
            {
                case 0:
                    return value.label;
                case 1:
                    return value.shallowHeap;
                case 2:
                    return value.retainedHeap;
                default:
                    return null;
            }
        }

        public IContextObject getContext(Object row)
        {
            final BiggestObject value = (BiggestObject) row;
            return new IContextObject()
            {
                public int getObjectId()
                {
                    return value.objectId;
                }
            };
        }

        public List<?> getElements()
        {
            return Collections.singletonList(root);
        }

        public boolean hasChildren(Object element)
        {
            return !((BiggestObject) element).children.isEmpty();
        }

        public List<?> getChildren(Object parent)
        {
            return ((BiggestObject) parent).children;
        }
    }

    private static final class PackageTree implements IResultTree
    {
        private final PackageNode root = new PackageNode("<all>", new Bytes(1000), Double.valueOf(1.0d), 4, -1, //$NON-NLS-1$
                        Collections.singletonList(new PackageNode("org", new Bytes(500), Double.valueOf(0.5d), 2, -1, //$NON-NLS-1$
                                        Collections.<PackageNode>emptyList())));
        private final Column[] columns = new Column[] { new Column("Package", String.class), //$NON-NLS-1$
                        new Column("Retained Heap", Bytes.class), new Column("Retained Heap, %", double.class), //$NON-NLS-1$ //$NON-NLS-2$
                        new Column("Top Dominators", int.class) }; //$NON-NLS-1$

        public ResultMetaData getResultMetaData()
        {
            return null;
        }

        public Column[] getColumns()
        {
            return columns;
        }

        public Object getColumnValue(Object row, int columnIndex)
        {
            PackageNode value = (PackageNode) row;
            switch (columnIndex)
            {
                case 0:
                    return value.name;
                case 1:
                    return value.retainedHeap;
                case 2:
                    return value.retainedPercent;
                case 3:
                    return Integer.valueOf(value.topDominators);
                default:
                    return null;
            }
        }

        public IContextObject getContext(Object row)
        {
            final PackageNode value = (PackageNode) row;
            return new IContextObject()
            {
                public int getObjectId()
                {
                    return value.objectId;
                }
            };
        }

        public List<?> getElements()
        {
            return Collections.singletonList(root);
        }

        public boolean hasChildren(Object element)
        {
            return !((PackageNode) element).children.isEmpty();
        }

        public List<?> getChildren(Object parent)
        {
            return ((PackageNode) parent).children;
        }
    }

    private static final class Row
    {
        private final String name;
        private final Integer count;
        private final int objectId;

        private Row(String name, Integer count, int objectId)
        {
            this.name = name;
            this.count = count;
            this.objectId = objectId;
        }
    }

    private static final class ErrorTable implements IResultTable
    {
        private final List<Row> rows = Collections.singletonList(new Row("Alpha", Integer.valueOf(7), 42)); //$NON-NLS-1$
        private final Column[] columns = new Column[] { new Column("Name", String.class), new Column("Count", int.class) }; //$NON-NLS-1$ //$NON-NLS-2$

        public ResultMetaData getResultMetaData()
        {
            return null;
        }

        public Column[] getColumns()
        {
            return columns;
        }

        public Object getColumnValue(Object row, int columnIndex)
        {
            Row value = (Row) row;
            if (columnIndex == 0)
                return value.name;
            throw new IllegalStateException("boom"); //$NON-NLS-1$
        }

        public IContextObject getContext(Object row)
        {
            final Row value = (Row) row;
            return new IContextObject()
            {
                public int getObjectId()
                {
                    return value.objectId;
                }
            };
        }

        public int getRowCount()
        {
            return rows.size();
        }

        public Object getRow(int rowId)
        {
            return rows.get(rowId);
        }
    }

    private static final class BiggestObject
    {
        private final String label;
        private final Integer shallowHeap;
        private final Bytes retainedHeap;
        private final int objectId;
        private final List<BiggestObject> children;

        private BiggestObject(String label, Integer shallowHeap, Bytes retainedHeap, int objectId,
                        List<BiggestObject> children)
        {
            this.label = label;
            this.shallowHeap = shallowHeap;
            this.retainedHeap = retainedHeap;
            this.objectId = objectId;
            this.children = children;
        }
    }

    private static final class Node
    {
        private final String name;
        private final Integer depth;
        private final int objectId;
        private final List<Node> children;

        private Node(String name, Integer depth, int objectId, List<Node> children)
        {
            this.name = name;
            this.depth = depth;
            this.objectId = objectId;
            this.children = children;
        }
    }

    private static final class PackageNode
    {
        private final String name;
        private final Bytes retainedHeap;
        private final Double retainedPercent;
        private final int topDominators;
        private final int objectId;
        private final List<PackageNode> children;

        private PackageNode(String name, Bytes retainedHeap, Double retainedPercent, int topDominators, int objectId,
                        List<PackageNode> children)
        {
            this.name = name;
            this.retainedHeap = retainedHeap;
            this.retainedPercent = retainedPercent;
            this.topDominators = topDominators;
            this.objectId = objectId;
            this.children = children;
        }
    }

    private static final class UnsupportedResult implements IResult
    {
        public ResultMetaData getResultMetaData()
        {
            return null;
        }
    }

    private static final class CyclicTree implements IResultTree
    {
        private final Node root = new Node("Root", Integer.valueOf(0), 90, //$NON-NLS-1$
                        Collections.singletonList(new Node("Repeat", Integer.valueOf(1), 90, Collections.emptyList()))); //$NON-NLS-1$
        private final Column[] columns = new Column[] { new Column("Name", String.class), new Column("Depth", int.class) }; //$NON-NLS-1$ //$NON-NLS-2$

        public ResultMetaData getResultMetaData()
        {
            return null;
        }

        public Column[] getColumns()
        {
            return columns;
        }

        public Object getColumnValue(Object row, int columnIndex)
        {
            Node value = (Node) row;
            return columnIndex == 0 ? value.name : value.depth;
        }

        public IContextObject getContext(Object row)
        {
            final Node value = (Node) row;
            return new IContextObject()
            {
                public int getObjectId()
                {
                    return value.objectId;
                }
            };
        }

        public List<?> getElements()
        {
            return Collections.singletonList(root);
        }

        public boolean hasChildren(Object element)
        {
            return !((Node) element).children.isEmpty();
        }

        public List<?> getChildren(Object parent)
        {
            return ((Node) parent).children;
        }
    }

    private static final class SamplePie implements IResultPie
    {
        private final List<Slice> slices = Arrays.<Slice>asList(new SampleSlice("Suspect 1", 42d, 101, Color.RED), //$NON-NLS-1$
                        new SampleSlice("Suspect 2", 21d, 102, null)); //$NON-NLS-1$

        public ResultMetaData getResultMetaData()
        {
            return null;
        }

        public List<? extends Slice> getSlices()
        {
            return slices;
        }
    }

    private static final class SampleSlice implements IResultPie.ColoredSlice
    {
        private final String label;
        private final double value;
        private final int objectId;
        private final Color color;

        private SampleSlice(String label, double value, int objectId, Color color)
        {
            this.label = label;
            this.value = value;
            this.objectId = objectId;
            this.color = color;
        }

        public String getLabel()
        {
            return label;
        }

        public double getValue()
        {
            return value;
        }

        public String getDescription()
        {
            return label + " details"; //$NON-NLS-1$
        }

        public IContextObject getContext()
        {
            return new IContextObject()
            {
                public int getObjectId()
                {
                    return objectId;
                }
            };
        }

        public Color getColor()
        {
            return color;
        }
    }
}
