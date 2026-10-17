package me.adamix.lint.translation;

import me.adamix.lint.diagnostics.Diagnostic;
import me.adamix.lint.translation.impl.StandardExceptionTranslation;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import java.util.List;

public interface ExceptionTranslation {
    @NotNull Diagnostic translate(
            @NotNull String file,
            @NotNull MarkedYAMLException exception,
            @NotNull List<String> lines
    );
    
    static ExceptionTranslation standard() {
        return new StandardExceptionTranslation();
    }
}
