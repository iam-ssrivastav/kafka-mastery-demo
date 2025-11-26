package com.shivam.kafka.intermediate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import com.shivam.kafka.model.User;

/**
 * Demonstrates sending JSON objects to Kafka.
 * 
 * @author Shivam Srivastav
 */
@Service
public class JsonProducer {

    private static final Logger log = LoggerFactory.getLogger(JsonProducer.class);

    private final KafkaTemplate<String, User> userKafkaTemplate;

    @Autowired
    public JsonProducer(@Qualifier("userKafkaTemplate") KafkaTemplate<String, User> userKafkaTemplate) {
        this.userKafkaTemplate = userKafkaTemplate;
    }

    public void sendUser(User user) {
        log.info("Sending User JSON: {}", user);

        // We can build a Spring Message to include headers if needed
        Message<User> message = MessageBuilder
                .withPayload(user)
                .setHeader(KafkaHeaders.TOPIC, "mastery-json-topic")
                .build();

        userKafkaTemplate.send(message);
    }
}
