package cc.desuka.demo.repository;

import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Tag;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.TaskStatusFilter;
import cc.desuka.demo.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TaskSpecificationsTest {

    @Autowired private TaskRepository taskRepository;
    @Autowired private TestEntityManager em;

    private User alice;
    private User bob;
    private Tag workTag;
    private Tag personalTag;

    @BeforeEach
    void setUp() {
        alice = em.persist(new User("Alice", "alice@example.com", "password"));
        bob = em.persist(new User("Bob", "bob@example.com", "password"));
        workTag = em.persist(new Tag("Work"));
        personalTag = em.persist(new Tag("Personal"));
        em.flush();
    }

    private Task createTask(String title, TaskStatus status, Priority priority,
                            User user, LocalDate dueDate, Tag... tags) {
        Task task = new Task(title, "Description for " + title);
        task.setStatus(status);
        task.setPriority(priority);
        task.setUser(user);
        task.setDueDate(dueDate);
        task.setTags(List.of(tags));
        return em.persist(task);
    }

    // ── Status filter ───────────────────────────────────────────────────

    @Test
    void filterByStatus_open() {
        createTask("Open Task", TaskStatus.OPEN, Priority.MEDIUM, alice, null);
        createTask("Completed Task", TaskStatus.COMPLETED, Priority.MEDIUM, alice, null);
        em.flush();

        Page<Task> result = taskRepository.findAll(
                TaskSpecifications.build(null, TaskStatusFilter.OPEN, false, null, null, null),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Task::getTitle).containsExactly("Open Task");
    }

    @Test
    void filterByStatus_all_returnsEverything() {
        createTask("Open Task", TaskStatus.OPEN, Priority.MEDIUM, alice, null);
        createTask("Completed Task", TaskStatus.COMPLETED, Priority.MEDIUM, alice, null);
        em.flush();

        Page<Task> result = taskRepository.findAll(
                TaskSpecifications.build(null, TaskStatusFilter.ALL, false, null, null, null),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
    }

    // ── Keyword search ──────────────────────────────────────────────────

    @Test
    void keywordSearch_matchesTitle() {
        createTask("Fix login bug", TaskStatus.OPEN, Priority.HIGH, alice, null);
        createTask("Add dashboard", TaskStatus.OPEN, Priority.MEDIUM, alice, null);
        em.flush();

        Page<Task> result = taskRepository.findAll(
                TaskSpecifications.build("login", TaskStatusFilter.ALL, false, null, null, null),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Task::getTitle).containsExactly("Fix login bug");
    }

    @Test
    void keywordSearch_caseInsensitive() {
        createTask("Fix LOGIN Bug", TaskStatus.OPEN, Priority.HIGH, alice, null);
        em.flush();

        Page<Task> result = taskRepository.findAll(
                TaskSpecifications.build("login", TaskStatusFilter.ALL, false, null, null, null),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    // ── User filter ─────────────────────────────────────────────────────

    @Test
    void filterByUser() {
        createTask("Alice Task", TaskStatus.OPEN, Priority.MEDIUM, alice, null);
        createTask("Bob Task", TaskStatus.OPEN, Priority.MEDIUM, bob, null);
        em.flush();

        Page<Task> result = taskRepository.findAll(
                TaskSpecifications.build(null, TaskStatusFilter.ALL, false, null, alice.getId(), null),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Task::getTitle).containsExactly("Alice Task");
    }

    // ── Priority filter ─────────────────────────────────────────────────

    @Test
    void filterByPriority() {
        createTask("High Task", TaskStatus.OPEN, Priority.HIGH, alice, null);
        createTask("Low Task", TaskStatus.OPEN, Priority.LOW, alice, null);
        em.flush();

        Page<Task> result = taskRepository.findAll(
                TaskSpecifications.build(null, TaskStatusFilter.ALL, false, Priority.HIGH, null, null),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Task::getTitle).containsExactly("High Task");
    }

    // ── Overdue filter ──────────────────────────────────────────────────

    @Test
    void filterOverdue_excludesCompletedAndFutureDates() {
        createTask("Overdue", TaskStatus.OPEN, Priority.MEDIUM, alice, LocalDate.now().minusDays(1));
        createTask("Future", TaskStatus.OPEN, Priority.MEDIUM, alice, LocalDate.now().plusDays(1));
        createTask("Done Overdue", TaskStatus.COMPLETED, Priority.MEDIUM, alice, LocalDate.now().minusDays(1));
        em.flush();

        Page<Task> result = taskRepository.findAll(
                TaskSpecifications.build(null, TaskStatusFilter.ALL, true, null, null, null),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Task::getTitle).containsExactly("Overdue");
    }

    // ── Tag filter (OR logic) ───────────────────────────────────────────

    @Test
    void filterByTags_orLogic() {
        createTask("Work Only", TaskStatus.OPEN, Priority.MEDIUM, alice, null, workTag);
        createTask("Personal Only", TaskStatus.OPEN, Priority.MEDIUM, alice, null, personalTag);
        createTask("No Tags", TaskStatus.OPEN, Priority.MEDIUM, alice, null);
        em.flush();

        Page<Task> result = taskRepository.findAll(
                TaskSpecifications.build(null, TaskStatusFilter.ALL, false, null, null, List.of(workTag.getId())),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Task::getTitle).containsExactly("Work Only");
    }

    @Test
    void filterByTags_multipleTagsUseOrLogic() {
        createTask("Work Only", TaskStatus.OPEN, Priority.MEDIUM, alice, null, workTag);
        createTask("Personal Only", TaskStatus.OPEN, Priority.MEDIUM, alice, null, personalTag);
        createTask("No Tags", TaskStatus.OPEN, Priority.MEDIUM, alice, null);
        em.flush();

        Page<Task> result = taskRepository.findAll(
                TaskSpecifications.build(null, TaskStatusFilter.ALL, false, null, null,
                        List.of(workTag.getId(), personalTag.getId())),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Task::getTitle)
                .containsExactlyInAnyOrder("Work Only", "Personal Only");
    }

    // ── Combined filters ────────────────────────────────────────────────

    @Test
    void combinedFilters_statusAndUserAndPriority() {
        createTask("Match", TaskStatus.OPEN, Priority.HIGH, alice, null);
        createTask("Wrong Status", TaskStatus.COMPLETED, Priority.HIGH, alice, null);
        createTask("Wrong User", TaskStatus.OPEN, Priority.HIGH, bob, null);
        createTask("Wrong Priority", TaskStatus.OPEN, Priority.LOW, alice, null);
        em.flush();

        Page<Task> result = taskRepository.findAll(
                TaskSpecifications.build(null, TaskStatusFilter.OPEN, false, Priority.HIGH, alice.getId(), null),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Task::getTitle).containsExactly("Match");
    }
}
