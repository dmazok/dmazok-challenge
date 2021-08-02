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
 * {@link ReactiveEmployeeService} implementation. Encapsulates interaction with Spring State Machine.
 * Works with the state machine in the following manner:
 * - on every state update request({@link EmployeeEvent}) loads the employee from the storage
 * and takes his/her actual state from {@link Employee}.state field
 * - instantiates new state machine and rehydrates it with the employee's state
 * - feeds event to the state machine and receives a result
 * - depending on the result changes employee's state and saves to the storage or not changes/saves
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

    //    @Value(value = "${com.pplflw.challenge.kafka.employee-status-topic}")
//    private String employeesTopic;
//
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
                .doOnSuccess(voidSenderResult -> log.info("Successfully sent add-employee event={} to Kafka", eventDto))
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
                .doOnSuccess(voidSenderResult -> log.info("Successfully sent change-employee-state event to Kafka: {}", eventDto))
                .map(voidSenderResult -> eventDto);
    }

//    @Override
//    public OutputEmployeeDto changeEmployeeState(Long employeeId, EmployeeEvent event) {
//
//        Optional<Employee> optionalEmployee = employeeStorage.getEmployee(employeeId);
//
//        if (optionalEmployee.isEmpty())
//            throw new EmployeeNotFoundException("Cannot find employee with id=" + employeeId);
//
//        Employee employee = optionalEmployee.get();
//
//        StateMachine<EmployeeState, EmployeeEvent> stateMachine = rehydrateStateMachine(employeeId, employee.getState());
//
//        return stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(event).build()))
//                .map(eventResult -> {
//
//                    log.debug("State machine result {}:", eventResult);
//
//                    if (StateMachineEventResult.ResultType.ACCEPTED == eventResult.getResultType()) {
//
//                        employee.setState(stateMachine.getState().getId());
//
//                        employeeStorage.updateEmployee(employee);
//
////                        sendToKafka(employee);
//                    }
//
//                    return new OutputEmployeeDto(eventResult.getResultType(), employee);
//                }).next().block();
//    }

    public Flux<EmployeeStatusEventDto> status(Long employeeId) {
        return getKafkaConsumer()
                .receive()
//                .receiveAtMostOnce()
//                .reduce((r1, r2) -> r1.offset() >= r2.offset() ? r1 : r2)
//                .flux()
//                .sort((o1, o2) -> Long.compare(o2.offset(), o1.offset()))
//                .doOnNext(receiverRecord -> receiverRecord.receiverOffset().commit())
                .map(ConsumerRecord::value)
                .filter(eventDto -> eventDto.getEmployee().getId().equals(employeeId))
                .doOnNext(eventDto -> log.info("Successfully consumed {}={}", EmployeeStatusEventDto.class.getSimpleName(), eventDto))
                .doOnError(throwable -> log.error("Something bad happened while consuming : {}", throwable.getMessage()));
    }

//    @KafkaListener(topics = "${com.pplflw.challenge.kafka.employee-add-topic}")
//    public void listenGroupFoo(EmployeeEventDto employeeEventDto) {
//        log.info("Received message: {}", employeeEventDto);
//    }
}