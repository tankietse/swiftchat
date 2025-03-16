package com.swiftchat.auth_service.config;

import com.swiftchat.auth_service.event.UserCreatedEvent;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.support.SendResult;

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
     * Creates a Kafka admin client only when Kafka is enabled.
     *
     * @return KafkaAdmin with proper bootstrap server configuration
     */
    @Bean
    @ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        logger.info("Creating KafkaAdmin with bootstrap servers: {}", bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /**
     * Creates a topic for user events only when Kafka is enabled.
     *
     * @return A new topic configuration
     */
    @Bean
    @ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
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
    @ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
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

        // Add additional connection settings
        configProps.put(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 180000);
        configProps.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1000);
        configProps.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 10000);

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
        if (!kafkaEnabled) {
            logger.info("Kafka is disabled. Creating dummy KafkaTemplate that will not send messages.");
            return createDummyKafkaTemplate();
        }

        try {
            // Validate bootstrap servers before creating the template
            if (bootstrapServers == null || bootstrapServers.trim().isEmpty()) {
                logger.warn("No bootstrap servers configured. Creating dummy KafkaTemplate.");
                return createDummyKafkaTemplate();
            }

            logger.info("Creating Kafka template with bootstrap servers: {}", bootstrapServers);

            Map<String, Object> configProps = new HashMap<>();
            configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
            configProps.put(ProducerConfig.ACKS_CONFIG, "1"); // Changed from "all" to "1" for better performance
            configProps.put(ProducerConfig.RETRIES_CONFIG, 3);

            // Add connection timeout settings
            configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000); // Reduced from 5000 to fail faster
            configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 2000); // Reduced from 3000

            // Add additional connection settings
            configProps.put(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 180000);
            configProps.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1000);
            configProps.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 10000);

            // Add error handling configuration
            configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false); // Disable idempotence for simplicity

            return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(configProps));
        } catch (Exception e) {
            logger.warn("Failed to create Kafka template: {}. Messages will not be sent to Kafka.",
                    e.getMessage());
            return createDummyKafkaTemplate();
        }
    }

    /**
     * Creates a dummy KafkaTemplate that won't throw exceptions when used
     */
    private KafkaTemplate<String, UserCreatedEvent> createDummyKafkaTemplate() {
        Map<String, Object> dummyProps = new HashMap<>();
        dummyProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        dummyProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        dummyProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Create a template with a ProducerFactory that won't actually connect
        DefaultKafkaProducerFactory<String, UserCreatedEvent> factory = new DefaultKafkaProducerFactory<>(dummyProps) {
            @Override
            public Producer<String, UserCreatedEvent> createProducer() {
                // Never actually called because we override the KafkaTemplate methods below
                return null;
            }
        };

        return new KafkaTemplate<>(factory) {
            @Override
            public CompletableFuture<SendResult<String, UserCreatedEvent>> send(String topic, UserCreatedEvent data) {
                logger.debug(
                        "Kafka disabled/unavailable: Not sending message to topic: " + topic + " with data: " + data);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<SendResult<String, UserCreatedEvent>> send(String topic, String key,
                    UserCreatedEvent data) {
                logger.debug("Kafka disabled/unavailable: Not sending message to topic: " + topic + " with key: " + key
                        + " and data: " + data);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<SendResult<String, UserCreatedEvent>> send(
                    ProducerRecord<String, UserCreatedEvent> record) {
                logger.debug("Kafka disabled/unavailable: Not sending record: " + record);
                return CompletableFuture.completedFuture(null);
            }
        };
    }
}
