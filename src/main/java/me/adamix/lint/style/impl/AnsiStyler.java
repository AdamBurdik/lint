package me.adamix.lint.style.impl;

import me.adamix.lint.style.Style;
import me.adamix.lint.style.Styler;

public final class AnsiStyler implements Styler {
    private static final String RESET = "\u001B[0m";

    public String apply(Style style, String text) {
        if (text.isEmpty()) return text;
        String code = switch (style) {
            case ERROR   -> "\u001B[1;31m"; // bold red
            case WARNING -> "\u001B[1;33m"; // bold yellow
            case NOTE    -> "\u001B[1;36m"; // bold cyan
            case GUTTER  -> "\u001B[36m";   // cyan
            case HELP    -> "\u001B[32m";   // green
        };
        return code + text + RESET;
    }
}