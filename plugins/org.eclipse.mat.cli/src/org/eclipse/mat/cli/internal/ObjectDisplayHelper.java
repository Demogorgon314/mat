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

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import org.eclipse.mat.snapshot.model.Field;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.model.IPrimitiveArray;
import org.eclipse.mat.snapshot.model.IObject.Type;

final class ObjectDisplayHelper
{
    private static final int TEXT_PREVIEW_LIMIT = 120;
    private static final int BYTE_TEXT_SAMPLE = 64;
    private static final int BYTE_HEX_SAMPLE = 32;

    private ObjectDisplayHelper()
    {}

    static String preview(IObject object)
    {
        Object preview = previewValue(object);
        return preview == null ? null : String.valueOf(preview);
    }

    static Object previewValue(IObject object)
    {
        if (object == null)
            return null;

        if (object instanceof IPrimitiveArray)
        {
            Object preview = primitiveArrayPreview((IPrimitiveArray) object);
            if (preview != null)
                return preview;
        }

        if ("java.lang.String".equals(object.getClazz().getName())) //$NON-NLS-1$
        {
            String stringValue = object.getClassSpecificName();
            if (stringValue != null)
                return textPreview(stringValue, Integer.valueOf(stringValue.length()), null);
        }

        String preview = object.getClassSpecificName();
        if (preview == null || preview.length() == 0)
            preview = object.getTechnicalName();
        return preview;
    }

    static String primitiveValue(Object value)
    {
        return value == null ? "null" : String.valueOf(value); //$NON-NLS-1$
    }

    static String fieldType(Field field)
    {
        return field == null ? null : field.getVerboseSignature();
    }

    static String primitiveTypeName(int type)
    {
        if (type < 0 || type >= IPrimitiveArray.TYPE.length || IPrimitiveArray.TYPE[type] == null)
            return null;

        String arrayType = IPrimitiveArray.TYPE[type];
        return arrayType.substring(0, arrayType.length() - 2);
    }

    static boolean isPseudoStatic(Field field)
    {
        return field != null && field.getName() != null && field.getName().startsWith("<"); //$NON-NLS-1$
    }

    private static Object primitiveArrayPreview(IPrimitiveArray array)
    {
        if (array == null)
            return null;

        switch (array.getType())
        {
            case Type.CHAR:
                return charArrayPreview(array);
            case Type.BYTE:
                return byteArrayPreview(array);
            default:
                return null;
        }
    }

    private static DisplayValue charArrayPreview(IPrimitiveArray array)
    {
        int length = array.getLength();
        int sampleLength = Math.min(length, TEXT_PREVIEW_LIMIT);
        if (sampleLength == 0)
            return textPreview("", Integer.valueOf(length), null, false); //$NON-NLS-1$
        char[] chars = (char[]) array.getValueArray(0, sampleLength);
        return textPreview(new String(chars), Integer.valueOf(length), null, length > sampleLength);
    }

    private static DisplayValue byteArrayPreview(IPrimitiveArray array)
    {
        int length = array.getLength();
        int sampleLength = Math.min(length, BYTE_TEXT_SAMPLE);
        if (sampleLength == 0)
            return textPreview("", Integer.valueOf(length), "utf8", false); //$NON-NLS-1$ //$NON-NLS-2$
        byte[] sample = (byte[]) array.getValueArray(0, sampleLength);
        String decoded = decodeUtf8(sample);
        if (decoded != null && isMostlyPrintable(decoded))
        {
            return textPreview(decoded, Integer.valueOf(length), "utf8", length > sampleLength); //$NON-NLS-1$
        }

        int hexLength = Math.min(length, BYTE_HEX_SAMPLE);
        byte[] hexBytes = (byte[]) array.getValueArray(0, hexLength);
        boolean truncated = length > hexLength;
        StringBuilder builder = new StringBuilder(Math.max(1, hexLength * 3));
        for (int ii = 0; ii < hexBytes.length; ii++)
        {
            if (ii > 0)
                builder.append(' ');
            int value = hexBytes[ii] & 0xff;
            if (value < 0x10)
                builder.append('0');
            builder.append(Integer.toHexString(value));
        }
        if (truncated)
            builder.append(" ..."); //$NON-NLS-1$
        return new DisplayValue(builder.toString(),
                        new DisplayValue.Metadata("hex_preview", Integer.valueOf(length), Boolean.valueOf(truncated), //$NON-NLS-1$
                                        "hex")); //$NON-NLS-1$
    }

    private static DisplayValue textPreview(String text, Integer length, String encoding)
    {
        return textPreview(text, length, encoding, text != null && text.length() > TEXT_PREVIEW_LIMIT);
    }

    private static DisplayValue textPreview(String text, Integer length, String encoding, boolean truncated)
    {
        String sample = text == null ? null : text.substring(0, Math.min(text.length(), TEXT_PREVIEW_LIMIT));
        String rendered = escapeText(sample);
        if (truncated)
            rendered = rendered + "..."; //$NON-NLS-1$
        return new DisplayValue(rendered, new DisplayValue.Metadata("text_preview", length, //$NON-NLS-1$
                        Boolean.valueOf(truncated), encoding));
    }

    private static String decodeUtf8(byte[] bytes)
    {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        try
        {
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        }
        catch (CharacterCodingException e)
        {
            return null;
        }
    }

    private static boolean isMostlyPrintable(String text)
    {
        if (text == null || text.length() == 0)
            return true;

        int printable = 0;
        for (int ii = 0; ii < text.length(); ii++)
        {
            char ch = text.charAt(ii);
            if (!Character.isISOControl(ch) || ch == '\n' || ch == '\r' || ch == '\t')
                printable++;
        }
        return printable * 100 >= text.length() * 85;
    }

    private static String escapeText(String value)
    {
        if (value == null)
            return null;

        StringBuilder builder = new StringBuilder(value.length());
        for (int ii = 0; ii < value.length(); ii++)
        {
            char ch = value.charAt(ii);
            switch (ch)
            {
                case '\\':
                    builder.append("\\\\"); //$NON-NLS-1$
                    break;
                case '\n':
                    builder.append("\\n"); //$NON-NLS-1$
                    break;
                case '\r':
                    builder.append("\\r"); //$NON-NLS-1$
                    break;
                case '\t':
                    builder.append("\\t"); //$NON-NLS-1$
                    break;
                default:
                    if (ch < 0x20)
                    {
                        String hex = Integer.toHexString(ch);
                        builder.append("\\u"); //$NON-NLS-1$
                        for (int pad = hex.length(); pad < 4; pad++)
                            builder.append('0');
                        builder.append(hex);
                    }
                    else
                    {
                        builder.append(ch);
                    }
                    break;
            }
        }
        return builder.toString();
    }
}
