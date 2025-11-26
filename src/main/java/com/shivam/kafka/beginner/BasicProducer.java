package com.shivam.kafka.beginner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Demonstrates a basic Kafka Producer.
 * <p>
 * This producer sends simple String messages to a Kafka topic.
 * It uses the {@link KafkaTemplate} provided by Spring Kafka.
 * </p>
 * 
 * @author Shivam Srivastav
 */
@Service
public class BasicProducer {

    private static final Logger log = LoggerFactory.getLogger(BasicProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public BasicProducer(
            @org.springframework.beans.factory.annotation.Qualifier("basicKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a message to the "mastery-beginner-topic".
     * 
     * @param message The message content to send.
     */
    public void sendMessage(String message) {
        String topicName = "mastery-beginner-topic";
        log.info("Sending message to topic '{}': {}", topicName, message);

        // The send() method is asynchronous. It returns a CompletableFuture.
        // For simple use cases, we can just call it and let it run in the background.
        kafkaTemplate.send(topicName, message);
    }
}
