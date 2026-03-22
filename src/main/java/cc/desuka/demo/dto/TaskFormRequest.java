package cc.desuka.demo.dto;

import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class TaskFormRequest {

    @NotBlank(message = "{task.title.notBlank}")
    @Size(min = 1, max = 100, message = "{task.title.size}")
    private String title;

    @Size(max = 500, message = "{task.description.size}")
    private String description;

    private TaskStatus status = TaskStatus.OPEN;

    private Priority priority = Priority.MEDIUM;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    private Long version;

    public static TaskFormRequest fromEntity(Task task) {
        TaskFormRequest request = new TaskFormRequest();
        request.setTitle(task.getTitle());
        request.setDescription(task.getDescription());
        request.setStatus(task.getStatus());
        request.setPriority(task.getPriority());
        request.setStartDate(task.getStartDate());
        request.setDueDate(task.getDueDate());
        request.setVersion(task.getVersion());
        return request;
    }

    public Task toEntity() {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(status);
        task.setPriority(priority);
        task.setStartDate(startDate);
        task.setDueDate(dueDate);
        return task;
    }
}
