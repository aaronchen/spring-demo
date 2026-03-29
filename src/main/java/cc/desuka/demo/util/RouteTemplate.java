package cc.desuka.demo.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * A URL template with {@code {placeholder}} tokens that can be resolved to a concrete URL.
 *
 * <p>Works identically to the JavaScript {@code route()} helper emitted by {@link
 * FrontendConfigController}:
 *
 * <pre>
 * // Java
 * route.resolve(Map.of("projectId", 1))
 * route.resolve(Map.of("projectId", 1), Map.of("q", "test"))
 *
 * // JavaScript
 * route.resolve({ projectId: 1 })
 * route.resolve({ projectId: 1 }, { q: "test" })
 * </pre>
 *
 * <p>{@link #toString()} returns the raw template string, so a {@code RouteTemplate} works
 * transparently anywhere a plain string is expected (Thymeleaf expressions, config.js
 * serialization).
 */
public class RouteTemplate {

    private final String template;

    public RouteTemplate(String template) {
        this.template = template;
    }

    /** Returns the raw template with unresolved {@code {placeholder}} tokens. */
    public String getTemplate() {
        return template;
    }

    /** Resolve with no parameters — returns the raw template. */
    public String resolve() {
        return template;
    }

    /** Shorthand for a single path parameter (covers the most common case). */
    public String resolve(String key, Object value) {
        return template.replace("{" + key + "}", String.valueOf(value));
    }

    /** Resolve path parameters from a map. */
    public String resolve(Map<String, Object> params) {
        return resolve(params, Map.of());
    }

    /** Resolve path parameters and append query parameters. */
    public String resolve(Map<String, Object> params, Map<String, Object> query) {
        String url = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            url = url.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        if (query != null && !query.isEmpty()) {
            StringJoiner joiner = new StringJoiner("&");
            for (Map.Entry<String, Object> entry : query.entrySet()) {
                String key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
                Object val = entry.getValue();
                if (val == null || val.toString().isEmpty()) {
                    joiner.add(key);
                } else {
                    joiner.add(
                            key + "=" + URLEncoder.encode(val.toString(), StandardCharsets.UTF_8));
                }
            }
            url = url + "?" + joiner;
        }
        return url;
    }

    /**
     * JavaScript {@code Route} class that mirrors this Java class's resolve API. Emitted by {@code
     * FrontendConfigController} into {@code /config.js} so that frontend route objects support the
     * same {@code resolve(params, query)} contract.
     */
    public static final String JS_CLASS =
            """
            class Route {
                constructor(template) {
                    this.template = template;
                }
                resolve(params, query) {
                    let url = this.template;
                    if (params) {
                        for (const [k, v] of Object.entries(params)) {
                            url = url.replace('{' + k + '}', encodeURIComponent(v));
                        }
                    }
                    if (query) {
                        const parts = Object.entries(query).map(([k, v]) =>
                            v == null || v === '' ? encodeURIComponent(k) : encodeURIComponent(k) + '=' + encodeURIComponent(v)
                        );
                        if (parts.length) url = url + '?' + parts.join('&');
                    }
                    return url;
                }
                toString() { return this.template; }
                valueOf() { return this.template; }
            }
            """;

    /**
     * Returns the raw template string. This allows {@code RouteTemplate} to be used transparently
     * in Thymeleaf expressions and string concatenation.
     */
    @Override
    public String toString() {
        return template;
    }

    /** Allows Spring {@code @ConfigurationProperties} to bind plain strings to RouteTemplate. */
    @Component
    @ConfigurationPropertiesBinding
    static class StringConverter implements Converter<String, RouteTemplate> {

        @Override
        public RouteTemplate convert(String source) {
            return new RouteTemplate(source);
        }
    }
}
