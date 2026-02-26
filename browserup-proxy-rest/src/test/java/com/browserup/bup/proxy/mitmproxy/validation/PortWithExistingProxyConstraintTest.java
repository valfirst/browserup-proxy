package com.browserup.bup.proxy.mitmproxy.validation;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.MitmProxyManager;
import com.browserup.bup.rest.validation.PortWithExistingProxyConstraint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintValidatorContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PortWithExistingProxyConstraintTest {

    @Test
    void validPort() {
        int port = 10;
        MitmProxyManager mockedProxyManager = mock(MitmProxyManager.class);
        ConstraintValidatorContext mockedContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        PortWithExistingProxyConstraint.PortWithExistingProxyConstraintValidator validator = new PortWithExistingProxyConstraint.PortWithExistingProxyConstraintValidator(mockedProxyManager);

        when(mockedContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockedBuilder);
        when(mockedProxyManager.get(eq(port))).thenReturn(mock(MitmProxyServer.class));

        boolean result = validator.isValid(port, mockedContext);

        Assertions.assertTrue(result, "Expected port validation to pass");
    }

    @Test
    void invalidPort() {
        int port = 10;
        int nonExistingProxyPort = 100;
        MitmProxyManager mockedProxyManager = mock(MitmProxyManager.class);
        PortWithExistingProxyConstraint.PortWithExistingProxyConstraintValidator validator = new PortWithExistingProxyConstraint.PortWithExistingProxyConstraintValidator(mockedProxyManager);
        ConstraintValidatorContext mockedContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext mockedCustomContext = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);

        when(mockedContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockedBuilder);
        when(mockedBuilder.addPropertyNode(anyString())).thenReturn(mockedCustomContext);
        when(mockedProxyManager.get(eq(nonExistingProxyPort))).thenReturn(mock(MitmProxyServer.class));

        boolean result = validator.isValid(port, mockedContext);

        Assertions.assertFalse(result, "Expected port validation to fail");
    }
}
