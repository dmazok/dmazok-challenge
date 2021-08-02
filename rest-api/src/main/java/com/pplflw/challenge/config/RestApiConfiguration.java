package com.pplflw.challenge.config;

import com.pplflw.challenge.dto.EmployeeStatusEventDto;
import com.pplflw.challenge.dto.EmployeeEventDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.SenderOptions;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableKafka
@Slf4j
public class RestApiConfiguration {

    private final AtomicInteger clientSuffixGenerator = new AtomicInteger();

    @Bean
    public ReactiveKafkaProducerTemplate<String, EmployeeEventDto> kafkaProducer(KafkaProperties kafkaProperties) {
        return new ReactiveKafkaProducerTemplate<>(SenderOptions.create(kafkaProperties.buildProducerProperties()));
    }

    @Bean
    @Scope("prototype")
    public ReactiveKafkaConsumerTemplate<String, EmployeeStatusEventDto> kafkaConsumer(
            @Value(value = "${com.pplflw.challenge.kafka.employee-status-topic}") String topic,
            @Value(value = "${spring.kafka.consumer.client-id}") String clientId,
            @Value(value = "${spring.kafka.consumer.group-id}") String groupId,
            KafkaProperties kafkaProperties) {

        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties();

        int consumerNumber = clientSuffixGenerator.incrementAndGet();

        // to be able to register more than one consumer
        consumerProperties.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId + "-" + consumerNumber);
        // To make all the consumers receive events in parallel
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-" + consumerNumber);

        return new ReactiveKafkaConsumerTemplate<>(ReceiverOptions
                .<String, EmployeeStatusEventDto>create(consumerProperties)
                .subscription(Collections.singleton(topic)));
    }
}