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
    void increment(String[] tags, Double amount, String name, String description);

    /**
     * Increments the value into a counter. If counter is not exist, will be generated based on tag
     *
     * @param tagKey      Key for tags of metrics (necessary to determine the required counter)
     * @param tagValue    Value for tag of metrics (necessary to determine the required counter)
     * @param amount      Value for inc - optional
     * @param name        Counter name (necessary if counter not found) - optional
     * @param description Description of counter (necessary if counter not found) - optional
     */
    default void increment(String tagKey, String tagValue, Double amount, String name, String description) {
        increment(new String[]{tagKey, tagValue}, amount, name, description);
    }

    default void increment(String[] tags, String name, String description) {
        increment(tags, 1.0, name, description);
    }

    default void increment(String tagKey, String tagValue, String name, String description) {
        increment(tagKey, tagValue, 1.0, name, description);
    }

    void increment(String[] tags);

    void increment(String[] tags, Double amount);

    void increment(String tagKey, String tagValue);

    void increment(String tagKey, String tagValue, Double amount);
}
