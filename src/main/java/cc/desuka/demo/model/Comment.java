package cc.desuka.demo.model;

import cc.desuka.demo.audit.AuditField;
import cc.desuka.demo.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "comments")
public class Comment implements OwnedEntity, Auditable {

    public static final String FIELD_TEXT = "text";
    public static final String FIELD_TASK = "task";
    public static final String FIELD_USER = "user";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "{comment.text.notBlank}")
    @Size(max = 500, message = "{comment.text.size}")
    @Column(nullable = false, length = 500)
    private String text;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // @ManyToOne: many comments belong to one task.
    // LAZY: don't load the full task when loading a comment.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    // @ManyToOne: many comments belong to one user (the author).
    // LAZY: don't load the full user when loading a comment.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Comment() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    @Override
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public Map<String, AuditField> toAuditSnapshot() {
        Map<String, AuditField> snapshot = new LinkedHashMap<>();
        snapshot.put(FIELD_TEXT, AuditField.text(text));
        snapshot.put(
                FIELD_TASK, AuditField.ref(task, Task::getId, Task::getTitle, AuditField.REF_TASK));
        snapshot.put(
                FIELD_USER, AuditField.ref(user, User::getId, User::getName, AuditField.REF_USER));
        return snapshot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Comment)) return false;
        Comment comment = (Comment) o;
        return Objects.equals(id, comment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
