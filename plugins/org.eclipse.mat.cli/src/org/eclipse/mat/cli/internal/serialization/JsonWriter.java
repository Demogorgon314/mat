/*******************************************************************************
 * Copyright (c) 2026 Eclipse Memory Analyzer Project.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.mat.cli.internal.serialization;

import java.util.ArrayDeque;
import java.util.Deque;

public final class JsonWriter
{
    private enum Scope
    {
        OBJECT, ARRAY
    }

    private final StringBuilder builder = new StringBuilder(2048);
    private final Deque<Scope> scopes = new ArrayDeque<Scope>();
    private final Deque<Boolean> firstValues = new ArrayDeque<Boolean>();

    public JsonWriter beginObject()
    {
        beforeValue();
        scopes.push(Scope.OBJECT);
        firstValues.push(Boolean.TRUE);
        builder.append('{');
        return this;
    }

    public JsonWriter endObject()
    {
        builder.append('}');
        scopes.pop();
        firstValues.pop();
        return this;
    }

    public JsonWriter beginArray()
    {
        beforeValue();
        scopes.push(Scope.ARRAY);
        firstValues.push(Boolean.TRUE);
        builder.append('[');
        return this;
    }

    public JsonWriter endArray()
    {
        builder.append(']');
        scopes.pop();
        firstValues.pop();
        return this;
    }

    public JsonWriter name(String name)
    {
        beforeName();
        string(name);
        builder.append(':');
        return this;
    }

    public JsonWriter nullValue()
    {
        beforeValue();
        builder.append("null"); //$NON-NLS-1$
        return this;
    }

    public JsonWriter value(String value)
    {
        if (value == null)
            return nullValue();
        beforeValue();
        string(value);
        return this;
    }

    public JsonWriter value(boolean value)
    {
        beforeValue();
        builder.append(value);
        return this;
    }

    public JsonWriter value(long value)
    {
        beforeValue();
        builder.append(value);
        return this;
    }

    public JsonWriter value(int value)
    {
        beforeValue();
        builder.append(value);
        return this;
    }

    public JsonWriter value(double value)
    {
        beforeValue();
        if (Double.isNaN(value) || Double.isInfinite(value))
            builder.append('"').append(value).append('"');
        else
            builder.append(value);
        return this;
    }

    public JsonWriter rawValue(Object value)
    {
        if (value == null)
            return nullValue();
        if (value instanceof String)
            return value((String) value);
        if (value instanceof Integer)
            return value(((Integer) value).intValue());
        if (value instanceof Long)
            return value(((Long) value).longValue());
        if (value instanceof Double)
            return value(((Double) value).doubleValue());
        if (value instanceof Float)
            return value(((Float) value).doubleValue());
        if (value instanceof Boolean)
            return value(((Boolean) value).booleanValue());
        if (value instanceof Number)
            return value(((Number) value).doubleValue());
        return value(String.valueOf(value));
    }

    @Override
    public String toString()
    {
        return builder.toString();
    }

    private void beforeName()
    {
        if (scopes.isEmpty() || scopes.peek() != Scope.OBJECT)
            throw new IllegalStateException("Not in an object"); //$NON-NLS-1$

        if (Boolean.TRUE.equals(firstValues.peek()))
        {
            firstValues.pop();
            firstValues.push(Boolean.FALSE);
        }
        else
        {
            builder.append(',');
        }
    }

    private void beforeValue()
    {
        if (scopes.isEmpty())
            return;

        if (scopes.peek() == Scope.ARRAY)
        {
            if (Boolean.TRUE.equals(firstValues.peek()))
            {
                firstValues.pop();
                firstValues.push(Boolean.FALSE);
            }
            else
            {
                builder.append(',');
            }
        }
    }

    private void string(String value)
    {
        builder.append('"');
        for (int ii = 0; ii < value.length(); ii++)
        {
            char ch = value.charAt(ii);
            switch (ch)
            {
                case '\\':
                    builder.append("\\\\"); //$NON-NLS-1$
                    break;
                case '"':
                    builder.append("\\\""); //$NON-NLS-1$
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
                        builder.append(String.format("\\u%04x", Integer.valueOf(ch))); //$NON-NLS-1$
                    }
                    else
                    {
                        builder.append(ch);
                    }
                    break;
            }
        }
        builder.append('"');
    }
}
