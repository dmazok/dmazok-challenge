package com.pplflw.challenge.dto;

import com.pplflw.challenge.domain.Employee;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
public class EmployeeAddEventDto extends EmployeeEventDto {

    private Employee employee;
}

