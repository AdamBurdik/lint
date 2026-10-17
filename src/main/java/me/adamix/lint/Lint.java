package me.adamix.lint;

import com.sun.nio.file.ExtendedWatchEventModifier;
import lombok.Builder;
import me.adamix.lint.config.DiagnosticsConfig;
import me.adamix.lint.diagnostics.Diagnostic;
import me.adamix.lint.diagnostics.Span;
import me.adamix.lint.error.DiagnosticError;
import me.adamix.lint.error.Severity;
import me.adamix.lint.render.AnsiRenderer;
import me.adamix.lint.render.DiagnosticsRenderer;
import me.adamix.lint.translation.ExceptionTranslation;
import me.adamix.lint.validator.Context;
import me.adamix.lint.validator.Validator;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import javax.tools.DiagnosticCollector;
import java.io.DataInput;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static me.adamix.lint.utils.StringUtils.describe;

public class Lint {
    private final DiagnosticsRenderer renderer;
    private final ExceptionTranslation exceptionTranslation;
    private final Yaml yaml;
    private final DiagnosticsConfig diagnosticsConfig;
    
    private final Map<String, List<String>> sources = new HashMap<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    @Builder
    private Lint(
            DiagnosticsRenderer renderer,
            Function<DiagnosticsConfig, DiagnosticsRenderer> rendererFactory,
            ExceptionTranslation exceptionTranslation,
            Yaml yaml,
            DiagnosticsConfig diagnosticsConfig
    ) {
        this.yaml = yaml != null ? yaml : simpleYaml();
        this.diagnosticsConfig = diagnosticsConfig != null
                ? diagnosticsConfig
                : DiagnosticsConfig.builder().build();
        this.exceptionTranslation = exceptionTranslation;

        if (renderer != null) {
            this.renderer = renderer;
        } else if (rendererFactory != null) {
            this.renderer = rendererFactory.apply(this.diagnosticsConfig);
        } else {
            throw new IllegalStateException("No renderer configured");
        }
    }
    
    public @NotNull Result load(@NotNull Path path, @NotNull Validator validator) throws IOException {
        String file = path.toString();
        String text = Files.readString(path);
        
        sources.put(file, text.lines().toList());

        Context context = new Context(diagnostics);

        try {
            Node root = yaml.compose(new StringReader(text));

            if (root instanceof MappingNode top) {
                // Tuple is item each key.
                for (NodeTuple tuple : top.getValue()) {
                    if (!(tuple.getKeyNode() instanceof ScalarNode id)) continue;
                    if (!(tuple.getValueNode() instanceof MappingNode body)) {
                        String name = id.getValue();
                        diagnostics.add(Diagnostic.of(
                                Lint.MUST_BE_MAPPING,
                                "item `" + name + "` must be a mapping",
                                Span.of(file, tuple.getValueNode()),
                                "found " + describe(tuple.getValueNode()) + ", expected a mapping",
                                "nest keys under `" + name + ":`, for example\n"
                                        + name + ":\n"
                                        + "  key: value"
                        ));
                        continue;
                    }

                    Cursor cursor = new Cursor(file, body, diagnostics, Span.of(file, id));
                    
                    validator.accept(id.getValue(), context, cursor);

                    cursor.finish();
                }
            }
        } catch (MarkedYAMLException e) {
            diagnostics.add(exceptionTranslation.translate(file, e, sources.get(file)));
        }

        renderer.sout(diagnostics, sources);
        
        return new Result(diagnosticsConfig, diagnostics);
    }

    public static class LintBuilder {
        public LintBuilder ansiRenderer() {
            this.rendererFactory = AnsiRenderer::new;
            return this;
        }

        public LintBuilder standardExceptionTranslation() {
            this.exceptionTranslation = ExceptionTranslation.standard();
            return this;
        }
    }
    
    public static Yaml simpleYaml() {
        return new Yaml(new LoaderOptions());
    }
    
    public static final DiagnosticError
            EXPECTED_PLAIN_VALUE = error("EXPECTED_PLAIN_VALUE", Severity.ERROR),
            MISSING_REQUIRED = error("MISSING_REQUIRED", Severity.ERROR),
            UNKNOWN = error("UNKNOWN", Severity.WARNING),
            WRONG_VALUE_TYPE = error("WRONG_VALUE_TYPE", Severity.ERROR),
            DEFINED_TWICE = error("DEFINED_TWICE", Severity.ERROR),
            MUST_BE_MAPPING = error("MUST_BE_MAPPING", Severity.ERROR),
            INVALID_YML = error("INVALID_YML", Severity.ERROR);
    
    private static DiagnosticError error(@NotNull String path, @NotNull Severity defaultSeverity) {
        return new DiagnosticError("lint", path, defaultSeverity);
    }
}
