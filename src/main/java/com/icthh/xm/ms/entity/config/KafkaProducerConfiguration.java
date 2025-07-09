package com.icthh.xm.ms.entity.config;

import brave.kafka.clients.KafkaTracing;
import com.icthh.xm.ms.entity.listener.TracingKafkaProducerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.kafka.support.converter.RecordMessageConverter;

@Configuration
public class KafkaProducerConfiguration {

    @Bean
    public TracingKafkaProducerFactory<?, ?> kafkaProducerFactory(KafkaProperties properties,
        KafkaTracing kafkaTracing) {
        TracingKafkaProducerFactory<?, ?> factory = new TracingKafkaProducerFactory<>(
            properties.buildProducerProperties(), kafkaTracing);
        String transactionIdPrefix = properties.getProducer().getTransactionIdPrefix();
        if (transactionIdPrefix != null) {
            factory.setTransactionIdPrefix(transactionIdPrefix);
        }

        return factory;
    }

    @Bean
    public KafkaTemplate<?, ?> kafkaTemplate(
        KafkaProperties properties,
        TracingKafkaProducerFactory<Object, Object> kafkaProducerFactory,
        ProducerListener<Object, Object> kafkaProducerListener,
        ObjectProvider<RecordMessageConverter> messageConverterProvider) {
        RecordMessageConverter messageConverter = messageConverterProvider.getIfUnique();
        KafkaTemplate<Object, Object> kafkaTemplate = new KafkaTemplate<>(kafkaProducerFactory);
        if (messageConverter != null) {
            kafkaTemplate.setMessageConverter(messageConverter);
        }
        kafkaTemplate.setProducerListener(kafkaProducerListener);
        kafkaTemplate.setDefaultTopic(properties.getTemplate().getDefaultTopic());
        return kafkaTemplate;
    }
}
