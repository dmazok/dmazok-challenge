package com.pplflw.challenge.controller;

import com.pplflw.challenge.domain.Employee;
import com.pplflw.challenge.dto.EmployeeChangeStateEventDto;
import com.pplflw.challenge.dto.EmployeeStatusEventDto;
import com.pplflw.challenge.service.ReactiveEmployeeService;
import com.pplflw.challenge.service.dto.InputEmployeeDto;
import com.pplflw.challenge.statemachine.EmployeeState;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.pplflw.challenge.statemachine.EmployeeEvent.ACTIVATE;
import static com.pplflw.challenge.statemachine.EmployeeEvent.CHECK;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@WebFluxTest(EmployeeController.class)
public class ControllerTests {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private ReactiveEmployeeService employeeService;

    @Test
    public void testBadRequest() {

        webTestClient.put().uri("/employees/1/delete")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").isEqualTo("Type mismatch.");
    }

    @Test
    public void testAddEmployee() {

        Employee employee = createTestEmployee();

        when(employeeService.addEmployee(any(InputEmployeeDto.class))).thenReturn(Mono.just(employee));

        webTestClient.post().uri("/employees/add")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(createTestInputEmployeeDto()), InputEmployeeDto.class)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Employee.class)
                .value(Employee::getId, equalTo(employee.getId()));
    }

    @Test
    public void testChangeState() {

        EmployeeChangeStateEventDto eventDto = new EmployeeChangeStateEventDto(1L, CHECK);

        when(employeeService.changeEmployeeState(eventDto.getEmployeeId(),
                eventDto.getEvent()))
                .thenReturn(Mono.just(eventDto));

        webTestClient.put().uri("/employees/{id}/{event}",
                eventDto.getEmployeeId(),
                eventDto.getEvent())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(EmployeeChangeStateEventDto.class)
                .value(EmployeeChangeStateEventDto::getEmployeeId, equalTo(eventDto.getEmployeeId()))
                .value(EmployeeChangeStateEventDto::getEvent, equalTo(eventDto.getEvent()));
    }

    @Test
    public void testErrorOnChangeState() {

        EmployeeChangeStateEventDto eventDto = new EmployeeChangeStateEventDto(1L, ACTIVATE);

        String errorMessage = "Timeout error.";

        when(employeeService.changeEmployeeState(eventDto.getEmployeeId(), eventDto.getEvent()))
                .thenThrow(new RuntimeException(errorMessage));

        webTestClient.put().uri("/employees/{id}/{event}",
                eventDto.getEmployeeId(),
                eventDto.getEvent())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody()
                .jsonPath("$.error").isEqualTo("Internal Server Error")
                .jsonPath("$.message").isEqualTo(errorMessage);
    }

    @Test
    public void testStatus() {

        Employee employee = createTestEmployee();

        EmployeeStatusEventDto statusEventDto = new EmployeeStatusEventDto(CHECK, "ACCEPTED", employee);

        when(employeeService.status(employee.getId()))
                .thenReturn(Flux.just(statusEventDto));

        webTestClient.get().uri("/employees/{id}/status",
                employee.getId())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(stringContainsInOrder("State change attempt",
                        CHECK.toString(),
                        statusEventDto.getResult(),
                        employee.getId().toString(),
                        employee.getState().toString()));
    }

    private Employee createTestEmployee() {
        return new Employee(1L, EmployeeState.ADDED, "Aliaksei Protas", "Washington Capitals prospect", 20);
    }

    private InputEmployeeDto createTestInputEmployeeDto() {
        return new InputEmployeeDto("Aliaksei Protas", "Washington Capitals prospect", 20);
    }
}
