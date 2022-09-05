package com.icthh.xm.ms.entity.service.metrics;

public interface MetricsAdapter {
    /**
     * Increments the value into a counter. If counter is not exist, will be generated based on tags
     *
     * @param tags        List of tag`s pair for metrics (necessary to determine the required counter)
     * @param amount      Value for inc - optional
     * @param name        Counter name (necessary if counter not found) - optional
     * @param description Description of counter (necessary if counter not found) - optional
     */
    void inc(String[] tags, Double amount, String name, String description);

    /**
     * Increments the value into a counter. If counter is not exist, will be generated based on tag
     *
     * @param tagKey      Key for tags of metrics (necessary to determine the required counter)
     * @param tagValue    Value for tag of metrics (necessary to determine the required counter)
     * @param amount      Value for inc - optional
     * @param name        Counter name (necessary if counter not found) - optional
     * @param description Description of counter (necessary if counter not found) - optional
     */
    default void inc(String tagKey, String tagValue, Double amount, String name, String description) {
        inc(new String[]{tagKey, tagValue}, amount, name, description);
    }

    default void inc(String[] tags, String name, String description) {
        inc(tags, 1.0, name, description);
    }

    default void inc(String tagKey, String tagValue, String name, String description) {
        inc(tagKey, tagValue, 1.0, name, description);
    }

    void inc(String[] tags);

    void inc(String[] tags, Double amount);

    void inc(String tagKey, String tagValue);

    void inc(String tagKey, String tagValue, Double amount);
}
