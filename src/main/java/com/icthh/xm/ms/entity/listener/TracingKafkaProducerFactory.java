package com.icthh.xm.ms.entity.listener;

import brave.kafka.clients.KafkaTracing;
import java.util.Map;
import org.apache.kafka.clients.producer.Producer;
import org.jetbrains.annotations.NotNull;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;

public class TracingKafkaProducerFactory<K, V> extends DefaultKafkaProducerFactory<K, V> {


    private final KafkaTracing kafkaTracing;

    public TracingKafkaProducerFactory(Map<String, Object> props, KafkaTracing kafkaTracing) {
        super(props);
        this.kafkaTracing = kafkaTracing;
    }

    @NotNull
    @Override
    public Producer<K, V> createProducer() {
        Producer<K, V> producer = super.createProducer();
        return kafkaTracing.producer(producer);
    }
}
