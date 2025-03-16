package com.swiftchat.auth_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Configuration class to disable Kafka auto-configuration when Kafka is
 * disabled.
 */
@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "false")
public class KafkaAutoConfigDisabler {

    private static final Logger logger = LoggerFactory.getLogger(KafkaAutoConfigDisabler.class);

    @Bean
    public KafkaDisablerBean kafkaDisablerBean() {
        logger.info("Kafka is disabled. Some auto-configuration will be skipped.");
        return new KafkaDisablerBean();
    }

    /**
     * Dummy bean to indicate Kafka is disabled
     */
    public static class KafkaDisablerBean {
        // This is just a marker bean
    }
}
