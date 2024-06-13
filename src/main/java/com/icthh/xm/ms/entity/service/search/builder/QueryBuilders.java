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

package com.icthh.xm.ms.entity.service.search.builder;

import com.icthh.xm.ms.entity.service.search.enums.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;

public final class QueryBuilders {
    private QueryBuilders() {
    }

    public static QueryStringQueryBuilder queryStringQuery(String queryString) {
        return new QueryStringQueryBuilder(queryString);
    }

    public static CommonTermsQueryBuilder commonTermsQuery(String fieldName, Object text) {
        return new CommonTermsQueryBuilder(fieldName, text);
    }

    public static NestedQueryBuilder nestedQuery(String path, QueryBuilder query, ScoreMode scoreMode) {
        return new NestedQueryBuilder(path, query, scoreMode);
    }

    public static BoolQueryBuilder boolQuery() {
        return new BoolQueryBuilder();
    }
}
