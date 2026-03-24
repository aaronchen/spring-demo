package cc.desuka.demo.service;

import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.TaskRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only task lookups and cross-service task operations. Breaks circular dependency: TaskService
 * → UserService/CommentService → TaskService.
 */
@Service
public class TaskQueryService {

    private final TaskRepository taskRepository;

    public TaskQueryService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Task getTaskById(Long id) {
        return taskRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Task.class, id));
    }

    public long countByUserAndStatus(User user, TaskStatus status) {
        return taskRepository.countByUserAndStatus(user, status);
    }

    public long countAssignedTasks(User user) {
        return taskRepository.findByUser(user).size();
    }

    /**
     * Unassign all tasks for a user and reset non-completed tasks to OPEN. Used when disabling or
     * deleting a user.
     */
    @Transactional
    public void unassignTasks(User user) {
        List<Task> tasks = taskRepository.findByUser(user);
        for (Task task : tasks) {
            task.setUser(null);
            if (!task.getStatus().isTerminal()) {
                task.setStatus(TaskStatus.OPEN);
            }
        }
        taskRepository.saveAll(tasks);
    }

    /**
     * Unassign non-terminal tasks for a user within a specific project. Used when demoting a member
     * to VIEWER — terminal tasks keep their assignee as historical record.
     */
    @Transactional
    public void unassignTasksInProject(User user, Long projectId) {
        List<Task> tasks =
                taskRepository.findByUserAndProjectIdAndStatusNotIn(
                        user, projectId, TaskStatus.terminalStatuses());
        for (Task task : tasks) {
            task.setUser(null);
            task.setStatus(TaskStatus.OPEN);
        }
        taskRepository.saveAll(tasks);
    }
}
