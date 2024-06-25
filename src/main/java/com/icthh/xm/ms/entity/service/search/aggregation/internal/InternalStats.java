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
package com.icthh.xm.ms.entity.service.search.aggregation.internal;

import com.icthh.xm.ms.entity.service.search.aggregation.stats.Stats;
import lombok.Getter;
import org.elasticsearch.search.DocValueFormat;

@Getter
public class InternalStats extends InternalNumericMetricsAggregation.MultiValue implements Stats {
    enum Metrics {

        count, sum, min, max, avg;

        public static Metrics resolve(String name) {
            return Metrics.valueOf(name);
        }
    }

    protected final long count;
    protected final double min;
    protected final double max;
    protected final double sum;

    public InternalStats(String name, long count, double sum, double min, double max, DocValueFormat formatter) {
        super(name);
        this.count = count;
        this.sum = sum;
        this.min = min;
        this.max = max;
        this.format = formatter;
    }

    @Override
    public double getAvg() {
        return sum / count;
    }

    @Override
    public String getMinAsString() {
        return valueAsString(Metrics.min.name());
    }

    @Override
    public String getMaxAsString() {
        return valueAsString(Metrics.max.name());
    }

    @Override
    public String getAvgAsString() {
        return valueAsString(Metrics.avg.name());
    }

    @Override
    public String getSumAsString() {
        return valueAsString(Metrics.sum.name());
    }

    @Override
    public double value(String name) {
        Metrics metrics = Metrics.valueOf(name);
        switch (metrics) {
            case min:
                return this.min;
            case max:
                return this.max;
            case avg:
                return this.getAvg();
            case count:
                return this.count;
            case sum:
                return this.sum;
            default:
                throw new IllegalArgumentException("Unknown value [" + name + "] in common stats aggregation");
        }
    }
}
