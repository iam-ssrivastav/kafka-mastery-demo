# Kafka Mastery - Architecture Flow Diagrams

**Author:** Shivam Srivastav

---

## Overall System Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        Client[REST Client/curl]
    end
    
    subgraph "Spring Boot Application :8080"
        Controller[KafkaController<br/>REST Endpoints]
        
        subgraph "Beginner Level"
            BP[BasicProducer]
            BC[BasicConsumer]
        end
        
        subgraph "Intermediate Level"
            JP[JsonProducer]
            JC[JsonConsumer]
        end
        
        subgraph "Advanced Level"
            AP[AdvancedProducer<br/>@Transactional]
            AC[AdvancedConsumer<br/>Retry + DLQ]
            DLT[DLT Consumer]
        end
        
        subgraph "Configuration Layer"
            Config[KafkaConfig<br/>Bean Definitions]
            BTmpl[basicKafkaTemplate]
            UTmpl[userKafkaTemplate]
            ATmpl[advancedKafkaTemplate]
            TxMgr[KafkaTransactionManager]
        end
    end
    
    subgraph "Kafka Cluster :9093"
        subgraph "Topics"
            T1[mastery-beginner-topic<br/>3 partitions]
            T2[mastery-json-topic<br/>3 partitions]
            T3[mastery-advanced-topic<br/>3 partitions]
            T4[mastery-advanced-topic.DLT<br/>Dead Letter Topic]
        end
    end
    
    Client -->|HTTP POST| Controller
    Controller --> BP
    Controller --> JP
    Controller --> AP
    
    Config -.->|provides| BTmpl
    Config -.->|provides| UTmpl
    Config -.->|provides| ATmpl
    Config -.->|provides| TxMgr
    
    BP -->|uses| BTmpl
    JP -->|uses| UTmpl
    AP -->|uses| ATmpl
    AP -->|uses| TxMgr
    
    BTmpl -->|String| T1
    UTmpl -->|JSON| T2
    ATmpl -->|Transactional| T3
    
    T1 -->|consume| BC
    T2 -->|consume| JC
    T3 -->|consume| AC
    AC -->|on failure| T4
    T4 -->|consume| DLT
    
    style Client fill:#e1f5ff
    style Controller fill:#fff4e1
    style Config fill:#f0f0f0
    style T1 fill:#c8e6c9
    style T2 fill:#c8e6c9
    style T3 fill:#c8e6c9
    style T4 fill:#ffcdd2
```

---

## Flow 1: Beginner Level - Basic String Messaging

```mermaid
sequenceDiagram
    participant Client
    participant Controller as KafkaController
    participant Producer as BasicProducer
    participant Template as basicKafkaTemplate
    participant Topic as mastery-beginner-topic
    participant Consumer as BasicConsumer
    
    Client->>Controller: POST /api/kafka/beginner/send?message=Hello
    Controller->>Producer: sendMessage("Hello")
    Producer->>Template: send("mastery-beginner-topic", "Hello")
    Template->>Topic: Publish String message
    Note over Template,Topic: StringSerializer
    Topic-->>Template: Ack
    Template-->>Producer: Success
    Producer-->>Controller: void
    Controller-->>Client: "Basic message sent: Hello"
    
    Topic->>Consumer: Poll messages
    Note over Topic,Consumer: StringDeserializer
    Consumer->>Consumer: log.info("Received: Hello")
    Consumer-->>Topic: Commit offset
