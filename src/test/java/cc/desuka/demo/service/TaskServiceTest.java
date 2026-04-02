package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.event.TaskAssignedEvent;
import cc.desuka.demo.event.TaskChangeEvent;
import cc.desuka.demo.exception.StaleDataException;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.Tag;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.TaskRepository;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.util.Messages;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskQueryService taskQueryService;
    @Mock private TaskDependencyService taskDependencyService;
    @Mock private SprintQueryService sprintQueryService;
    @Mock private TagService tagService;
    @Mock private UserService userService;
    @Mock private RecentViewService recentViewService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private Messages messages;

    @InjectMocks private TaskService taskService;

    private User alice;
    private User bob;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        alice = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        alice.setId(1L);
        bob = new User("Bob", "bob@example.com", "password", Role.USER);
        bob.setId(2L);

        project = new Project("Test Project", "Description");
        project.setId(1L);

        task = new Task("Test Task", "Description");
        task.setId(1L);
        task.setVersion(0L);
        task.setUser(alice);
        task.setProject(project);
        task.setStatus(TaskStatus.OPEN);
        task.setPriority(Priority.MEDIUM);
    }

    // ── createTask ──────────────────────────────────────────────────────

    @Test
    void createTask_setsTagsAndUser_publishesEvents() {
        Tag tag = new Tag("Work");
        tag.setId(1L);
        when(tagService.findAllByIds(List.of(1L))).thenReturn(Set.of(tag));
        when(userService.findUserById(1L)).thenReturn(alice);
        when(taskRepository.save(any(Task.class)))
                .thenAnswer(
                        inv -> {
                            Task t = inv.getArgument(0);
                            t.setId(1L);
                            return t;
                        });

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(alice);

            Task newTask = new Task("New Task", "Desc");
            newTask.setProject(project);
            Task result = taskService.createTask(newTask, List.of(1L), 1L);

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
        when(tagService.findAllByIds(anyList())).thenReturn(Set.of());
        when(userService.findUserById(null)).thenReturn(null);
        when(taskRepository.save(any(Task.class)))
                .thenAnswer(
                        inv -> {
                            Task t = inv.getArgument(0);
                            t.setId(2L);
                            return t;
                        });

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(alice);

            Task unassigned = new Task("Unassigned", null);
            unassigned.setProject(project);
            Task result = taskService.createTask(unassigned, List.of(), null);

            assertThat(result.getTags()).isEmpty();
            assertThat(result.getUser()).isNull();
        }
    }

    // ── updateTask ──────────────────────────────────────────────────────

    @Test
    void updateTask_optimisticLock_throwsStaleDataException() {
        when(taskQueryService.getTaskById(1L)).thenReturn(task);

        assertThatThrownBy(() -> taskService.updateTask(1L, task, List.of(), 1L, 999L))
                .isInstanceOf(StaleDataException.class);
    }

    @Test
    void updateTask_reassignInProgress_resetsToOpen() {
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setUser(alice);
        when(taskQueryService.getTaskById(1L)).thenReturn(task);
        when(tagService.findAllByIds(anyList())).thenReturn(Set.of());
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
    void deleteTask_deletesTaskAndPublishesEvents() {
        when(taskQueryService.getTaskWithDependencies(1L)).thenReturn(task);

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(alice);

            taskService.deleteTask(1L);

            verify(taskRepository).delete(task);
            verify(eventPublisher, atLeast(1)).publishEvent(any(Object.class));
        }
    }

    // ── advanceStatus ───────────────────────────────────────────────────

    @Test
    void advanceStatus_openToInProgress() {
        task.setStatus(TaskStatus.OPEN);
        when(taskQueryService.getTaskById(1L)).thenReturn(task);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(alice);

            Task result = taskService.advanceStatus(1L);

            assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }
    }

    @Test
    void advanceStatus_inReviewToCompleted_setsCompletedAt() {
        task.setStatus(TaskStatus.IN_REVIEW);
        when(taskQueryService.getTaskById(1L)).thenReturn(task);
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
        when(taskQueryService.getTaskById(1L)).thenReturn(task);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(alice);

            Task result = taskService.advanceStatus(1L);

            assertThat(result.getStatus()).isEqualTo(TaskStatus.OPEN);
            assertThat(result.getCompletedAt()).isNull();
        }
    }

    // ── deleteTask (completed) ─────────────────────────────────────────

    @Test
    void deleteTask_completedTask_throwsIllegalStateException() {
        task.setStatus(TaskStatus.COMPLETED);
        when(taskQueryService.getTaskWithDependencies(1L)).thenReturn(task);

        assertThatThrownBy(() -> taskService.deleteTask(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deleteTask_openTask_deletesAndPublishesEvents() {
        when(taskQueryService.getTaskWithDependencies(1L)).thenReturn(task);

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(alice);

            taskService.deleteTask(1L);

            verify(taskRepository).delete(task);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
            verify(eventPublisher).publishEvent(any(TaskChangeEvent.class));
        }
    }
}
