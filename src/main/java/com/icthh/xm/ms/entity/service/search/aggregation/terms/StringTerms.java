/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.icthh.xm.ms.entity.service.search.aggregation.terms;

import com.icthh.xm.ms.entity.service.search.aggregation.DocValueFormat;
import org.apache.lucene.util.BytesRef;

import java.util.List;

public class StringTerms extends InternalMappedTerms<StringTerms, StringTerms.Bucket> {

    public static final String NAME = "sterms";

    protected StringTerms(List<Bucket> buckets) {
        super(buckets);
    }

    public static class Bucket extends InternalTerms.Bucket<StringTerms.Bucket> {

        BytesRef termBytes;

        public Bucket(DocValueFormat format) {
            super(format);
        }

        @Override
        public Number getKeyAsNumber() {
            return null;
        }

        @Override
        public String getKeyAsString() {
            return format.format(termBytes).toString();
        }
    }
}
