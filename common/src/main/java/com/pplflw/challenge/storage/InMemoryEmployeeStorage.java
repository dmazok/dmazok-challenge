package com.pplflw.challenge.storage;

import com.pplflw.challenge.domain.Employee;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory {@link EmployeeStorage} implementation.
 */
public class InMemoryEmployeeStorage implements EmployeeStorage {

    private final Map<Long, Employee> employees = new ConcurrentHashMap<>();

    @Override
    public Employee updateEmployee(Employee employee) {

        employees.put(employee.getId(), employee);

        return employee;
    }

    @Override
    public Optional<Employee> getEmployee(Long id) {
        return Optional.ofNullable(employees.get(id));
    }
}
