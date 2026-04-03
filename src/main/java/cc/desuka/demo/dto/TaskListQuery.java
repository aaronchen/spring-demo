package cc.desuka.demo.dto;

import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.TaskStatusFilter;
import java.util.List;
import java.util.UUID;

/**
 * Binds the shared filter query parameters for task list endpoints. Used by both web controllers
 * ({@code /tasks}, {@code /projects/{id}}) and the REST API ({@code /api/tasks}).
 *
 * <p>Spring MVC binds {@code @ModelAttribute} fields by name from query parameters.
 */
public class TaskListQuery {

    private String search;
    private TaskStatusFilter statusFilter = TaskStatusFilter.ALL;
    private boolean overdue;
    private Priority priority;
    private UUID selectedUserId;
    private Long sprintId;
    private List<Long> tags;

    /** Populates a {@link TaskSearchCriteria} from these filter values. */
    public TaskSearchCriteria toCriteria(List<UUID> accessibleProjectIds) {
        TaskSearchCriteria criteria = TaskSearchCriteria.forProjects(accessibleProjectIds);
        applyCriteria(criteria);
        return criteria;
    }

    /** Populates a {@link TaskSearchCriteria} scoped to a single project. */
    public TaskSearchCriteria toCriteria(UUID projectId) {
        TaskSearchCriteria criteria = TaskSearchCriteria.forProject(projectId);
        applyCriteria(criteria);
        return criteria;
    }

    private void applyCriteria(TaskSearchCriteria criteria) {
        criteria.setKeyword(search);
        criteria.setStatusFilter(statusFilter);
        criteria.setOverdue(overdue);
        criteria.setPriority(priority);
        criteria.setUserId(selectedUserId);
        criteria.setSprintId(sprintId);
        criteria.setTagIds(tags);
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
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

    public UUID getSelectedUserId() {
        return selectedUserId;
    }

    public void setSelectedUserId(UUID selectedUserId) {
        this.selectedUserId = selectedUserId;
    }

    public Long getSprintId() {
        return sprintId;
    }

    public void setSprintId(Long sprintId) {
        this.sprintId = sprintId;
    }

    public List<Long> getTags() {
        return tags;
    }

    public void setTags(List<Long> tags) {
        this.tags = tags;
    }
}
