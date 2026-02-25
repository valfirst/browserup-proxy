package com.browserup.bup.proxy.mitmproxy.validation;

import com.browserup.bup.rest.validation.HttpStatusCodeConstraint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.validation.ConstraintValidatorContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

class HttpStatusCodeConstraintTest {

    @Test
    void validStatus() {
        HttpStatusCodeConstraint.HttpStatusCodeValidator validator = new HttpStatusCodeConstraint.HttpStatusCodeValidator();
        String status = "400";
        ConstraintValidatorContext mockedContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockedBuilder);

        boolean result = validator.isValid(status, mockedContext);

        Assertions.assertTrue(result, "Expected http status validation to pass");
    }

    @Test
    void inValidStatusFormat() {
        HttpStatusCodeConstraint.HttpStatusCodeValidator validator = new HttpStatusCodeConstraint.HttpStatusCodeValidator();
        String status = "400invalid";
        ConstraintValidatorContext mockedContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockedBuilder);

        boolean result = validator.isValid(status, mockedContext);

        Assertions.assertFalse(result, "Expected http status validation to fail");
    }

    @Test
    void inValidStatusRange() {
        HttpStatusCodeConstraint.HttpStatusCodeValidator validator = new HttpStatusCodeConstraint.HttpStatusCodeValidator();
        String status = "699";
        ConstraintValidatorContext mockedContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockedBuilder);

        boolean result = validator.isValid(status, mockedContext);

        Assertions.assertFalse(result, "Expected http status validation to fail");
    }
}
