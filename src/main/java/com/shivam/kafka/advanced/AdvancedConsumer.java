package com.shivam.kafka.advanced;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Demonstrates Advanced Kafka Consumer concepts: Retries and Dead Letter Queue
 * (DLQ).
 * 
 * @author Shivam Srivastav
 */
@Service
public class AdvancedConsumer {

    private static final Logger log = LoggerFactory.getLogger(AdvancedConsumer.class);

    /**
     * Consumes messages with a custom error handler (Retries + DLQ).
     * <p>
     * If processing fails, it will retry 3 times (as configured in KafkaConfig).
     * If it still fails, the message is sent to "mastery-advanced-topic.DLT".
     * </p>
     */
    @KafkaListener(topics = "mastery-advanced-topic", groupId = "advanced-group", containerFactory = "advancedListenerContainerFactory")
    public void consume(String message) {
        log.info("Received message in AdvancedConsumer: {}", message);

        if (message.contains("error")) {
            throw new RuntimeException("Simulated processing error!");
        }

        log.info("Successfully processed message: {}", message);
    }

    /**
     * Listens to the Dead Letter Topic (DLT).
     * This is where failed messages end up.
     */
    @KafkaListener(topics = "mastery-advanced-topic.DLT", groupId = "dlt-group")
    public void consumeDLT(String message) {
        log.warn("Received message in DLT: {}", message);
        // Alerting or manual intervention logic goes here
    }
}
