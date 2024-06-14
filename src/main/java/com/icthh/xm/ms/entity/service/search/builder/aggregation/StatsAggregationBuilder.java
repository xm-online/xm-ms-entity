/*
 * Original version of this file is located at:
 * https://github.com/elastic/elasticsearch/blob/v6.4.3/server/src/main/java/org/elasticsearch/search/aggregations/metrics/stats/StatsAggregationBuilder.java
 *
 *  Licensed to Elasticsearch under one or more contributor
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
package com.icthh.xm.ms.entity.service.search.builder.aggregation;

import com.icthh.xm.ms.entity.service.search.builder.aggregation.support.ValuesSource;
import com.icthh.xm.ms.entity.service.search.builder.aggregation.support.ValuesSourceType;
import com.icthh.xm.ms.entity.service.search.builder.aggregation.support.ValueType;

public class StatsAggregationBuilder extends ValuesSourceAggregationBuilder<ValuesSource.Numeric, StatsAggregationBuilder> {

    public static final String NAME = "stats";

    public StatsAggregationBuilder(String name) {
        super(name, ValuesSourceType.NUMERIC, ValueType.NUMERIC);
    }
}
