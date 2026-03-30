package cc.desuka.demo.dto;

import cc.desuka.demo.model.Priority;
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

    private Short effort;

    private Long sprintId;

    private Long version;

    // Display-only: name of the recurring template that generated this task (null if not generated)
    private String templateName;
}
