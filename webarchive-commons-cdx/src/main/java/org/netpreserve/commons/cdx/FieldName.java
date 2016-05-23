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
package org.netpreserve.commons.cdx;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Representation of a field name.
 */
public final class FieldName {
    public enum Type {
        STRING,
        NUMBER,
        BOOLEAN,
        URI,
        ANY
    }

    private static final Map<String, FieldName> FIELDS_BY_NAME = new HashMap<>();

    private static final Map<Character, FieldName> FIELDS_BY_CODE = new HashMap<>();

    /**
     * Ceta tags (AIF).
     * Code 'M' in legacy CDX format
     */
    public static final FieldName ROBOT_FLAGS = forNameAndCode("robotflags", 'M');

    /**
     * Url key (massaged url e.g. Surt format).
     * Code 'N' in legacy CDX format
     */
    public static final FieldName URI_KEY = forNameAndCodeAndType("urlkey", 'N', Type.STRING);

    /**
     * Language string.
     * Code 'Q' in legacy CDX format
     */
    public static final FieldName LANGUAGE = forNameAndCode("lang", 'Q');

    /**
     * Length.
     * Code 'S' in legacy CDX format
     */
    public static final FieldName LENGTH = forNameAndCodeAndType("length", 'S', Type.NUMBER);

    /**
     * Compressed arc file offset.
     * Code 'V' in legacy CDX format
     */
    public static final FieldName OFFSET = forNameAndCodeAndType("offset", 'V', Type.NUMBER);

    /**
     * Original Url.
     * Code 'a' in legacy CDX format
     */
    public static final FieldName ORIGINAL_URI = forNameAndCodeAndType("url", 'a', Type.URI);

    /**
     * Date.
     * Code 'b' in legacy CDX format
     */
    public static final FieldName TIMESTAMP = forNameAndCodeAndType("timestamp", 'b', Type.STRING);

    /**
     * File name.
     * Code 'g' in legacy CDX format
     */
    public static final FieldName FILENAME = forNameAndCodeAndType("filename", 'g', Type.STRING);

    /**
     * New style checksum.
     * Code 'k' in legacy CDX format
     */
    public static final FieldName DIGEST = forNameAndCodeAndType("digest", 'k', Type.STRING);

    /**
     * Mime type of original document.
     * Code 'm' in legacy CDX format
     */
    public static final FieldName MIME_TYPE = forNameAndCodeAndType("mime", 'm', Type.STRING);

    /**
     * Redirect.
     * Code 'r' in legacy CDX format
     */
    public static final FieldName REDIRECT = forNameAndCode("redirect", 'r');

    /**
     * Response code.
     * Code 's' in legacy CDX format
     */
    public static final FieldName RESPONSE_CODE = forNameAndCode("statuscode", 's');

    /**
     * Comment.
     */
    public static final FieldName COMMENT = forNameAndCode("comment", '#');

    /**
     * Record id in warc file.
     */
    public static final FieldName WARC_ID = forNameAndType("warcid", Type.STRING);

    /**
     * Locator used to fetch the record.
     */
    public static final FieldName LOCATOR = forNameAndType("loc", Type.URI);

    private final String name;

    private final char code;

    private final Type type;

    private FieldName(String name, char code, Type type) {
        this.name = Objects.requireNonNull(name);
        this.code = code;
        this.type = Objects.requireNonNull(type);
    }

    public static FieldName forName(String name) {
        FieldName field = FIELDS_BY_NAME.get(name);
        if (field == null) {
            field = new FieldName(name, '?', Type.ANY);
            FIELDS_BY_NAME.put(name, field);
        }
        return field;
    }

    public static FieldName forCode(char code) {
        FieldName field = FIELDS_BY_CODE.get(code);
        if (field == null) {
            throw new IllegalArgumentException("Illegal field code: " + code);
        }
        return field;
    }

    private static FieldName forNameAndCode(String name, char code) {
        return forNameAndCodeAndType(name, code, Type.STRING);
    }

    private static FieldName forNameAndType(String name, Type type) {
        return forNameAndCodeAndType(name, '?', type);
    }

    private static FieldName forNameAndCodeAndType(String name, char code, Type type) {
        FieldName field = FIELDS_BY_NAME.get(name);
        if (field == null) {
            field = new FieldName(name, code, type);
            FIELDS_BY_NAME.put(name, field);
            FIELDS_BY_CODE.put(code, field);
        }
        return field;
    }

    public String getName() {
        return name;
    }

    public char getCode() {
        return code;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 11 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FieldName other = (FieldName) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

}
