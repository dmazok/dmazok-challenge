package com.pplflw.challenge.service;

import com.pplflw.challenge.domain.Employee;
import com.pplflw.challenge.dto.EmployeeChangeStateEventDto;
import com.pplflw.challenge.dto.EmployeeStatusEventDto;
import com.pplflw.challenge.service.dto.InputEmployeeDto;
import com.pplflw.challenge.statemachine.EmployeeEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Facade interface to be used on API layer.
 * Returns reactive types because of Webflux nature.
 */
public interface ReactiveEmployeeService {

    /**
     * Adds new employee.
     *
     * @param employeeDto input employee DTO
     * @return an employee that will be created shortly
     */
    Mono<Employee> addEmployee(InputEmployeeDto employeeDto);

    /**
     * Accepts employee change state event.
     *
     * @param employeeId employee's ID
     * @param event state machine event
     * @return {@link EmployeeChangeStateEventDto} instance, not really updated employee
     */
    Mono<EmployeeChangeStateEventDto> changeEmployeeState(Long employeeId, EmployeeEvent event);

    /**
     * Provides 'real' employee state change flow.
     *
     * @param employeeId employee's ID
     * @return stream of {@link EmployeeStatusEventDto} instances
     */
    Flux<EmployeeStatusEventDto> status(Long employeeId);
}
