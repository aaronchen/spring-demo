package cc.desuka.demo.model;

import cc.desuka.demo.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "tasks")
public class Task implements OwnedEntity, Auditable {

  public static final String FIELD_TITLE = "title";
  public static final String FIELD_DESCRIPTION = "description";
  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_COMPLETED = "completed";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "{task.title.notBlank}")
  @Size(min = 1, max = 100, message = "{task.title.size}")
  @Column(nullable = false)
  private String title;

  @Size(max = 500, message = "{task.description.size}")
  private String description;

  private boolean completed = false;

  @Column(name = "created_at")
  private LocalDateTime createdAt = LocalDateTime.now();

  // @ManyToMany: a task can have many tags; a tag can belong to many tasks.
  //
  // Task is the OWNING side — @JoinTable lives here, not on Tag.
  // Hibernate creates the "task_tags" join table with two FK columns:
  //   task_id  → tasks.id
  //   tag_id   → tags.id
  //
  // FetchType.LAZY: tag list is NOT loaded when loading a task. Unlike @ManyToOne
  // where EAGER is the dangerous default, @ManyToMany defaults to LAZY already —
  // but we make it explicit for clarity.
  //
  // No cascade: deleting a task removes its rows from task_tags, but leaves tags intact.
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "task_tags",
      joinColumns = @JoinColumn(name = "task_id"),
      inverseJoinColumns = @JoinColumn(name = "tag_id")
  )
  private List<Tag> tags = new ArrayList<>();

  // @ManyToOne: many tasks can belong to one user.
  //
  // Task is the OWNING side — @JoinColumn lives here, adding a "user_id" FK column to tasks.
  // User is the inverse side (@OneToMany mappedBy = "user").
  //
  // FetchType.LAZY: CRITICAL override — @ManyToOne defaults to EAGER, which would load the
  // full User every time any Task is loaded. LAZY defers that until explicitly accessed.
  //
  // nullable = true (default): tasks can be unassigned (user_id IS NULL).
  // No cascade: user deletion is handled by UserService (sets task.user = null first).
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  // Default constructor (required by JPA)
  public Task() {
  }

  // Constructor for convenience
  public Task(String title, String description) {
    this.title = title;
    this.description = description;
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isCompleted() {
    return completed;
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public List<Tag> getTags() {
    return tags;
  }

  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  @Override
  public Map<String, Object> toAuditSnapshot() {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("title", title);
    snapshot.put("description", description);
    snapshot.put("completed", completed);
    snapshot.put("user", user != null ? user.getName() : null);
    snapshot.put("tags", tags != null ? tags.stream().map(Tag::getName).sorted().toList() : List.of());
    return snapshot;
  }
}
