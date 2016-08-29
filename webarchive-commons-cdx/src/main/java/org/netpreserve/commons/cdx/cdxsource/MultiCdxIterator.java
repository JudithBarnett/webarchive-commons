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
package org.netpreserve.commons.cdx.cdxsource;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.netpreserve.commons.cdx.CdxRecord;

/**
 * A {@link CdxIterator} which iterates over a collection of iterators.
 */
public class MultiCdxIterator implements CdxIterator {

    final CdxSourceExecutorService executorService = CdxSourceExecutorService.getInstance();

    CdxIterator[] iterators;

    int count;

    int takeptr = 0;

    Comparator<CdxIterator> iteratorComparator;

    /**
     * Constructs a new MultiCdxIterator from an array of CdxIterators.
     * <p>
     * @param parallel true if source iterators should be running in separate threads to achieve parallel processing.
     * @param reverse if true the result list will be sorted in descending order
     * @param iterators the CdxIterators to use as sources.
     */
    public MultiCdxIterator(boolean parallel, boolean reverse, final CdxIterator... iterators) {
        this.iteratorComparator = reverse ? new ReverseComparator() : new ForwardComparator();

        int len = iterators.length;
        this.iterators = new CdxIterator[len];

        if (parallel) {
            synchronized (executorService) {
                for (int i = 0; i < len; i++) {
                    CdxIterator iter = new IteratorTask(iterators[i]);
                    if (iter.peek() != null) {
                        this.iterators[count] = iter;
                        count++;
                    } else {
                        iter.close();
                    }
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                CdxIterator iter = iterators[i];
                if (iter.peek() != null) {
                    this.iterators[count] = iter;
                    count++;
                } else {
                    iter.close();
                }
            }
        }

        Arrays.sort(this.iterators, 0, count, iteratorComparator);
    }

    @Override
    public CdxRecord next() {
        if (count > 0) {
            CdxIterator current = iterators[takeptr];
            CdxRecord result = current.next();
            if (current.peek() == null) {
                count--;
                takeptr++;
                current.close();
            } else {
                for (int i = takeptr + 1; i < count; i++) {
                    if (iteratorComparator.compare(current, iterators[i]) > 0) {
                        iterators[i - 1] = iterators[i];
                    } else {
                        iterators[i - 1] = current;
                        current = null;
                        break;
                    }
                }
                if (current != null) {
                    iterators[count - 1] = current;
                }
            }
            return result;
        }
        return null;

    }

    @Override
    public CdxRecord peek() {
        if (count > 0) {
            return iterators[takeptr].peek();
        } else {
            return null;
        }
    }

    @Override
    public CdxIterator limit(long maxSize) {
        return new SizeLimitingCdxIterator(this, maxSize);
    }

    @Override
    public void close() {
        for (CdxIterator iter : iterators) {
            if (iter != null) {
                iter.close();
            }
        }
    }

    @Override
    public boolean hasNext() {
        return peek() != null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Class for wrapping an iterator in its own thread.
     */
    private final class IteratorTask implements CdxIterator {

        private final ArrayBlockingQueue<CdxRecord> queue;

        private final Future future;

        private final CdxIterator iterator;

        private CdxRecord next;

        private boolean hasMore = true;

        /**
         * Constructs a new IteratorTask and submits it to the ExecutorService.
         * <p>
         * @param iterator the iterator to wrap
         */
        IteratorTask(final CdxIterator iterator) {
            this.iterator = iterator;
            this.queue = new ArrayBlockingQueue<>(8);

            future = executorService.submit(() -> {
                try {
                    while (iterator.hasNext()) {
                        queue.put(iterator.next());
                    }
                    hasMore = false;
                } catch (InterruptedException ex) {
                    hasMore = false;
                }
            });
        }

        @Override
        public CdxRecord next() {
            CdxRecord result = peek();
            next = null;
            return result;
        }

        @Override
        public CdxRecord peek() {
            if (next == null) {
                getNext();
            }
            return next;
        }

        @Override
        public boolean hasNext() {
            return peek() != null;
        }

        @Override
        public CdxIterator limit(long maxSize) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            hasMore = false;
            future.cancel(true);
            iterator.close();
            queue.clear();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Helper method for getting the next item from the queue. Waiting if necessary for one to become available.
         */
        private void getNext() {
            while (next == null && (hasMore || !queue.isEmpty())) {
                try {
                    next = queue.poll(10, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ex) {
                    return;
                }
            }
        }

    }

    /**
     * Comparator for initial sorting of iterators.
     */
    static class ForwardComparator implements Comparator<CdxIterator> {

        @Override
        public int compare(CdxIterator cdx1, CdxIterator cdx2) {
            if (cdx1 == null && cdx2 == null) {
                return 0;
            }
            if (cdx1 == null) {
                return 1;
            }
            if (cdx2 == null) {
                return -1;
            }
            return cdx1.peek().compareTo(cdx2.peek());
        }

    };

    /**
     * Comparator for initial sorting of iterators.
     */
    static class ReverseComparator implements Comparator<CdxIterator> {

        @Override
        public int compare(CdxIterator cdx1, CdxIterator cdx2) {
            if (cdx1 == null && cdx2 == null) {
                return 0;
            }
            if (cdx1 == null) {
                return -1;
            }
            if (cdx2 == null) {
                return 1;
            }
//            System.out.println(cdx2.peek() + " <> " + cdx1.peek() + " == " + cdx2.peek().compareTo(cdx1.peek()));
            return cdx2.peek().compareTo(cdx1.peek());
        }

    };
}
