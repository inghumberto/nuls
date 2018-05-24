package io.nuls.contract.vm.util;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Validators {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    public static <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
        return VALIDATOR.validate(object, groups);
    }

    public static <T> void validateWithException(T object, Class<?>... groups) throws ConstraintViolationException {
        Set<ConstraintViolation<T>> constraintViolations = validate(object, groups);
        if (!constraintViolations.isEmpty()) {
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    public static <T> void validateWithRuntimeException(T object, Class<?>... groups) {
        Set<ConstraintViolation<T>> constraintViolations = validate(object, groups);
        if (!constraintViolations.isEmpty()) {
            String message = message(constraintViolations);
            throw new RuntimeException(message);
        }
    }

    public static List<String> extractMessage(ConstraintViolationException e) {
        return extractMessage(e.getConstraintViolations());
    }

    public static List<String> extractMessage(Set<? extends ConstraintViolation> constraintViolations) {
        return constraintViolations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toList());
    }

    public static Map<String, String> extractPropertyAndMessage(ConstraintViolationException e) {
        return extractPropertyAndMessage(e.getConstraintViolations());
    }

    public static Map<String, String> extractPropertyAndMessage(Set<? extends ConstraintViolation> constraintViolations) {
        Map<String, String> errorMessages = Maps.newHashMap();
        for (ConstraintViolation violation : constraintViolations) {
            errorMessages.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        return errorMessages;
    }

    public static List<String> extractPropertyAndMessageAsList(ConstraintViolationException e) {
        return extractPropertyAndMessageAsList(e.getConstraintViolations(), " ");
    }

    public static List<String> extractPropertyAndMessageAsList(Set<? extends ConstraintViolation> constraintViolations) {
        return extractPropertyAndMessageAsList(constraintViolations, " ");
    }

    public static List<String> extractPropertyAndMessageAsList(ConstraintViolationException e, String separator) {
        return extractPropertyAndMessageAsList(e.getConstraintViolations(), separator);
    }

    public static List<String> extractPropertyAndMessageAsList(Set<? extends ConstraintViolation> constraintViolations, String separator) {
        return constraintViolations.stream().map(o -> o.getPropertyPath() + separator + o.getMessage()).collect(Collectors.toList());
    }

    public static <T> String message(Set<ConstraintViolation<T>> constraintViolations) {
        Map<String, String> errorMessages = extractPropertyAndMessage(constraintViolations);
        return Joiner.on(", ").withKeyValueSeparator(" ").join(errorMessages);
    }

}