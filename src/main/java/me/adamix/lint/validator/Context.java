package me.adamix.lint.validator;

import lombok.RequiredArgsConstructor;
import me.adamix.lint.diagnostics.Diagnostic;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RequiredArgsConstructor
public class Context {
    private final List<Diagnostic> diagnostics;
    
    public void addDiagnostic(@NotNull Diagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }
}
