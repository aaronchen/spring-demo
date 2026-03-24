package cc.desuka.demo.dto;

import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.TaskStatusFilter;
import java.util.List;

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
    private Long selectedUserId;
    private List<Long> tags;

    /** Populates a {@link TaskSearchCriteria} from these filter values. */
    public TaskSearchCriteria toCriteria(List<Long> accessibleProjectIds) {
        TaskSearchCriteria criteria = TaskSearchCriteria.forProjects(accessibleProjectIds);
        applyCriteria(criteria);
        return criteria;
    }

    /** Populates a {@link TaskSearchCriteria} scoped to a single project. */
    public TaskSearchCriteria toCriteria(Long projectId) {
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

    public Long getSelectedUserId() {
        return selectedUserId;
    }

    public void setSelectedUserId(Long selectedUserId) {
        this.selectedUserId = selectedUserId;
    }

    public List<Long> getTags() {
        return tags;
    }

    public void setTags(List<Long> tags) {
        this.tags = tags;
    }
}
