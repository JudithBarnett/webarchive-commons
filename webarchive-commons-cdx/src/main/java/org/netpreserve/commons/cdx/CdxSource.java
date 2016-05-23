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
package org.netpreserve.commons.cdx;

import org.netpreserve.commons.cdx.processor.Processor;

import java.io.Closeable;
import java.util.List;

/**
 * Representation of a source of Cdx lines.
 * <p>
 * Could be a file, a compressed Cdx cluster or some other source like a database or a web service.
 */
public interface CdxSource extends Closeable {

    /**
     * Get a list of Cdx lines.
     * <p>
     * The startKey and endKey is constructed from surt + space + timestamp or a part thereof.
     * <p>
     * @param startKey the lexiografically lowest key (inclusive) to get
     * @param endKey the lexiografically highest key (exclusive) to get
     * @param processors a list of processors for filtering the list
     * @param reverse if true the result list will be sorted in descending order
     * @return an {@link SearchResult} returning iterators over the requested list
     */
    SearchResult search(String startKey, String endKey, List<Processor> processors,
            boolean reverse);

    /**
     * Get a count of Cdx lines within the submitted range.
     * <p>
     * @param startKey the lexiografically lowest key (inclusive) to get
     * @param endKey the lexiografically highest key (exclusive) to get
     * @return the number of lines
     */
    long count(String startKey, String endKey);

}
