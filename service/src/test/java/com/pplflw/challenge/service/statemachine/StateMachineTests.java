package com.pplflw.challenge.service.statemachine;

import com.pplflw.challenge.statemachine.EmployeeEvent;
import com.pplflw.challenge.statemachine.EmployeeState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.config.StateMachineFactory;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StateMachineTests {

    @Autowired
    private StateMachineFactory<EmployeeState, EmployeeEvent> stateMachineFactory;

    /**
     * Checks state machine is up and ready.
     */
    @Test
    void testInit() {

        StateMachine<EmployeeState, EmployeeEvent> stateMachine = stateMachineFactory.getStateMachine();

        assertThat(stateMachine).isNotNull();
        assertThat(stateMachine.getState().getId()).isEqualTo(EmployeeState.ADDED);
    }

    /**
     * Tests state machine 'happy-end' path.
     */
    @Test
    public void testHappyEndPath() {

        StateMachine<EmployeeState, EmployeeEvent> stateMachine = stateMachineFactory.getStateMachine();

        assertThat(stateMachine.getState().getId()).isEqualTo(EmployeeState.ADDED);

        stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(EmployeeEvent.CHECK).build())).blockFirst();

        assertThat(stateMachine.getState().getId()).isEqualTo(EmployeeState.IN_CHECK);

        stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(EmployeeEvent.REJECT).build())).blockFirst();

        assertThat(stateMachine.getState().getId()).isEqualTo(EmployeeState.ADDED);

        stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(EmployeeEvent.CHECK).build())).blockFirst();
        stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(EmployeeEvent.APPROVE).build())).blockFirst();

        assertThat(stateMachine.getState().getId()).isEqualTo(EmployeeState.APPROVED);

        stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(EmployeeEvent.ACTIVATE).build())).blockFirst();

        assertThat(stateMachine.getState().getId()).isEqualTo(EmployeeState.ACTIVE);
    }

    /**
     * Tests state machine DENIED responses.
     */
    @Test
    public void testDeniedResponses() {

        StateMachine<EmployeeState, EmployeeEvent> stateMachine = stateMachineFactory.getStateMachine();

        assertThat(stateMachine.getState().getId()).isEqualTo(EmployeeState.ADDED);

        StateMachineEventResult<EmployeeState, EmployeeEvent> result
                = stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(EmployeeEvent.REJECT).build())).blockFirst();

        assertThat(result).isNotNull();
        assertThat(result.getResultType()).isEqualTo(StateMachineEventResult.ResultType.DENIED);
        assertThat(stateMachine.getState().getId()).isEqualTo(EmployeeState.ADDED);

        stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(EmployeeEvent.APPROVE).build())).blockFirst();

        assertThat(stateMachine.getState().getId()).isEqualTo(EmployeeState.ADDED);

        stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(EmployeeEvent.CHECK).build())).blockFirst();
        stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(EmployeeEvent.ACTIVATE).build())).blockFirst();

        assertThat(stateMachine.getState().getId()).isEqualTo(EmployeeState.IN_CHECK);

        stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(EmployeeEvent.APPROVE).build())).blockFirst();
        stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(EmployeeEvent.CHECK).build())).blockFirst();

        assertThat(stateMachine.getState().getId()).isEqualTo(EmployeeState.APPROVED);

        stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(EmployeeEvent.ACTIVATE).build())).blockFirst();
        stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(EmployeeEvent.REJECT).build())).blockFirst();

        assertThat(stateMachine.getState().getId()).isEqualTo(EmployeeState.ACTIVE);
    }
}
