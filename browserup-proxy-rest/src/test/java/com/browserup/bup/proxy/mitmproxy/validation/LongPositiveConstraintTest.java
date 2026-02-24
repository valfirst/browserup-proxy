package com.browserup.bup.proxy.mitmproxy.validation;

import com.browserup.bup.rest.validation.LongPositiveConstraint;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.validation.ConstraintValidatorContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

public class LongPositiveConstraintTest {

    @Test
    public void validLongPositive() {
        LongPositiveConstraint.LongPositiveValidator validator = new LongPositiveConstraint.LongPositiveValidator();
        String value = "10000";
        ConstraintValidatorContext mockedContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockedBuilder);

        boolean result = validator.isValid(value, mockedContext);

        Assert.assertTrue("Expected long positive validation to pass", result);
    }

    @Test
    public void invalidLongNegative() {
        LongPositiveConstraint.LongPositiveValidator validator = new LongPositiveConstraint.LongPositiveValidator();
        String value = "-1";
        ConstraintValidatorContext mockedContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockedBuilder);

        boolean result = validator.isValid(value, mockedContext);

        Assert.assertFalse("Expected long positive validation to fail", result);
    }

    @Test
    public void invalidLongFormat() {
        LongPositiveConstraint.LongPositiveValidator validator = new LongPositiveConstraint.LongPositiveValidator();
        String value = "invalid_format";
        ConstraintValidatorContext mockedContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockedBuilder);

        boolean result = validator.isValid(value, mockedContext);

        Assert.assertFalse("Expected long positive validation to fail", result);
    }
}
