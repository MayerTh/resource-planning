package org.resourceplanning.io;

import java.util.List;

public class DataValidationException extends RuntimeException {

    private final List<String> errors;

    public DataValidationException(List<String> errors) {
        super(buildMessage(errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }

    private static String buildMessage(List<String> errors) {
        StringBuilder message = new StringBuilder(errors.size() + " validation error(s) found:");
        for (String error : errors) {
            message.append("\n  - ").append(error);
        }
        return message.toString();
    }
}
