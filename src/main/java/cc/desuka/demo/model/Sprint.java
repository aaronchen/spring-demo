package cc.desuka.demo.model;

import cc.desuka.demo.audit.AuditField;
import cc.desuka.demo.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "sprints")
public class Sprint implements Auditable {

    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_GOAL = "goal";
    public static final String FIELD_START_DATE = "startDate";
    public static final String FIELD_END_DATE = "endDate";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";
    public static final String FIELD_PROJECT = "project";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "{sprint.name.notBlank}")
    @Size(min = 1, max = 100, message = "{sprint.name.size}")
    @Column(nullable = false)
    private String name;

    @Size(max = 500, message = "{sprint.goal.size}")
    private String goal;

    @NotNull(message = "{sprint.startDate.notNull}")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "{sprint.endDate.notNull}")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @PrePersist
    protected void onPrePersist() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onPreUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Sprint() {}

    // ── Derived status ──────────────────────────────────────────────────

    /** Past: end date is before today. */
    public boolean isPast() {
        return endDate != null && endDate.isBefore(LocalDate.now());
    }

    /** Active: today falls within [startDate, endDate]. */
    public boolean isActive() {
        if (startDate == null || endDate == null) return false;
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    /** Future: start date is after today. */
    public boolean isFuture() {
        return startDate != null && startDate.isAfter(LocalDate.now());
    }

    // ── Getters and Setters ─────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
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

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    @Override
    public Map<String, AuditField> toAuditSnapshot() {
        Map<String, AuditField> snapshot = new LinkedHashMap<>();
        snapshot.put(FIELD_NAME, AuditField.text(name));
        snapshot.put(FIELD_GOAL, AuditField.text(goal));
        snapshot.put(FIELD_START_DATE, AuditField.date(startDate));
        snapshot.put(FIELD_END_DATE, AuditField.date(endDate));
        return snapshot;
    }
}
