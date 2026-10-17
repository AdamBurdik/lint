package me.adamix.lint.config;

import lombok.Builder;
import me.adamix.lint.Lint;
import me.adamix.lint.error.DiagnosticError;
import me.adamix.lint.error.Severity;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Builder
public class DiagnosticsConfig {
    @Builder.Default
    private final Yaml yaml = Lint.simpleYaml();

    private final Map<String, Severity> map = new HashMap<>();

    public void loadFile(@NotNull Path path) throws IOException {
        Node root;
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            root = yaml.compose(reader);
        }
        if (root == null) return;

        for (NodeTuple entry : asMapping(root, "<root>").getValue()) {
            if (scalar(entry.getKeyNode()).equals("severity")) {
                parseSeverity(asMapping(entry.getValueNode(), "severity"));
            }
        }
    }

    public Severity severityOf(DiagnosticError error) {
        return map.getOrDefault(key(error.namespace(), error.path()), error.defaultSeverity());
    }
    
    private void parseSeverity(MappingNode severity) {
        for (NodeTuple tuple : severity.getValue()) {
            String namespace = scalar(tuple.getKeyNode());
            MappingNode ids = asMapping(tuple.getValueNode(), "severity." + namespace);

            for (NodeTuple e : ids.getValue()) {
                String id = scalar(e.getKeyNode());
                map.put(key(namespace, id), parseLevel(e.getValueNode()));
            }
        }
    }

    private static String key(String namespace, String id) {
        return (namespace + "." + id).toLowerCase(Locale.ROOT);
    }

    private static Severity parseLevel(Node node) {
        String value = scalar(node);
        try {
            return Severity.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw error(node, "Unknown severity '" + value + "', expected error, warning or off");
        }
    }

    private static MappingNode asMapping(Node node, String where) {
        if (node instanceof MappingNode m) return m;
        throw error(node, "'" + where + "' must be a mapping");
    }

    private static String scalar(Node node) {
        if (node instanceof ScalarNode s) return s.getValue();
        throw error(node, "Expected a plain value");
    }

    private static IllegalArgumentException error(Node node, String message) {
        Mark mark = node.getStartMark();
        return new IllegalArgumentException(
                message + " (line " + (mark.getLine() + 1) + ", column " + (mark.getColumn() + 1) + ")");
    }
}
