/*
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

import com.icthh.xm.ms.entity.service.search.query.dto.NativeSearchQuery;
import com.icthh.xm.ms.entity.service.search.filter.SourceFilter;
import org.springframework.data.domain.Pageable;

public class NativeSearchQueryBuilder {

    private QueryBuilder queryBuilder;
    private Pageable pageable = Pageable.unpaged();
    private SourceFilter sourceFilter;

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

    public NativeSearchQuery build() {
        NativeSearchQuery nativeSearchQuery = new NativeSearchQuery(queryBuilder);
        nativeSearchQuery.setPageable(pageable);

        if (sourceFilter != null) {
            nativeSearchQuery.addSourceFilter(sourceFilter);
        }

        return nativeSearchQuery;
    }
}
