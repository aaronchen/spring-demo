package cc.desuka.demo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

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
}