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
public class MetricsAdapterImpl implements MetricsAdapter {
    private static final String DEFAULT_NAME = "defaultName";
    private static final String DEFAULT_DESC = "Default counter";
    private final MeterRegistry meterRegistry;

    @Autowired
    public MetricsAdapterImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void inc(String[] tags, Double amount, String name, String description) {
        log.debug("Inc : {}, {}, {}, {}", tags, amount, name, description);
        try {
            validation(tags, name, description, amount);
            if (tags == null || tags.length == 0) {
                throw new IllegalArgumentException("List of tags is empty");
            }
            Counter counter = register(tags, name, description);
            counter.increment(amount);
        } catch (Throwable e) {
            log.error("Metric increment failed: ", e);
        }
    }

    public void inc(String[] tags) {
        inc(tags, 1.0, DEFAULT_NAME, DEFAULT_DESC);
    }

    public void inc(String[] tags, Double amount) {
        inc(tags, amount, DEFAULT_NAME, DEFAULT_DESC);
    }

    public void inc(String tagKey, String tagValue) {
        inc(tagKey, tagValue, 1.0, DEFAULT_NAME, DEFAULT_DESC);
    }

    public void inc(String tagKey, String tagValue, Double amount) {
        inc(tagKey, tagValue, amount, DEFAULT_NAME, DEFAULT_DESC);
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

    private void validation(String tagKey, String tagValue, String name, String description, Double amount) {
        Optional.ofNullable(tagKey).filter(s -> !s.isBlank()).orElseThrow(() -> new IllegalArgumentException("Tag key is empty"));
        Optional.ofNullable(tagValue).filter(s -> !s.isBlank()).orElseThrow(() -> new IllegalArgumentException("Tag value is empty"));
        Optional.ofNullable(name).filter(s -> !s.isBlank()).orElseThrow(() -> new IllegalArgumentException("Name is empty"));
        Optional.ofNullable(description).filter(s -> !s.isBlank()).orElseThrow(() -> new IllegalArgumentException("Description is empty"));
        Optional.ofNullable(amount).orElseThrow(() -> new IllegalArgumentException("Amount is `null`"));
    }

    private void validation(String[] tags, String name, String description, Double amount) {
        Optional.ofNullable(tags).orElseThrow(() -> new IllegalArgumentException("Tags are `null`"));
        Optional.ofNullable(name).filter(s -> !s.isBlank()).orElseThrow(() -> new IllegalArgumentException("Name is empty"));
        Optional.ofNullable(description).filter(s -> !s.isBlank()).orElseThrow(() -> new IllegalArgumentException("Description is empty"));
        Optional.ofNullable(amount).orElseThrow(() -> new IllegalArgumentException("Amount is `null`"));
    }
}
