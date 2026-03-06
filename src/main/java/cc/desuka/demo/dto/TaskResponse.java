package cc.desuka.demo.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * @Data generates getters, setters, toString, equals, hashCode.
 * No @AllArgsConstructor here — MapStruct sets fields via setters (no-args constructor + setters).
 * We drop @AllArgsConstructor because adding a List<TagResponse> tags field would require
 * updating every constructor call site; MapStruct doesn't need it.
 */
@Data
public class TaskResponse {
  private Long id;
  private String title;
  private String description;
  private boolean completed;
  private LocalDateTime createdAt;
  // Empty list when the task has no tags (never null in the response)
  private List<TagResponse> tags;

  // null when the task is unassigned
  private UserResponse user;

  private Long version;
}
