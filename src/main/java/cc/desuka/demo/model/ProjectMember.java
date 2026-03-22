package cc.desuka.demo.model;

import cc.desuka.demo.audit.Auditable;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(
        name = "project_members",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"project_id", "user_id"})})
public class ProjectMember implements Auditable {

    public static final String FIELD_ID = "id";
    public static final String FIELD_PROJECT = "project";
    public static final String FIELD_USER = "user";
    public static final String FIELD_ROLE = "role";
    public static final String FIELD_CREATED_AT = "createdAt";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectRole role = ProjectRole.EDITOR;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public ProjectMember() {}

    public ProjectMember(Project project, User user, ProjectRole role) {
        this.project = project;
        this.user = user;
        this.role = role;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ProjectRole getRole() {
        return role;
    }

    public void setRole(ProjectRole role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public Map<String, Object> toAuditSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put(FIELD_USER, user != null ? user.getName() : null);
        snapshot.put(FIELD_ROLE, role != null ? role.name() : null);
        return snapshot;
    }
}
