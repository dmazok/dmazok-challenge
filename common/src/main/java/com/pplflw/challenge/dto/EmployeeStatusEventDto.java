package com.pplflw.challenge.dto;

import com.pplflw.challenge.domain.Employee;
import com.pplflw.challenge.statemachine.EmployeeEvent;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@EqualsAndHashCode
public class EmployeeStatusEventDto {
    private EmployeeEvent event;
    private String result;
    private Employee employee;
}
