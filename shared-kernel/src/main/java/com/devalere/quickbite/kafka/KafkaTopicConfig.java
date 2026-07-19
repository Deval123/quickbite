package com.devalere.quickbite.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Creation des topics kafka au démarrage de l'application.
 * Placé dans order-service, car c'est le premier service qui sera lancé.
 * En prod, les topics sont créé par l'équipe ops (pas par le code).
 *
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderEventsTopic(){
        return TopicBuilder.name(KafkaTopics.ORDER_EVENTS)
                .partitions(KafkaTopics.PARTITIONS)
                .replicas(KafkaTopics.REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic paymentEventsTopic(){
        return TopicBuilder.name(KafkaTopics.PAYMENT_EVENTS)
                .partitions(KafkaTopics.PARTITIONS)
                .replicas(KafkaTopics.REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic restaurantEventsTopic(){
        return TopicBuilder.name(KafkaTopics.RESTAURANT_EVENTS)
                .partitions(KafkaTopics.PARTITIONS)
                .replicas(KafkaTopics.REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic deliveryEventsTopic(){
        return TopicBuilder.name(KafkaTopics.DELIVERY_EVENTS)
                .partitions(KafkaTopics.PARTITIONS)
                .replicas(KafkaTopics.REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic notificationEventsTopic(){
        return TopicBuilder.name(KafkaTopics.NOTIFICATION_EVENTS)
                .partitions(KafkaTopics.PARTITIONS)
                .replicas(KafkaTopics.REPLICATION_FACTOR)
                .build();
    }

}
