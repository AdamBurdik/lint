package me.adamix.lint.translation.impl;

import me.adamix.lint.Lint;
import me.adamix.lint.diagnostics.Diagnostic;
import me.adamix.lint.diagnostics.Note;
import me.adamix.lint.diagnostics.Pos;
import me.adamix.lint.diagnostics.Span;
import me.adamix.lint.error.Severity;
import me.adamix.lint.translation.ExceptionTranslation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import java.util.ArrayList;
import java.util.List;

public class StandardExceptionTranslation implements ExceptionTranslation {
    @Override
    public @NotNull Diagnostic translate(
            @NotNull String file,
            @NotNull MarkedYAMLException exception,
            @NotNull List<String> lines
    ) {
        Mark p = exception.getProblemMark();
        Mark c = exception.getContextMark();

        // Hardcoded messages
        if (c != null
                && "while scanning a simple key".equals(exception.getContext())
                && "could not find expected ':'".equals(exception.getProblem())) {
            return new Diagnostic(
                    Lint.INVALID_YML,
                    "expected `:` after key",
                    restOfLine(file, c, lines),
                    "this key has no value",
                    List.of(),
                    "add `:` and a value, or remove this line"
            );
        }

        List<Note> notes = new ArrayList<>();
        if (c != null && exception.getContext() != null) {
            notes.add(new Note(pointSpan(file, c), exception.getContext()));
        }
        Span primary = p != null
                ? pointSpan(file, p)
                : new Span(file, new Pos(0, 0), new Pos(0, 1));
        
        return new Diagnostic(
                Lint.INVALID_YML,
                "invalid YAML: " + exception.getProblem(),
                primary,
                "unexpected here",
                notes,
                null
        );
    }

    private @NotNull Span pointSpan(@NotNull String file, @NotNull Mark m) {
        Pos start = new Pos(m.getLine(), m.getColumn());
        Pos end = new Pos(m.getLine(), m.getColumn() + 1);
        return new Span(file, start, end);
    }

    private @NotNull Span restOfLine(
            @NotNull String file,
            @NotNull Mark m,
            @Nullable List<String> lines
    ) {
        int line = m.getLine();
        int start = m.getColumn();
        int end = start + 1;
        if (lines != null && line >= 0 && line < lines.size()) {
            end = Math.max(end, lines.get(line).stripTrailing().length());
        }
        return new Span(file, new Pos(line, start), new Pos(line, end));
    }
}
