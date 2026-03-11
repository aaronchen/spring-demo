package cc.desuka.demo.repository;

import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Tag;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.TaskStatusFilter;
import cc.desuka.demo.model.User;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

public class TaskSpecifications {

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
          cb.like(cb.lower(root.get(Task.FIELD_DESCRIPTION)), pattern)
      );
    };
  }

  public static Specification<Task> withUserId(Long userId) {
    return (root, query, cb) -> {
      if (userId == null) return cb.conjunction();
      return cb.equal(root.get(Task.FIELD_USER).get(User.FIELD_ID), userId);
    };
  }

  public static Specification<Task> withOverdue(boolean overdue) {
    return (root, query, cb) -> {
      if (!overdue) return cb.conjunction();
      return cb.and(
          cb.notEqual(root.get(Task.FIELD_STATUS), TaskStatus.COMPLETED),
          cb.isNotNull(root.get(Task.FIELD_DUE_DATE)),
          cb.lessThan(root.get(Task.FIELD_DUE_DATE), LocalDate.now())
      );
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

  public static Specification<Task> build(String keyword, TaskStatusFilter statusFilter,
                                          boolean overdue, Priority priority,
                                          Long userId, List<Long> tagIds) {
    return Specification.where(withStatusFilter(statusFilter))
        .and(withOverdue(overdue))
        .and(withPriority(priority))
        .and(withKeyword(keyword))
        .and(withUserId(userId))
        .and(withTagIds(tagIds));
  }
}
