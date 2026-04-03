package cc.desuka.demo.dto;

import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;

/**
 * @Data generates getters, setters, toString, equals, hashCode. No @AllArgsConstructor here —
 * MapStruct sets fields via setters (no-args constructor + setters). We drop @AllArgsConstructor
 * because adding a List<TagResponse> tags field would require updating every constructor call site;
 * MapStruct doesn't need it.
 */
@Data
public class TaskResponse {
    private UUID id;
    private UUID projectId;
    private String projectName;
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private LocalDate startDate;
    private LocalDate dueDate;
    private Short effort;
    private Long sprintId;
    private String sprintName;
    private Long templateId;
    private String templateName;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;
    // Empty list when the task has no tags (never null in the response)
    private List<TagResponse> tags;

    // null when the task is unassigned
    private UserResponse user;

    private boolean blocked;
    private List<TaskDependencyResponse> blockedBy;
    private List<TaskDependencyResponse> blocks;

    private Long version;
}
