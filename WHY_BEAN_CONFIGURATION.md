# Why Custom Kafka Bean Configuration?

**Author:** Shivam Srivastav

## The Question
"Why do we need bean configuration? Can't we use built-in methods provided by Kafka directly?"

## Short Answer
**Yes, you CAN use Kafka's built-in methods directly**, but Spring Boot's bean configuration approach gives you:
- **Dependency Injection** - Automatic wiring
- **Multiple configurations** - Different settings for different use cases
- **Transaction management** - Spring's @Transactional support
- **Lifecycle management** - Spring handles startup/shutdown
- **Testability** - Easy mocking and testing

---

## Comparison: Native Kafka vs Spring Boot Beans

### ❌ Option 1: Using Native Kafka (Without Spring Beans)

```java
public class NativeKafkaExample {
    
    public void sendMessage(String message) {
        // You have to create Properties EVERY TIME
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9093");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        
        // Create producer manually
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        
        try {
            producer.send(new ProducerRecord<>("my-topic", message));
        } finally {
            // YOU must remember to close it!
            producer.close();
        }
    }
}
```

**Problems:**
1. ❌ **Repetitive code** - Create Properties every time
2. ❌ **Manual lifecycle** - You must close() the producer
3. ❌ **No dependency injection** - Can't autowire
4. ❌ **Hard to test** - Can't easily mock
5. ❌ **Resource leaks** - Forget to close() = memory leak
6. ❌ **No Spring features** - No @Transactional, no AOP

---

### ✅ Option 2: Using Spring Boot Beans (Our Approach)

```java
@Configuration
public class KafkaConfig {
    
    @Bean
    public ProducerFactory<String, String> basicProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, String> basicKafkaTemplate() {
        return new KafkaTemplate<>(basicProducerFactory());
    }
}

@Service
public class BasicProducer {
    
    @Autowired  // Spring injects this automatically!
    private KafkaTemplate<String, String> kafkaTemplate;
    
    public void sendMessage(String message) {
        // That's it! No setup, no cleanup
        kafkaTemplate.send("my-topic", message);
    }
}
```

**Benefits:**
1. ✅ **Write once, use everywhere** - Configuration centralized
2. ✅ **Automatic lifecycle** - Spring manages creation/destruction
3. ✅ **Dependency injection** - Just @Autowired
4. ✅ **Easy testing** - Mock the KafkaTemplate
5. ✅ **No resource leaks** - Spring handles cleanup
6. ✅ **Spring features** - @Transactional works!

---

## Real-World Scenarios: Why Beans Are Essential

### Scenario 1: Multiple Kafka Configurations

**Problem:** You need to send:
- String messages to one topic
- JSON messages to another topic
- Transactional messages to a third topic

**Without Beans (Native Kafka):**
```java
// You'd need to manage 3 different producers manually
KafkaProducer<String, String> stringProducer = new KafkaProducer<>(stringProps);
KafkaProducer<String, User> jsonProducer = new KafkaProducer<>(jsonProps);
KafkaProducer<String, String> txProducer = new KafkaProducer<>(txProps);

// And remember to close all 3!
stringProducer.close();
jsonProducer.close();
txProducer.close();
```

**With Beans (Our Approach):**
```java
@Autowired
@Qualifier("basicKafkaTemplate")
private KafkaTemplate<String, String> basicTemplate;

@Autowired
@Qualifier("userKafkaTemplate")
private KafkaTemplate<String, User> userTemplate;

@Autowired
@Qualifier("advancedKafkaTemplate")
private KafkaTemplate<String, String> advancedTemplate;

// Spring manages everything! No close() needed!
```

---

### Scenario 2: Transactions

**Problem:** You want to use `@Transactional` for atomic operations

**Without Beans:**
```java
// You CANNOT use @Transactional with native Kafka
// You must manually manage transactions:
producer.beginTransaction();
try {
    producer.send(record1);
    producer.send(record2);
    producer.commitTransaction();
} catch (Exception e) {
    producer.abortTransaction();
}
```

**With Beans:**
```java
@Transactional  // That's it! Spring handles begin/commit/rollback
public void sendMessages() {
    kafkaTemplate.send("topic", "message1");
    kafkaTemplate.send("topic", "message2");
    // If exception thrown, Spring automatically rolls back!
}
```

---

### Scenario 3: Testing

**Without Beans:**
```java
// Hard to test - tightly coupled to Kafka
public class MyService {
    public void doSomething() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9093");
        KafkaProducer producer = new KafkaProducer(props);
        producer.send(...);
        producer.close();
    }
}

// Test is difficult - need real Kafka or complex mocking
```

