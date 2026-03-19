package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.event.TaskAssignedEvent;
import cc.desuka.demo.event.TaskChangeEvent;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.exception.StaleDataException;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.Tag;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.TaskRepository;
import cc.desuka.demo.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TagService tagService;
    @Mock private UserService userService;
    @Mock private CommentService commentService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private TaskService taskService;

    private User alice;
    private User bob;
    private Task task;

    @BeforeEach
    void setUp() {
        alice = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        alice.setId(1L);
        bob = new User("Bob", "bob@example.com", "password", Role.USER);
        bob.setId(2L);

        task = new Task("Test Task", "Description");
        task.setId(1L);
        task.setVersion(0L);
        task.setUser(alice);
        task.setStatus(TaskStatus.OPEN);
        task.setPriority(Priority.MEDIUM);
    }

    // ── getTaskById ─────────────────────────────────────────────────────

    @Test
    void getTaskById_found() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Task result = taskService.getTaskById(1L);

        assertThat(result).isEqualTo(task);
    }

    @Test
    void getTaskById_notFound_throwsEntityNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── createTask ──────────────────────────────────────────────────────

    @Test
    void createTask_setsTagsAndUser_publishesEvents() {
        Tag tag = new Tag("Work");
        tag.setId(1L);
        when(tagService.findAllByIds(List.of(1L))).thenReturn(List.of(tag));
        when(userService.findUserById(1L)).thenReturn(alice);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(alice);

            Task result = taskService.createTask(new Task("New Task", "Desc"), List.of(1L), 1L);

            assertThat(result.getTags()).containsExactly(tag);
            assertThat(result.getUser()).isEqualTo(alice);

            // Verify 3 events published: AuditEvent, TaskAssignedEvent, TaskChangeEvent
            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher, times(3)).publishEvent(captor.capture());
            List<Object> events = captor.getAllValues();
            assertThat(events.get(0)).isInstanceOf(AuditEvent.class);
            assertThat(events.get(1)).isInstanceOf(TaskAssignedEvent.class);
            assertThat(events.get(2)).isInstanceOf(TaskChangeEvent.class);
        }
    }

    @Test
    void createTask_noTags_noUser() {
        when(tagService.findAllByIds(anyList())).thenReturn(List.of());
        when(userService.findUserById(null)).thenReturn(null);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(2L);
            return t;
        });

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(alice);

            Task result = taskService.createTask(new Task("Unassigned", null), List.of(), null);

            assertThat(result.getTags()).isEmpty();
            assertThat(result.getUser()).isNull();
        }
    }

    // ── updateTask ──────────────────────────────────────────────────────

    @Test
    void updateTask_optimisticLock_throwsStaleDataException() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() ->
                taskService.updateTask(1L, task, List.of(), 1L, 999L))
                .isInstanceOf(StaleDataException.class);
    }

    @Test
    void updateTask_reassignInProgress_resetsToOpen() {
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setUser(alice);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(tagService.findAllByIds(anyList())).thenReturn(List.of());
        when(userService.findUserById(2L)).thenReturn(bob);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(alice);

            Task details = new Task("Updated", "Desc");
            details.setStatus(TaskStatus.IN_PROGRESS);
            details.setPriority(Priority.HIGH);

            Task result = taskService.updateTask(1L, details, List.of(), 2L, 0L);

            // Reassigning resets IN_PROGRESS to OPEN
            assertThat(result.getStatus()).isEqualTo(TaskStatus.OPEN);
            assertThat(result.getUser()).isEqualTo(bob);
        }
    }

    // ── deleteTask ──────────────────────────────────────────────────────

    @Test
    void deleteTask_deletesCommentsFirst() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(alice);

            taskService.deleteTask(1L);

            // Comments deleted before task
            var inOrder = inOrder(commentService, taskRepository);
            inOrder.verify(commentService).deleteByTaskId(1L);
            inOrder.verify(taskRepository).delete(task);
        }
    }

    // ── advanceStatus ───────────────────────────────────────────────────

    @Test
    void advanceStatus_openToInProgress() {
        task.setStatus(TaskStatus.OPEN);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(alice);

            Task result = taskService.advanceStatus(1L);

            assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }
    }

    @Test
    void advanceStatus_inProgressToCompleted_setsCompletedAt() {
        task.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(alice);

            Task result = taskService.advanceStatus(1L);

            assertThat(result.getStatus()).isEqualTo(TaskStatus.COMPLETED);
            assertThat(result.getCompletedAt()).isNotNull();
        }
    }

    @Test
    void advanceStatus_completedToOpen_clearsCompletedAt() {
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(java.time.LocalDateTime.now());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(alice);

            Task result = taskService.advanceStatus(1L);

            assertThat(result.getStatus()).isEqualTo(TaskStatus.OPEN);
            assertThat(result.getCompletedAt()).isNull();
        }
    }

    // ── getAllTasks / getIncompleteTasks ─────────────────────────────────

    @Test
    void getAllTasks_delegatesToRepository() {
        when(taskRepository.findAll()).thenReturn(List.of(task));

        List<Task> result = taskService.getAllTasks();

        assertThat(result).containsExactly(task);
    }

    @Test
    void getIncompleteTasks_excludesCompleted() {
        when(taskRepository.findByStatusNot(TaskStatus.COMPLETED)).thenReturn(List.of(task));

        List<Task> result = taskService.getIncompleteTasks();

        assertThat(result).containsExactly(task);
        verify(taskRepository).findByStatusNot(TaskStatus.COMPLETED);
    }

    // ── searchTasks ─────────────────────────────────────────────────────

    @Test
    void searchTasks_blankKeyword_returnsAll() {
        when(taskRepository.findAll()).thenReturn(List.of(task));

        List<Task> result = taskService.searchTasks("  ");

        assertThat(result).containsExactly(task);
        verify(taskRepository).findAll();
    }

    @Test
    void searchTasks_withKeyword_searchesTitleAndDescription() {
        when(taskRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("test", "test"))
                .thenReturn(List.of(task));

        List<Task> result = taskService.searchTasks("test");

        assertThat(result).containsExactly(task);
    }
}
