package cc.desuka.demo.audit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Typed audit snapshot value. Each field in an entity's audit snapshot carries its type so the
 * rendering layer can display it appropriately (enum labels, reference links, collection diffs,
 * etc.).
 */
public record AuditField(
        FieldType type,
        String value,
        String enumClass,
        String refId,
        String refName,
        String refType,
        List<String> items) {

    public enum FieldType {
        TEXT,
        ENUM,
        DATE,
        NUMBER,
        BOOLEAN,
        REFERENCE,
        COLLECTION,
        CHECKLIST
    }

    // --- Checklist prefix constants ---

    public static final String CHECKED_PREFIX = "[x] ";
    public static final String UNCHECKED_PREFIX = "[ ] ";
    public static final int CHECKLIST_PREFIX_LENGTH = CHECKED_PREFIX.length();

    public static String encodeChecklistItem(boolean checked, String text) {
        return (checked ? CHECKED_PREFIX : UNCHECKED_PREFIX) + text;
    }

    // --- JSON field name constants (record component names used as map keys after deserialization)
    // ---

    public static final String FIELD_TYPE = "type";
    public static final String FIELD_VALUE = "value";
    public static final String FIELD_ENUM_CLASS = "enumClass";
    public static final String FIELD_REF_ID = "refId";
    public static final String FIELD_REF_NAME = "refName";
    public static final String FIELD_REF_TYPE = "refType";
    public static final String FIELD_ITEMS = "items";

    // --- Factory methods ---

    public static AuditField text(String value) {
        return new AuditField(FieldType.TEXT, value, null, null, null, null, null);
    }

    public static AuditField enumValue(Enum<?> value) {
        return new AuditField(
                FieldType.ENUM,
                value != null ? value.name() : null,
                value != null ? value.getDeclaringClass().getName() : null,
                null,
                null,
                null,
                null);
    }

    public static AuditField date(LocalDate value) {
        return new AuditField(
                FieldType.DATE,
                value != null ? value.toString() : null,
                null,
                null,
                null,
                null,
                null);
    }

    public static AuditField number(Number value) {
        return new AuditField(
                FieldType.NUMBER,
                value != null ? value.toString() : null,
                null,
                null,
                null,
                null,
                null);
    }

    public static AuditField bool(Boolean value) {
        return new AuditField(
                FieldType.BOOLEAN,
                value != null ? value.toString() : null,
                null,
                null,
                null,
                null,
                null);
    }

    public static AuditField ref(Class<?> entityClass, Object id, String name) {
        return new AuditField(
                FieldType.REFERENCE,
                null,
                null,
                id != null ? id.toString() : null,
                name,
                entityClass.getSimpleName(),
                null);
    }

    public static <T> AuditField ref(
            T entity, Class<T> entityClass, Function<T, ?> id, Function<T, String> name) {
        return ref(
                entityClass,
                entity != null ? id.apply(entity) : null,
                entity != null ? name.apply(entity) : null);
    }

    public static AuditField collection(List<String> items) {
        return new AuditField(FieldType.COLLECTION, null, null, null, null, null, items);
    }

    public static <T> AuditField collection(Collection<T> items, Function<T, String> mapper) {
        return collection(items != null ? items.stream().map(mapper).sorted().toList() : List.of());
    }

    public static AuditField checklist(List<String> items) {
        return new AuditField(FieldType.CHECKLIST, null, null, null, null, null, items);
    }

    public static <T> AuditField checklist(
            Collection<T> items, Predicate<T> checked, Function<T, String> text) {
        return checklist(
                items != null
                        ? items.stream()
                                .map(
                                        item ->
                                                encodeChecklistItem(
                                                        checked.test(item), text.apply(item)))
                                .toList()
                        : List.of());
    }

    /**
     * Returns the human-readable display value for scalar types. For REFERENCE, returns the display
     * name. For COLLECTION/CHECKLIST, returns null (use items instead).
     */
    public String displayValue() {
        return switch (type) {
            case REFERENCE -> refName;
            default -> value;
        };
    }

    /** Returns true if this field carries no meaningful value (null scalar, empty collection). */
    public boolean isBlank() {
        return switch (type) {
            case REFERENCE -> refId == null && refName == null;
            case COLLECTION, CHECKLIST -> items == null || items.isEmpty();
            default -> value == null;
        };
    }

    // --- Checklist diff ---

    public enum ChecklistChangeType {
        ADDED_UNCHECKED,
        ADDED_CHECKED,
        CHECKED,
        UNCHECKED,
        REMOVED_UNCHECKED,
        REMOVED_CHECKED
    }

    public record ChecklistDiffEntry(ChecklistChangeType changeType, String text) {}

    /**
     * Computes item-level diff between two checklist snapshots. Each item is a string like {@code
     * "[x] Buy milk"} or {@code "[ ] Walk dog"}. Items are matched by their text content (after
     * stripping the 4-char prefix). Returns a list of diff entries describing what changed.
     */
    public static List<ChecklistDiffEntry> diffChecklist(
            List<String> oldItems, List<String> newItems) {
        if (oldItems == null) oldItems = List.of();
        if (newItems == null) newItems = List.of();

        // Build maps: text → checked state
        Map<String, Boolean> oldMap = toChecklistMap(oldItems);
        Map<String, Boolean> newMap = toChecklistMap(newItems);

        List<ChecklistDiffEntry> entries = new ArrayList<>();

        // Iterate new items: detect adds and check/uncheck changes
        for (String text : newMap.keySet()) {
            boolean newChecked = newMap.get(text);
            if (!oldMap.containsKey(text)) {
                entries.add(
                        new ChecklistDiffEntry(
                                newChecked
                                        ? ChecklistChangeType.ADDED_CHECKED
                                        : ChecklistChangeType.ADDED_UNCHECKED,
                                text));
            } else {
                boolean oldChecked = oldMap.get(text);
                if (!oldChecked && newChecked) {
                    entries.add(new ChecklistDiffEntry(ChecklistChangeType.CHECKED, text));
                } else if (oldChecked && !newChecked) {
                    entries.add(new ChecklistDiffEntry(ChecklistChangeType.UNCHECKED, text));
                }
            }
        }

        // Iterate old items: detect removals
        for (String text : oldMap.keySet()) {
            if (!newMap.containsKey(text)) {
                boolean wasChecked = oldMap.get(text);
                entries.add(
                        new ChecklistDiffEntry(
                                wasChecked
                                        ? ChecklistChangeType.REMOVED_CHECKED
                                        : ChecklistChangeType.REMOVED_UNCHECKED,
                                text));
            }
        }

        return entries;
    }

    private static Map<String, Boolean> toChecklistMap(List<String> items) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (String item : items) {
            if (item.length() > CHECKLIST_PREFIX_LENGTH) {
                boolean checked = item.startsWith(CHECKED_PREFIX);
                String text = item.substring(CHECKLIST_PREFIX_LENGTH);
                map.put(text, checked);
            } else {
                map.put(item, false);
            }
        }
        return map;
    }

    /**
     * Compares two AuditFields by their semantic value, ignoring display-only differences.
     * REFERENCE fields compare by refId (not name), so renaming an entity doesn't trigger a false
     * diff. Falls back to refName comparison when refId is null (legacy data).
     */
    public static boolean valueEquals(AuditField a, AuditField b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.type != b.type) return false;
        return switch (a.type) {
            case REFERENCE -> {
                if (a.refId != null && b.refId != null) {
                    yield Objects.equals(a.refId, b.refId);
                }
                yield Objects.equals(a.refName, b.refName);
            }
            case COLLECTION -> Objects.equals(a.items, b.items);
            // Checklist: order-insensitive — reordering is not a meaningful change
            case CHECKLIST ->
                    a.items != null && b.items != null
                            ? a.items.size() == b.items.size()
                                    && new HashSet<>(a.items).equals(new HashSet<>(b.items))
                            : Objects.equals(a.items, b.items);
            default -> Objects.equals(a.value, b.value);
        };
    }
}
