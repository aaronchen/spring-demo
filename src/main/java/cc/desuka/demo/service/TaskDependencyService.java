package cc.desuka.demo.service;

import cc.desuka.demo.exception.CyclicDependencyException;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.util.Messages;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public void reconcile(Task task, List<Long> blockedByIds, List<Long> blocksIds) {
        if (blockedByIds != null) {
            reconcileBlockedBy(task, blockedByIds);
        }
        if (blocksIds != null) {
            reconcileBlocks(task, blocksIds);
        }
    }

    /** Returns non-terminal tasks that block the given task. */
    @Transactional(readOnly = true)
    public List<Task> getActiveBlockers(Long taskId) {
        Task task = taskQueryService.getTaskById(taskId);
        return task.getBlockedBy().stream().filter(t -> !t.getStatus().isTerminal()).toList();
    }

    /** Returns true if the task has at least one non-terminal blocker. */
    @Transactional(readOnly = true)
    public boolean hasActiveBlockers(Long taskId) {
        Task task = taskQueryService.getTaskById(taskId);
        return task.getBlockedBy().stream().anyMatch(t -> !t.getStatus().isTerminal());
    }

    /**
     * Cycle detection via BFS. Returns true if adding "blockingTask blocks blockedTask" would
     * create a cycle. A cycle exists if blockedTask can reach blockingTask by following the
     * "blocks" edges — meaning blockingTask is already (transitively) blocked by blockedTask.
     */
    public boolean wouldCreateCycle(Long blockedTaskId, Long blockingTaskId) {
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(blockedTaskId);

        while (!queue.isEmpty()) {
            Long current = queue.poll();
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
    private void reconcileBlockedBy(Task task, List<Long> blockedByIds) {
        Set<Long> currentIds =
                task.getBlockedBy().stream().map(Task::getId).collect(Collectors.toSet());
        Set<Long> newIds = new HashSet<>(blockedByIds);

        // Remove: blockers no longer in the list
        for (Task blocker : new HashSet<>(task.getBlockedBy())) {
            if (!newIds.contains(blocker.getId())) {
                blocker.getBlocks().remove(task);
                task.getBlockedBy().remove(blocker);
            }
        }

        // Add: new blockers
        for (Long blockerId : newIds) {
            if (!currentIds.contains(blockerId)) {
                validateNewEdge(task, blockerId);
                // "blockerId blocks task" → wouldCreateCycle(task.id, blockerId)
                if (wouldCreateCycle(task.getId(), blockerId)) {
                    Task blocker = taskQueryService.getTaskById(blockerId);
                    throw new CyclicDependencyException(
                            messages.get("task.dependency.error.cycle")
                                    + ": "
                                    + blocker.getTitle());
                }
                Task blocker = taskQueryService.getTaskById(blockerId);
                blocker.getBlocks().add(task);
                task.getBlockedBy().add(blocker);
            }
        }
    }

    // blocks is the owning side — manipulate task.blocks directly.
    private void reconcileBlocks(Task task, List<Long> blocksIds) {
        Set<Long> currentIds =
                task.getBlocks().stream().map(Task::getId).collect(Collectors.toSet());
        Set<Long> newIds = new HashSet<>(blocksIds);

        // Remove: tasks no longer blocked
        task.getBlocks().removeIf(t -> !newIds.contains(t.getId()));

        // Add: newly blocked tasks
        for (Long blockedId : newIds) {
            if (!currentIds.contains(blockedId)) {
                validateNewEdge(task, blockedId);
                // "task blocks blockedId" → wouldCreateCycle(blockedId, task.id)
                if (wouldCreateCycle(blockedId, task.getId())) {
                    Task blocked = taskQueryService.getTaskById(blockedId);
                    throw new CyclicDependencyException(
                            messages.get("task.dependency.error.cycle")
                                    + ": "
                                    + blocked.getTitle());
                }
                Task blocked = taskQueryService.getTaskById(blockedId);
                task.getBlocks().add(blocked);
            }
        }
    }

    private void validateNewEdge(Task task, Long targetId) {
        if (task.getId().equals(targetId)) {
            throw new IllegalArgumentException(messages.get("task.dependency.error.selfReference"));
        }
        Task target = taskQueryService.getTaskById(targetId);
        if (!task.getProject().getId().equals(target.getProject().getId())) {
            throw new IllegalArgumentException(messages.get("task.dependency.error.sameProject"));
        }
    }
}
