package cc.desuka.demo.audit;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

public final class AuditDetails {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AuditDetails() {}

    public static String toJson(Map<String, ?> snapshot) {
        if (snapshot == null) return null;
        return MAPPER.writeValueAsString(snapshot);
    }

    public static Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JacksonException e) {
            return null;
        }
    }

    public static Map<String, Object> resolveDisplayNames(
            Map<String, Object> map, MessageSource messageSource, Locale locale) {
        if (map == null) return null;
        Map<String, Object> resolved = new LinkedHashMap<>();
        for (var entry : map.entrySet()) {
            String displayName =
                    messageSource.getMessage(
                            "audit.field." + entry.getKey(), null, entry.getKey(), locale);
            resolved.put(displayName, entry.getValue());
        }
        return resolved;
    }

    /**
     * Compares two typed snapshots. Uses {@link AuditField#valueEquals} for semantic comparison
     * (e.g., REFERENCE compares by ID, not display name).
     */
    public static Map<String, Object> diff(
            Map<String, AuditField> before, Map<String, AuditField> after) {
        Map<String, Object> changes = new LinkedHashMap<>();
        for (String key : after.keySet()) {
            AuditField oldVal = before.get(key);
            AuditField newVal = after.get(key);
            if (!AuditField.valueEquals(oldVal, newVal)) {
                changes.put(
                        key,
                        Map.of(
                                "old",
                                oldVal != null ? oldVal : "",
                                "new",
                                newVal != null ? newVal : ""));
            }
        }
        return changes;
    }
}
