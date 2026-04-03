package cc.desuka.demo.model;

import cc.desuka.demo.audit.AuditField;
import cc.desuka.demo.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project implements Auditable {

    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_CREATED_BY = "createdBy";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";
    public static final String FIELD_SPRINT_ENABLED = "sprintEnabled";
    public static final String FIELD_TASKS = "tasks";
    public static final String FIELD_SPRINTS = "sprints";
    public static final String FIELD_MEMBERS = "members";
    public static final String FIELD_MEMBER = "member";
    public static final String FIELD_ROLE = "role";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "{project.name.notBlank}")
    @Size(min = 1, max = 100, message = "{project.name.size}")
    @Column(nullable = false)
    private String name;

    @Size(max = 500, message = "{project.description.size}")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @Column(name = "sprint_enabled", nullable = false)
    private boolean sprintEnabled = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "project",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<ProjectMember> members = new LinkedHashSet<>();

    @OneToMany(
            mappedBy = "project",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<Sprint> sprints = new LinkedHashSet<>();

    @OneToMany(
            mappedBy = "project",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<Task> tasks = new LinkedHashSet<>();

    @PrePersist
    protected void onPrePersist() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onPreUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Project() {}

    public Project(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public boolean isSprintEnabled() {
        return sprintEnabled;
    }

    public void setSprintEnabled(boolean sprintEnabled) {
        this.sprintEnabled = sprintEnabled;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<ProjectMember> getMembers() {
        return members;
    }

    public void setMembers(Set<ProjectMember> members) {
        this.members = members;
    }

    public Set<Sprint> getSprints() {
        return sprints;
    }

    public void setSprints(Set<Sprint> sprints) {
        this.sprints = sprints;
    }

    public Set<Task> getTasks() {
        return tasks;
    }

    public void setTasks(Set<Task> tasks) {
        this.tasks = tasks;
    }

    @Override
    public Map<String, AuditField> toAuditSnapshot() {
        Map<String, AuditField> snapshot = new LinkedHashMap<>();
        snapshot.put(FIELD_NAME, AuditField.text(name));
        snapshot.put(FIELD_DESCRIPTION, AuditField.text(description));
        snapshot.put(FIELD_STATUS, AuditField.enumValue(status));
        snapshot.put(FIELD_SPRINT_ENABLED, AuditField.bool(sprintEnabled));
        return snapshot;
    }
}
