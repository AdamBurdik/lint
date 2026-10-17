package me.adamix.lint.diagnostics;

import org.yaml.snakeyaml.nodes.Node;

public record Span(
        String file,
        Pos start,
        Pos end
) {
    public static Span of(String file, Node valueNode) {
        return new Span(
                file,
                new Pos(
                        valueNode.getStartMark().getLine(),
                        valueNode.getStartMark().getColumn()
                ),
                new Pos(
                        valueNode.getEndMark().getLine(),
                        valueNode.getEndMark().getColumn()
                )
        );
    }
}
