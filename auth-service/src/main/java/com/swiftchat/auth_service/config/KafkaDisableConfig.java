package com.swiftchat.auth_service.config;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Configuration to disable Kafka when not needed or not available in
 * development environment.
 */
@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "false")
public class KafkaDisableConfig {
    private static final Logger logger = LoggerFactory.getLogger(KafkaDisableConfig.class);

    @Bean
    @Primary
    public <K, V> KafkaTemplate<K, V> disabledKafkaTemplate() {
        logger.info("Kafka is disabled. Creating dummy KafkaTemplate that will not send messages.");

        // Create an empty producer factory that doesn't actually create producers
        ProducerFactory<K, V> factory = new DefaultKafkaProducerFactory<>(new HashMap<>()) {
            @Override
            public Producer<K, V> createProducer() {
                return null; // Never actually called because we override KafkaTemplate methods
            }
        };

        return new KafkaTemplate<K, V>(factory) {
            @Override
            public CompletableFuture<SendResult<K, V>> send(String topic, V data) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Kafka disabled: Not sending message to topic: " + topic + " with data: " + data);
                }
                // Return a completed future instead of an exceptionally completed one
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<SendResult<K, V>> send(String topic, K key, V data) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Kafka disabled: Not sending message to topic: " + topic +
                            " with key: " + key + " and data: " + data);
                }
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<SendResult<K, V>> send(ProducerRecord<K, V> record) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Kafka disabled: Not sending record: " + record);
                }
                return CompletableFuture.completedFuture(null);
            }
        };
    }
}
