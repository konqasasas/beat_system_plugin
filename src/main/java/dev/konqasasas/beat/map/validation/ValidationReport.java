package dev.konqasasas.beat.map.validation;

import java.util.ArrayList;
import java.util.List;

public final class ValidationReport {
    private final List<ValidationIssue> issues = new ArrayList<>();

    public void error(String code, String message) {
        issues.add(new ValidationIssue(ValidationSeverity.ERROR, code, message));
    }

    public void warning(String code, String message) {
        issues.add(new ValidationIssue(ValidationSeverity.WARNING, code, message));
    }

    public void include(ValidationReport other) {
        issues.addAll(other.issues);
    }

    public List<ValidationIssue> issues() {
        return List.copyOf(issues);
    }

    public long errorCount() {
        return count(ValidationSeverity.ERROR);
    }

    public long warningCount() {
        return count(ValidationSeverity.WARNING);
    }

    public boolean passed() {
        return errorCount() == 0;
    }

    private long count(ValidationSeverity severity) {
        return issues.stream().filter(issue -> issue.severity() == severity).count();
    }
}
