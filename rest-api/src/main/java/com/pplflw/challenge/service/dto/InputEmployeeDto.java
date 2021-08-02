package com.pplflw.challenge.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Input employee DTO. Also describes validation rules for it's fields.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class InputEmployeeDto {

    @NotBlank
    private String name;

    private String contractInfo;

    @NotNull
    @Min(value = 18)
    private int age;
}

