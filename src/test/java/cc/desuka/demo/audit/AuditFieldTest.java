package cc.desuka.demo.audit;

import static org.assertj.core.api.Assertions.assertThat;

import cc.desuka.demo.model.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AuditFieldTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void textFactoryCreatesCorrectType() {
        AuditField field = AuditField.text("hello");
        assertThat(field.type()).isEqualTo(AuditField.FieldType.TEXT);
        assertThat(field.value()).isEqualTo("hello");
        assertThat(field.displayValue()).isEqualTo("hello");
    }

    @Test
    void textFactoryHandlesNull() {
        AuditField field = AuditField.text(null);
        assertThat(field.type()).isEqualTo(AuditField.FieldType.TEXT);
        assertThat(field.value()).isNull();
    }

    @Test
    void enumFactoryStoresClassAndConstant() {
        AuditField field = AuditField.enumValue("OPEN", TaskStatus.class);
        assertThat(field.type()).isEqualTo(AuditField.FieldType.ENUM);
        assertThat(field.value()).isEqualTo("OPEN");
        assertThat(field.enumClass()).isEqualTo("TaskStatus");
    }

    @Test
    void dateFactoryFormatsDate() {
        AuditField field = AuditField.date(LocalDate.of(2026, 4, 1));
        assertThat(field.type()).isEqualTo(AuditField.FieldType.DATE);
        assertThat(field.value()).isEqualTo("2026-04-01");
    }

    @Test
    void dateFactoryHandlesNull() {
        AuditField field = AuditField.date(null);
        assertThat(field.value()).isNull();
    }

    @Test
    void numberFactory() {
        AuditField field = AuditField.number(42);
        assertThat(field.type()).isEqualTo(AuditField.FieldType.NUMBER);
        assertThat(field.value()).isEqualTo("42");
    }

    @Test
    void boolFactory() {
        AuditField field = AuditField.bool(true);
        assertThat(field.type()).isEqualTo(AuditField.FieldType.BOOLEAN);
        assertThat(field.value()).isEqualTo("true");
    }

    @Test
    void refFactoryStoresIdAndNameAndType() {
        AuditField field = AuditField.ref(5L, "My Project", "Project");
        assertThat(field.type()).isEqualTo(AuditField.FieldType.REFERENCE);
        assertThat(field.refId()).isEqualTo(5L);
        assertThat(field.refName()).isEqualTo("My Project");
        assertThat(field.refType()).isEqualTo("Project");
        assertThat(field.displayValue()).isEqualTo("My Project");
    }

    @Test
    void collectionFactory() {
        AuditField field = AuditField.collection(List.of("tag1", "tag2"));
        assertThat(field.type()).isEqualTo(AuditField.FieldType.COLLECTION);
        assertThat(field.items()).containsExactly("tag1", "tag2");
    }

    @Test
    void checklistFactory() {
        AuditField field = AuditField.checklist(List.of("[x] done", "[ ] todo"));
        assertThat(field.type()).isEqualTo(AuditField.FieldType.CHECKLIST);
        assertThat(field.items()).containsExactly("[x] done", "[ ] todo");
    }

    // --- valueEquals ---

    @Test
    void valueEqualsReturnsTrueForIdenticalText() {
        assertThat(AuditField.valueEquals(AuditField.text("a"), AuditField.text("a"))).isTrue();
    }

    @Test
    void valueEqualsReturnsFalseForDifferentText() {
        assertThat(AuditField.valueEquals(AuditField.text("a"), AuditField.text("b"))).isFalse();
    }

    @Test
    void valueEqualsReturnsFalseForNull() {
        assertThat(AuditField.valueEquals(AuditField.text("a"), null)).isFalse();
        assertThat(AuditField.valueEquals(null, AuditField.text("a"))).isFalse();
    }

    @Test
    void valueEqualsBothNullIsTrue() {
        assertThat(AuditField.valueEquals(null, null)).isTrue();
    }

    @Test
    void valueEqualsReferenceComparesById() {
        AuditField ref1 = AuditField.ref(5L, "Old Name", "Project");
        AuditField ref2 = AuditField.ref(5L, "New Name", "Project");
        assertThat(AuditField.valueEquals(ref1, ref2)).isTrue();
    }

    @Test
    void valueEqualsReferenceDifferentIds() {
        AuditField ref1 = AuditField.ref(5L, "Project A", "Project");
        AuditField ref2 = AuditField.ref(7L, "Project B", "Project");
        assertThat(AuditField.valueEquals(ref1, ref2)).isFalse();
    }

    @Test
    void valueEqualsReferenceFallsBackToNameWhenIdNull() {
        AuditField ref1 = AuditField.ref(null, "Same Name", "Project");
        AuditField ref2 = AuditField.ref(null, "Same Name", "Project");
        assertThat(AuditField.valueEquals(ref1, ref2)).isTrue();
    }

    @Test
    void valueEqualsCollectionComparesByItems() {
        AuditField c1 = AuditField.collection(List.of("a", "b"));
        AuditField c2 = AuditField.collection(List.of("a", "b"));
        AuditField c3 = AuditField.collection(List.of("a", "c"));
        assertThat(AuditField.valueEquals(c1, c2)).isTrue();
        assertThat(AuditField.valueEquals(c1, c3)).isFalse();
    }

    // --- JSON round-trip ---

    @Test
    @SuppressWarnings("unchecked")
    void jsonRoundTripPreservesAuditField() {
        AuditField field = AuditField.ref(5L, "Test", "Project");
        String json = MAPPER.writeValueAsString(field);

        Map<String, Object> parsed = MAPPER.readValue(json, Map.class);
        assertThat(parsed.get("type")).isEqualTo("REFERENCE");
        assertThat(((Number) parsed.get("refId")).longValue()).isEqualTo(5L);
        assertThat(parsed.get("refName")).isEqualTo("Test");
        assertThat(parsed.get("refType")).isEqualTo("Project");
    }

    @Test
    @SuppressWarnings("unchecked")
    void jsonRoundTripForEnum() {
        AuditField field = AuditField.enumValue("OPEN", TaskStatus.class);
        String json = MAPPER.writeValueAsString(field);

        Map<String, Object> parsed = MAPPER.readValue(json, Map.class);
        assertThat(parsed.get("type")).isEqualTo("ENUM");
        assertThat(parsed.get("value")).isEqualTo("OPEN");
        assertThat(parsed.get("enumClass")).isEqualTo("TaskStatus");
    }

    // --- diffChecklist ---

    @Test
    void diffChecklistDetectsAddedUnchecked() {
        List<String> oldItems = List.of("[ ] existing");
        List<String> newItems = List.of("[ ] existing", "[ ] new item");

        List<AuditField.ChecklistDiffEntry> entries = AuditField.diffChecklist(oldItems, newItems);
        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst().changeType())
                .isEqualTo(AuditField.ChecklistChangeType.ADDED_UNCHECKED);
        assertThat(entries.getFirst().text()).isEqualTo("new item");
    }

    @Test
    void diffChecklistDetectsAddedChecked() {
        List<String> oldItems = List.of();
        List<String> newItems = List.of("[x] done item");

        List<AuditField.ChecklistDiffEntry> entries = AuditField.diffChecklist(oldItems, newItems);
        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst().changeType())
                .isEqualTo(AuditField.ChecklistChangeType.ADDED_CHECKED);
        assertThat(entries.getFirst().text()).isEqualTo("done item");
    }

    @Test
    void diffChecklistDetectsChecked() {
        List<String> oldItems = List.of("[ ] buy milk");
        List<String> newItems = List.of("[x] buy milk");

        List<AuditField.ChecklistDiffEntry> entries = AuditField.diffChecklist(oldItems, newItems);
        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst().changeType())
                .isEqualTo(AuditField.ChecklistChangeType.CHECKED);
        assertThat(entries.getFirst().text()).isEqualTo("buy milk");
    }

    @Test
    void diffChecklistDetectsUnchecked() {
        List<String> oldItems = List.of("[x] buy milk");
        List<String> newItems = List.of("[ ] buy milk");

        List<AuditField.ChecklistDiffEntry> entries = AuditField.diffChecklist(oldItems, newItems);
        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst().changeType())
                .isEqualTo(AuditField.ChecklistChangeType.UNCHECKED);
        assertThat(entries.getFirst().text()).isEqualTo("buy milk");
    }

    @Test
    void diffChecklistDetectsRemovedUnchecked() {
        List<String> oldItems = List.of("[ ] removed item");
        List<String> newItems = List.of();

        List<AuditField.ChecklistDiffEntry> entries = AuditField.diffChecklist(oldItems, newItems);
        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst().changeType())
                .isEqualTo(AuditField.ChecklistChangeType.REMOVED_UNCHECKED);
        assertThat(entries.getFirst().text()).isEqualTo("removed item");
    }

    @Test
    void diffChecklistDetectsRemovedChecked() {
        List<String> oldItems = List.of("[x] done and removed");
        List<String> newItems = List.of();

        List<AuditField.ChecklistDiffEntry> entries = AuditField.diffChecklist(oldItems, newItems);
        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst().changeType())
                .isEqualTo(AuditField.ChecklistChangeType.REMOVED_CHECKED);
        assertThat(entries.getFirst().text()).isEqualTo("done and removed");
    }

    @Test
    void diffChecklistHandlesMultipleChanges() {
        List<String> oldItems = List.of("[ ] check me", "[x] uncheck me", "[ ] delete me");
        List<String> newItems = List.of("[x] check me", "[ ] uncheck me", "[ ] add me");

        List<AuditField.ChecklistDiffEntry> entries = AuditField.diffChecklist(oldItems, newItems);
        assertThat(entries).hasSize(4);

        Map<AuditField.ChecklistChangeType, String> byType = new java.util.LinkedHashMap<>();
        entries.forEach(e -> byType.put(e.changeType(), e.text()));

        assertThat(byType).containsEntry(AuditField.ChecklistChangeType.CHECKED, "check me");
        assertThat(byType).containsEntry(AuditField.ChecklistChangeType.UNCHECKED, "uncheck me");
        assertThat(byType).containsEntry(AuditField.ChecklistChangeType.ADDED_UNCHECKED, "add me");
        assertThat(byType)
                .containsEntry(AuditField.ChecklistChangeType.REMOVED_UNCHECKED, "delete me");
    }

    @Test
    void diffChecklistNoChangesReturnsEmpty() {
        List<String> items = List.of("[x] done", "[ ] todo");
        assertThat(AuditField.diffChecklist(items, items)).isEmpty();
    }

    @Test
    void diffChecklistHandlesNullInputs() {
        assertThat(AuditField.diffChecklist(null, null)).isEmpty();
        assertThat(AuditField.diffChecklist(null, List.of("[ ] new"))).hasSize(1);
        assertThat(AuditField.diffChecklist(List.of("[ ] old"), null)).hasSize(1);
    }
}
