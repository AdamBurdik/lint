package me.adamix.lint.error;

import org.jetbrains.annotations.NotNull;

public record DiagnosticError(
        @NotNull String namespace,
        @NotNull String path,
        @NotNull Severity defaultSeverity
) {
    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
