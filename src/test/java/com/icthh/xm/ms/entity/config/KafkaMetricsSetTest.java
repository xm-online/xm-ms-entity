package com.icthh.xm.ms.entity.config;

import lombok.SneakyThrows;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class KafkaMetricsSetTest {

    @MockBean
    private ApplicationProperties applicationProperties;

    @MockBean
    private KafkaAdmin kafkaAdmin;

    private static KafkaEmbedded kafkaEmbedded;

    private Map<String, Object> mockConfig = new HashMap<>();

    @BeforeClass
    @SneakyThrows
    public static void setupAtOnce() {
        kafkaEmbedded = new KafkaEmbedded(1, true, "test_topic");
        kafkaEmbedded.setKafkaPorts(9092);
        kafkaEmbedded.before();
    }

    @Test
    public void connectionToKafkaIsSuccess() {
        KafkaMetricsSet kafkaMetricsSet = initKafkaMetricSet();
        assertTrue(kafkaMetricsSet.connectionToKafkaIsSuccess());
    }

    @Test
    @SneakyThrows
    public void connectionToKafkaNotSuccess() {
        KafkaMetricsSet kafkaMetricsSet = initKafkaMetricSet();
        kafkaEmbedded.destroy();
        assertFalse(kafkaMetricsSet.connectionToKafkaIsSuccess());
    }

    private KafkaMetricsSet initKafkaMetricSet() {
        mockConfig.put("bootstrap.servers", "localhost:9092");
        when(applicationProperties.getKafkaSystemTopic()).thenReturn("test_topic");
        when(applicationProperties.getConnectionTimeoutTopic()).thenReturn(1000);
        when(kafkaAdmin.getConfig()).thenReturn(mockConfig);
        return new KafkaMetricsSet(kafkaAdmin, applicationProperties);
    }
}
