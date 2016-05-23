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
import java.util.List;
import java.util.regex.Pattern;

import org.netpreserve.commons.cdx.CdxRecord;

/**
 * Matches a FieldSplitLine against a string of regex.
 * <p>
 * Supports matching against individual fields if specified eg:
 * <p>
 * ~&lt;containsstr&gt; = look for containing string {@code <containsstr>} and not a regex
 * <p>
 * &lt;regex&gt; = match whole line &lt;field&gt;:&lt;regex&gt; = match &lt;field&gt; in
 * FieldSplitLine, by name or number, and match only that field
 * <p>
 * Supports !&lt;regex&gt; for not matching
 * <p>
 * @author ilya
 * <p>
 */
public class FieldRegexFilter implements Filter {

    static final String INVERT_CHAR = "!";

    static final String CONTAINS_CHAR = "~";

    static final String FIELD_SEP_CHAR = ":";

    final List<RegexMatch> regexMatchers;

    class RegexMatch {

        final Pattern regex;

        final boolean inverted;

        final String containsStr;

        String field;

        RegexMatch(String str) {
            boolean contains = false;

            if (str.startsWith(CONTAINS_CHAR)) {
                str = str.substring(1);
                contains = true;
            }

            if (str.startsWith(INVERT_CHAR)) {
                str = str.substring(1);
                inverted = true;
            } else {
                inverted = false;
            }

            int sepIndex = str.indexOf(FIELD_SEP_CHAR);

            // Match entire line
            if (sepIndex < 0) {
                if (contains) {
                    containsStr = str;
                    regex = null;
                } else {
                    containsStr = null;
                    regex = Pattern.compile(str);
                }
                return;
            }

            field = str.substring(0, sepIndex);
            String pattern = str.substring(sepIndex + 1);

            if (contains) {
                containsStr = pattern;
                regex = null;
            } else {
                containsStr = null;
                regex = Pattern.compile(pattern);
            }
        }

        boolean matches(CdxRecord line) {
            boolean matched;

            if (field == null) {
                if (containsStr != null) {
                    matched = String.valueOf(line).contains(containsStr);
                } else {
                    matched = regex.matcher(line.toString()).matches();
                }
            } else {
                if (containsStr != null) {
                    matched = line.get(field).toString().contains(containsStr);
                } else {
                    matched = regex.matcher(line.get(field).toString()).matches();
                }
            }

            if (inverted) {
                matched = !matched;
            }

            return matched;
        }

    }

    public FieldRegexFilter(List<String> regexs) {
        this.regexMatchers = new ArrayList<RegexMatch>(regexs.size());

        for (String regex : regexs) {
            if (!regex.isEmpty()) {
                regexMatchers.add(new RegexMatch(regex));
            }
        }
    }

    @Override
    public boolean include(CdxRecord line) {
        for (RegexMatch regexMatch : regexMatchers) {
            if (!regexMatch.matches(line)) {
                return false;
            }
        }

        return true;
    }

}
