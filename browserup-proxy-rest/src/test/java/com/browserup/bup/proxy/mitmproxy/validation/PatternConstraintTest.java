package com.browserup.bup.proxy.mitmproxy.validation;

import com.browserup.bup.rest.validation.PatternConstraint;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.validation.ConstraintValidatorContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

public class PatternConstraintTest {

    @Test
    public void validPattern() {
        PatternConstraint.PatternValidator validator = new PatternConstraint.PatternValidator();
        String pattern = ".*";
        ConstraintValidatorContext mockedContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockedBuilder);

        boolean result = validator.isValid(pattern, mockedContext);

        Assert.assertTrue("Expected pattern validation to pass", result);
    }

    @Test
    public void invalidPattern() {
        PatternConstraint.PatternValidator validator = new PatternConstraint.PatternValidator();
        String pattern = "[";
        ConstraintValidatorContext mockedContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockedBuilder);

        boolean result = validator.isValid(pattern, mockedContext);

        Assert.assertFalse("Expected pattern validation to fail", result);
    }
}
