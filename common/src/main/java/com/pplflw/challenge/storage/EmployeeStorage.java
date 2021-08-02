package com.pplflw.challenge.storage;

import com.pplflw.challenge.domain.Employee;

import java.util.Optional;

/**
 * Employee storage interface.
 */
public interface EmployeeStorage {
    Employee updateEmployee(Employee employee);

    Optional<Employee> getEmployee(Long id);
}
