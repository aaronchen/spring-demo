package cc.desuka.demo.service;

import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.TaskRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskCommandService {

    private final TaskRepository taskRepository;

    public TaskCommandService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

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
