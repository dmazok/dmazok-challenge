package com.pplflw.challenge.service;

import com.pplflw.challenge.EmployeeNotFoundException;
import com.pplflw.challenge.domain.Employee;
import com.pplflw.challenge.dto.EmployeeAddEventDto;
import com.pplflw.challenge.dto.EmployeeStatusEventDto;
import com.pplflw.challenge.statemachine.EmployeeEvent;
import com.pplflw.challenge.statemachine.EmployeeState;
import com.pplflw.challenge.storage.EmployeeStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class EmployeeServiceImplTest {

    @MockBean
    private EmployeeStorage employeeStorage;

    @MockBean
    private ReactiveKafkaProducerTemplate<String, EmployeeStatusEventDto> kafkaProducer;

    @Autowired
    private EmployeeService employeeService;

    @Test
    public void addEmployee() {

        Employee employee = createTestEmployee();

        EmployeeAddEventDto employeeAddEventDto = new EmployeeAddEventDto(employee);

        when(employeeStorage.updateEmployee(any(Employee.class))).thenReturn(employee);
        when(kafkaProducer.send(any(String.class), any(EmployeeStatusEventDto.class))).thenReturn(Mono.empty());

        Employee savedEmployee = employeeService.addEmployee(employeeAddEventDto);

        assertEquals(employee.getId(), savedEmployee.getId());
        assertEquals(EmployeeState.ADDED, savedEmployee.getState());

        verify(employeeStorage, times(1)).updateEmployee(same(employee));
        verify(kafkaProducer, times(1)).send(any(String.class), any(EmployeeStatusEventDto.class));
    }

    @Test
    public void addEmployeeThrowsRuntimeException() {

        when(employeeStorage.updateEmployee(any(Employee.class))).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> employeeService.addEmployee(new EmployeeAddEventDto(createTestEmployee())));

        verify(kafkaProducer, never()).send(any(String.class), any(EmployeeStatusEventDto.class));
    }

    @Test
    public void changeEmployeeStateThrowsEmployeeNotFoundException() {
        assertThrows(EmployeeNotFoundException.class,
                () -> employeeService.changeEmployeeState(-1L, EmployeeEvent.CHECK));

        verify(kafkaProducer, never()).send(any(String.class), any(EmployeeStatusEventDto.class));
        verify(employeeStorage, never()).updateEmployee(any(Employee.class));
    }

    @Test
    public void changeEmployeeStateAccepted() {

        Employee employee = createTestEmployee();
        EmployeeAddEventDto employeeAddEventDto = new EmployeeAddEventDto(createTestEmployee());

        when(kafkaProducer.send(any(String.class), any(EmployeeStatusEventDto.class))).thenReturn(Mono.empty());
        when(employeeStorage.updateEmployee(any(Employee.class))).thenAnswer(i -> i.getArgument(0));
        when(employeeStorage.getEmployee(any(Long.class))).thenReturn(Optional.of(employee));

        Employee savedEmployee = employeeService.addEmployee(employeeAddEventDto);

        assertEquals(savedEmployee.getId(), employee.getId());
        assertEquals(savedEmployee.getState(), EmployeeState.ADDED);

        EmployeeStatusEventDto employeeStatusEventDto
                = employeeService.changeEmployeeState(employee.getId(), EmployeeEvent.CHECK);

        assertEquals(employeeStatusEventDto.getEmployee().getId(), savedEmployee.getId());
        assertEquals(employeeStatusEventDto.getEmployee().getState(), EmployeeState.IN_CHECK);
        assertEquals(employeeStatusEventDto.getResult(), "ACCEPTED");

        verify(kafkaProducer, times(2)).send(any(String.class), any(EmployeeStatusEventDto.class));
        verify(employeeStorage, times(2)).updateEmployee(any(Employee.class));
    }

    @Test
    public void changeEmployeeStateDenied() {

        Employee employee = createTestEmployee();
        EmployeeAddEventDto employeeAddEventDto = new EmployeeAddEventDto(createTestEmployee());

        when(kafkaProducer.send(any(String.class), any(EmployeeStatusEventDto.class))).thenReturn(Mono.empty());
        when(employeeStorage.updateEmployee(any(Employee.class))).thenAnswer(i -> i.getArgument(0));
        when(employeeStorage.getEmployee(any(Long.class))).thenReturn(Optional.of(employee));

        Employee savedEmployee = employeeService.addEmployee(employeeAddEventDto);

        assertEquals(savedEmployee.getId(), employee.getId());
        assertEquals(savedEmployee.getState(), EmployeeState.ADDED);

        EmployeeStatusEventDto employeeStatusEventDto
                = employeeService.changeEmployeeState(employee.getId(), EmployeeEvent.APPROVE);

        assertEquals(employeeStatusEventDto.getEmployee().getId(), savedEmployee.getId());
        assertEquals(employeeStatusEventDto.getEmployee().getState(), EmployeeState.ADDED);
        assertEquals(employeeStatusEventDto.getResult(), "DENIED");

        verify(kafkaProducer, times(2)).send(any(String.class), any(EmployeeStatusEventDto.class));
        verify(employeeStorage, times(1)).updateEmployee(eq(employee));
    }

    @Test
    public void changeEmployeeStateThrows() {

        Employee employee = createTestEmployee();

        when(kafkaProducer.send(any(String.class), any(EmployeeStatusEventDto.class))).thenReturn(Mono.empty());
        when(employeeStorage.getEmployee(any(Long.class))).thenReturn(Optional.of(employee));
        when(employeeStorage.updateEmployee(any(Employee.class))).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> employeeService.changeEmployeeState(employee.getId(), EmployeeEvent.CHECK));

        verify(kafkaProducer, never()).send(any(String.class), any(EmployeeStatusEventDto.class));
    }

    private Employee createTestEmployee() {
        return new Employee(1L, EmployeeState.ADDED, "Aliaksei Protas", "Washington Capitals prospect", 20);
    }
}