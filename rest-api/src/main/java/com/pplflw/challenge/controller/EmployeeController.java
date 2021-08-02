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

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final ReactiveEmployeeService reactiveEmployeeService;

    public EmployeeController(ReactiveEmployeeService reactiveEmployeeService) {
        this.reactiveEmployeeService = reactiveEmployeeService;
    }

    //    @ResponseStatus(HttpStatus.CREATED)
//    @PostMapping(value = "/add")
//    public Employee add(@Valid @RequestBody InputEmployeeDto employeeDto) {
//        return employeeService.addEmployee(employeeDto);
//    }

//    @ResponseStatus(HttpStatus.CREATED)
//    @PostMapping(value = "/add",
//            consumes = MediaType.APPLICATION_JSON_VALUE,
//            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<Employee> addAsync(@Valid @RequestBody InputEmployeeDto employeeDto) {
//        Employee employee = employeeService.addEmployee(employeeDto);
//
//        return employeeService.events(employee.getId());
//    }

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

//    @PutMapping("/{employeeId}/{event}")
//    public ResponseEntity<Employee> changeState(@PathVariable Long employeeId, @PathVariable EmployeeEvent event) {
//
//        OutputEmployeeDto outputEmployeeDto = employeeService.changeEmployeeState(employeeId, event);
//
//        return StateMachineEventResult.ResultType.ACCEPTED == outputEmployeeDto.getResultType() ?
//                ResponseEntity.accepted().body(outputEmployeeDto.getEmployee())
//                : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(outputEmployeeDto.getEmployee());
//    }

//    @PutMapping(value = "/{employeeId}/{event}", produces = MediaType.APPLICATION_JSON_VALUE)
//    public Mono<ResponseEntity<String>> changeState(@PathVariable Long employeeId, @PathVariable EmployeeEvent event) {
//        return employeeService.changeEmployeeState(employeeId, event)
//                .map(s -> ResponseEntity.ok().body("It's accepted"))
////                .onErrorMap(TimeoutException.class, throwable -> )
//                .onErrorResume(e -> Mono.just(e.getMessage())
//                        .map(s -> ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(s)));
//    }


//
//    @PutMapping(value = "/{employeeId}/{event}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<ServerSentEvent<Object>> changeState(@PathVariable Long employeeId, @PathVariable EmployeeEvent event) {
//
//        return kafkaService.changeEmployeeState(employeeId, event)
//                .flux()
//                .map(voidSenderResult -> ServerSentEvent.builder().data("It's OK! Wait for response...").build())
//                .onErrorResume(throwable -> Mono.just(ServerSentEvent.builder().data(throwable.getMessage()).build()))
//                .concatWith(kafkaService.employee(employeeId).map(employee -> ServerSentEvent.builder().data(employee).build()));
////        return StateMachineEventResult.ResultType.ACCEPTED == outputEmployeeDto.getResultType() ?
////                ResponseEntity.accepted().body(outputEmployeeDto.getEmployee())
////                : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(outputEmployeeDto.getEmployee());
//    }
}
