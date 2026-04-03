package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.Sprint;
import cc.desuka.demo.repository.SprintRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SprintQueryServiceTest {

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock private SprintRepository sprintRepository;

    @InjectMocks private SprintQueryService sprintQueryService;

    private Sprint sprint;
    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project("Test Project", "Description");
        project.setId(PROJECT_ID);

        sprint = new Sprint();
        sprint.setId(1L);
        sprint.setName("Sprint 1");
        sprint.setGoal("Complete core features");
        sprint.setStartDate(LocalDate.now().minusDays(7));
        sprint.setEndDate(LocalDate.now().plusDays(7));
        sprint.setProject(project);
    }

    // ── getSprintById ───────────────────────────────────────────────────

    @Test
    void getSprintById_found() {
        when(sprintRepository.findWithProjectById(1L)).thenReturn(Optional.of(sprint));

        Sprint result = sprintQueryService.getSprintById(1L);

        assertThat(result).isEqualTo(sprint);
    }

    @Test
    void getSprintById_notFound_throwsEntityNotFoundException() {
        when(sprintRepository.findWithProjectById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sprintQueryService.getSprintById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── getSprintsByProject ─────────────────────────────────────────────

    @Test
    void getSprintsByProject_delegatesToRepository() {
        when(sprintRepository.findByProjectIdOrderByStartDateDesc(PROJECT_ID))
                .thenReturn(List.of(sprint));

        List<Sprint> result = sprintQueryService.getSprintsByProject(PROJECT_ID);

        assertThat(result).containsExactly(sprint);
    }

    // ── getActiveSprint ─────────────────────────────────────────────────

    @Test
    void getActiveSprint_found() {
        when(sprintRepository.findActiveByProjectId(eq(PROJECT_ID), any(LocalDate.class)))
                .thenReturn(Optional.of(sprint));

        Optional<Sprint> result = sprintQueryService.getActiveSprint(PROJECT_ID);

        assertThat(result).contains(sprint);
    }

    @Test
    void getActiveSprint_empty() {
        when(sprintRepository.findActiveByProjectId(eq(PROJECT_ID), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        Optional<Sprint> result = sprintQueryService.getActiveSprint(PROJECT_ID);

        assertThat(result).isEmpty();
    }
}
