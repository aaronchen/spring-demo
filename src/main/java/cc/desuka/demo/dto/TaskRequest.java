package cc.desuka.demo.dto;

import cc.desuka.demo.model.Priority;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

/**
 * @Data generates: - getters for all fields - setters for all fields - toString() - equals() and
 * hashCode() - a constructor for required fields (final fields)
 */
@Data
public class TaskRequest {

    // Required on create — task must belong to a project
    private Long projectId;

    @NotBlank(message = "{task.title.notBlank}")
    @Size(min = 1, max = 100, message = "{task.title.size}")
    private String title;

    @Size(max = 500, message = "{task.description.size}")
    private String description;

    private Priority priority;

    private LocalDate startDate;

    private LocalDate dueDate;

    private Short effort;

    // null = no sprint assignment change; explicit 0 or absent = backlog
    private Long sprintId;

    // null or empty = no tags (or remove all existing tags from this task)
    private List<Long> tagIds;

    // null = unassigned (task not assigned to any user)
    private Long userId;

    // null on create, present on update (optimistic locking)
    private Long version;
}
