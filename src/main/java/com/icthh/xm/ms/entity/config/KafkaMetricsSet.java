package com.icthh.xm.ms.entity.config;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.util.Objects.nonNull;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty("application.kafka-enabled")
public class KafkaMetricsSet implements MetricSet {

    private final KafkaAdmin kafkaAdmin;
    private final ApplicationProperties applicationProperties;

    @Override
    public Map<String, Metric> getMetrics() {
        Map<String, Metric> metrics = new HashMap<>();
        metrics.put("kafka.connection.success", (Gauge) () -> {
            String topicName = applicationProperties.getKafkaSystemTopic();
            DescribeTopicsOptions describeTopicsOptions = new DescribeTopicsOptions().timeoutMs(
                applicationProperties.getConnectionTimeoutTopic());
            try(AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfig())) {
                try {
                    DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(
                        asList(topicName),
                        describeTopicsOptions);
                    Map<String, TopicDescription> topicDescriptionMap = describeTopicsResult.all().get();
                    //TODO after test delete this log
                    log.info("result topicDescriptionMap: {}", topicDescriptionMap);
                    return nonNull(topicDescriptionMap);
                } catch (InterruptedException | ExecutionException e) {
                    log.warn("Exception when try connect to kafka topic: {}, exception: {}", topicName, e);
                    return false;
                }
            }
        });
        return metrics;
    }

}
