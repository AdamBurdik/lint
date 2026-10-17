package me.adamix.lint.diagnostics;


import me.adamix.lint.error.DiagnosticError;
import me.adamix.lint.error.Severity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record Diagnostic(
        @NotNull DiagnosticError error,
        @NotNull String message,
        @NotNull Span span,
        @Nullable String label,
        @Nullable List<Note> notes,
        @Nullable String help
) {
    public Diagnostic(
            @NotNull DiagnosticError error,
            @NotNull String message,
            @NotNull Span span,
            @NotNull String label,
            @Nullable List<Note> notes,
            @Nullable String help
    ) {
        this.error = error;
        this.message = message;
        this.span = span;
        this.label = label;
        this.notes = Objects.requireNonNullElse(notes, Collections.emptyList());
        this.help = help;
    }
    
    public static Diagnostic of(
            @NotNull DiagnosticError error,
            @NotNull String message,
            @NotNull Span span,
            @NotNull String label,
            @NotNull List<Note> notes,
            @NotNull String help
    ) {
        return new Diagnostic(error, message, span, label, notes, help);
    }

    public static Diagnostic of(
            @NotNull DiagnosticError error,
            @NotNull String message,
            @NotNull Span span,
            @NotNull String label,
            @NotNull List<Note> notes
    ) {
        return new Diagnostic(error, message, span, label, notes, null);
    }

    public static Diagnostic of(
            @NotNull DiagnosticError error,
            @NotNull String message,
            @NotNull Span span,
            @NotNull String label
    ) {
        return new Diagnostic(error, message, span, label, null, null);
    }

    public static Diagnostic of(
            @NotNull DiagnosticError error,
            @NotNull String message,
            @NotNull Span span,
            @NotNull String label,
            @Nullable String help
    ) {
        return new Diagnostic(error, message, span, label, null, help);
    }
}
