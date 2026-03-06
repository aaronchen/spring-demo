package cc.desuka.demo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

/**
 * @Data generates:
 * - getters for all fields
 * - setters for all fields
 * - toString()
 * - equals() and hashCode()
 * - a constructor for required fields (final fields)
 */
@Data
public class TaskRequest {

  @NotBlank(message = "{task.title.notBlank}")
  @Size(min = 1, max = 100, message = "{task.title.size}")
  private String title;

  @Size(max = 500, message = "{task.description.size}")
  private String description;

  // null or empty = no tags (or remove all existing tags from this task)
  private List<Long> tagIds;

  // null = unassigned (task not assigned to any user)
  private Long userId;

  // null on create, present on update (optimistic locking)
  private Long version;
}