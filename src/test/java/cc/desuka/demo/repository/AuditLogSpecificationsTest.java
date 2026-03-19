package cc.desuka.demo.repository;

import cc.desuka.demo.model.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AuditLogSpecificationsTest {

    @Autowired private AuditLogRepository auditLogRepository;

    private AuditLog taskLog;
    private AuditLog userLog;
    private AuditLog authLog;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();

        Instant now = Instant.now();

        taskLog = new AuditLog();
        taskLog.setAction("TASK_CREATED");
        taskLog.setPrincipal("alice@example.com");
        taskLog.setDetails("{\"title\":\"Fix bug\"}");
        taskLog.setTimestamp(now.minus(2, ChronoUnit.HOURS));
        taskLog = auditLogRepository.save(taskLog);

        userLog = new AuditLog();
        userLog.setAction("USER_UPDATED");
        userLog.setPrincipal("alice@example.com");
        userLog.setDetails("{\"name\":\"Bob\"}");
        userLog.setTimestamp(now.minus(1, ChronoUnit.HOURS));
        userLog = auditLogRepository.save(userLog);

        authLog = new AuditLog();
        authLog.setAction("AUTH_SUCCESS");
        authLog.setPrincipal("bob@example.com");
        authLog.setDetails(null);
        authLog.setTimestamp(now);
        authLog = auditLogRepository.save(authLog);
    }

    // ── withCategory ─────────────────────────────────────────────────────

    @Test
    void withCategory_filtersbyPrefix() {
        List<AuditLog> result = auditLogRepository.findAll(
                AuditLogSpecifications.withCategory("TASK"));

        assertThat(result).extracting(AuditLog::getAction).containsExactly("TASK_CREATED");
    }

    @Test
    void withCategory_caseInsensitive() {
        List<AuditLog> result = auditLogRepository.findAll(
                AuditLogSpecifications.withCategory("task"));

        assertThat(result).extracting(AuditLog::getAction).containsExactly("TASK_CREATED");
    }

    @Test
    void withCategory_null_returnsAll() {
        List<AuditLog> result = auditLogRepository.findAll(
                AuditLogSpecifications.withCategory(null));

        assertThat(result).hasSize(3);
    }

    @Test
    void withCategory_unknownCategory_returnsAll() {
        List<AuditLog> result = auditLogRepository.findAll(
                AuditLogSpecifications.withCategory("UNKNOWN"));

        assertThat(result).hasSize(3);
    }

    // ── withSearch ───────────────────────────────────────────────────────

    @Test
    void withSearch_matchesPrincipal() {
        List<AuditLog> result = auditLogRepository.findAll(
                AuditLogSpecifications.withSearch("bob@example"));

        assertThat(result).extracting(AuditLog::getPrincipal).containsExactly("bob@example.com");
    }

    @Test
    void withSearch_matchesDetails() {
        List<AuditLog> result = auditLogRepository.findAll(
                AuditLogSpecifications.withSearch("Fix bug"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAction()).isEqualTo("TASK_CREATED");
    }

    @Test
    void withSearch_blank_returnsAll() {
        List<AuditLog> result = auditLogRepository.findAll(
                AuditLogSpecifications.withSearch("  "));

        assertThat(result).hasSize(3);
    }

    // ── withFrom / withTo ────────────────────────────────────────────────

    @Test
    void withFrom_filtersOlderEntries() {
        Instant cutoff = Instant.now().minus(90, ChronoUnit.MINUTES);

        List<AuditLog> result = auditLogRepository.findAll(
                AuditLogSpecifications.withFrom(cutoff));

        // Should include userLog and authLog, but not taskLog
        assertThat(result).hasSize(2);
    }

    @Test
    void withTo_filtersNewerEntries() {
        Instant cutoff = Instant.now().minus(90, ChronoUnit.MINUTES);

        List<AuditLog> result = auditLogRepository.findAll(
                AuditLogSpecifications.withTo(cutoff));

        // Should include only taskLog
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAction()).isEqualTo("TASK_CREATED");
    }

    // ── build (combined) ─────────────────────────────────────────────────

    @Test
    void build_combinesAllFilters() {
        Instant from = Instant.now().minus(3, ChronoUnit.HOURS);
        Instant to = Instant.now().minus(30, ChronoUnit.MINUTES);

        Specification<AuditLog> spec = AuditLogSpecifications.build("TASK", "alice", from, to);
        List<AuditLog> result = auditLogRepository.findAll(spec);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAction()).isEqualTo("TASK_CREATED");
    }

    @Test
    void build_allNulls_returnsAll() {
        List<AuditLog> result = auditLogRepository.findAll(
                AuditLogSpecifications.build(null, null, null, null));

        assertThat(result).hasSize(3);
    }
}
