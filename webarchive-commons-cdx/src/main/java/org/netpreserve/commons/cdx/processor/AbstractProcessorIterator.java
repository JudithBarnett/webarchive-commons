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
package org.netpreserve.commons.cdx.processor;


import org.netpreserve.commons.cdx.functions.Function;
import org.netpreserve.commons.cdx.cdxsource.CdxIterator;
import org.netpreserve.commons.cdx.CdxRecord;

public abstract class AbstractProcessorIterator<T extends Function> implements CdxIterator {

    protected CdxIterator wrappedCdxIterator;

    private volatile State state = State.NOT_READY;

    private volatile boolean passtrough = false;

    private volatile CdxRecord next;

    public AbstractProcessorIterator(CdxIterator iterator) {
        this.wrappedCdxIterator = iterator;
    }

    @Override
    public boolean hasNext() {
        if (passtrough) {
            return wrappedCdxIterator.hasNext();
        }

        switch (state) {
            case DONE:
                return false;
            case READY:
                return true;
            case FAILED:
                throw new IllegalStateException();
            default:
        }
        return tryToComputeNext();
    }

    @Override
    public CdxRecord next() {
        if (passtrough) {
            return wrappedCdxIterator.next();
        }

        if (!hasNext()) {
            return null;
        }

        state = State.NOT_READY;
        CdxRecord result = next;
        next = null;
        return result;
    }

    /**
     * Returns the next element in the iteration without advancing the iteration, according to the
     * contract of {@link PeekingIterator#peek()}.
     * <p>
     * @return the next element
     */
    @Override
    public CdxRecord peek() {
        if (passtrough) {
            return wrappedCdxIterator.peek();
        }

        if (!hasNext()) {
            return null;
        }
        return next;
    }

    @Override
    public void close() {
        state = State.FAILED;
        wrappedCdxIterator.close();
    }

    protected void passtrough() {
        passtrough = true;
    }

    /**
     * Guaranteed to throw an exception and leave the underlying data unmodified.
     * <p>
     * @throws UnsupportedOperationException always
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Implementations of {@link #computeNext} <b>must</b> invoke this method when there are no
     * elements left in the iteration.
     * <p>
     * @return {@code null}; a convenience so your {@code computeNext} implementation can use the
     * simple statement {@code return endOfData();}
     */
    protected final CdxRecord endOfData() {
        state = State.DONE;
        return null;
    }

    private boolean tryToComputeNext() {
        state = State.FAILED; // temporary pessimism

        while (next == null && state != State.DONE) {
            next = computeNext();
        }

        if (state != State.DONE) {
            state = State.READY;
            return true;
        }
        return false;
    }

    protected abstract CdxRecord computeNext();

    private enum State {

        /**
         * We have computed the next element and haven't returned it yet.
         */
        READY,
        /**
         * We haven't yet computed or have already returned the element.
         */
        NOT_READY,
        /**
         * We have reached the end of the data and are finished.
         */
        DONE,
        /**
         * We've suffered an exception and are kaput.
         */
        FAILED,
    }
}
