package com.pplflw.challenge.dto;

import com.pplflw.challenge.statemachine.EmployeeEvent;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@EqualsAndHashCode(callSuper = false)
public class EmployeeChangeStateEventDto extends EmployeeEventDto {
    private Long employeeId;
    private EmployeeEvent event;
}
