package com.shivam.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Configuration class for creating Kafka topics programmatically.
 * <p>
 * In a production environment, topics are often created by an admin team,
 * but for development and learning, we can use Spring Kafka's TopicBuilder.
 * </p>
 * 
 * @author Shivam Srivastav
 */
@Configuration
public class KafkaConfig {

        /**
         * Creates a topic named "mastery-beginner-topic".
         * <p>
         * partitions: 3 - Allows for parallel processing if we have multiple consumers
         * in a group.
         * replicas: 1 - Since we are running a single broker locally. In prod, this
         * should be > 1.
         * </p>
         */
        @Bean
        public NewTopic beginnerTopic() {
                return TopicBuilder.name("mastery-beginner-topic")
                                .partitions(3)
                                .replicas(1)
                                .build();
        }

        /**
         * Creates a topic named "mastery-json-topic" for intermediate concepts.
         */
        @Bean
        public NewTopic jsonTopic() {
                return TopicBuilder.name("mastery-json-topic")
                                .partitions(3)
                                .replicas(1)
                                .build();
        }

        /**
         * Creates a topic named "mastery-advanced-topic" for advanced concepts.
         */
        @Bean
        public NewTopic advancedTopic() {
                return TopicBuilder.name("mastery-advanced-topic")
                                .partitions(3)
                                .replicas(1)
                                .build();
        }

        // ========================================================================
        // Basic Configuration
        // ========================================================================

        @Bean
        public org.springframework.kafka.core.ProducerFactory<String, String> basicProducerFactory() {
                java.util.Map<String, Object> configProps = new java.util.HashMap<>();
                configProps.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                                bootstrapServers);
                configProps.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                                org.apache.kafka.common.serialization.StringSerializer.class);
                configProps.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                                org.apache.kafka.common.serialization.StringSerializer.class);
                return new org.springframework.kafka.core.DefaultKafkaProducerFactory<>(configProps);
        }

        @Bean
        public org.springframework.kafka.core.KafkaTemplate<String, String> basicKafkaTemplate() {
                return new org.springframework.kafka.core.KafkaTemplate<>(basicProducerFactory());
        }

        // ========================================================================
        // JSON Configuration
        // ========================================================================

        @org.springframework.beans.factory.annotation.Value("${spring.kafka.bootstrap-servers}")
        private String bootstrapServers;

        @Bean
        public org.springframework.kafka.core.ProducerFactory<String, com.shivam.kafka.model.User> userProducerFactory() {
                java.util.Map<String, Object> configProps = new java.util.HashMap<>();
                configProps.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                                bootstrapServers);
                configProps.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                                org.apache.kafka.common.serialization.StringSerializer.class);
                configProps.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                                org.springframework.kafka.support.serializer.JsonSerializer.class);
                return new org.springframework.kafka.core.DefaultKafkaProducerFactory<>(configProps);
        }

        @Bean
        public org.springframework.kafka.core.KafkaTemplate<String, com.shivam.kafka.model.User> userKafkaTemplate() {
                return new org.springframework.kafka.core.KafkaTemplate<>(userProducerFactory());
        }

        @Bean
        public org.springframework.kafka.core.ConsumerFactory<String, com.shivam.kafka.model.User> userConsumerFactory() {
                java.util.Map<String, Object> configProps = new java.util.HashMap<>();
                configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                                bootstrapServers);
                configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, "json-group");
                configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                                org.apache.kafka.common.serialization.StringDeserializer.class);
                configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                                org.springframework.kafka.support.serializer.JsonDeserializer.class);
                configProps.put(org.springframework.kafka.support.serializer.JsonDeserializer.TRUSTED_PACKAGES,
                                "com.shivam.kafka.model");
                return new org.springframework.kafka.core.DefaultKafkaConsumerFactory<>(configProps,
                                new org.apache.kafka.common.serialization.StringDeserializer(),
                                new org.springframework.kafka.support.serializer.JsonDeserializer<>(
                                                com.shivam.kafka.model.User.class,
                                                false));
        }

        @Bean
        public org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<String, com.shivam.kafka.model.User> userKafkaListenerContainerFactory() {
                org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<String, com.shivam.kafka.model.User> factory = new org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<>();
                factory.setConsumerFactory(userConsumerFactory());
                return factory;
        }

        // ========================================================================
        // Advanced Configuration (Transactions, DLQ, Retries)
        // ========================================================================

        @Bean
        public org.springframework.kafka.core.ProducerFactory<String, String> advancedProducerFactory() {
                java.util.Map<String, Object> configProps = new java.util.HashMap<>();
                configProps.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                                bootstrapServers);
                configProps.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                                org.apache.kafka.common.serialization.StringSerializer.class);
                configProps.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                                org.apache.kafka.common.serialization.StringSerializer.class);
                // Enable Idempotence (default true in recent versions, but good to be explicit)
                configProps.put(org.apache.kafka.clients.producer.ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
                // Transactional ID Prefix enables transactions
                configProps.put(org.apache.kafka.clients.producer.ProducerConfig.TRANSACTIONAL_ID_CONFIG, "tx-");
                return new org.springframework.kafka.core.DefaultKafkaProducerFactory<>(configProps);
        }

        @Bean
        public org.springframework.kafka.core.KafkaTemplate<String, String> advancedKafkaTemplate() {
                return new org.springframework.kafka.core.KafkaTemplate<>(advancedProducerFactory());
        }

        @Bean
        public org.springframework.kafka.transaction.KafkaTransactionManager<String, String> kafkaTransactionManager(
                        org.springframework.kafka.core.ProducerFactory<String, String> advancedProducerFactory) {
                return new org.springframework.kafka.transaction.KafkaTransactionManager<>(advancedProducerFactory);
        }

        /**
         * Configures a listener container factory with a Dead Letter Queue (DLQ)
         * recovery mechanism.
         * When retries are exhausted, the message is sent to a DLT topic
         * (topic-name.DLT).
         */
        @Bean
        public org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<String, String> advancedListenerContainerFactory(
                        @org.springframework.beans.factory.annotation.Qualifier("advancedKafkaTemplate") org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate) {

                org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<String, String> factory = new org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<>();
                factory.setConsumerFactory(new org.springframework.kafka.core.DefaultKafkaConsumerFactory<>(
                                java.util.Map.of(
                                                org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                                                bootstrapServers,
                                                org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                                                org.apache.kafka.common.serialization.StringDeserializer.class,
                                                org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                                                org.apache.kafka.common.serialization.StringDeserializer.class)));

                // Configure Error Handler with DeadLetterPublishingRecoverer
                org.springframework.kafka.listener.DeadLetterPublishingRecoverer recoverer = new org.springframework.kafka.listener.DeadLetterPublishingRecoverer(
                                kafkaTemplate);

                // DefaultErrorHandler with 3 retries (backoff 1s)
                org.springframework.kafka.listener.DefaultErrorHandler errorHandler = new org.springframework.kafka.listener.DefaultErrorHandler(
                                recoverer, new org.springframework.util.backoff.FixedBackOff(1000L, 3));

                factory.setCommonErrorHandler(errorHandler);

                return factory;
        }
}
