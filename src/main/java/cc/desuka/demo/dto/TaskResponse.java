package cc.desuka.demo.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Data generates:
 * - getters for all fields
 * - setters for all fields
 * - toString()
 * - equals() and hashCode()
 *
 * @AllArgsConstructor generates a constructor with all fields as parameters
 */
@Data
@AllArgsConstructor
public class TaskResponse {
  private Long id;
  private String title;
  private String description;
  private boolean completed;
  private LocalDateTime createdAt;
}