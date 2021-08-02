package com.pplflw.challenge.service;

import com.pplflw.challenge.domain.Employee;
import com.pplflw.challenge.dto.EmployeeAddEventDto;
import com.pplflw.challenge.dto.EmployeeStatusEventDto;
import com.pplflw.challenge.statemachine.EmployeeEvent;

/**
 * This interface describes all employee processing use cases.
 */
public interface EmployeeService {

    /**
     * Adds new employee.
     *
     * @param employeeAddEventDto add employee event DTO
     * @return just created employee with ADDED state
     */
    Employee addEmployee(EmployeeAddEventDto employeeAddEventDto);

    /**
     * Processes a state machine event as a request for employee's state change.
     * Returns DTO with state machine result and an updated(or not) employee.
     *
     * @param employeeId employee's ID
     * @param event      state machine event
     * @return DTO with state machine result and an updated employee
     * @throws com.pplflw.challenge.EmployeeNotFoundException if employee cannot be found
     */
    EmployeeStatusEventDto changeEmployeeState(Long employeeId, EmployeeEvent event);
}
