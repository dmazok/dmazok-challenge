package com.pplflw.challenge.service;

import com.pplflw.challenge.domain.Employee;
import com.pplflw.challenge.dto.EmployeeChangeStateEventDto;
import com.pplflw.challenge.dto.EmployeeStatusEventDto;
import com.pplflw.challenge.service.dto.InputEmployeeDto;
import com.pplflw.challenge.statemachine.EmployeeEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * TODO: add description
 */
public interface ReactiveEmployeeService {

    /**
     * Adds new employee.
     *
     * @param employeeDto input employee DTO
     * @return just created employee with ADDED state
     */
    Mono<Employee> addEmployee(InputEmployeeDto employeeDto);

    /**
     * Processes a state machine event as a request for employee's state change.
     * Returns DTO with state machine result and an updated(or not) employee.
     *
     * @param employeeId employee's ID
     * @param event state machine event
     * @return DTO with state machine result and an updated employee
     * @throws com.pplflw.challenge.EmployeeNotFoundException if employee cannot be found
     */
    Mono<EmployeeChangeStateEventDto> changeEmployeeState(Long employeeId, EmployeeEvent event);

    Flux<EmployeeStatusEventDto> status(Long employeeId);
}
