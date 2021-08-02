package com.pplflw.challenge.service;

import com.pplflw.challenge.domain.Employee;
import com.pplflw.challenge.dto.EmployeeAddEventDto;
import com.pplflw.challenge.dto.EmployeeChangeStateEventDto;
import com.pplflw.challenge.dto.EmployeeStatusEventDto;
import com.pplflw.challenge.dto.EmployeeEventDto;
import com.pplflw.challenge.service.dto.InputEmployeeDto;
import com.pplflw.challenge.statemachine.EmployeeEvent;
import com.pplflw.challenge.statemachine.EmployeeState;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link ReactiveEmployeeService} implementation. Encapsulates interaction with Apache Kafka.
 */
@Slf4j
@Service
public class KafkaReactiveEmployeeService implements ReactiveEmployeeService {

    private final AtomicLong ID_GENERATOR = new AtomicLong();

    private final ReactiveKafkaProducerTemplate<String, EmployeeEventDto> kafkaProducer;

    @Value(value = "${com.pplflw.challenge.kafka.employee-add-topic}")
    private String employeeAddTopic;

    @Value(value = "${com.pplflw.challenge.kafka.employee-change-state-topic}")
    private String employeeEventsTopic;

    @Lookup
    public ReactiveKafkaConsumerTemplate<String, EmployeeStatusEventDto> getKafkaConsumer() {
        return null;
    }

    @Value(value = "${com.pplflw.challenge.kafka.timeout-in-seconds}")
    private int timeoutInSeconds;

    public KafkaReactiveEmployeeService(ReactiveKafkaProducerTemplate<String, EmployeeEventDto> kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @Override
    public Mono<Employee> addEmployee(InputEmployeeDto employeeDto) {

        EmployeeAddEventDto eventDto = new EmployeeAddEventDto(new Employee(
                ID_GENERATOR.incrementAndGet(),
                EmployeeState.ADDED,
                employeeDto.getName(),
                employeeDto.getContractInfo(),
                employeeDto.getAge()));

        log.debug("Sending add-employee event={} to Kafka", eventDto);

        return kafkaProducer.send(employeeAddTopic, eventDto)
                .timeout(Duration.ofSeconds(timeoutInSeconds))
                .doOnError(throwable
                        ->
                        log.error("An exception occurred while sending add-employee event={} to Kafka: {}",
                                eventDto,
                                throwable.getMessage()))
                .doOnSuccess(voidSenderResult -> log.debug("Successfully sent add-employee event={} to Kafka", eventDto))
                .map(voidSenderResult -> eventDto.getEmployee());
    }

    public Mono<EmployeeChangeStateEventDto> changeEmployeeState(Long employeeId, EmployeeEvent event) {

        EmployeeChangeStateEventDto eventDto = new EmployeeChangeStateEventDto(employeeId, event);

        log.debug("Sending change-employee-state event={} to Kafka", eventDto);

        return kafkaProducer.send(employeeEventsTopic, eventDto)
                .timeout(Duration.ofSeconds(timeoutInSeconds))
                .doOnError(throwable
                        ->
                        log.error("An exception occurred while sending change-employee-state event={} to Kafka: {}",
                                eventDto,
                                throwable.getMessage()))
                .doOnSuccess(voidSenderResult -> log.debug("Successfully sent change-employee-state event to Kafka: {}", eventDto))
                .map(voidSenderResult -> eventDto);
    }

    public Flux<EmployeeStatusEventDto> status(Long employeeId) {
        return getKafkaConsumer()
                .receive()
                .map(ConsumerRecord::value)
                .filter(eventDto -> eventDto.getEmployee().getId().equals(employeeId))
                .doOnNext(eventDto -> log.debug("Successfully consumed {}={}", EmployeeStatusEventDto.class.getSimpleName(), eventDto))
                .doOnError(throwable -> log.error("Something bad happened while consuming : {}", throwable.getMessage()));
    }
}