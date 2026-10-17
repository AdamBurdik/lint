package me.adamix.lint;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.adamix.lint.config.DiagnosticsConfig;
import me.adamix.lint.diagnostics.Diagnostic;
import me.adamix.lint.error.Severity;

import java.util.List;

@RequiredArgsConstructor
public class Result {
    private final DiagnosticsConfig diagnosticsConfig;
    @Getter
    private final List<Diagnostic> diagnostics;
    
    public boolean hasErrors() {
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnosticsConfig.severityOf(diagnostic.error()) == Severity.ERROR) {
                return true;
            }
        }
        return false;
    }
}
