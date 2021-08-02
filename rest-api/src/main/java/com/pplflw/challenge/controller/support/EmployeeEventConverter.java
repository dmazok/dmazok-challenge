package com.pplflw.challenge.controller.support;

import com.pplflw.challenge.statemachine.EmployeeEvent;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class EmployeeEventConverter implements Converter<String, EmployeeEvent> {

    @Override
    public EmployeeEvent convert(String value) {
        return EmployeeEvent.valueOf(value.toUpperCase());
    }
}