package cc.desuka.demo.model;

import cc.desuka.demo.audit.AuditField;
import cc.desuka.demo.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.Formula;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "tasks")
public class Task implements OwnedEntity, Auditable {

    public static final String FIELD_ID = "id";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_PRIORITY = "priority";
    public static final String FIELD_PRIORITY_ORDER = "priorityOrder";
    public static final String FIELD_DUE_DATE = "dueDate";
    public static final String FIELD_START_DATE = "startDate";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_COMPLETED_AT = "completedAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";
    public static final String FIELD_PROJECT = "project";
    public static final String FIELD_TAGS = "tags";
    public static final String FIELD_USER = "user";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_COMMENTS = "comments";
    public static final String FIELD_CHECKLIST_ITEMS = "checklistItems";
    public static final String FIELD_CHECKLIST_TOTAL = "checklistTotal";
    public static final String FIELD_CHECKLIST_CHECKED = "checklistChecked";
    public static final String FIELD_EFFORT = "effort";
    public static final String FIELD_SPRINT = "sprint";
    public static final String FIELD_TEMPLATE = "template";
    public static final String FIELD_BLOCKED_BY = "blockedBy";
    public static final String FIELD_BLOCKS = "blocks";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version private Long version;

    @NotBlank(message = "{task.title.notBlank}")
    @Size(min = 1, max = 100, message = "{task.title.size}")
    @Column(nullable = false)
    private String title;

    @Size(max = 500, message = "{task.description.size}")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    // Virtual column for correct priority sorting (LOW=0, MEDIUM=1, HIGH=2).
    // @Enumerated(STRING) sorts alphabetically (H→L→M) which is wrong.
    // @Formula generates a CASE expression in the ORDER BY clause.
    @Formula("CASE priority WHEN 'LOW' THEN 0 WHEN 'MEDIUM' THEN 1 WHEN 'HIGH' THEN 2 END")
    private int priorityOrder;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "start_date")
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "effort")
    private Short effort;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

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
    // Every task belongs to exactly one project.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Optional link back to the recurring template that generated this task
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private RecurringTaskTemplate template;

    @OneToMany(
            mappedBy = "task",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private Set<Comment> comments = new LinkedHashSet<>();

    @OneToMany(
            mappedBy = "task",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<ChecklistItem> checklistItems = new ArrayList<>();

    // Virtual columns for checklist progress — avoids loading the full collection on list views.
    @Formula("(SELECT COUNT(*) FROM checklist_items ci WHERE ci.task_id = id)")
    private int checklistTotal;

    @Formula(
            "(SELECT COUNT(*) FROM checklist_items ci WHERE ci.task_id = id AND ci.checked = TRUE)")
    private int checklistChecked;

    // Tasks that THIS task blocks (this task is the blocker).
    // Task is the owning side — @JoinTable lives here.
    // No cascade: dependency management is explicit through the service layer.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_dependencies",
            joinColumns = @JoinColumn(name = "blocking_task_id"),
            inverseJoinColumns = @JoinColumn(name = "blocked_task_id"))
    private Set<Task> blocks = new LinkedHashSet<>();

    // Tasks that block THIS task (this task is blocked).
    @ManyToMany(mappedBy = "blocks", fetch = FetchType.LAZY)
    private Set<Task> blockedBy = new LinkedHashSet<>();

    // Virtual column — true when at least one non-terminal task blocks this one.
    // Avoids loading the full dependency graph on list views.
    @Formula(
            "(SELECT CASE WHEN EXISTS("
                    + "SELECT 1 FROM task_dependencies td "
                    + "JOIN tasks bt ON bt.id = td.blocking_task_id "
                    + "WHERE td.blocked_task_id = id "
                    + "AND bt.status NOT IN ('COMPLETED', 'CANCELLED')"
                    + ") THEN TRUE ELSE FALSE END)")
    private boolean blocked;

    @PrePersist
    protected void onPrePersist() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onPreUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Default constructor (required by JPA)
    public Task() {}

    // Constructor for convenience
    public Task(String title, String description) {
        this.title = title;
        this.description = description;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Short getEffort() {
        return effort;
    }

    public void setEffort(Short effort) {
        this.effort = effort;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
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

    public Sprint getSprint() {
        return sprint;
    }

    public void setSprint(Sprint sprint) {
        this.sprint = sprint;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public RecurringTaskTemplate getTemplate() {
        return template;
    }

    public void setTemplate(RecurringTaskTemplate template) {
        this.template = template;
    }

    public Set<Comment> getComments() {
        return comments;
    }

    public void setComments(Set<Comment> comments) {
        this.comments = comments;
    }

    public List<ChecklistItem> getChecklistItems() {
        return checklistItems;
    }

    public void setChecklistItems(List<ChecklistItem> checklistItems) {
        this.checklistItems = checklistItems;
    }

    public int getChecklistTotal() {
        return checklistTotal;
    }

    public int getChecklistChecked() {
        return checklistChecked;
    }

    public Set<Task> getBlocks() {
        return blocks;
    }

    public void setBlocks(Set<Task> blocks) {
        this.blocks = blocks;
    }

    public Set<Task> getBlockedBy() {
        return blockedBy;
    }

    public void setBlockedBy(Set<Task> blockedBy) {
        this.blockedBy = blockedBy;
    }

    public boolean isBlocked() {
        return blocked;
    }

    @Override
    public Map<String, AuditField> toAuditSnapshot() {
        Map<String, AuditField> snapshot = new LinkedHashMap<>();
        snapshot.put(
                FIELD_PROJECT,
                AuditField.ref(project, Project::getId, Project::getName, AuditField.REF_PROJECT));
        snapshot.put(FIELD_TITLE, AuditField.text(title));
        snapshot.put(FIELD_DESCRIPTION, AuditField.text(description));
        snapshot.put(FIELD_STATUS, AuditField.enumValue(status));
        snapshot.put(FIELD_PRIORITY, AuditField.enumValue(priority));
        snapshot.put(FIELD_START_DATE, AuditField.date(startDate));
        snapshot.put(FIELD_DUE_DATE, AuditField.date(dueDate));
        snapshot.put(FIELD_EFFORT, AuditField.number(effort));
        snapshot.put(
                FIELD_SPRINT,
                AuditField.ref(sprint, Sprint::getId, Sprint::getName, AuditField.REF_SPRINT));
        snapshot.put(
                FIELD_USER, AuditField.ref(user, User::getId, User::getName, AuditField.REF_USER));
        snapshot.put(
                FIELD_TEMPLATE,
                AuditField.ref(
                        template,
                        RecurringTaskTemplate::getId,
                        RecurringTaskTemplate::getTitle,
                        AuditField.REF_TEMPLATE));
        snapshot.put(FIELD_TAGS, AuditField.collection(tags, Tag::getName));
        snapshot.put(
                FIELD_CHECKLIST_ITEMS,
                AuditField.checklist(
                        checklistItems, ChecklistItem::isChecked, ChecklistItem::getText));
        snapshot.put(FIELD_BLOCKED_BY, AuditField.collection(blockedBy, Task::getTitle));
        snapshot.put(FIELD_BLOCKS, AuditField.collection(blocks, Task::getTitle));
        return snapshot;
    }
}
