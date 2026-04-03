package cc.desuka.demo.audit;

import static org.assertj.core.api.Assertions.assertThat;

import cc.desuka.demo.model.TaskStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditDetailsTest {

    @Test
    void diffTypedDetectsChangedText() {
        Map<String, AuditField> before = new LinkedHashMap<>();
        before.put("title", AuditField.text("Old Title"));

        Map<String, AuditField> after = new LinkedHashMap<>();
        after.put("title", AuditField.text("New Title"));

        Map<String, Object> changes = AuditDetails.diff(before, after);
        assertThat(changes).containsKey("title");
    }

    @Test
    void diffTypedIgnoresUnchangedFields() {
        Map<String, AuditField> before = new LinkedHashMap<>();
        before.put("title", AuditField.text("Same"));

        Map<String, AuditField> after = new LinkedHashMap<>();
        after.put("title", AuditField.text("Same"));

        Map<String, Object> changes = AuditDetails.diff(before, after);
        assertThat(changes).isEmpty();
    }

    @Test
    void diffTypedReferenceComparesById() {
        Map<String, AuditField> before = new LinkedHashMap<>();
        before.put(
                "project",
                AuditField.ref("00000000-0000-0000-0000-000000000001", "Old Name", "Project"));

        Map<String, AuditField> after = new LinkedHashMap<>();
        after.put(
                "project",
                AuditField.ref("00000000-0000-0000-0000-000000000001", "New Name", "Project"));

        Map<String, Object> changes = AuditDetails.diff(before, after);
        assertThat(changes).isEmpty();
    }

    @Test
    void diffTypedReferenceDetectsDifferentId() {
        Map<String, AuditField> before = new LinkedHashMap<>();
        before.put(
                "project",
                AuditField.ref("00000000-0000-0000-0000-000000000001", "Project A", "Project"));

        Map<String, AuditField> after = new LinkedHashMap<>();
        after.put(
                "project",
                AuditField.ref("00000000-0000-0000-0000-000000000002", "Project B", "Project"));

        Map<String, Object> changes = AuditDetails.diff(before, after);
        assertThat(changes).containsKey("project");
    }

    @Test
    @SuppressWarnings("unchecked")
    void diffTypedProducesOldNewAuditFields() {
        Map<String, AuditField> before = new LinkedHashMap<>();
        before.put("status", AuditField.enumValue("OPEN", TaskStatus.class));

        Map<String, AuditField> after = new LinkedHashMap<>();
        after.put("status", AuditField.enumValue("IN_PROGRESS", TaskStatus.class));

        Map<String, Object> changes = AuditDetails.diff(before, after);
        Map<String, Object> statusChange = (Map<String, Object>) changes.get("status");
        assertThat(statusChange.get("old")).isInstanceOf(AuditField.class);
        assertThat(statusChange.get("new")).isInstanceOf(AuditField.class);
    }

    @Test
    void diffTypedCollectionDetectsChange() {
        Map<String, AuditField> before = new LinkedHashMap<>();
        before.put("tags", AuditField.collection(List.of("bug", "ui")));

        Map<String, AuditField> after = new LinkedHashMap<>();
        after.put("tags", AuditField.collection(List.of("bug", "backend")));

        Map<String, Object> changes = AuditDetails.diff(before, after);
        assertThat(changes).containsKey("tags");
    }

    @Test
    void toJsonHandlesTypedSnapshot() {
        Map<String, AuditField> snapshot = new LinkedHashMap<>();
        snapshot.put("title", AuditField.text("Test"));
        snapshot.put("status", AuditField.enumValue("OPEN", TaskStatus.class));

        String json = AuditDetails.toJson(snapshot);
        assertThat(json).contains("\"type\"");
        assertThat(json).contains("\"OPEN\"");
    }

    @Test
    void fromJsonBackwardsCompatWithLegacyData() {
        // Legacy format: plain strings, no type metadata
        String legacyJson = "{\"title\":\"My Task\",\"status\":\"OPEN\"}";
        Map<String, Object> parsed = AuditDetails.fromJson(legacyJson);
        assertThat(parsed).containsEntry("title", "My Task");
        assertThat(parsed).containsEntry("status", "OPEN");
    }

    @Test
    @SuppressWarnings("unchecked")
    void fromJsonBackwardsCompatWithLegacyDiff() {
        // Legacy diff format: {old: "value", new: "value"}
        String legacyJson = "{\"status\":{\"old\":\"OPEN\",\"new\":\"IN_PROGRESS\"}}";
        Map<String, Object> parsed = AuditDetails.fromJson(legacyJson);
        Map<String, Object> statusDiff = (Map<String, Object>) parsed.get("status");
        assertThat(statusDiff.get("old")).isEqualTo("OPEN");
        assertThat(statusDiff.get("new")).isEqualTo("IN_PROGRESS");
    }

    @Test
    void toJsonReturnsNullForNullInput() {
        assertThat(AuditDetails.toJson(null)).isNull();
    }

    @Test
    void fromJsonReturnsNullForNullInput() {
        assertThat(AuditDetails.fromJson(null)).isNull();
    }

    @Test
    void fromJsonReturnsNullForBlankInput() {
        assertThat(AuditDetails.fromJson("")).isNull();
    }
}
