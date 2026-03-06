package cc.desuka.demo.audit;

import org.springframework.context.MessageSource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class AuditDetails {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AuditDetails() {}

    public static String toJson(Map<String, Object> snapshot) {
        if (snapshot == null) return null;
        return MAPPER.writeValueAsString(snapshot);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, Map.class);
        } catch (JacksonException e) {
            return null;
        }
    }

    public static Map<String, Object> resolveDisplayNames(Map<String, Object> map,
                                                          MessageSource messageSource, Locale locale) {
        if (map == null) return null;
        Map<String, Object> resolved = new LinkedHashMap<>();
        for (var entry : map.entrySet()) {
            String displayName = messageSource.getMessage(
                "audit.field." + entry.getKey(), null, entry.getKey(), locale);
            resolved.put(displayName, entry.getValue());
        }
        return resolved;
    }

    public static Map<String, Object> diff(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> changes = new LinkedHashMap<>();
        for (String key : after.keySet()) {
            Object oldVal = before.get(key);
            Object newVal = after.get(key);
            if (!Objects.equals(oldVal, newVal)) {
                changes.put(key, Map.of("old", oldVal != null ? oldVal : "", "new", newVal != null ? newVal : ""));
            }
        }
        return changes;
    }
}
