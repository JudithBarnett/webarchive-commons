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

import org.junit.Test;
import org.netpreserve.commons.cdx.CdxLine;
import org.netpreserve.commons.cdx.cdxsource.CdxIterator;
import org.netpreserve.commons.cdx.CdxLineFormat;
import org.netpreserve.commons.cdx.CdxRecord;
import org.netpreserve.commons.cdx.cdxsource.MockCdxIterator;
import org.netpreserve.commons.cdx.functions.Filter;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for FilterProcessor.
 */
public class FilterProcessorTest {

    /**
     * Test of processorIterator method, of class FilterProcessor.
     */
    @Test
    public void testProcessorIterator() {
        CdxRecord line1 = new CdxLine("no,dagbladet)/premier2000/spiller_2519.html 20070908002541 "
                + "http://www.dagbladet.no/premier2000/spiller_2519.html text/html 404 "
                + "4GYIEA43CYREJWAD2NSGSIWYVGXJNGB7 - - 1506 68224437 "
                + "IAH-20070907235053-00459-heritrix2.nb.no.arc.gz", CdxLineFormat.CDX11LINE);

        CdxRecord line2 = new CdxLine("no,dagbladet)/premier2000/spiller_2520.html 20070908002532 "
                + "http://www.dagbladet.no/premier2000/spiller_2520.html text/html 200 "
                + "5RRATYEFXZV5V64KA6AP3KFDK7LGF7TT - - 4014 89051462 "
                + "IAH-20070907231717-00457-heritrix2.nb.no.arc.gz", CdxLineFormat.CDX11LINE);

        FilterProcessor fp = new FilterProcessor();
        fp.addFunction(new Filter() {
            @Override
            public boolean include(CdxRecord line) {
                return String.valueOf(line.get("hsc")).equals("200");
            }

        });

        MockCdxIterator iter = new MockCdxIterator();
        iter.add(line1).add(line2);

        CdxIterator processedIterator = fp.processorIterator(iter);

        assertThat(processedIterator).hasSize(1);
    }

}
