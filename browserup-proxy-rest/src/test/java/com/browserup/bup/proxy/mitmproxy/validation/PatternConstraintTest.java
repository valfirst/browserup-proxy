package com.browserup.bup.proxy.mitmproxy.validation;

import com.browserup.bup.rest.validation.PatternConstraint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.validation.ConstraintValidatorContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

class PatternConstraintTest {

    @Test
    void validPattern() {
        PatternConstraint.PatternValidator validator = new PatternConstraint.PatternValidator();
        String pattern = ".*";
        ConstraintValidatorContext mockedContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockedBuilder);

        boolean result = validator.isValid(pattern, mockedContext);

        Assertions.assertTrue(result, "Expected pattern validation to pass");
    }

    @Test
    void invalidPattern() {
        PatternConstraint.PatternValidator validator = new PatternConstraint.PatternValidator();
        String pattern = "[";
        ConstraintValidatorContext mockedContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockedBuilder);

        boolean result = validator.isValid(pattern, mockedContext);

        Assertions.assertFalse(result, "Expected pattern validation to fail");
    }
}
