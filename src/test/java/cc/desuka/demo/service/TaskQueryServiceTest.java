package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.TaskRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskQueryServiceTest {

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ID_3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID ID_99 = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Mock private TaskRepository taskRepository;

    @InjectMocks private TaskQueryService taskQueryService;

    private Task task;

    @BeforeEach
    void setUp() {
        User alice = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        alice.setId(ID_1);

        Project project = new Project("Test Project", "Description");
        project.setId(ID_1);

        task = new Task("Test Task", "Description");
        task.setId(ID_1);
        task.setVersion(0L);
        task.setUser(alice);
        task.setProject(project);
        task.setStatus(TaskStatus.OPEN);
        task.setPriority(Priority.MEDIUM);
    }

    // ── getTaskById ─────────────────────────────────────────────────────

    @Test
    void getTaskById_found() {
        when(taskRepository.findById(ID_1)).thenReturn(Optional.of(task));

        Task result = taskQueryService.getTaskById(ID_1);

        assertThat(result).isEqualTo(task);
    }

    @Test
    void getTaskById_notFound_throwsEntityNotFoundException() {
        when(taskRepository.findById(ID_99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskQueryService.getTaskById(ID_99))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── getAllTasks / getIncompleteTasks ─────────────────────────────────

    @Test
    void getAllTasks_delegatesToRepository() {
        when(taskRepository.findAll()).thenReturn(List.of(task));

        List<Task> result = taskQueryService.getAllTasks();

        assertThat(result).containsExactly(task);
    }

    @Test
    void getIncompleteTasks_excludesCompleted() {
        when(taskRepository.findByStatusNotIn(TaskStatus.terminalStatuses()))
                .thenReturn(List.of(task));

        List<Task> result = taskQueryService.getIncompleteTasks();

        assertThat(result).containsExactly(task);
        verify(taskRepository).findByStatusNotIn(TaskStatus.terminalStatuses());
    }

    // ── searchTasks ─────────────────────────────────────────────────────

    @Test
    void searchTasks_blankKeyword_returnsAll() {
        when(taskRepository.findAll()).thenReturn(List.of(task));

        List<Task> result = taskQueryService.searchTasks("  ");

        assertThat(result).containsExactly(task);
        verify(taskRepository).findAll();
    }

    @Test
    void searchTasks_withKeyword_searchesTitleAndDescription() {
        when(taskRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        "test", "test"))
                .thenReturn(List.of(task));

        List<Task> result = taskQueryService.searchTasks("test");

        assertThat(result).containsExactly(task);
    }

    // ── getActiveBlockers / hasActiveBlockers ────────────────────────────

    @Test
    void getActiveBlockers_filtersTerminalTasks() {
        Task blocker = new Task("Blocker", "");
        blocker.setId(ID_2);
        blocker.setStatus(TaskStatus.OPEN);

        Task completedBlocker = new Task("Completed Blocker", "");
        completedBlocker.setId(ID_3);
        completedBlocker.setStatus(TaskStatus.COMPLETED);

        task.setBlockedBy(new LinkedHashSet<>(Set.of(blocker, completedBlocker)));
        task.setChecklistItems(new ArrayList<>());
        when(taskRepository.findWithDependenciesById(ID_1)).thenReturn(Optional.of(task));

        List<Task> active = taskQueryService.getActiveBlockers(ID_1);

        assertThat(active).containsExactly(blocker);
    }

    @Test
    void getActiveBlockers_noBlockers_returnsEmpty() {
        task.setBlockedBy(new LinkedHashSet<>());
        task.setChecklistItems(new ArrayList<>());
        when(taskRepository.findWithDependenciesById(ID_1)).thenReturn(Optional.of(task));

        assertThat(taskQueryService.getActiveBlockers(ID_1)).isEmpty();
    }

    @Test
    void hasActiveBlockers_withActiveBlocker_returnsTrue() {
        Task blocker = new Task("Blocker", "");
        blocker.setId(ID_2);
        blocker.setStatus(TaskStatus.OPEN);
        task.setBlockedBy(new LinkedHashSet<>(Set.of(blocker)));
        task.setChecklistItems(new ArrayList<>());
        when(taskRepository.findWithDependenciesById(ID_1)).thenReturn(Optional.of(task));

        assertThat(taskQueryService.hasActiveBlockers(ID_1)).isTrue();
    }

    @Test
    void hasActiveBlockers_onlyTerminalBlockers_returnsFalse() {
        Task completedBlocker = new Task("Done", "");
        completedBlocker.setId(ID_3);
        completedBlocker.setStatus(TaskStatus.COMPLETED);
        task.setBlockedBy(new LinkedHashSet<>(Set.of(completedBlocker)));
        task.setChecklistItems(new ArrayList<>());
        when(taskRepository.findWithDependenciesById(ID_1)).thenReturn(Optional.of(task));

        assertThat(taskQueryService.hasActiveBlockers(ID_1)).isFalse();
    }
}
