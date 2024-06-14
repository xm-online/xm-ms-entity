/*
 * Original version of this file is located at:
 * https://github.com/spring-projects/spring-data-elasticsearch/blob/3.1.12.RELEASE/src/main/java/org/springframework/data/elasticsearch/core/query/NativeSearchQueryBuilder.java
 *
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.icthh.xm.ms.entity.service.search.builder;

import com.icthh.xm.ms.entity.service.search.builder.aggregation.AbstractAggregationBuilder;
import com.icthh.xm.ms.entity.service.search.query.dto.NativeSearchQuery;
import com.icthh.xm.ms.entity.service.search.filter.SourceFilter;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

public class NativeSearchQueryBuilder {

    private QueryBuilder queryBuilder;
    private Pageable pageable = Pageable.unpaged();
    private SourceFilter sourceFilter;
    private List<AbstractAggregationBuilder> aggregationBuilders = new ArrayList<>();

    public NativeSearchQueryBuilder withQuery(QueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
        return this;
    }

    public NativeSearchQueryBuilder withPageable(Pageable pageable) {
        this.pageable = pageable;
        return this;
    }

    public NativeSearchQueryBuilder withSourceFilter(SourceFilter sourceFilter) {
        this.sourceFilter = sourceFilter;
        return this;
    }

    public NativeSearchQueryBuilder addAggregation(AbstractAggregationBuilder aggregationBuilder) {
        this.aggregationBuilders.add(aggregationBuilder);
        return this;
    }

    public NativeSearchQuery build() {
        NativeSearchQuery nativeSearchQuery = new NativeSearchQuery(queryBuilder);
        nativeSearchQuery.setPageable(pageable);

        if (sourceFilter != null) {
            nativeSearchQuery.addSourceFilter(sourceFilter);
        }

        if (!isEmpty(aggregationBuilders)) {
            nativeSearchQuery.setAggregations(aggregationBuilders);
        }

        return nativeSearchQuery;
    }
}
