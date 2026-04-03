package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.ProjectStatus;
import cc.desuka.demo.model.Sprint;
import cc.desuka.demo.repository.SprintRepository;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.util.Messages;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SprintServiceTest {

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock private SprintRepository sprintRepository;
    @Mock private TaskService taskService;
    @Mock private SprintQueryService sprintQueryService;
    @Mock private ProjectQueryService projectQueryService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private Messages messages;

    @InjectMocks private SprintService sprintService;

    private Project project;
    private Sprint sprint;

    @BeforeEach
    void setUp() {
        project = new Project("Test Project", "Description");
        project.setId(PROJECT_ID);
        project.setStatus(ProjectStatus.ACTIVE);

        sprint = new Sprint();
        sprint.setId(1L);
        sprint.setName("Sprint 1");
        sprint.setGoal("Deliver MVP");
        sprint.setStartDate(LocalDate.of(2026, 4, 1));
        sprint.setEndDate(LocalDate.of(2026, 4, 14));
        sprint.setProject(project);
    }

    // ── createSprint ──────────────────────────────────────────────────────

    @Test
    void createSprint_validDates_savesAndPublishesAuditEvent() {
        when(projectQueryService.getProjectById(PROJECT_ID)).thenReturn(project);
        when(sprintRepository.existsOverlapping(eq(PROJECT_ID), eq(0L), any(), any()))
                .thenReturn(false);
        when(sprintRepository.save(any(Sprint.class)))
                .thenAnswer(
                        inv -> {
                            Sprint s = inv.getArgument(0);
                            s.setId(1L);
                            return s;
                        });

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            Sprint newSprint = new Sprint();
            newSprint.setName("Sprint 1");
            newSprint.setStartDate(LocalDate.of(2026, 4, 1));
            newSprint.setEndDate(LocalDate.of(2026, 4, 14));

            Sprint result = sprintService.createSprint(PROJECT_ID, newSprint);

            assertThat(result.getProject()).isEqualTo(project);
            assertThat(result.getName()).isEqualTo("Sprint 1");
            verify(sprintRepository).save(newSprint);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    @Test
    void createSprint_invalidDates_throwsException() {
        when(projectQueryService.getProjectById(PROJECT_ID)).thenReturn(project);
        when(messages.get("sprint.error.endBeforeStart"))
                .thenReturn("End date must be after start date");

        Sprint badSprint = new Sprint();
        badSprint.setName("Bad Sprint");
        badSprint.setStartDate(LocalDate.of(2026, 4, 14));
        badSprint.setEndDate(LocalDate.of(2026, 4, 1));

        assertThatThrownBy(() -> sprintService.createSprint(PROJECT_ID, badSprint))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createSprint_overlappingDates_throwsException() {
        when(projectQueryService.getProjectById(PROJECT_ID)).thenReturn(project);
        when(sprintRepository.existsOverlapping(eq(PROJECT_ID), eq(0L), any(), any()))
                .thenReturn(true);
        when(messages.get("sprint.error.overlapping")).thenReturn("Sprint dates overlap");

        Sprint newSprint = new Sprint();
        newSprint.setName("Overlapping Sprint");
        newSprint.setStartDate(LocalDate.of(2026, 4, 1));
        newSprint.setEndDate(LocalDate.of(2026, 4, 14));

        assertThatThrownBy(() -> sprintService.createSprint(PROJECT_ID, newSprint))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── updateSprint ──────────────────────────────────────────────────────

    @Test
    void updateSprint_changesFieldsAndPublishesEvent() {
        when(sprintQueryService.getSprintById(1L)).thenReturn(sprint);
        when(sprintRepository.existsOverlapping(eq(PROJECT_ID), eq(1L), any(), any()))
                .thenReturn(false);
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            Sprint details = new Sprint();
            details.setName("Updated Sprint");
            details.setGoal("New goal");
            details.setStartDate(LocalDate.of(2026, 4, 2));
            details.setEndDate(LocalDate.of(2026, 4, 15));

            Sprint result = sprintService.updateSprint(1L, details);

            assertThat(result.getName()).isEqualTo("Updated Sprint");
            assertThat(result.getGoal()).isEqualTo("New goal");
            assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2026, 4, 2));
            assertThat(result.getEndDate()).isEqualTo(LocalDate.of(2026, 4, 15));
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    @Test
    void updateSprint_overlappingDates_throwsException() {
        when(sprintQueryService.getSprintById(1L)).thenReturn(sprint);
        when(sprintRepository.existsOverlapping(eq(PROJECT_ID), eq(1L), any(), any()))
                .thenReturn(true);
        when(messages.get("sprint.error.overlapping")).thenReturn("Sprint dates overlap");

        Sprint details = new Sprint();
        details.setName("Updated");
        details.setStartDate(LocalDate.of(2026, 4, 2));
        details.setEndDate(LocalDate.of(2026, 4, 15));

        assertThatThrownBy(() -> sprintService.updateSprint(1L, details))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── deleteSprint ──────────────────────────────────────────────────────

    @Test
    void deleteSprint_nullifiesTasksAndDeletesSprint() {
        when(sprintQueryService.getSprintById(1L)).thenReturn(sprint);

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            sprintService.deleteSprint(1L);

            verify(taskService).clearSprintFromTasks(1L);
            verify(sprintRepository).delete(sprint);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }
}
