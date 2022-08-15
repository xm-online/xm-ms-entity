package com.icthh.xm.ms.entity.service.metrics;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.LRUMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class MetricsAdapter {
    private static final int MAX_SIZE_CACHE = 1000;
    private static final String DEFAULT_NAME = "defaultName";
    private static final String DEFAULT_DESC = "Default counter";
    private final Map<String, Counter> countersCache;
    private final MeterRegistry meterRegistry;

    public MetricsAdapter(Map<String, Counter> countersCache, MeterRegistry meterRegistry) {
        this.countersCache = Optional.ofNullable(countersCache).orElse(new LRUMap<>(MAX_SIZE_CACHE));
        this.meterRegistry = meterRegistry;
    }

    @Autowired
    public MetricsAdapter(MeterRegistry meterRegistry) {
        this(new LRUMap<>(MAX_SIZE_CACHE), meterRegistry);
    }

    /**
     * Increments the value into a counter. If counter is not exist, will be generated based on tags
     *
     * @param tags        List of tags for metrics (necessary to determine the required counter)
     * @param amount      Value for inc - optional
     * @param name        Counter name (necessary if counter not found) - optional
     * @param description Description of counter (necessary if counter not found) - optional
     */
    public void increment(String[] tags, Double amount, String name, String description) {
        log.debug("Inc : {}, {}, {}, {}", tags, amount, name, description);
        try {
            validation(tags, amount, name, description);
            if (tags == null || tags.length == 0) {
                throw new IllegalArgumentException("List of tags is empty");
            }
            String key = buildUniqueKey(tags, name);
            Counter counter = countersCache.get(key);
            if (counter == null) {
                counter = register(tags, name, description);
            }
            counter.increment(amount);
            log.debug("Got counter by key {} after inc", key);
        } catch (Throwable e) {
            log.error("Metric increment failed: ", e);
        }
    }

    /**
     * Increments the value into a counter. If counter is not exist, will be generated based on tag
     *
     * @param tag         List of tags for metrics (necessary to determine the required counter)
     * @param amount      Value for inc - optional
     * @param name        Counter name (necessary if counter not found) - optional
     * @param description Description of counter (necessary if counter not found) - optional
     */
    public void increment(Tag tag, Double amount, String name, String description) {
        log.debug("Inc : {}, {}, {}, {}", tag, amount, name, description);
        try {
            validation(tag, amount, name, description);
            var key = buildUniqueKey(tag, name);
            Counter counter = countersCache.get(key);
            if (counter == null) {
                counter = register(tag, name, description);
            }
            counter.increment(amount);
            log.debug("Got counter by key {} after inc", key);
        } catch (Throwable e) {
            log.error("Metric increment failed: ", e);
        }
    }

    public void increment(String[] tags, String name, String description) {
        increment(tags, 1.0, name, description);
    }

    public void increment(String[] tags) {
        increment(tags, 1.0, DEFAULT_NAME, DEFAULT_DESC);
    }

    public void increment(String[] tags, Double amount) {
        increment(tags, amount, DEFAULT_NAME, DEFAULT_DESC);
    }

    public void increment(Tag tag, String name, String description) {
        increment(tag, 1.0, name, description);
    }

    public void increment(Tag tag) {
        increment(tag, 1.0, DEFAULT_NAME, DEFAULT_DESC);
    }

    public void increment(Tag tag, Double amount) {
        increment(tag, amount, DEFAULT_NAME, DEFAULT_DESC);
    }

    /**
     * Registers counter to collect metrics
     *
     * @param tags        List of tags (pair of key\value) for metrics
     * @param name        Counter name
     * @param description Description of counter
     * @return Created counter
     */
    private Counter register(String[] tags, String name, String description) {
        log.debug("Reg : {}, {}, {}", tags, name, description);
        String key = buildUniqueKey(tags, name);
        var counter = countersCache.get(key);
        if (counter == null) {
            counter = Counter.builder(name).description(description).tags(tags).register(meterRegistry);
            countersCache.put(key, counter);
        }
        log.debug("Got counter by key {}", key);
        return counter;
    }

    /**
     * Registers counter to collect metrics
     *
     * @param tag         Tag with key and value
     * @param name        Counter name
     * @param description Description of counter
     * @return Created counter
     */
    private Counter register(Tag tag, String name, String description) {
        log.debug("Reg : {}, {}, {}", tag, name, description);
        var key = buildUniqueKey(tag, name);
        var counter = countersCache.get(key);
        if (counter == null) {
            counter = Counter.builder(name).description(description).tag(tag.getKey(), tag.getValue()).register(meterRegistry);
            countersCache.put(key, counter);
        }
        log.debug("Got counter by key {}", key);
        return counter;
    }

    private static String buildUniqueKey(String[] tags, String name) {
        return String.join(",", tags) + "_" + name;
    }

    private static String buildUniqueKey(Tag tag, String name) {
        return tag.toString() + "_" + name;
    }

    private void validation(Object tags, Double amount, String name, String description) {
        Optional.ofNullable(tags).orElseThrow(() -> new IllegalArgumentException("Tag is `null`"));
        Optional.ofNullable(amount).orElseThrow(() -> new IllegalArgumentException("Amount is `null`"));
        Optional.ofNullable(name).filter(s -> !s.isBlank()).orElseThrow(() -> new IllegalArgumentException("Name is empty"));
        Optional.ofNullable(description).filter(s -> !s.isBlank()).orElseThrow(() -> new IllegalArgumentException("Description is empty"));
    }
}
