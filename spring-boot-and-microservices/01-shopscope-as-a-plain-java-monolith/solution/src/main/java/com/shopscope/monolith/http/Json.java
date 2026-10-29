package com.shopscope.monolith.http;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A hand-rolled JSON encoder and a "parser" that only understands one flat object with string and
 * integer values (insertion-ordered maps keep the field order). It is here so you can feel what a real message converter saves you. Nested objects,
 * arrays, escapes and unicode in input: not handled. Demo 5 replaces it with Jackson.
 */
public final class Json {

    private static final Pattern FIELD = Pattern.compile("\"(\\w+)\"\\s*:\\s*(?:\"([^\"]*)\"|(\\d+))");

    private Json() {
    }

    public static String object(Map<String, Object> fields) {
        return fields.entrySet().stream()
                .map(e -> quote(e.getKey()) + ":" + value(e.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    public static String array(List<?> values) {
        return values.stream().map(Json::value).collect(Collectors.joining(",", "[", "]"));
    }

    @SuppressWarnings("unchecked")
    private static String value(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            return object((Map<String, Object>) map);
        }
        if (value instanceof List<?> list) {
            return array(list);
        }
        return quote(value.toString());
    }

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /** Reads {@code {"a":"x","b":2}} into a map. Anything else is silently misread. */
    public static Map<String, String> parseFlatObject(String json) {
        Matcher matcher = FIELD.matcher(json);
        var fields = new java.util.LinkedHashMap<String, String>();
        while (matcher.find()) {
            fields.put(matcher.group(1), matcher.group(2) != null ? matcher.group(2) : matcher.group(3));
        }
        return fields;
    }
}
