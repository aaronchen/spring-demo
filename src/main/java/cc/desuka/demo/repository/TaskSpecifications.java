package cc.desuka.demo.repository;

import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatusFilter;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;

public class TaskSpecifications {

  public static Specification<Task> withStatusFilter(TaskStatusFilter statusFilter) {
    return (root, query, cb) -> {
      if (statusFilter == TaskStatusFilter.COMPLETED) {
        return cb.isTrue(root.get(Task.FIELD_COMPLETED));
      } else if (statusFilter == TaskStatusFilter.INCOMPLETE) {
        return cb.isFalse(root.get(Task.FIELD_COMPLETED));
      }
      return cb.conjunction();
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
      return cb.equal(root.get("user").get("id"), userId);
    };
  }

  public static Specification<Task> withTagIds(List<Long> tagIds) {
    return (root, query, cb) -> {
      if (tagIds == null || tagIds.isEmpty()) return cb.conjunction();
      query.distinct(true);
      return root.join("tags", JoinType.INNER).get("id").in(tagIds);
    };
  }

  public static Specification<Task> build(String keyword, TaskStatusFilter statusFilter,
                                          Long userId, List<Long> tagIds) {
    return Specification.where(withStatusFilter(statusFilter))
        .and(withKeyword(keyword))
        .and(withUserId(userId))
        .and(withTagIds(tagIds));
  }
}
