package dev.konqasasas.beat.map.validation;

import java.util.Objects;

public record ValidationIssue(ValidationSeverity severity, String code, String message) {
    public ValidationIssue {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
    }
}
