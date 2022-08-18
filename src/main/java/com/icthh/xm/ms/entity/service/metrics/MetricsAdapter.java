package com.icthh.xm.ms.entity.service.metrics;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class MetricsAdapter {
    private static final String DEFAULT_NAME = "defaultName";
    private static final String DEFAULT_DESC = "Default counter";
    private final MeterRegistry meterRegistry;

    @Autowired
    public MetricsAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Increments the value into a counter. If counter is not exist, will be generated based on tags
     *
     * @param tags        List of tags for metrics (necessary to determine the required counter)
     * @param amount      Value for inc - optional
     * @param name        Counter name (necessary if counter not found) - optional
     * @param description Description of counter (necessary if counter not found) - optional
     */
    public void inc(String[] tags, Double amount, String name, String description) {
        log.debug("Inc : {}, {}, {}, {}", tags, amount, name, description);
        try {
            validation(tags, amount, name, description);
            if (tags == null || tags.length == 0) {
                throw new IllegalArgumentException("List of tags is empty");
            }
            Counter counter = register(tags, name, description);
            counter.increment(amount);
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
    public void inc(Tag tag, Double amount, String name, String description) {
        log.debug("Inc : {}, {}, {}, {}", tag, amount, name, description);
        try {
            validation(tag, amount, name, description);
            Counter counter = register(tag, name, description);
            counter.increment(amount);
        } catch (Throwable e) {
            log.error("Metric increment failed: ", e);
        }
    }

    public void inc(String[] tags, String name, String description) {
        inc(tags, 1.0, name, description);
    }

    public void inc(String[] tags) {
        inc(tags, 1.0, DEFAULT_NAME, DEFAULT_DESC);
    }

    public void inc(String[] tags, Double amount) {
        inc(tags, amount, DEFAULT_NAME, DEFAULT_DESC);
    }

    public void inc(Tag tag, String name, String description) {
        inc(tag, 1.0, name, description);
    }

    public void inc(Tag tag) {
        inc(tag, 1.0, DEFAULT_NAME, DEFAULT_DESC);
    }

    public void inc(Tag tag, Double amount) {
        inc(tag, amount, DEFAULT_NAME, DEFAULT_DESC);
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
        return Counter.builder(name).description(description).tags(tags).register(meterRegistry);
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
        return Counter.builder(name).description(description).tag(tag.getKey(), tag.getValue()).register(meterRegistry);
    }

    private void validation(Object tags, Double amount, String name, String description) {
        Optional.ofNullable(tags).orElseThrow(() -> new IllegalArgumentException("Tag is `null`"));
        Optional.ofNullable(amount).orElseThrow(() -> new IllegalArgumentException("Amount is `null`"));
        Optional.ofNullable(name).filter(s -> !s.isBlank()).orElseThrow(() -> new IllegalArgumentException("Name is empty"));
        Optional.ofNullable(description).filter(s -> !s.isBlank()).orElseThrow(() -> new IllegalArgumentException("Description is empty"));
    }
}
