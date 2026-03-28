package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import cc.desuka.demo.exception.CyclicDependencyException;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.util.Messages;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskDependencyServiceTest {

    @Mock private TaskQueryService taskQueryService;
    @Mock private Messages messages;

    @InjectMocks private TaskDependencyService taskDependencyService;

    private Project project;
    private Task taskA;
    private Task taskB;
    private Task taskC;

    @BeforeEach
    void setUp() {
        project = new Project("Test Project", "Description");
        project.setId(1L);

        taskA = createTask(1L, "Task A");
        taskB = createTask(2L, "Task B");
        taskC = createTask(3L, "Task C");
    }

    private Task createTask(Long id, String title) {
        Task task = new Task(title, "Description");
        task.setId(id);
        task.setProject(project);
        task.setStatus(TaskStatus.OPEN);
        task.setPriority(Priority.MEDIUM);
        return task;
    }

    // ── reconcile — blockedBy ────────────────────────────────────────────

    @Test
    void reconcile_addBlockedBy_addsToBlockerBlocksSet() {
        // validateNewEdge + wouldCreateCycle + load blocker all call getTaskById
        when(taskQueryService.getTaskById(1L)).thenReturn(taskA);
        when(taskQueryService.getTaskById(2L)).thenReturn(taskB);

        taskDependencyService.reconcile(taskA, List.of(2L), null);

        assertThat(taskB.getBlocks()).contains(taskA);
    }

    @Test
    void reconcile_removeBlockedBy_removesFromBlockerBlocksSet() {
        // taskB currently blocks taskA
        taskB.getBlocks().add(taskA);
        taskA.getBlockedBy().add(taskB);

        taskDependencyService.reconcile(taskA, List.of(), null);

        assertThat(taskB.getBlocks()).doesNotContain(taskA);
    }

    @Test
    void reconcile_blockedBySelfReference_throwsIllegalArgument() {
        when(messages.get("task.dependency.error.selfReference"))
                .thenReturn("Cannot self-reference");

        assertThatThrownBy(() -> taskDependencyService.reconcile(taskA, List.of(1L), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reconcile_blockedByDifferentProject_throwsIllegalArgument() {
        Project otherProject = new Project("Other", "Desc");
        otherProject.setId(2L);
        taskB.setProject(otherProject);

        when(taskQueryService.getTaskById(2L)).thenReturn(taskB);
        when(messages.get("task.dependency.error.sameProject")).thenReturn("Same project required");

        assertThatThrownBy(() -> taskDependencyService.reconcile(taskA, List.of(2L), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reconcile_blockedByCycle_throwsCyclicDependency() {
        // taskA blocks taskB. Adding taskB as blocker of taskA would create A→B→A.
        taskA.getBlocks().add(taskB);

        when(taskQueryService.getTaskById(1L)).thenReturn(taskA);
        when(taskQueryService.getTaskById(2L)).thenReturn(taskB);
        when(messages.get("task.dependency.error.cycle")).thenReturn("Cycle detected");

        assertThatThrownBy(() -> taskDependencyService.reconcile(taskA, List.of(2L), null))
                .isInstanceOf(CyclicDependencyException.class);
    }

    // ── reconcile — blocks ───────────────────────────────────────────────

    @Test
    void reconcile_addBlocks_addsToTaskBlocksSet() {
        // validateNewEdge + wouldCreateCycle + load blocked task all call getTaskById
        when(taskQueryService.getTaskById(2L)).thenReturn(taskB);

        taskDependencyService.reconcile(taskA, null, List.of(2L));

        assertThat(taskA.getBlocks()).contains(taskB);
    }

    @Test
    void reconcile_removeBlocks_removesFromTaskBlocksSet() {
        taskA.getBlocks().add(taskB);

        taskDependencyService.reconcile(taskA, null, List.of());

        assertThat(taskA.getBlocks()).isEmpty();
    }

    @Test
    void reconcile_blocksCycle_throwsCyclicDependency() {
        // taskB blocks taskA. Adding taskA blocks taskB would create A→B→A.
        taskB.getBlocks().add(taskA);

        when(taskQueryService.getTaskById(2L)).thenReturn(taskB);
        when(messages.get("task.dependency.error.cycle")).thenReturn("Cycle detected");

        assertThatThrownBy(() -> taskDependencyService.reconcile(taskA, null, List.of(2L)))
                .isInstanceOf(CyclicDependencyException.class);
    }

    @Test
    void reconcile_nullListsLeavesUnchanged() {
        taskB.getBlocks().add(taskA);
        taskA.getBlockedBy().add(taskB);
        taskA.getBlocks().add(taskC);

        taskDependencyService.reconcile(taskA, null, null);

        assertThat(taskB.getBlocks()).contains(taskA);
        assertThat(taskA.getBlocks()).contains(taskC);
    }

    // ── getActiveBlockers ────────────────────────────────────────────────

    @Test
    void getActiveBlockers_filtersTerminalTasks() {
        Task completedTask = createTask(4L, "Completed Blocker");
        completedTask.setStatus(TaskStatus.COMPLETED);

        taskA.getBlockedBy().addAll(Set.of(taskB, completedTask));

        when(taskQueryService.getTaskById(1L)).thenReturn(taskA);

        var activeBlockers = taskDependencyService.getActiveBlockers(1L);

        assertThat(activeBlockers).containsExactly(taskB);
    }

    @Test
    void getActiveBlockers_noBlockers_returnsEmpty() {
        when(taskQueryService.getTaskById(1L)).thenReturn(taskA);

        var activeBlockers = taskDependencyService.getActiveBlockers(1L);

        assertThat(activeBlockers).isEmpty();
    }

    // ── hasActiveBlockers ────────────────────────────────────────────────

    @Test
    void hasActiveBlockers_withActiveBlocker_returnsTrue() {
        taskA.getBlockedBy().add(taskB);

        when(taskQueryService.getTaskById(1L)).thenReturn(taskA);

        assertThat(taskDependencyService.hasActiveBlockers(1L)).isTrue();
    }

    @Test
    void hasActiveBlockers_onlyTerminalBlockers_returnsFalse() {
        Task completedTask = createTask(4L, "Done");
        completedTask.setStatus(TaskStatus.COMPLETED);
        taskA.getBlockedBy().add(completedTask);

        when(taskQueryService.getTaskById(1L)).thenReturn(taskA);

        assertThat(taskDependencyService.hasActiveBlockers(1L)).isFalse();
    }

    // ── wouldCreateCycle ─────────────────────────────────────────────────

    @Test
    void wouldCreateCycle_noCycle_returnsFalse() {
        when(taskQueryService.getTaskById(1L)).thenReturn(taskA);

        assertThat(taskDependencyService.wouldCreateCycle(1L, 2L)).isFalse();
    }

    @Test
    void wouldCreateCycle_directCycle_returnsTrue() {
        taskA.getBlocks().add(taskB);

        when(taskQueryService.getTaskById(1L)).thenReturn(taskA);

        assertThat(taskDependencyService.wouldCreateCycle(1L, 2L)).isTrue();
    }

    @Test
    void wouldCreateCycle_transitiveCycle_returnsTrue() {
        taskA.getBlocks().add(taskB);
        taskB.getBlocks().add(taskC);

        when(taskQueryService.getTaskById(1L)).thenReturn(taskA);
        when(taskQueryService.getTaskById(2L)).thenReturn(taskB);

        assertThat(taskDependencyService.wouldCreateCycle(1L, 3L)).isTrue();
    }
}
