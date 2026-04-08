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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "recurring_task_templates")
public class RecurringTaskTemplate implements Auditable {

    public static final String FIELD_ID = "id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_PRIORITY = "priority";
    public static final String FIELD_EFFORT = "effort";
    public static final String FIELD_RECURRENCE = "recurrence";
    public static final String FIELD_DAY_OF_WEEK = "dayOfWeek";
    public static final String FIELD_DAY_OF_MONTH = "dayOfMonth";
    public static final String FIELD_DUE_DAYS_AFTER = "dueDaysAfter";
    public static final String FIELD_NEXT_RUN_DATE = "nextRunDate";
    public static final String FIELD_END_DATE = "endDate";
    public static final String FIELD_ENABLED = "enabled";
    public static final String FIELD_LAST_GENERATED_AT = "lastGeneratedAt";
    public static final String FIELD_PROJECT = "project";
    public static final String FIELD_ASSIGNEE = "assignee";
    public static final String FIELD_CREATED_BY = "createdBy";
    public static final String FIELD_TAGS = "tags";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "{task.title.notBlank}")
    @Size(min = 1, max = 100, message = "{task.title.size}")
    @Column(nullable = false)
    private String title;

    @Size(max = 500, message = "{task.description.size}")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "effort")
    private Short effort;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Recurrence recurrence;

    // 1=Monday .. 7=Sunday (ISO-8601 DayOfWeek.getValue())
    @Column(name = "day_of_week")
    private Short dayOfWeek;

    // 1-31 for MONTHLY recurrence
    @Column(name = "day_of_month")
    private Short dayOfMonth;

    // Number of days after generation to set as due date; null = no due date
    @Column(name = "due_days_after")
    private Short dueDaysAfter;

    @NotNull
    @Column(name = "next_run_date", nullable = false)
    private LocalDate nextRunDate;

    // null = open-ended (runs indefinitely)
    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_generated_at")
    private LocalDateTime lastGeneratedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "recurring_template_tags",
            joinColumns = @JoinColumn(name = "template_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    @PrePersist
    protected void onPrePersist() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onPreUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public RecurringTaskTemplate() {}

    // ── Getters and Setters ─────────────────────────────────────────────

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

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Short getEffort() {
        return effort;
    }

    public void setEffort(Short effort) {
        this.effort = effort;
    }

    public Recurrence getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(Recurrence recurrence) {
        this.recurrence = recurrence;
    }

    public Short getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Short dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Short getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(Short dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public Short getDueDaysAfter() {
        return dueDaysAfter;
    }

    public void setDueDaysAfter(Short dueDaysAfter) {
        this.dueDaysAfter = dueDaysAfter;
    }

    public LocalDate getNextRunDate() {
        return nextRunDate;
    }

    public void setNextRunDate(LocalDate nextRunDate) {
        this.nextRunDate = nextRunDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getLastGeneratedAt() {
        return lastGeneratedAt;
    }

    public void setLastGeneratedAt(LocalDateTime lastGeneratedAt) {
        this.lastGeneratedAt = lastGeneratedAt;
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

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    /** Calculates the next run date after the given date based on the recurrence pattern. */
    public LocalDate calculateNextRunDate(LocalDate afterDate) {
        return switch (recurrence) {
            case DAILY -> afterDate.plusDays(1);
            case WEEKLY -> afterDate.plusWeeks(1);
            case BIWEEKLY -> afterDate.plusWeeks(2);
            case MONTHLY -> {
                LocalDate next = afterDate.plusMonths(1);
                // Re-apply configured day, clamped to month's last day
                if (dayOfMonth != null) {
                    int day = Math.min(dayOfMonth, next.lengthOfMonth());
                    next = next.withDayOfMonth(day);
                }
                yield next;
            }
        };
    }

    @Override
    public Map<String, AuditField> toAuditSnapshot() {
        Map<String, AuditField> snapshot = new LinkedHashMap<>();
        snapshot.put(FIELD_TITLE, AuditField.text(title));
        snapshot.put(FIELD_DESCRIPTION, AuditField.text(description));
        snapshot.put(FIELD_PRIORITY, AuditField.enumValue(priority));
        snapshot.put(FIELD_EFFORT, AuditField.number(effort));
        snapshot.put(FIELD_RECURRENCE, AuditField.enumValue(recurrence));
        snapshot.put(FIELD_DAY_OF_WEEK, AuditField.number(dayOfWeek));
        snapshot.put(FIELD_DAY_OF_MONTH, AuditField.number(dayOfMonth));
        snapshot.put(FIELD_DUE_DAYS_AFTER, AuditField.number(dueDaysAfter));
        snapshot.put(FIELD_NEXT_RUN_DATE, AuditField.date(nextRunDate));
        snapshot.put(FIELD_END_DATE, AuditField.date(endDate));
        snapshot.put(FIELD_ENABLED, AuditField.bool(enabled));
        snapshot.put(
                FIELD_ASSIGNEE, AuditField.ref(assignee, User.class, User::getId, User::getName));
        snapshot.put(FIELD_TAGS, AuditField.collection(tags, Tag::getName));
        return snapshot;
    }
}
