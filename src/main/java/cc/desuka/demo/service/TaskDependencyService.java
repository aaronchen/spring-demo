package cc.desuka.demo.service;

import cc.desuka.demo.exception.CyclicDependencyException;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.util.Messages;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Task dependency management — reconciliation and cycle detection. */
@Service
@Transactional
public class TaskDependencyService {

    private final TaskQueryService taskQueryService;
    private final Messages messages;

    public TaskDependencyService(TaskQueryService taskQueryService, Messages messages) {
        this.taskQueryService = taskQueryService;
        this.messages = messages;
    }

    /**
     * Reconcile dependencies for a task based on submitted form data. Compares current state with
     * submitted IDs, validates new edges, and updates the owning side of each relationship.
     *
     * <p>blockedByIds: IDs of tasks that block this task (inverse side — must manipulate each
     * blocker's {@code blocks} set). blocksIds: IDs of tasks this task blocks (owning side —
     * manipulate directly).
     *
     * <p>Null means "not submitted" (leave unchanged). Empty list means "clear all".
     */
    public void reconcile(Task task, List<UUID> blockedByIds, List<UUID> blocksIds) {
        if (blockedByIds != null) {
            reconcileBlockedBy(task, blockedByIds);
        }
        if (blocksIds != null) {
            reconcileBlocks(task, blocksIds);
        }
    }

    /**
     * Cycle detection via BFS. Returns true if adding "blockingTask blocks blockedTask" would
     * create a cycle. A cycle exists if blockedTask can reach blockingTask by following the
     * "blocks" edges — meaning blockingTask is already (transitively) blocked by blockedTask.
     *
     * <p>Note: loads each node individually (N queries for depth N). Acceptable because dependency
     * chains are shallow in practice — if chains grow, consider a batch-loading or recursive SQL
     * approach.
     */
    public boolean wouldCreateCycle(UUID blockedTaskId, UUID blockingTaskId) {
        Set<UUID> visited = new HashSet<>();
        Queue<UUID> queue = new ArrayDeque<>();
        queue.add(blockedTaskId);

        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            if (current.equals(blockingTaskId)) {
                return true;
            }
            if (visited.add(current)) {
                Task task = taskQueryService.getTaskById(current);
                for (Task blocked : task.getBlocks()) {
                    queue.add(blocked.getId());
                }
            }
        }
        return false;
    }

    // blockedBy is the inverse side (mappedBy = "blocks").
    // To add/remove, we must manipulate each blocker's owning "blocks" set.
    private void reconcileBlockedBy(Task task, List<UUID> blockedByIds) {
        Set<UUID> currentIds =
                task.getBlockedBy().stream().map(Task::getId).collect(Collectors.toSet());
        Set<UUID> newIds = new HashSet<>(blockedByIds);

        // Remove: blockers no longer in the list
        for (Task blocker : new HashSet<>(task.getBlockedBy())) {
            if (!newIds.contains(blocker.getId())) {
                blocker.getBlocks().remove(task);
                task.getBlockedBy().remove(blocker);
            }
        }

        // Add: new blockers
        for (UUID blockerId : newIds) {
            if (!currentIds.contains(blockerId)) {
                Task blocker = validateAndLoadTarget(task, blockerId);
                // "blockerId blocks task" → wouldCreateCycle(task.id, blockerId)
                if (wouldCreateCycle(task.getId(), blockerId)) {
                    throw new CyclicDependencyException(
                            messages.get("task.dependency.error.cycle")
                                    + ": "
                                    + blocker.getTitle());
                }
                blocker.getBlocks().add(task);
                task.getBlockedBy().add(blocker);
            }
        }
    }

    // blocks is the owning side — manipulate task.blocks directly.
    private void reconcileBlocks(Task task, List<UUID> blocksIds) {
        Set<UUID> currentIds =
                task.getBlocks().stream().map(Task::getId).collect(Collectors.toSet());
        Set<UUID> newIds = new HashSet<>(blocksIds);

        // Remove: tasks no longer blocked
        task.getBlocks().removeIf(t -> !newIds.contains(t.getId()));

        // Add: newly blocked tasks
        for (UUID blockedId : newIds) {
            if (!currentIds.contains(blockedId)) {
                Task blocked = validateAndLoadTarget(task, blockedId);
                // "task blocks blockedId" → wouldCreateCycle(blockedId, task.id)
                if (wouldCreateCycle(blockedId, task.getId())) {
                    throw new CyclicDependencyException(
                            messages.get("task.dependency.error.cycle")
                                    + ": "
                                    + blocked.getTitle());
                }
                task.getBlocks().add(blocked);
            }
        }
    }

    private Task validateAndLoadTarget(Task task, UUID targetId) {
        if (task.getId().equals(targetId)) {
            throw new IllegalArgumentException(messages.get("task.dependency.error.selfReference"));
        }
        Task target = taskQueryService.getTaskById(targetId);
        if (!task.getProject().getId().equals(target.getProject().getId())) {
            throw new IllegalArgumentException(messages.get("task.dependency.error.sameProject"));
        }
        return target;
    }
}
