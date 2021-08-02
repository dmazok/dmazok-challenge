package com.pplflw.challenge.service;

import com.pplflw.challenge.EmployeeNotFoundException;
import com.pplflw.challenge.domain.Employee;
import com.pplflw.challenge.dto.EmployeeAddEventDto;
import com.pplflw.challenge.dto.EmployeeChangeStateEventDto;
import com.pplflw.challenge.dto.EmployeeStatusEventDto;
import com.pplflw.challenge.statemachine.EmployeeEvent;
import com.pplflw.challenge.statemachine.EmployeeState;
import com.pplflw.challenge.storage.EmployeeStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

/**
 * {@link EmployeeService} implementation. Encapsulates interaction with Spring State Machine.
 * Works with the state machine in the following manner:
 * - on every state update request({@link EmployeeEvent}) loads the employee from the storage
 * and takes his/her actual state from {@link Employee}.state field
 * - instantiates new state machine and rehydrates it with the employee's state
 * - feeds event to the state machine and receives a result
 * - depending on the result changes employee's state and saves to the storage or not changes/saves
 */
@Slf4j
@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final StateMachineFactory<EmployeeState, EmployeeEvent> stateMachineFactory;

    private final EmployeeStorage employeeStorage;

    private final ReactiveKafkaProducerTemplate<String, EmployeeStatusEventDto> kafkaProducer;

    @Value(value = "${com.pplflw.challenge.kafka.employee-status-topic}")
    private String employeeStatusTopic;

    @Value(value = "${com.pplflw.challenge.kafka.timeout-in-seconds}")
    private int timeoutInSeconds;

    public EmployeeServiceImpl(StateMachineFactory<EmployeeState, EmployeeEvent> stateMachineFactory,
                               EmployeeStorage employeeStorage,
                               ReactiveKafkaProducerTemplate<String, EmployeeStatusEventDto> kafkaProducer) {
        this.stateMachineFactory = stateMachineFactory;
        this.employeeStorage = employeeStorage;
        this.kafkaProducer = kafkaProducer;
    }

    @Override
    public Employee addEmployee(EmployeeAddEventDto employeeAddEventDto) {
        Employee employee = employeeStorage.updateEmployee(employeeAddEventDto.getEmployee());

        sendStatusEvent(new EmployeeStatusEventDto(null, null, employee));

        return employee;
    }

    public EmployeeStatusEventDto changeEmployeeState(Long employeeId, EmployeeEvent event) {

        Optional<Employee> optionalEmployee = employeeStorage.getEmployee(employeeId);

        if (optionalEmployee.isEmpty())
            throw new EmployeeNotFoundException("Cannot find employee with id=" + employeeId);

        Employee employee = optionalEmployee.get();

        StateMachine<EmployeeState, EmployeeEvent> stateMachine = rehydrateStateMachine(employeeId, employee.getState());

        return stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(event).build()))
                .map(eventResult -> {

                    log.debug("State machine result for event {}: {}", event, eventResult);

                    employee.setState(stateMachine.getState().getId());

                    EmployeeStatusEventDto employeeStatusEventDto
                            = new EmployeeStatusEventDto(event, eventResult.getResultType().toString(), employee);

                    if (StateMachineEventResult.ResultType.ACCEPTED == eventResult.getResultType()) {

                        employee.setState(stateMachine.getState().getId());

                        employeeStorage.updateEmployee(employee);
                    }

                    sendStatusEvent(employeeStatusEventDto);

                    return employeeStatusEventDto;
                }).next().block();
    }

    private StateMachine<EmployeeState, EmployeeEvent> rehydrateStateMachine(Long employeeId, EmployeeState employeeState) {

        StateMachine<EmployeeState, EmployeeEvent> stateMachine = stateMachineFactory.getStateMachine(employeeId.toString());

        stateMachine.stopReactively().block();
        stateMachine.getStateMachineAccessor()
                .doWithAllRegions(sma ->
                        sma.resetStateMachineReactively(new DefaultStateMachineContext<>(employeeState,
                                null,
                                null,
                                stateMachine.getExtendedState())).block());
        stateMachine.startReactively().block();

        return stateMachine;
    }

    public void sendStatusEvent(EmployeeStatusEventDto employeeStatusEventDto) {
        log.debug("Sending employee-status event to Kafka: {}", employeeStatusEventDto);

        kafkaProducer.send(employeeStatusTopic, employeeStatusEventDto)
                .doOnSuccess(voidSenderResult
                        ->
                        log.debug("Successfully sent employee-status event to Kafka: {}", employeeStatusEventDto))
                .timeout(Duration.ofSeconds(timeoutInSeconds))
                .doOnError(throwable
                        ->
                        log.error("An exception occurred while sending employee-status event={} to Kafka: {}",
                                employeeStatusEventDto,
                                throwable.getMessage()))
                .subscribe();
    }

    @KafkaListener(topics = "${com.pplflw.challenge.kafka.employee-add-topic}", clientIdPrefix = "add")
    public void listenEmployeeAddEvents(EmployeeAddEventDto employeeAddEventDto) {
        log.debug("Received EmployeeAddEventDto message: {}", employeeAddEventDto);

        addEmployee(employeeAddEventDto);
    }

    @KafkaListener(topics = "${com.pplflw.challenge.kafka.employee-change-state-topic}",
            clientIdPrefix = "change-state",
            errorHandler = "errorHandler")
    public void listenEmployeeChangeStateEvent(EmployeeChangeStateEventDto employeeChangeStateEventDto) {
        log.debug("Received EmployeeChangeStateEventDto message: {}", employeeChangeStateEventDto);

        changeEmployeeState(employeeChangeStateEventDto.getEmployeeId(), employeeChangeStateEventDto.getEvent());
    }
}