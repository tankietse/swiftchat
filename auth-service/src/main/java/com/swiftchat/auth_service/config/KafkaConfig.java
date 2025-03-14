package com.swiftchat.auth_service.config;

import com.swiftchat.auth_service.event.UserCreatedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for messaging.
 */
@Configuration
public class KafkaConfig {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.enabled:true}")
    private boolean kafkaEnabled;

    private static final String TOPIC_USER_EVENTS = "user-events";
    private static final int DEFAULT_PARTITIONS = 3;
    private static final short DEFAULT_REPLICATION_FACTOR = 1;

    /**
     * Creates a topic for user events.
     *
     * @return A new topic configuration
     */
    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name(TOPIC_USER_EVENTS)
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICATION_FACTOR)
                .build();
    }

    /**
     * Configures producer factory for Kafka templates.
     *
     * @return ProducerFactory with proper settings
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);

        // Add connection timeout settings
        configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);

        logger.info("Configuring Kafka producer with bootstrap servers: {}", bootstrapServers);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Creates a KafkaTemplate for sending UserCreatedEvents.
     *
     * @return KafkaTemplate for UserCreatedEvents
     */
    @Bean
    public KafkaTemplate<String, UserCreatedEvent> userEventKafkaTemplate() {
        try {
            return new KafkaTemplate<>(
                    new DefaultKafkaProducerFactory<>(producerFactory().getConfigurationProperties()));
        } catch (KafkaException e) {
            logger.warn("Failed to create Kafka template: {}. Messages will not be sent to Kafka.",
                    e.getMessage());
            // Return a null-safe template that won't break the application if Kafka is
            // unavailable
            return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(new HashMap<>()));
        }
    }
}
