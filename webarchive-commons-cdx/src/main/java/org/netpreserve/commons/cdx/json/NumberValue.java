/*
 * Copyright 2016 IIPC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.netpreserve.commons.cdx.json;

import java.io.IOException;
import java.io.Writer;

/**
 *
 */
public final class NumberValue implements Value<Number> {

    private final Number value;

    private NumberValue(Number value) {
        this.value = value;
    }

    public static NumberValue valueOf(char[] src, int start, int end) {
        Number value;
        Double d = Double.parseDouble(String.copyValueOf(src, start, end - start));
        if (d.longValue() == d) {
            value = d.longValue();
        } else {
            value = d;
        }
        return new NumberValue(value);
    }

    public static NumberValue valueOf(Number value) {
        return new NumberValue(value);
    }

    public static NumberValue valueOf(String value) {
        return valueOf(value.toCharArray(), 0, value.length());
    }

    @Override
    public Number getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public void toJson(Writer out) throws IOException {
        out.write(value.toString());
    }

}
