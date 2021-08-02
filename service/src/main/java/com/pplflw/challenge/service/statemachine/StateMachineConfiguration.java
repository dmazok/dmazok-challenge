package com.pplflw.challenge.service.statemachine;

import com.pplflw.challenge.statemachine.EmployeeEvent;
import com.pplflw.challenge.statemachine.EmployeeState;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

@Configuration
@EnableStateMachineFactory
public class StateMachineConfiguration extends EnumStateMachineConfigurerAdapter<EmployeeState, EmployeeEvent> {

    @Override
    public void configure(StateMachineConfigurationConfigurer<EmployeeState, EmployeeEvent> config) throws Exception {
        config.withConfiguration().autoStartup(true);
    }

    @Override
    public void configure(StateMachineStateConfigurer<EmployeeState, EmployeeEvent> states) throws Exception {
        states.withStates()
                .initial(EmployeeState.ADDED)
                .state(EmployeeState.IN_CHECK)
                .state(EmployeeState.APPROVED)
                .state(EmployeeState.ACTIVE);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<EmployeeState, EmployeeEvent> transitions) throws Exception {
        transitions
                .withExternal().source(EmployeeState.ADDED).target(EmployeeState.IN_CHECK).event(EmployeeEvent.CHECK).and()
                .withExternal().source(EmployeeState.IN_CHECK).target(EmployeeState.ADDED).event(EmployeeEvent.REJECT).and()
                .withExternal().source(EmployeeState.IN_CHECK).target(EmployeeState.APPROVED).event(EmployeeEvent.APPROVE).and()
                .withExternal().source(EmployeeState.APPROVED).target(EmployeeState.ACTIVE).event(EmployeeEvent.ACTIVATE);
    }
}
