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
 *
 * Original version of this file is located at: URL
 */
package com.icthh.xm.ms.entity.service.search.aggregation.terms;

import com.icthh.xm.ms.entity.service.search.aggregation.DocValueFormat;

import java.util.List;

public abstract class InternalTerms<A extends InternalTerms<A, B>, B extends InternalTerms.Bucket<B>> implements Terms {

    public abstract static class Bucket<B extends Bucket<B>> implements Terms.Bucket {

        protected final DocValueFormat format;

        protected Bucket(DocValueFormat format) {
            this.format = format;
        }
    }

    public abstract List<B> getBuckets();

}
