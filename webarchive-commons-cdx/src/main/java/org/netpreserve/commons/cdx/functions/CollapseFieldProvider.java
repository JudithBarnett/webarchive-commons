/*
 * Copyright 2015 IIPC.
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
package org.netpreserve.commons.cdx.functions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.netpreserve.commons.cdx.CdxRecord;
import org.netpreserve.commons.cdx.FieldName;

/**
 * Provider for constructing CollapseField functions.
 */
public class CollapseFieldProvider implements
        FunctionProvider<CollapseFieldProvider.CollapseField> {

    private final List<CollapseFieldDef> collapseFields;

    /**
     * Constructs a new CollapseFieldProvider using a collapse description for creating new Collapse
     * Field functions.
     * <p>
     * @param collapseStrings the collapse descriptions
     */
    public CollapseFieldProvider(List<String> collapseStrings) {
        collapseFields = new ArrayList<>(collapseStrings.size());
        for (String collapseString : collapseStrings) {
            collapseFields.add(new CollapseFieldDef(collapseString));
        }
    }

    @Override
    public CollapseField newFunction() {
        return new CollapseField(collapseFields);
    }

    /**
     * A function which collapses CdxLines by a specific field, or part of a specific field.
     * <p>
     * eg:
     * <p>
     * {@code <field>} = if {@code <field>} matches previous match, then its a duplicate
     * {@code <field>:<n>} = if first {@code <n>} character of {@code <field>} match, then it is a
     * duplicate
     * <p>
     */
    public static class CollapseField implements BiFunction {

        private static final int NOT_USED = -1;

        List<CollapseFieldDef> collapseFields;

        FieldName[] fieldIndexes;

        private boolean isCollapsing = false;

        private Set<String> collapseValues = new HashSet<>(1024);

        private int collapseOnKeyLength = NOT_USED;

        public CollapseField(List<CollapseFieldDef> collapseFields) {
            this.collapseFields = collapseFields;
            this.fieldIndexes = new FieldName[collapseFields.size()];
        }

        @Override
        public CdxRecord apply(CdxRecord previousLine, CdxRecord currentLine) {
            if (previousLine == null) {
                for (int i = 0; i < this.collapseFields.size(); i++) {
                    fieldIndexes[i] = FieldName.forName(collapseFields.get(i).fieldName);
                    if (fieldIndexes[i] == FieldName.URI_KEY) {
                        collapseOnKeyLength = collapseFields.get(i).matchLength;
                    }
                }

                collapseValues.add(getMatchValues(currentLine));
                return currentLine;
            }

            String currMatchValue = getMatchValues(currentLine);

            if (collapseOnKeyLength != NOT_USED) {
                String prevValue = previousLine.getKey().getUriKey().toString();
                String currValue = currentLine.getKey().getUriKey().toString();
                if (collapseOnKeyLength > 0
                        && collapseOnKeyLength < prevValue.length()
                        && collapseOnKeyLength < currValue.length()) {
                    prevValue = prevValue.substring(0, collapseOnKeyLength);
                    currValue = currValue.substring(0, collapseOnKeyLength);
                }

                if (!currValue.equals(prevValue)) {
                    collapseValues.clear();
                    isCollapsing = false;
                    return currentLine;
                }
            }

            if (!currMatchValue.isEmpty() && !collapseValues.contains(currMatchValue)) {
                collapseValues.add(currMatchValue);
                isCollapsing = false;
                return currentLine;
            }

            isCollapsing = true;
            return null;
        }

        private String getMatchValues(CdxRecord cdxLine) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < fieldIndexes.length; i++) {
                if (fieldIndexes[i] == FieldName.URI_KEY) {
                    continue;
                }

                CollapseFieldDef fieldDef = collapseFields.get(i);

                if (fieldDef.matchLength == CollapseFieldDef.MATCH_WHOLE_FIELD) {
                    sb.append(cdxLine.get(fieldIndexes[i]));
                } else {
                    String fieldValue = cdxLine.get(fieldIndexes[i]).toString();
                    sb.append(fieldValue, 0, Math.min(fieldValue.length(), fieldDef.matchLength));
                }
            }
            return sb.toString();
        }

    }

    private static class CollapseFieldDef {

        static final String FIELD_SEP_CHAR = ":";

        static final int MATCH_WHOLE_FIELD = 0;

        final String fieldName;

        final int matchLength;

        CollapseFieldDef(String collapseString) {
            int sepIndex = collapseString.indexOf(FIELD_SEP_CHAR);

            // Match entire field
            if (sepIndex < 0) {
                fieldName = collapseString;
                matchLength = MATCH_WHOLE_FIELD;
            } else {
                fieldName = collapseString.substring(0, sepIndex);
                matchLength = Integer.parseInt(collapseString.substring(sepIndex + 1));
            }
        }

    }
}
