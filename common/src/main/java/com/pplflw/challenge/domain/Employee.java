package com.pplflw.challenge.domain;

import com.pplflw.challenge.statemachine.EmployeeState;
import lombok.*;

/**
 * Entity to represent an employee.
 */
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class Employee {

    @Getter
    private Long id;

    @Getter
    @Setter
    private EmployeeState state;

    @Getter
    private String name;

    @Getter
    private String contractInfo;

    @Getter
    private int age;
}
