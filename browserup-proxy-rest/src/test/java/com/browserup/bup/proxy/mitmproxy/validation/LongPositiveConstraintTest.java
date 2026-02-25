package com.browserup.bup.proxy.mitmproxy.validation;

import com.browserup.bup.rest.validation.LongPositiveConstraint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.validation.ConstraintValidatorContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

class LongPositiveConstraintTest {

    @Test
    void validLongPositive() {
        LongPositiveConstraint.LongPositiveValidator validator = new LongPositiveConstraint.LongPositiveValidator();
        String value = "10000";
        ConstraintValidatorContext mockedContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockedBuilder);

        boolean result = validator.isValid(value, mockedContext);

        Assertions.assertTrue(result, "Expected long positive validation to pass");
    }

    @Test
    void invalidLongNegative() {
        LongPositiveConstraint.LongPositiveValidator validator = new LongPositiveConstraint.LongPositiveValidator();
        String value = "-1";
        ConstraintValidatorContext mockedContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockedBuilder);

        boolean result = validator.isValid(value, mockedContext);

        Assertions.assertFalse(result, "Expected long positive validation to fail");
    }

    @Test
    void invalidLongFormat() {
        LongPositiveConstraint.LongPositiveValidator validator = new LongPositiveConstraint.LongPositiveValidator();
        String value = "invalid_format";
        ConstraintValidatorContext mockedContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockedBuilder);

        boolean result = validator.isValid(value, mockedContext);

        Assertions.assertFalse(result, "Expected long positive validation to fail");
    }
}
