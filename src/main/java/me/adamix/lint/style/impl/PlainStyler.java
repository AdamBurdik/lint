package me.adamix.lint.style.impl;

import me.adamix.lint.style.Style;
import me.adamix.lint.style.Styler;

final class PlainStyler implements Styler {
    public String apply(Style style, String text) { return text; }
}