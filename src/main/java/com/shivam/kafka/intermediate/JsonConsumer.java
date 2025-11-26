package com.shivam.kafka.intermediate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.shivam.kafka.model.User;

/**
 * Demonstrates consuming JSON objects from Kafka.
 * 
 * @author Shivam Srivastav
 */
@Service
public class JsonConsumer {

    private static final Logger log = LoggerFactory.getLogger(JsonConsumer.class);

    /**
     * Consumes User objects.
     * We specify the containerFactory to use our custom JSON-configured factory.
     */
    @KafkaListener(topics = "mastery-json-topic", groupId = "json-group", containerFactory = "userKafkaListenerContainerFactory")
    public void consumeUser(User user) {
        log.info("Received User JSON: {}", user);
        // Process user...
    }
}
