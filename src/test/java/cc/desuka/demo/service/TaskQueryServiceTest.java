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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskQueryServiceTest {

    @Mock private TaskRepository taskRepository;

    @InjectMocks private TaskQueryService taskQueryService;

    private Task task;

    @BeforeEach
    void setUp() {
        User alice = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        alice.setId(1L);

        Project project = new Project("Test Project", "Description");
        project.setId(1L);

        task = new Task("Test Task", "Description");
        task.setId(1L);
        task.setVersion(0L);
        task.setUser(alice);
        task.setProject(project);
        task.setStatus(TaskStatus.OPEN);
        task.setPriority(Priority.MEDIUM);
    }

    // ── getTaskById ─────────────────────────────────────────────────────

    @Test
    void getTaskById_found() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Task result = taskQueryService.getTaskById(1L);

        assertThat(result).isEqualTo(task);
    }

    @Test
    void getTaskById_notFound_throwsEntityNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskQueryService.getTaskById(99L))
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
}
