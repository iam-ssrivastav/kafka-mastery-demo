package com.shivam.kafka.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shivam.kafka.advanced.AdvancedProducer;
import com.shivam.kafka.beginner.BasicProducer;
import com.shivam.kafka.intermediate.JsonProducer;
import com.shivam.kafka.model.User;

/**
 * REST Controller to trigger various Kafka producer scenarios.
 * 
 * @author Shivam Srivastav
 */
@RestController
@RequestMapping("/api/kafka")
public class KafkaController {

    private static final Logger log = LoggerFactory.getLogger(KafkaController.class);

    private final BasicProducer basicProducer;
    private final JsonProducer jsonProducer;
    private final AdvancedProducer advancedProducer;

    @Autowired
    public KafkaController(BasicProducer basicProducer, JsonProducer jsonProducer, AdvancedProducer advancedProducer) {
        this.basicProducer = basicProducer;
        this.jsonProducer = jsonProducer;
        this.advancedProducer = advancedProducer;
    }

    /**
     * Sends a simple string message (Beginner Concept).
     * Usage: POST /api/kafka/beginner/send?message=Hello
     */
    @PostMapping("/beginner/send")
    public String sendBasicMessage(@RequestParam String message) {
        basicProducer.sendMessage(message);
        return "Basic message sent: " + message;
    }

    /**
     * Sends a JSON User object (Intermediate Concept).
     * Usage: POST /api/kafka/intermediate/send?name=John&email=john@example.com
     */
    @PostMapping("/intermediate/send")
    public String sendJsonMessage(@RequestParam String name, @RequestParam String email) {
        User user = new User(System.currentTimeMillis(), name, email);
        jsonProducer.sendUser(user);
        return "JSON message sent for user: " + user;
    }

    /**
     * Sends a message transactionally (Advanced Concept).
     * Usage: POST /api/kafka/advanced/send?message=TransactionTest
     * To simulate failure: POST /api/kafka/advanced/send?message=fail
     * To simulate consumer error (DLQ): POST /api/kafka/advanced/send?message=error
     */
    @PostMapping("/advanced/send")
    public String sendAdvancedMessage(@RequestParam String message) {
        try {
            advancedProducer.sendMessageInTransaction(message);
            return "Advanced message sent transactionally: " + message;
        } catch (Exception e) {
            log.error("Transaction failed", e);
            return "Transaction failed: " + e.getMessage();
        }
    }
}