```

**Key Components:**
- **Serialization:** String → Bytes
- **Deserialization:** Bytes → String
- **Consumer Group:** `beginner-group`
- **Partitions:** 3 (for parallel processing)

---

## Flow 2: Intermediate Level - JSON Serialization

```mermaid
sequenceDiagram
    participant Client
    participant Controller as KafkaController
    participant Producer as JsonProducer
    participant Template as userKafkaTemplate
    participant Topic as mastery-json-topic
    participant Consumer as JsonConsumer
    
    Client->>Controller: POST /api/kafka/intermediate/send<br/>?name=John&email=john@example.com
    Controller->>Controller: new User(id, "John", "john@example.com")
    Controller->>Producer: sendUser(user)
    Producer->>Template: send(Message<User>)
    Note over Producer,Template: MessageBuilder with headers
    Template->>Topic: Publish JSON
    Note over Template,Topic: JsonSerializer<br/>User → JSON → Bytes
    Topic-->>Template: Ack
    Template-->>Producer: Success
    Producer-->>Controller: void
    Controller-->>Client: "JSON message sent for user: User{...}"
    
    Topic->>Consumer: Poll messages
    Note over Topic,Consumer: JsonDeserializer<br/>Bytes → JSON → User
    Consumer->>Consumer: log.info("Received User: {}", user)
    Consumer-->>Topic: Commit offset
```

**Key Components:**
- **Serialization:** User Object → JSON → Bytes
- **Deserialization:** Bytes → JSON → User Object
- **Consumer Group:** `json-group`
- **Custom Factory:** `userKafkaListenerContainerFactory`

---

## Flow 3: Advanced Level - Transactional Success

```mermaid
sequenceDiagram
    participant Client
    participant Controller as KafkaController
    participant Producer as AdvancedProducer<br/>@Transactional
    participant TxMgr as KafkaTransactionManager
    participant Template as advancedKafkaTemplate
    participant Topic as mastery-advanced-topic
    participant Consumer as AdvancedConsumer
    
    Client->>Controller: POST /api/kafka/advanced/send<br/>?message=Success
    Controller->>Producer: sendMessageInTransaction("Success")
    Producer->>TxMgr: Begin Transaction
    TxMgr->>Template: beginTransaction()
    Template-->>TxMgr: Transaction started
    TxMgr-->>Producer: Transaction context
    
    Producer->>Template: send("mastery-advanced-topic", "Success")
    Template->>Topic: Publish (uncommitted)
    Note over Template,Topic: Message buffered,<br/>not visible to consumers
    Topic-->>Template: Ack (buffered)
    
    Producer->>Producer: Business logic executes
    Note over Producer: No exception thrown
    
    Producer->>TxMgr: Commit Transaction
    TxMgr->>Template: commitTransaction()
    Template->>Topic: Commit all messages
    Note over Template,Topic: Messages now visible<br/>to consumers
    Topic-->>Template: Commit Ack
    Template-->>TxMgr: Committed
    TxMgr-->>Producer: Success
    Producer-->>Controller: void
    Controller-->>Client: "Advanced message sent transactionally"
    
    Topic->>Consumer: Poll messages
    Consumer->>Consumer: log.info("Received: Success")
    Consumer->>Consumer: Process successfully
    Consumer-->>Topic: Commit offset
```

**Key Components:**
- **Idempotency:** `enable.idempotence=true`
- **Transaction ID:** `transactional.id=tx-`
- **Isolation:** Messages invisible until commit
- **Atomicity:** All or nothing

---

## Flow 4: Advanced Level - Transaction Rollback

```mermaid
sequenceDiagram
    participant Client
    participant Controller as KafkaController
    participant Producer as AdvancedProducer<br/>@Transactional
    participant TxMgr as KafkaTransactionManager
    participant Template as advancedKafkaTemplate
    participant Topic as mastery-advanced-topic
    
    Client->>Controller: POST /api/kafka/advanced/send<br/>?message=fail
    Controller->>Producer: sendMessageInTransaction("fail")
    Producer->>TxMgr: Begin Transaction
    TxMgr->>Template: beginTransaction()
    Template-->>TxMgr: Transaction started
    
    Producer->>Template: send("mastery-advanced-topic", "fail")
    Template->>Topic: Publish (uncommitted)
    Note over Template,Topic: Message buffered
    Topic-->>Template: Ack (buffered)
    
    Producer->>Producer: if (message.contains("fail"))
    Producer->>Producer: throw RuntimeException
    Note over Producer: Exception thrown!
    
    Producer->>TxMgr: Rollback Transaction (automatic)
    TxMgr->>Template: abortTransaction()
    Template->>Topic: Abort - discard messages
    Note over Template,Topic: Buffered messages<br/>DISCARDED
    Topic-->>Template: Abort Ack
    Template-->>TxMgr: Rolled back
    TxMgr-->>Producer: Exception propagated
    Producer-->>Controller: RuntimeException
    Controller->>Controller: catch (Exception e)
    Controller-->>Client: "Transaction failed: Simulated transaction failure!"
    
    Note over Topic: Consumer NEVER sees<br/>the "fail" message
