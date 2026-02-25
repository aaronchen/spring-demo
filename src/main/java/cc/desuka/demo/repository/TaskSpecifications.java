package cc.desuka.demo.repository;

import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskFilter;
import org.springframework.data.jpa.domain.Specification;

public class TaskSpecifications {

  public static Specification<Task> withFilter(TaskFilter filter) {
    return (root, query, cb) -> {
      if (filter == TaskFilter.COMPLETED) {
        return cb.isTrue(root.get(Task.FIELD_COMPLETED));
      } else if (filter == TaskFilter.INCOMPLETE) {
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

  public static Specification<Task> build(String keyword, TaskFilter filter) {
    return Specification.where(withFilter(filter)).and(withKeyword(keyword));
  }
}
