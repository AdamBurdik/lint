package me.adamix.lint.utils;

import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

public class StringUtils {
    public static @NotNull String describe(@NotNull Node node) {
        if (node instanceof ScalarNode) return "a value";
        if (node instanceof SequenceNode) return "a list";
        return "something else";
    }
}
