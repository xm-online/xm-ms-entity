package com.icthh.xm.ms.entity.service.metrics;

import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

@Slf4j
public class MetricsAdapterImplTest extends AbstractSpringBootTest {

    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    private MetricsAdapterImpl adapter;

    @Test
    public void inc() {
        final String counterName = "defaultName";
        final String counterDescription = "Default counter";
        final String[] tags = new String[]{"foo", "bar"};
        adapter.increment(tags, 3d, counterName, counterDescription);
        var counter = meterRegistry.counter(counterName, tags);
        assertEquals(counter.count(), 3, 0.1);
        adapter.increment(tags, 2d, counterName, counterDescription);
        assertEquals(counter.count(), 5, 0.1);
        adapter.increment(tags, 1d, counterName, counterDescription);
        assertEquals(counter.count(), 6, 0.1);

        adapter.increment(tags);
        assertEquals(counter.count(), 7, 0.1);

        adapter.increment(tags[0], tags[1]);
        assertEquals(counter.count(), 8, 0.1);

        adapter.increment(tags, 3d);
        assertEquals(counter.count(), 11, 0.1);

        adapter.increment(tags[0], tags[1], 2d);
        assertEquals(counter.count(), 13, 0.1);

        adapter.increment(tags, counterName, counterDescription);
        assertEquals(counter.count(), 14, 0.1);

        adapter.increment(tags[0], tags[1], counterName, counterDescription);
        assertEquals(counter.count(), 15, 0.1);
    }

    @Test
    public void incWithException() {
        final String counterNameExp = "defaultNameExp";
        final String counterDescriptionExp = "Default counter exp";
        final String[] tags = new String[]{"bar", "foo"};
        // tags are null
        adapter.increment(null, 1d, counterNameExp, counterDescriptionExp);
        var counter = meterRegistry.counter(counterNameExp, tags);
        assertEquals(counter.count(), 0, 0.1);
        // empty tags
        adapter.increment(new String[]{}, 1d, counterNameExp, counterDescriptionExp);
        assertEquals(counter.count(), 0, 0.1);
        // amount is null
        adapter.increment(tags, null, counterNameExp, counterDescriptionExp);
        assertEquals(counter.count(), 0, 0.1);
        // name of counter is null
        adapter.increment(tags, 1d, null, counterDescriptionExp);
        assertEquals(counter.count(), 0, 0.1);
        // desc of counter is null
        adapter.increment(tags, 1d, counterNameExp, null);
        assertEquals(counter.count(), 0, 0.1);
        // tags are null
        adapter.increment(null, null, 1d, counterNameExp, counterDescriptionExp);
        assertEquals(counter.count(), 0, 0.1);
        // tagValue is null
        adapter.increment(tags[0], null, 1d, counterNameExp, counterDescriptionExp);
        assertEquals(counter.count(), 0, 0.1);
        // tagKey is null
        adapter.increment(null, tags[1], 1d, counterNameExp, counterDescriptionExp);
        assertEquals(counter.count(), 0, 0.1);
        // amount is null
        adapter.increment(tags[0], tags[1], null, counterNameExp, counterDescriptionExp);
        assertEquals(counter.count(), 0, 0.1);
        // name of counter is null
        adapter.increment(tags[0], tags[1], 1d, null, counterDescriptionExp);
        assertEquals(counter.count(), 0, 0.1);
        // desc of counter is null
        adapter.increment(tags[0], tags[1], 1d, counterNameExp, null);
        assertEquals(counter.count(), 0, 0.1);

        adapter.increment(tags, 1d, counterNameExp, counterDescriptionExp); // success
        assertEquals(counter.count(), 1, 0.1);  // success
    }
}

