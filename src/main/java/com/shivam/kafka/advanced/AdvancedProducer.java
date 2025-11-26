package com.shivam.kafka.advanced;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demonstrates Advanced Kafka Producer concepts: Idempotency and Transactions.
 * 
 * @author Shivam Srivastav
 */
@Service
public class AdvancedProducer {

    private static final Logger log = LoggerFactory.getLogger(AdvancedProducer.class);

    private final KafkaTemplate<String, String> advancedKafkaTemplate;

    @Autowired
    public AdvancedProducer(@Qualifier("advancedKafkaTemplate") KafkaTemplate<String, String> advancedKafkaTemplate) {
        this.advancedKafkaTemplate = advancedKafkaTemplate;
    }

    /**
     * Sends a message transactionally.
     * <p>
     * With @Transactional, the message is only committed if the method completes
     * successfully.
     * If an exception is thrown, the transaction is rolled back, and the message is
     * not visible to consumers
     * (configured with isolation.level=read_committed).
     * </p>
     */
    @Transactional
    public void sendMessageInTransaction(String message) {
        String topic = "mastery-advanced-topic";
        log.info("Sending message in transaction to '{}': {}", topic, message);

        advancedKafkaTemplate.send(topic, message);

        // Simulate some business logic
        if (message.contains("fail")) {
            throw new RuntimeException("Simulated transaction failure!");
        }
    }
}
