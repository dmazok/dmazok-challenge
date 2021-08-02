package com.pplflw.challenge.service.dto;

import com.pplflw.challenge.domain.Employee;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.statemachine.StateMachineEventResult;

/**
 * Service output DTO. Contains state machine result and an employee entity.
 */
@Getter
@AllArgsConstructor
public class OutputEmployeeDto {

    private final StateMachineEventResult.ResultType resultType;

    private final Employee employee;
}
