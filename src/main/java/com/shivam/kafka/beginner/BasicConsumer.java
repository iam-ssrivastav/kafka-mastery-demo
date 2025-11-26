package com.shivam.kafka.beginner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Demonstrates a basic Kafka Consumer.
 * <p>
 * This consumer listens to the "mastery-beginner-topic" and processes messages.
 * It uses the {@link KafkaListener} annotation to mark a method as a message
 * handler.
 * </p>
 * 
 * @author Shivam Srivastav
 */
@Service
public class BasicConsumer {

    private static final Logger log = LoggerFactory.getLogger(BasicConsumer.class);

    /**
     * Listens to the "mastery-beginner-topic".
     * <p>
     * groupId: Overrides the default group ID from properties if needed.
     * Here we use "beginner-group" to isolate this consumer.
     * </p>
     * 
     * @param message The received message.
     */
    @KafkaListener(topics = "mastery-beginner-topic", groupId = "beginner-group")
    public void consume(String message) {
        log.info("Received message from 'mastery-beginner-topic': {}", message);
        // In a real application, you would process the message here.
    }
}
