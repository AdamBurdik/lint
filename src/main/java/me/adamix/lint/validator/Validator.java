package me.adamix.lint.validator;

import me.adamix.lint.Cursor;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Validator {
    void accept(
            @NotNull String rootId,
            @NotNull Context context,
            @NotNull Cursor cursor
    );
}