```

**Key Points:**
- ❌ Exception thrown → Transaction rolled back
- ❌ Message never committed to Kafka
- ❌ Consumer never receives the message
- ✅ Atomicity guaranteed

---

## Flow 5: Advanced Level - Dead Letter Queue (DLQ)

```mermaid
sequenceDiagram
    participant Client
    participant Controller as KafkaController
    participant Producer as AdvancedProducer
    participant Topic as mastery-advanced-topic
    participant Consumer as AdvancedConsumer
    participant ErrorHandler as DefaultErrorHandler
    participant DLT as mastery-advanced-topic.DLT
    participant DLTConsumer as DLT Consumer
    
    Client->>Controller: POST /api/kafka/advanced/send<br/>?message=error
    Controller->>Producer: sendMessageInTransaction("error")
    Producer->>Topic: Publish "error"
    Topic-->>Producer: Ack
    Producer-->>Controller: Success
    Controller-->>Client: "Advanced message sent transactionally: error"
    
    Topic->>Consumer: Poll message "error"
    Consumer->>Consumer: if (message.contains("error"))
    Consumer->>Consumer: throw RuntimeException
    Note over Consumer: Processing failed!
    Consumer-->>ErrorHandler: Exception thrown
    
    Note over ErrorHandler: Retry 1 of 3
    ErrorHandler->>ErrorHandler: Wait 1 second (FixedBackOff)
    ErrorHandler->>Consumer: Retry processing
    Consumer->>Consumer: throw RuntimeException
    Note over Consumer: Still fails!
    Consumer-->>ErrorHandler: Exception thrown
    
    Note over ErrorHandler: Retry 2 of 3
    ErrorHandler->>ErrorHandler: Wait 1 second
    ErrorHandler->>Consumer: Retry processing
    Consumer->>Consumer: throw RuntimeException
    Consumer-->>ErrorHandler: Exception thrown
    
    Note over ErrorHandler: Retry 3 of 3 (Final)
    ErrorHandler->>ErrorHandler: Wait 1 second
    ErrorHandler->>Consumer: Retry processing
    Consumer->>Consumer: throw RuntimeException
    Consumer-->>ErrorHandler: Exception thrown
    
    Note over ErrorHandler: All retries exhausted!
    ErrorHandler->>ErrorHandler: DeadLetterPublishingRecoverer
    ErrorHandler->>DLT: Publish to DLT topic
    Note over DLT: Original message + exception info
    DLT-->>ErrorHandler: Ack
    ErrorHandler-->>Topic: Commit offset (message handled)
    
    DLT->>DLTConsumer: Poll failed message
    DLTConsumer->>DLTConsumer: log.warn("Received in DLT: error")
    Note over DLTConsumer: Manual intervention needed
    DLTConsumer-->>DLT: Commit offset
