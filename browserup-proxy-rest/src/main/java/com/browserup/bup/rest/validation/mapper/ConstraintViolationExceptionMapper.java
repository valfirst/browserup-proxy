package com.browserup.bup.rest.validation.mapper;

import com.browserup.bup.rest.validation.mapper.model.ConstraintsErrors;
import org.apache.commons.lang3.StringUtils;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
    private static final String PARAMETER_NAME_ATTRIBUTE = "paramName";

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(createConstraintErrors(exception))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    private ConstraintsErrors createConstraintErrors(ConstraintViolationException exception) {
        ConstraintsErrors errors = new ConstraintsErrors();
        exception.getConstraintViolations().stream()
                .filter(v -> StringUtils.isNotEmpty(v.getMessage()))
                .forEach(violation -> errors.addError(getArgumentName(violation), violation.getMessage()));

        return errors;
    }

    private String getArgumentName(ConstraintViolation<?> violation) {
        String argumentIdentifier;
        Object paramName = violation.getConstraintDescriptor().getAttributes().get(PARAMETER_NAME_ATTRIBUTE);
        if (paramName instanceof String && paramName.toString().length() > 0) {
            argumentIdentifier = (String) paramName;
        } else {
            argumentIdentifier = StringUtils.substringAfterLast(violation.getPropertyPath().toString(), ".");
        }
        return argumentIdentifier;
    }
}