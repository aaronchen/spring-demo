package cc.desuka.demo.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import cc.desuka.demo.config.AppRoutesProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class AuditTemplateHelperTest {

    @Mock private MessageSource messageSource;
    @Spy private AppRoutesProperties appRoutes = new AppRoutesProperties();
    @InjectMocks private AuditTemplateHelper helper;

    // --- Enum resolution ---

    @Test
    void resolvesTaskStatusLabel() {
        when(messageSource.getMessage(eq("task.status.open"), any(), eq("OPEN"), any()))
                .thenReturn("Open");
        assertThat(helper.resolveEnumLabel("TaskStatus", "OPEN")).isEqualTo("Open");
    }

    @Test
    void resolvesPriorityLabel() {
        when(messageSource.getMessage(eq("task.priority.high"), any(), eq("HIGH"), any()))
                .thenReturn("High");
        assertThat(helper.resolveEnumLabel("Priority", "HIGH")).isEqualTo("High");
    }

    @Test
    void resolvesProjectRoleLabel() {
        when(messageSource.getMessage(eq("project.role.owner"), any(), eq("OWNER"), any()))
                .thenReturn("Owner");
        assertThat(helper.resolveEnumLabel("ProjectRole", "OWNER")).isEqualTo("Owner");
    }

    @Test
    void enumFallsBackToConstantForUnknownClass() {
        assertThat(helper.resolveEnumLabel("UnknownEnum", "VALUE")).isEqualTo("VALUE");
    }

    @Test
    void enumFallsBackToConstantForInvalidConstant() {
        assertThat(helper.resolveEnumLabel("TaskStatus", "NONEXISTENT")).isEqualTo("NONEXISTENT");
    }

    @Test
    void enumReturnsEmptyStringForNullInputs() {
        assertThat(helper.resolveEnumLabel(null, "OPEN")).isEmpty();
        assertThat(helper.resolveEnumLabel("TaskStatus", null)).isEmpty();
    }

    // --- Reference URL resolution ---

    @Test
    void resolvesProjectUrl() {
        assertThat(helper.resolveUrl("Project", 5L)).isEqualTo("/projects/5");
    }

    @Test
    void resolvesTaskUrl() {
        assertThat(helper.resolveUrl("Task", 42L)).isEqualTo("/tasks/42");
    }

    @Test
    void returnsNullForUserType() {
        assertThat(helper.resolveUrl("User", 1L)).isNull();
    }

    @Test
    void returnsNullForSprintType() {
        assertThat(helper.resolveUrl("Sprint", 1L)).isNull();
    }

    @Test
    void returnsNullForNullRefType() {
        assertThat(helper.resolveUrl(null, 1L)).isNull();
    }

    @Test
    void returnsNullForNullRefId() {
        assertThat(helper.resolveUrl("Project", null)).isNull();
    }

    // --- isBlank ---

    @Test
    void isBlankReturnsTrueForNull() {
        assertThat(helper.isBlank(null)).isTrue();
    }

    @Test
    void isBlankReturnsFalseForUntypedField() {
        assertThat(helper.isBlank(Map.of("value", "hello"))).isFalse();
    }

    @Test
    void isBlankReturnsTrueForNullScalar() {
        Map<String, Object> field = new HashMap<>();
        field.put("type", "TEXT");
        field.put("value", null);
        assertThat(helper.isBlank(field)).isTrue();
    }

    @Test
    void isBlankReturnsFalseForNonNullScalar() {
        assertThat(helper.isBlank(Map.of("type", "TEXT", "value", "hello"))).isFalse();
    }

    @Test
    void isBlankReturnsTrueForEmptyCollection() {
        assertThat(helper.isBlank(Map.of("type", "COLLECTION", "items", List.of()))).isTrue();
    }

    @Test
    void isBlankReturnsFalseForNonEmptyCollection() {
        assertThat(helper.isBlank(Map.of("type", "COLLECTION", "items", List.of("a")))).isFalse();
    }

    @Test
    void isBlankReturnsTrueForNullReference() {
        Map<String, Object> field = new HashMap<>();
        field.put("type", "REFERENCE");
        field.put("refId", null);
        field.put("refName", null);
        assertThat(helper.isBlank(field)).isTrue();
    }

    @Test
    void isBlankReturnsFalseForUnknownType() {
        assertThat(helper.isBlank(Map.of("type", "UNKNOWN"))).isFalse();
    }
}
