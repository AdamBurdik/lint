package me.adamix.lint;

import me.adamix.lint.diagnostics.Diagnostic;
import me.adamix.lint.diagnostics.Note;
import me.adamix.lint.diagnostics.Span;
import me.adamix.lint.validator.Context;
import me.adamix.lint.validator.Validator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class Cursor {

    /** How to parse a scalar into {@code T} and how to describe it in diagnostics. */
    private record Kind<T>(Function<String, T> parser, String expected, String label) {}
    private static final Kind<Integer> INT = new Kind<>(Integer::parseInt, "a whole number", "not a whole number");
    private static final Kind<Float> FLOAT = new Kind<>(Float::parseFloat, "a float number", "not a float number");
    private static final Kind<Double> DOUBLE = new Kind<>(Double::parseDouble, "a double number", "not a double number");
    private static final Kind<Boolean> BOOLEAN = new Kind<>(Cursor::parseBoolean, "a boolean", "not a boolean");
    private static final Kind<String> STRING = new Kind<>(Function.identity(), "a string", "not a string");

    private final String file;
    private final List<Diagnostic> diagnostics;
    private final Span owner;
    private final Map<String, NodeTuple> entries = new LinkedHashMap<>();
    private final Set<String> known = new LinkedHashSet<>();

    public Cursor(
            @NotNull String file,
            @NotNull MappingNode mappingNode,
            @NotNull List<Diagnostic> diagnostics,
            @NotNull Span owner
    ) {
        this.file = file;
        this.diagnostics = diagnostics;
        this.owner = owner;

        Map<String, Span> firstSeen = new HashMap<>();
        for (NodeTuple tuple : mappingNode.getValue()) {
            if (!(tuple.getKeyNode() instanceof ScalarNode keyNode)) continue;

            String name = keyNode.getValue();
            Span span = Span.of(file, keyNode);

            Span first = firstSeen.get(name);
            if (first != null) {
                diagnostics.add(Diagnostic.of(
                        Lint.DEFINED_TWICE,
                        "key `" + name + "` is defined twice",
                        span,
                        "redefined here",
                        List.of(new Note(first, "first defined here"))
                ));
            }
            firstSeen.put(name, span);
            entries.put(name, tuple);
        }
    }


    /**
     * Returns the scalar for {@code key}, or null if absent or not a plain value (reported).
     * */
    private @Nullable ScalarNode getScalar(@NotNull String key) {
        known.add(key);
        NodeTuple tuple = entries.get(key);
        if (tuple == null) return null;

        if (tuple.getValueNode() instanceof ScalarNode scalar) return scalar;

        diagnostics.add(Diagnostic.of(
                Lint.EXPECTED_PLAIN_VALUE,
                "expected a single value for `" + key + "`",
                Span.of(file, tuple.getValueNode()),
                "not a plain value"
        ));
        return null;
    }

    /**
     * Like {@link #getScalar}, but reports a missing key.
     * */
    private @Nullable ScalarNode requireScalar(@NotNull String key) {
        ScalarNode scalar = getScalar(key);
        if (scalar == null && !entries.containsKey(key)) reportMissing(key);
        return scalar;
    }

    private void reportMissing(@NotNull String key) {
        diagnostics.add(Diagnostic.of(
                Lint.MISSING_REQUIRED,
                "missing required key `" + key + "`",
                owner,
                "needs a `" + key + "`"
        ));
    }

    private <T> @Nullable T parse(@NotNull String key, @NotNull ScalarNode scalar, @NotNull Kind<T> kind) {
        try {
            return kind.parser().apply(scalar.getValue().trim());
        } catch (IllegalArgumentException e) {
            diagnostics.add(Diagnostic.of(
                    Lint.WRONG_VALUE_TYPE,
                    "expected " + kind.expected() + " for `" + key + "`, found `" + scalar.getValue() + "`",
                    Span.of(file, scalar),
                    kind.label()
            ));
            return null;
        }
    }

    private <T> @Nullable T required(@NotNull String key, @NotNull Kind<T> kind) {
        ScalarNode scalar = requireScalar(key);
        return scalar == null ? null : parse(key, scalar, kind);
    }

    private <T> @NotNull T withDefault(@NotNull String key, @NotNull T def, @NotNull Kind<T> kind) {
        ScalarNode scalar = getScalar(key);
        if (scalar == null) return def;

        T value = parse(key, scalar, kind);
        return value != null ? value : def;
    }

    private <T> @NotNull Optional<T> optional(@NotNull String key, @NotNull Kind<T> kind) {
        ScalarNode scalar = getScalar(key);
        if (scalar == null) return Optional.empty();

        return Optional.ofNullable(parse(key, scalar, kind));
    }

    private static boolean parseBoolean(String text) {
        if (text.equalsIgnoreCase("true")) return true;
        if (text.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException(text);
    }

    public @Nullable String oneOf(@NotNull String key, @NotNull Set<String> possibilities) {
        ScalarNode scalar = requireScalar(key);
        if (scalar == null) return null;

        String value = scalar.getValue();
        if (possibilities.contains(value)) return value;

        diagnostics.add(Diagnostic.of(
                Lint.UNKNOWN,
                "unknown " + key + " `" + value + "`",
                Span.of(file, scalar),
                "no such " + key,
                suggest(value, possibilities)
        ));
        return null;
    }

    // ---- string ----
    public @Nullable String getString(@NotNull String key) {
        ScalarNode scalar = requireScalar(key);
        return scalar == null ? null : scalar.getValue();
    }

    public @NotNull String getString(@NotNull String key, @NotNull String def) {
        ScalarNode scalar = getScalar(key);
        return scalar == null ? def : scalar.getValue();
    }

    public @NotNull Optional<String> optString(@NotNull String key) {
        return Optional.ofNullable(getScalar(key)).map(ScalarNode::getValue);
    }

    // ---- int ----
    public @Nullable Integer getInt(@NotNull String key) { return required(key, INT); }
    public int getInt(@NotNull String key, int def) { return withDefault(key, def, INT); }
    public @NotNull Optional<Integer> optInt(@NotNull String key) { return optional(key, INT); }

    // ---- float ----
    public @Nullable Float getFloat(@NotNull String key) { return required(key, FLOAT); }
    public float getFloat(@NotNull String key, float def) { return withDefault(key, def, FLOAT); }
    public @NotNull Optional<Float> optFloat(@NotNull String key) { return optional(key, FLOAT); }

    // ---- double ----
    public @Nullable Double getDouble(@NotNull String key) { return required(key, DOUBLE); }
    public double getDouble(@NotNull String key, double def) { return withDefault(key, def, DOUBLE); }
    public @NotNull Optional<Double> optDouble(@NotNull String key) { return optional(key, DOUBLE); }

    // ---- boolean ----
    public @Nullable Boolean getBoolean(@NotNull String key) { return required(key, BOOLEAN); }
    public boolean getBoolean(@NotNull String key, boolean def) { return withDefault(key, def, BOOLEAN); }
    public @NotNull Optional<Boolean> optBoolean(@NotNull String key) { return optional(key, BOOLEAN); }

    // ---- sections (nested mappings) ----

    /**
     * Optional nested mapping with fixed keys. Unknown keys inside it are reported.
     * */
    public void section(@NotNull String key, @NotNull Consumer<Cursor> body) {
        run(child(key, false), body);
    }

    /**
     * Like {@link #section}, but reports the key as missing when it is absent.
     * */
    public void requireSection(@NotNull String key, @NotNull Consumer<Cursor> body) {
        run(child(key, true), body);
    }

    /**
     * Optional nested mapping of arbitrary ids to mappings, like the top level of a file.
     * Each id's body gets its own cursor.
     */
    public void each(@NotNull String key, @NotNull Validator validator) {
        Cursor items = child(key, false);
        if (items == null) return;

        Context context = new Context(diagnostics);
        for (Map.Entry<String, NodeTuple> entry : items.entries.entrySet()) {
            String id = entry.getKey();
            NodeTuple tuple = entry.getValue();
            items.known.add(id);

            if (!(tuple.getValueNode() instanceof MappingNode body)) {
                mustBeMapping("item `" + id + "`", tuple.getValueNode());
                continue;
            }

            Cursor cursor = new Cursor(file, body, diagnostics, Span.of(file, tuple.getKeyNode()));
            validator.accept(id, context, cursor);
            cursor.finish();
        }
    }

    /**
     * Optional list of mappings; the body runs once per item.
     * */
    public void sectionList(@NotNull String key, @NotNull Consumer<Cursor> body) {
        SequenceNode sequence = getSequence(key);
        if (sequence == null) return;

        int index = 0;
        for (Node item : sequence.getValue()) {
            if (item instanceof MappingNode mapping) {
                run(new Cursor(file, mapping, diagnostics, Span.of(file, item)), body);
            } else {
                mustBeMapping("`" + key + "[" + index + "]`", item);
            }
            index++;
        }
    }

    private @Nullable Cursor child(@NotNull String key, boolean required) {
        known.add(key);
        NodeTuple tuple = entries.get(key);
        if (tuple == null) {
            if (required) reportMissing(key);
            return null;
        }

        if (tuple.getValueNode() instanceof MappingNode body) {
            return new Cursor(file, body, diagnostics, Span.of(file, tuple.getKeyNode()));
        }
        mustBeMapping("`" + key + "`", tuple.getValueNode());
        return null;
    }

    private static void run(@Nullable Cursor cursor, @NotNull Consumer<Cursor> body) {
        if (cursor == null) return;
        body.accept(cursor);
        cursor.finish();
    }

    private void mustBeMapping(@NotNull String subject, @NotNull Node found) {
        diagnostics.add(Diagnostic.of(
                Lint.MUST_BE_MAPPING,
                subject + " must be a mapping",
                Span.of(file, found),
                "found " + describe(found) + ", expected a mapping"
        ));
    }

    // ---- lists ----

    /**
     * Reads each list item with {@code reader}; null results are dropped. Absent key gives an empty list.
     * */
    public <T> @NotNull List<T> list(@NotNull String key, @NotNull ItemReader<T> reader) {
        SequenceNode sequence = getSequence(key);
        if (sequence == null) return List.of();

        List<T> result = new ArrayList<>();
        int index = 0;
        for (Node item : sequence.getValue()) {
            T value = reader.read(index++, item);
            if (value != null) result.add(value);
        }
        return result;
    }

    public @NotNull List<String> stringList(@NotNull String key) { return scalarList(key, STRING); }
    public @NotNull List<Integer> intList(@NotNull String key) { return scalarList(key, INT); }
    public @NotNull List<Float> floatList(@NotNull String key) { return scalarList(key, FLOAT); }
    public @NotNull List<Double> doubleList(@NotNull String key) { return scalarList(key, DOUBLE); }
    public @NotNull List<Boolean> booleanList(@NotNull String key) { return scalarList(key, BOOLEAN); }

    private <T> @NotNull List<T> scalarList(@NotNull String key, @NotNull Kind<T> kind) {
        return list(key, (index, node) -> {
            String itemKey = key + "[" + index + "]";
            if (node instanceof ScalarNode scalar) return parse(itemKey, scalar, kind);

            diagnostics.add(Diagnostic.of(
                    Lint.EXPECTED_PLAIN_VALUE,
                    "expected a single value for `" + itemKey + "`",
                    Span.of(file, node),
                    "found " + describe(node)
            ));
            return null;
        });
    }

    private @Nullable SequenceNode getSequence(@NotNull String key) {
        known.add(key);
        NodeTuple tuple = entries.get(key);
        if (tuple == null) return null;

        Node value = tuple.getValueNode();
        if (value instanceof SequenceNode sequence) return sequence;

        diagnostics.add(Diagnostic.of(
                Lint.WRONG_VALUE_TYPE,
                "expected a list for `" + key + "`",
                Span.of(file, value),
                "found " + describe(value) + ", expected a list"
        ));
        return null;
    }

    @FunctionalInterface
    public interface ItemReader<T> {
        @Nullable T read(int index, @NotNull Node node);
    }

    private static String describe(Node node) {
        if (node instanceof ScalarNode) return "a value";
        if (node instanceof SequenceNode) return "a list";
        return "a mapping";
    }


    /**
     * Reports every key that was never read through this cursor.
     * */
    public void finish() {
        for (Map.Entry<String, NodeTuple> entry : entries.entrySet()) {
            String key = entry.getKey();
            if (known.contains(key)) continue;

            diagnostics.add(Diagnostic.of(
                    Lint.UNKNOWN,
                    "unknown key `" + key + "`",
                    Span.of(file, entry.getValue().getKeyNode()),
                    "not a recognised option",
                    suggest(key, known)
            ));
        }
    }
    
    static @Nullable String suggest(@NotNull String input, @NotNull Collection<String> candidates) {
        String lowerInput = input.toLowerCase();
        String best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (String candidate : candidates) {
            int distance = levenshtein(lowerInput, candidate.toLowerCase());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }

        boolean closeEnough = best != null && bestDistance <= Math.max(1, input.length() / 3);
        return closeEnough ? "did you mean `" + best + "`?" : null;
    }

    /**
     * Levenshtein edit distance: the number of single-character edits
     * (insert, delete, substitute) needed to turn one string into another.
     */
    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) prev[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int substitution = prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                cur[j] = Math.min(Math.min(cur[j - 1], prev[j]) + 1, substitution);
            }
            int[] swap = prev;
            prev = cur;
            cur = swap;
        }
        return prev[b.length()];
    }
}