**With Beans:**
```java
@Service
public class MyService {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    public void doSomething() {
        kafkaTemplate.send("topic", "message");
    }
}

// Test is easy - just mock the template!
@Test
public void testDoSomething() {
    KafkaTemplate mockTemplate = mock(KafkaTemplate.class);
    MyService service = new MyService(mockTemplate);
    service.doSomething();
    verify(mockTemplate).send("topic", "message");
}
```

---

## What Spring Boot Auto-Configuration Gives You

Even with `application.properties`, Spring Boot provides **basic** configuration:

```properties
spring.kafka.bootstrap-servers=localhost:9093
spring.kafka.consumer.group-id=my-group
```

This creates a **default** `KafkaTemplate` bean automatically.

### But You STILL Need Custom Beans When:

1. **Multiple configurations** - Different serializers for different topics
2. **Transactions** - Need `KafkaTransactionManager`
3. **Error handling** - Custom `ErrorHandler` for DLQ
4. **Advanced features** - Idempotency, compression, batching
5. **Different Kafka clusters** - Connect to multiple Kafka instances

---

## Our Project: Why We Needed Custom Beans

### 1. Basic Producer Bean
```java
@Bean
public KafkaTemplate<String, String> basicKafkaTemplate() {
    return new KafkaTemplate<>(basicProducerFactory());
}
```
**Why?** Non-transactional, simple string messages

### 2. JSON Producer Bean
```java
@Bean
public KafkaTemplate<String, User> userKafkaTemplate() {
    return new KafkaTemplate<>(userProducerFactory());
}
```
**Why?** Different serializer (JsonSerializer) for User objects

### 3. Advanced Producer Bean
```java
@Bean
public KafkaTemplate<String, String> advancedKafkaTemplate() {
    return new KafkaTemplate<>(advancedProducerFactory());
}
```
**Why?** Transactional configuration with `transactional.id`

### 4. Transaction Manager Bean
```java
@Bean
public KafkaTransactionManager<String, String> kafkaTransactionManager() {
    return new KafkaTransactionManager<>(advancedProducerFactory());
}
```
**Why?** Enable `@Transactional` support for Kafka

### 5. Error Handler Bean
```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, String> advancedListenerContainerFactory() {
    // Custom error handler with retries and DLQ
    DefaultErrorHandler errorHandler = new DefaultErrorHandler(
        new DeadLetterPublishingRecoverer(kafkaTemplate),
        new FixedBackOff(1000L, 3)
    );
    factory.setCommonErrorHandler(errorHandler);
    return factory;
}
```
**Why?** Implement retry logic and Dead Letter Queue

---

## Summary Table

| Feature | Native Kafka | Spring Boot Auto-Config | Custom Beans |
|---------|-------------|------------------------|--------------|
| Basic messaging | ✅ | ✅ | ✅ |
| Dependency injection | ❌ | ✅ | ✅ |
| Multiple configs | ❌ Manual | ❌ Single default | ✅ Multiple beans |
| Transactions | ❌ Manual | ❌ | ✅ With manager |
| Error handling | ❌ Manual | ❌ Basic | ✅ Custom DLQ |
| Testing | ❌ Hard | ✅ OK | ✅ Easy |
| Lifecycle management | ❌ Manual | ✅ Auto | ✅ Auto |
| Code complexity | ❌ High | ✅ Low | ✅ Medium |

---

## The Bottom Line

**You CAN use native Kafka**, but you'll end up writing:
- More boilerplate code
- Manual resource management
- Your own transaction handling
- Your own error handling
- Your own lifecycle management

**Spring Boot beans give you:**
- Less code to write
- Automatic resource management
- Built-in transaction support
- Easy error handling
- Spring ecosystem integration

**For a learning project like ours**, custom beans are **essential** because they demonstrate:
1. How to configure different Kafka scenarios
2. How Spring manages Kafka lifecycle
3. How to integrate Kafka with Spring features (@Transactional, DI, etc.)
4. Best practices for production applications

**In production**, you'll almost always use Spring beans because:
- Easier to maintain
- Easier to test
- Easier to extend
- Follows Spring Boot conventions
- Team members already know Spring patterns

---

## Conclusion

**Native Kafka** = You're a mechanic building a car from scratch  
**Spring Boot Beans** = You're a driver using a well-designed car

Both get you to the destination, but one is much easier! 🚗
