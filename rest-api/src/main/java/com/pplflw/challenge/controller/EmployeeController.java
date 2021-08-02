package com.pplflw.challenge.controller;

import com.pplflw.challenge.domain.Employee;
import com.pplflw.challenge.dto.EmployeeChangeStateEventDto;
import com.pplflw.challenge.dto.EmployeeStatusEventDto;
import com.pplflw.challenge.service.ReactiveEmployeeService;
import com.pplflw.challenge.service.dto.InputEmployeeDto;
import com.pplflw.challenge.statemachine.EmployeeEvent;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

/**
 * Employee controller. Describes API endpoints, validates input data then passes it to {@link ReactiveEmployeeService}.
 */
@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final ReactiveEmployeeService reactiveEmployeeService;

    public EmployeeController(ReactiveEmployeeService reactiveEmployeeService) {
        this.reactiveEmployeeService = reactiveEmployeeService;
    }

    @PostMapping(value = "/add",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Employee> add(@Valid @RequestBody InputEmployeeDto employeeDto) {
        return reactiveEmployeeService.addEmployee(employeeDto);
    }

    @PutMapping(value = "/{employeeId}/{event}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<EmployeeChangeStateEventDto> changeState(@PathVariable Long employeeId, @PathVariable EmployeeEvent event) {
        return reactiveEmployeeService.changeEmployeeState(employeeId, event);
    }

    @GetMapping(value = "{employeeId}/status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Flux<ServerSentEvent<EmployeeStatusEventDto>> status(@PathVariable Long employeeId) {
        return reactiveEmployeeService.status(employeeId)
                .map(dto -> ServerSentEvent.builder(dto)
                        .comment(dto.getEvent() == null ? "Employee creation" : "State change attempt")
                        .build());
    }
}
