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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskDependencyServiceTest {

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ID_3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID ID_4 = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID PROJECT_ID_2 =
            UUID.fromString("00000000-0000-0000-0000-000000000020");

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
        project.setId(PROJECT_ID);

        taskA = createTask(ID_1, "Task A");
        taskB = createTask(ID_2, "Task B");
        taskC = createTask(ID_3, "Task C");
    }

    private Task createTask(UUID id, String title) {
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
        when(taskQueryService.getTaskById(ID_1)).thenReturn(taskA);
        when(taskQueryService.getTaskById(ID_2)).thenReturn(taskB);

        taskDependencyService.reconcile(taskA, List.of(ID_2), null);

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

        assertThatThrownBy(() -> taskDependencyService.reconcile(taskA, List.of(ID_1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reconcile_blockedByDifferentProject_throwsIllegalArgument() {
        Project otherProject = new Project("Other", "Desc");
        otherProject.setId(PROJECT_ID_2);
        taskB.setProject(otherProject);

        when(taskQueryService.getTaskById(ID_2)).thenReturn(taskB);
        when(messages.get("task.dependency.error.sameProject")).thenReturn("Same project required");

        assertThatThrownBy(() -> taskDependencyService.reconcile(taskA, List.of(ID_2), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reconcile_blockedByCycle_throwsCyclicDependency() {
        // taskA blocks taskB. Adding taskB as blocker of taskA would create A→B→A.
        taskA.getBlocks().add(taskB);

        when(taskQueryService.getTaskById(ID_1)).thenReturn(taskA);
        when(taskQueryService.getTaskById(ID_2)).thenReturn(taskB);
        when(messages.get("task.dependency.error.cycle")).thenReturn("Cycle detected");

        assertThatThrownBy(() -> taskDependencyService.reconcile(taskA, List.of(ID_2), null))
                .isInstanceOf(CyclicDependencyException.class);
    }

    // ── reconcile — blocks ───────────────────────────────────────────────

    @Test
    void reconcile_addBlocks_addsToTaskBlocksSet() {
        // validateNewEdge + wouldCreateCycle + load blocked task all call getTaskById
        when(taskQueryService.getTaskById(ID_2)).thenReturn(taskB);

        taskDependencyService.reconcile(taskA, null, List.of(ID_2));

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

        when(taskQueryService.getTaskById(ID_2)).thenReturn(taskB);
        when(messages.get("task.dependency.error.cycle")).thenReturn("Cycle detected");

        assertThatThrownBy(() -> taskDependencyService.reconcile(taskA, null, List.of(ID_2)))
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

    // ── wouldCreateCycle ─────────────────────────────────────────────────

    @Test
    void wouldCreateCycle_noCycle_returnsFalse() {
        when(taskQueryService.getTaskById(ID_1)).thenReturn(taskA);

        assertThat(taskDependencyService.wouldCreateCycle(ID_1, ID_2)).isFalse();
    }

    @Test
    void wouldCreateCycle_directCycle_returnsTrue() {
        taskA.getBlocks().add(taskB);

        when(taskQueryService.getTaskById(ID_1)).thenReturn(taskA);

        assertThat(taskDependencyService.wouldCreateCycle(ID_1, ID_2)).isTrue();
    }

    @Test
    void wouldCreateCycle_transitiveCycle_returnsTrue() {
        taskA.getBlocks().add(taskB);
        taskB.getBlocks().add(taskC);

        when(taskQueryService.getTaskById(ID_1)).thenReturn(taskA);
        when(taskQueryService.getTaskById(ID_2)).thenReturn(taskB);

        assertThat(taskDependencyService.wouldCreateCycle(ID_1, ID_3)).isTrue();
    }
}
