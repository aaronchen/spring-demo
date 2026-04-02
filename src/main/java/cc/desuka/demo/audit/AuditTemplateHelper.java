package cc.desuka.demo.audit;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.model.Translatable;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Audit rendering helpers for Thymeleaf templates. Provides enum label resolution, reference URL
 * resolution, checklist diff computation, item formatting, and blank-field detection.
 *
 * <p>Called from templates via {@code ${@auditTemplateHelper.method(...)}}.
 */
@Component
public class AuditTemplateHelper {

    private final MessageSource messageSource;
    private final AppRoutesProperties appRoutes;

    public AuditTemplateHelper(MessageSource messageSource, AppRoutesProperties appRoutes) {
        this.messageSource = messageSource;
        this.appRoutes = appRoutes;
    }

    // --- Enum resolution ---

    public String resolveEnumLabel(String enumClass, String constant) {
        if (enumClass == null || constant == null) return "";
        Class<? extends Enum<?>> clazz = AuditField.ENUM_REGISTRY.get(enumClass);
        if (clazz == null) return constant;
        for (Enum<?> value : clazz.getEnumConstants()) {
            if (value.name().equals(constant) && value instanceof Translatable translatable) {
                return messageSource.getMessage(
                        translatable.getMessageKey(),
                        null,
                        constant,
                        LocaleContextHolder.getLocale());
            }
        }
        return constant;
    }

    // --- Reference URL resolution ---

    public String resolveUrl(String refType, Long refId) {
        if (refType == null || refId == null) return null;
        return switch (refType) {
            case AuditField.REF_PROJECT -> appRoutes.getProjectDetail().resolve("projectId", refId);
            case AuditField.REF_TASK -> appRoutes.getTaskDetail().resolve("taskId", refId);
            default -> null;
        };
    }

    // --- Checklist helpers ---

    public List<AuditField.ChecklistDiffEntry> diffChecklist(
            List<String> oldItems, List<String> newItems) {
        return AuditField.diffChecklist(oldItems, newItems);
    }

    /**
     * Formats a raw checklist item (e.g., {@code "[x] Buy milk"}) with Unicode symbols for display
     * in audit snapshots. Returns {@code "☑ Buy milk"} or {@code "☐ Buy milk"}.
     */
    public String formatItem(String item) {
        Locale locale = LocaleContextHolder.getLocale();
        if (item != null && item.length() > AuditField.CHECKLIST_PREFIX_LENGTH) {
            String text = item.substring(AuditField.CHECKLIST_PREFIX_LENGTH);
            if (item.startsWith(AuditField.CHECKED_PREFIX)) {
                String symbol =
                        messageSource.getMessage(
                                "audit.checklist.checkedItem", null, "\u2611", locale);
                return symbol + " " + text;
            }
            if (item.startsWith(AuditField.UNCHECKED_PREFIX)) {
                String symbol =
                        messageSource.getMessage(
                                "audit.checklist.uncheckedItem", null, "\u2610", locale);
                return symbol + " " + text;
            }
        }
        return item;
    }

    // --- Blank-field detection ---

    /**
     * Returns true if a typed audit field (deserialized as a Map) carries no meaningful value. Used
     * by the template to skip empty fields in CREATE snapshots.
     */
    public boolean isBlank(Map<String, Object> fieldValue) {
        if (fieldValue == null) return true;
        String typeName = (String) fieldValue.get(AuditField.FIELD_TYPE);
        if (typeName == null) return false;
        AuditField.FieldType type;
        try {
            type = AuditField.FieldType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return switch (type) {
            case REFERENCE ->
                    fieldValue.get(AuditField.FIELD_REF_ID) == null
                            && fieldValue.get(AuditField.FIELD_REF_NAME) == null;
            case COLLECTION, CHECKLIST -> {
                Object items = fieldValue.get(AuditField.FIELD_ITEMS);
                yield items == null || (items instanceof Collection<?> c && c.isEmpty());
            }
            default -> fieldValue.get(AuditField.FIELD_VALUE) == null;
        };
    }
}