```

**Key Components:**
- **Error Handler:** `DefaultErrorHandler`
- **Retry Strategy:** `FixedBackOff(1000L, 3)` - 3 retries, 1s apart
- **Recoverer:** `DeadLetterPublishingRecoverer`
- **DLT Topic:** Auto-created as `{original-topic}.DLT`

---

## Component Dependency Graph

```mermaid
graph LR
    subgraph "Spring Beans"
        Config[KafkaConfig]
        
        BPF[basicProducerFactory]
        BKT[basicKafkaTemplate]
        
        UPF[userProducerFactory]
        UKT[userKafkaTemplate]
        UCF[userConsumerFactory]
        UCLF[userKafkaListenerContainerFactory]
        
        APF[advancedProducerFactory]
        AKT[advancedKafkaTemplate]
        TxMgr[kafkaTransactionManager]
        ACLF[advancedListenerContainerFactory]
    end
    
    subgraph "Producers"
        BP[BasicProducer]
        JP[JsonProducer]
        AP[AdvancedProducer]
    end
    
    subgraph "Consumers"
        BC[BasicConsumer]
        JC[JsonConsumer]
        AC[AdvancedConsumer]
        DLT[DLT Consumer]
    end
    
    Config -->|creates| BPF
    Config -->|creates| UPF
    Config -->|creates| APF
    
    BPF -->|used by| BKT
    UPF -->|used by| UKT
    APF -->|used by| AKT
    APF -->|used by| TxMgr
    
    UCF -->|used by| UCLF
    
    BP -->|@Autowired| BKT
    JP -->|@Autowired| UKT
    AP -->|@Autowired| AKT
    AP -->|@Autowired| TxMgr
    
    BC -.->|default factory| Config
    JC -->|containerFactory| UCLF
    AC -->|containerFactory| ACLF
    DLT -.->|default factory| Config
    
    style Config fill:#f0f0f0
    style TxMgr fill:#ffe0b2
    style ACLF fill:#ffe0b2
```

---

## Data Flow Summary

### Beginner Flow
```
Client → Controller → BasicProducer → basicKafkaTemplate 
→ mastery-beginner-topic → BasicConsumer
```

### Intermediate Flow
```
Client → Controller → JsonProducer → userKafkaTemplate 
→ mastery-json-topic → JsonConsumer
```

### Advanced Flow (Success)
```
Client → Controller → AdvancedProducer → @Transactional 
→ advancedKafkaTemplate → mastery-advanced-topic 
→ AdvancedConsumer
```

### Advanced Flow (Rollback)
```
Client → Controller → AdvancedProducer → @Transactional 
→ Exception → Rollback → Consumer never sees message
```

### Advanced Flow (DLQ)
```
Client → Controller → AdvancedProducer → mastery-advanced-topic 
→ AdvancedConsumer → Fails → Retry 3x → DLT 
→ DLT Consumer
```

---

## Technology Stack

```mermaid
graph TB
    subgraph "Infrastructure"
        Docker[Docker Compose]
        ZK[Zookeeper :2181]
        Kafka[Kafka Broker :9093]
    end
    
    subgraph "Application"
        SB[Spring Boot 3.2.3]
        SK[Spring Kafka 3.1.2]
        Java[Java 17]
    end
    
    subgraph "Serialization"
        SS[StringSerializer]
        JS[JsonSerializer]
        SD[StringDeserializer]
        JD[JsonDeserializer]
    end
    
    Docker --> ZK
    Docker --> Kafka
    ZK -.->|coordinates| Kafka
    
    SB --> SK
    SB --> Java
    
    SK --> SS
    SK --> JS
    SK --> SD
    SK --> JD
```

---

## All Concepts Covered

| Level | Concept | Implementation |
|-------|---------|----------------|
| **Beginner** | Basic Producer | `BasicProducer.java` |
| | Basic Consumer | `BasicConsumer.java` |
| | String Serialization | `StringSerializer` |
| | Topic Configuration | `@Bean NewTopic` |
| **Intermediate** | JSON Serialization | `JsonSerializer` |
| | JSON Deserialization | `JsonDeserializer` |
| | Custom Factories | `userProducerFactory()` |
| | Consumer Groups | `groupId="json-group"` |
| **Advanced** | Idempotent Producer | `enable.idempotence=true` |
| | Transactions | `@Transactional` |
| | Transaction Manager | `KafkaTransactionManager` |
| | Error Handling | `DefaultErrorHandler` |
| | Retry Mechanism | `FixedBackOff(1000L, 3)` |
| | Dead Letter Queue | `DeadLetterPublishingRecoverer` |
| | Bean Qualification | `@Qualifier` |

