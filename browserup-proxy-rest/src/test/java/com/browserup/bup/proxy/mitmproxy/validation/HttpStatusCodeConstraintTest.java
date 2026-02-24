package com.browserup.bup.proxy.mitmproxy.validation;

import com.browserup.bup.rest.validation.HttpStatusCodeConstraint;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.validation.ConstraintValidatorContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

public class HttpStatusCodeConstraintTest {

    @Test
    public void validStatus() {
        HttpStatusCodeConstraint.HttpStatusCodeValidator validator = new HttpStatusCodeConstraint.HttpStatusCodeValidator();
        String status = "400";
        ConstraintValidatorContext mockedContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockedBuilder);

        boolean result = validator.isValid(status, mockedContext);

        Assert.assertTrue("Expected http status validation to pass", result);
    }

    @Test
    public void inValidStatusFormat() {
        HttpStatusCodeConstraint.HttpStatusCodeValidator validator = new HttpStatusCodeConstraint.HttpStatusCodeValidator();
        String status = "400invalid";
        ConstraintValidatorContext mockedContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockedBuilder);

        boolean result = validator.isValid(status, mockedContext);

        Assert.assertFalse("Expected http status validation to fail", result);
    }

    @Test
    public void inValidStatusRange() {
        HttpStatusCodeConstraint.HttpStatusCodeValidator validator = new HttpStatusCodeConstraint.HttpStatusCodeValidator();
        String status = "699";
        ConstraintValidatorContext mockedContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockedBuilder);

        boolean result = validator.isValid(status, mockedContext);

        Assert.assertFalse("Expected http status validation to fail", result);
    }
}
