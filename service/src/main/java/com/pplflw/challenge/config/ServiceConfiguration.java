package com.pplflw.challenge.config;

import com.pplflw.challenge.EmployeeNotFoundException;
import com.pplflw.challenge.dto.EmployeeStatusEventDto;
import com.pplflw.challenge.storage.EmployeeStorage;
import com.pplflw.challenge.storage.InMemoryEmployeeStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import reactor.kafka.sender.SenderOptions;

@Configuration
@EnableKafka
@Slf4j
public class ServiceConfiguration {

    @Bean
    public ReactiveKafkaProducerTemplate<String, EmployeeStatusEventDto> kafkaProducer(KafkaProperties kafkaProperties) {
        return new ReactiveKafkaProducerTemplate<>(SenderOptions.create(kafkaProperties.buildProducerProperties()));
    }

    @Bean
    public EmployeeStorage employeeStorage() {
        return new InMemoryEmployeeStorage();
    }

    @Bean
    public RecordMessageConverter messageConverter() {
        return new StringJsonMessageConverter();
    }

    @Bean
    public KafkaListenerErrorHandler errorHandler() {
        return (message, exception) -> {

            Throwable cause = exception.getCause();

            if (cause instanceof EmployeeNotFoundException) {
                log.warn("Got '{}' exception while processing '{}' message", cause.getMessage(), message.getPayload());
            } else {
                throw exception;
            }

            return message;
        };
    }
}