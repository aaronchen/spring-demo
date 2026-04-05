package cc.desuka.demo.repository;

import cc.desuka.demo.dto.TaskSearchCriteria;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.Sprint;
import cc.desuka.demo.model.Tag;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.TaskStatusFilter;
import cc.desuka.demo.model.User;
import jakarta.persistence.criteria.JoinType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public class TaskSpecifications {

    public static Specification<Task> withProjectId(UUID projectId) {
        return (root, query, cb) -> {
            if (projectId == null) return cb.conjunction();
            return cb.equal(root.get(Task.FIELD_PROJECT).get(Project.FIELD_ID), projectId);
        };
    }

    public static Specification<Task> withProjectIds(List<UUID> projectIds) {
        return (root, query, cb) -> {
            if (projectIds == null) return cb.conjunction();
            return root.get(Task.FIELD_PROJECT).get(Project.FIELD_ID).in(projectIds);
        };
    }

    public static Specification<Task> withStatusFilter(TaskStatusFilter statusFilter) {
        return (root, query, cb) -> {
            if (statusFilter == TaskStatusFilter.ALL) return cb.conjunction();
            TaskStatus status = TaskStatus.valueOf(statusFilter.name());
            return cb.equal(root.get(Task.FIELD_STATUS), status);
        };
    }

    public static Specification<Task> withKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get(Task.FIELD_TITLE)), pattern),
                    cb.like(cb.lower(root.get(Task.FIELD_DESCRIPTION)), pattern));
        };
    }

    public static Specification<Task> withTitleContaining(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.like(cb.lower(root.get(Task.FIELD_TITLE)), pattern);
        };
    }

    public static Specification<Task> withUserId(UUID userId) {
        return (root, query, cb) -> {
            if (userId == null) return cb.conjunction();
            return cb.equal(root.get(Task.FIELD_USER).get(User.FIELD_ID), userId);
        };
    }

    public static Specification<Task> withOverdue(boolean overdue) {
        return (root, query, cb) -> {
            if (!overdue) return cb.conjunction();
            return cb.and(
                    cb.not(root.get(Task.FIELD_STATUS).in(TaskStatus.terminalStatuses())),
                    cb.isNotNull(root.get(Task.FIELD_DUE_DATE)),
                    cb.lessThan(root.get(Task.FIELD_DUE_DATE), LocalDate.now()));
        };
    }

    public static Specification<Task> withPriority(Priority priority) {
        return (root, query, cb) -> {
            if (priority == null) return cb.conjunction();
            return cb.equal(root.get(Task.FIELD_PRIORITY), priority);
        };
    }

    public static Specification<Task> withTagIds(List<Long> tagIds) {
        return (root, query, cb) -> {
            if (tagIds == null || tagIds.isEmpty()) return cb.conjunction();
            query.distinct(true);
            return root.join(Task.FIELD_TAGS, JoinType.INNER).get(Tag.FIELD_ID).in(tagIds);
        };
    }

    public static Specification<Task> withDueDateBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null || to == null) return cb.conjunction();
            return cb.between(root.get(Task.FIELD_DUE_DATE), from, to);
        };
    }

    public static Specification<Task> withSprintId(Long sprintId) {
        return (root, query, cb) -> {
            if (sprintId == null) return cb.conjunction();
            if (sprintId == 0L) return cb.isNull(root.get(Task.FIELD_SPRINT));
            return cb.equal(root.get(Task.FIELD_SPRINT).get(Sprint.FIELD_ID), sprintId);
        };
    }

    public static Specification<Task> withDateInRange(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null || to == null) return cb.conjunction();
            // Show task on its due date; if no due date, show on start date instead
            return cb.or(
                    cb.between(root.get(Task.FIELD_DUE_DATE), from, to),
                    cb.and(
                            cb.isNull(root.get(Task.FIELD_DUE_DATE)),
                            cb.between(root.get(Task.FIELD_START_DATE), from, to)));
        };
    }

    // ── Build helper ────────────────────────────────────────────────────

    public static Specification<Task> build(TaskSearchCriteria criteria) {
        Specification<Task> scope =
                (criteria.getProjectId() != null)
                        ? withProjectId(criteria.getProjectId())
                        : withProjectIds(criteria.getProjectIds());

        return Specification.where(scope)
                .and(withStatusFilter(criteria.getStatusFilter()))
                .and(withOverdue(criteria.isOverdue()))
                .and(withPriority(criteria.getPriority()))
                .and(withKeyword(criteria.getKeyword()))
                .and(withUserId(criteria.getUserId()))
                .and(withTagIds(criteria.getTagIds()))
                .and(withSprintId(criteria.getSprintId()))
                .and(withDateInRange(criteria.getDueDateFrom(), criteria.getDueDateTo()));
    }
}
