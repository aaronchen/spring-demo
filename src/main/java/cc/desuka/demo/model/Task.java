package cc.desuka.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {

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
}