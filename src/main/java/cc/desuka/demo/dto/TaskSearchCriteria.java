package cc.desuka.demo.dto;

import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.TaskStatusFilter;
import java.time.LocalDate;
import java.util.List;

/**
 * Encapsulates task search/filter parameters for the service and repository layers.
 *
 * <p>Exactly one of {@code projectId} or {@code projectIds} should be set for scoping:
 *
 * <ul>
 *   <li>{@code projectId} non-null — single-project view
 *   <li>{@code projectIds} non-null — cross-project view (filtered to accessible projects)
 *   <li>both null — admin cross-project view (all projects)
 * </ul>
 */
public class TaskSearchCriteria {

    // ── Scoping ───────────────────────────────────────────────────────────

    private Long projectId;
    private List<Long> projectIds;

    // ── Filters ───────────────────────────────────────────────────────────

    private String keyword;
    private TaskStatusFilter statusFilter = TaskStatusFilter.ALL;
    private boolean overdue;
    private Priority priority;
    private Long userId;
    private Long sprintId;
    private List<Long> tagIds;

    // ── Date range (calendar view) ────────────────────────────────────────

    private LocalDate dueDateFrom;
    private LocalDate dueDateTo;

    // ── Static factories ──────────────────────────────────────────────────

    public static TaskSearchCriteria forProject(Long projectId) {
        TaskSearchCriteria criteria = new TaskSearchCriteria();
        criteria.projectId = projectId;
        return criteria;
    }

    public static TaskSearchCriteria forProjects(List<Long> accessibleProjectIds) {
        TaskSearchCriteria criteria = new TaskSearchCriteria();
        criteria.projectIds = accessibleProjectIds;
        return criteria;
    }

    // ── Getters and setters ───────────────────────────────────────────────

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public List<Long> getProjectIds() {
        return projectIds;
    }

    public void setProjectIds(List<Long> projectIds) {
        this.projectIds = projectIds;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public TaskStatusFilter getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(TaskStatusFilter statusFilter) {
        this.statusFilter = statusFilter;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSprintId() {
        return sprintId;
    }

    public void setSprintId(Long sprintId) {
        this.sprintId = sprintId;
    }

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds;
    }

    public LocalDate getDueDateFrom() {
        return dueDateFrom;
    }

    public void setDueDateFrom(LocalDate dueDateFrom) {
        this.dueDateFrom = dueDateFrom;
    }

    public LocalDate getDueDateTo() {
        return dueDateTo;
    }

    public void setDueDateTo(LocalDate dueDateTo) {
        this.dueDateTo = dueDateTo;
    }
}
