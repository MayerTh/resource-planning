package org.resourceplanning.domain;

import java.util.Optional;

public enum PaySchema {
    D3, D4, D5;

    public static Optional<PaySchema> fromCode(String code) {
        for (PaySchema schema : values()) {
            if (schema.name().equalsIgnoreCase(code)) {
                return Optional.of(schema);
            }
        }
        return Optional.empty();
    }
}
