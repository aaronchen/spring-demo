package cc.desuka.demo.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * A URL template with {@code {placeholder}} tokens that can be resolved to a concrete URL.
 *
 * <p>Builder API for constructing URLs:
 *
 * <pre>
 * // Single param
 * route.params("projectId", id).build()
 *
 * // Multiple params
 * route.params("projectId", id, "taskId", tid).build()
 *
 * // Params + query
 * route.params("projectId", id).query("view", "table").build()
 *
 * // Query only (no path params)
 * route.query("projectId", id).build()
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

    // ── Builder API (preferred for Thymeleaf and general use) ────────────

    /** Start building a URL with path parameters: {@code route.params("id", 1).build()}. */
    public Builder params(String key, Object value, Object... rest) {
        if (rest.length % 2 != 0) {
            throw new IllegalArgumentException("params() requires key-value pairs");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, value);
        for (int i = 0; i < rest.length; i += 2) {
            map.put(String.valueOf(rest[i]), rest[i + 1]);
        }
        return new Builder(template, map, Map.of());
    }

    /** Start building a URL with path parameters from a map. */
    public Builder params(Map<String, ?> values) {
        return new Builder(template, new LinkedHashMap<>(values), Map.of());
    }

    /** Start building a URL with query parameters (no path params). */
    public Builder query(String key, Object value, Object... rest) {
        if (rest.length % 2 != 0) {
            throw new IllegalArgumentException("query() requires key-value pairs");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, value);
        for (int i = 0; i < rest.length; i += 2) {
            map.put(String.valueOf(rest[i]), rest[i + 1]);
        }
        return new Builder(template, Map.of(), map);
    }

    /** Start building a URL with query parameters from a map (no path params). */
    public Builder query(Map<String, ?> values) {
        return new Builder(template, Map.of(), new LinkedHashMap<>(values));
    }

    // ── Builder inner class ──────────────────────────────────────────────

    /**
     * Immutable builder for constructing URLs. Supports fluent chaining of path parameters and
     * query parameters. Each method returns a new instance — the original is never mutated.
     *
     * <p>Created via {@link RouteTemplate#params} or {@link RouteTemplate#query}.
     */
    public static final class Builder {

        private final String template;
        private final Map<String, Object> params;
        private final Map<String, Object> query;

        private Builder(String template, Map<String, Object> params, Map<String, Object> query) {
            this.template = template;
            this.params = params;
            this.query = query;
        }

        public Builder params(String key, Object value, Object... rest) {
            if (rest.length % 2 != 0) {
                throw new IllegalArgumentException("params() requires key-value pairs");
            }
            Map<String, Object> next = new LinkedHashMap<>(params);
            next.put(key, value);
            for (int i = 0; i < rest.length; i += 2) {
                next.put(String.valueOf(rest[i]), rest[i + 1]);
            }
            return new Builder(template, next, query);
        }

        public Builder params(Map<String, ?> values) {
            if (values == null || values.isEmpty()) {
                return this;
            }
            Map<String, Object> next = new LinkedHashMap<>(params);
            next.putAll(values);
            return new Builder(template, next, query);
        }

        public Builder query(String key, Object value, Object... rest) {
            if (rest.length % 2 != 0) {
                throw new IllegalArgumentException("query() requires key-value pairs");
            }
            Map<String, Object> next = new LinkedHashMap<>(query);
            next.put(key, value);
            for (int i = 0; i < rest.length; i += 2) {
                next.put(String.valueOf(rest[i]), rest[i + 1]);
            }
            return new Builder(template, params, next);
        }

        public Builder query(Map<String, ?> values) {
            if (values == null || values.isEmpty()) {
                return this;
            }
            Map<String, Object> next = new LinkedHashMap<>(query);
            next.putAll(values);
            return new Builder(template, params, next);
        }

        public String build() {
            String url = template;
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                url = url.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
            if (!query.isEmpty()) {
                StringJoiner joiner = new StringJoiner("&");
                for (Map.Entry<String, Object> entry : query.entrySet()) {
                    String key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
                    Object val = entry.getValue();
                    if (val == null || val.toString().isEmpty()) {
                        joiner.add(key);
                    } else {
                        joiner.add(
                                key
                                        + "="
                                        + URLEncoder.encode(
                                                val.toString(), StandardCharsets.UTF_8));
                    }
                }
                url += "?" + joiner;
            }
            return url;
        }

        @Override
        public String toString() {
            return build();
        }
    }

    // ── JavaScript counterpart ───────────────────────────────────────────

    /**
     * JavaScript {@code Route} and {@code RouteBuilder} classes that mirror the Java builder API.
     * Emitted by {@code FrontendConfigController} into {@code /config.js}.
     */
    public static final String JS_CLASS =
            """
            class Route {
                constructor(template) {
                    this.template = template;
                }
                params(paramsOrKey, value, ...rest) {
                    let p;
                    if (typeof paramsOrKey === 'string') {
                        p = { [paramsOrKey]: value };
                        for (let i = 0; i < rest.length - 1; i += 2) {
                            p[rest[i]] = rest[i + 1];
                        }
                    } else {
                        p = paramsOrKey || {};
                    }
                    return new RouteBuilder(this.template, p, {});
                }
                query(queryOrKey, value, ...rest) {
                    return new RouteBuilder(this.template, {}, {}).query(queryOrKey, value, ...rest);
                }
                toString() { return this.template; }
                valueOf() { return this.template; }
            }
            class RouteBuilder {
                constructor(template, params, query) {
                    this._template = template;
                    this._params = { ...params };
                    this._query = { ...query };
                }
                params(paramsOrKey, value, ...rest) {
                    const p = { ...this._params };
                    if (typeof paramsOrKey === 'string') {
                        p[paramsOrKey] = value;
                        for (let i = 0; i < rest.length - 1; i += 2) {
                            p[rest[i]] = rest[i + 1];
                        }
                    } else if (paramsOrKey) {
                        Object.assign(p, paramsOrKey);
                    }
                    return new RouteBuilder(this._template, p, this._query);
                }
                query(queryOrKey, value, ...rest) {
                    const q = { ...this._query };
                    if (typeof queryOrKey === 'string') {
                        q[queryOrKey] = value;
                        for (let i = 0; i < rest.length - 1; i += 2) {
                            q[rest[i]] = rest[i + 1];
                        }
                    } else if (queryOrKey) {
                        Object.assign(q, queryOrKey);
                    }
                    return new RouteBuilder(this._template, this._params, q);
                }
                build() {
                    let url = this._template;
                    for (const [k, v] of Object.entries(this._params)) {
                        url = url.replace('{' + k + '}', encodeURIComponent(v));
                    }
                    const qEntries = Object.entries(this._query);
                    if (qEntries.length) {
                        const qs = qEntries.map(([k, v]) =>
                            v == null || v === '' ? encodeURIComponent(k) : encodeURIComponent(k) + '=' + encodeURIComponent(v)
                        ).join('&');
                        url += '?' + qs;
                    }
                    return url;
                }
                toString() { return this.build(); }
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
