package cc.desuka.demo.repository;

import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

  @EntityGraph(attributePaths = {"tags", "user", "checklistItems"})
  Optional<Task> findById(Long id);

  @EntityGraph(attributePaths = {"tags", "user"})
  List<Task> findAll();

  @EntityGraph(attributePaths = {"tags", "user"})
  List<Task> findByStatusNot(TaskStatus status);

  List<Task> findByUser(User user);

  @EntityGraph(attributePaths = {"tags", "user"})
  List<Task> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);

  long countByUserAndStatus(User user, TaskStatus status);

  long countByUserAndDueDateBeforeAndStatusNot(User user, LocalDate date, TaskStatus status);

  long countByStatus(TaskStatus status);

  long countByDueDateBeforeAndStatusNot(LocalDate date, TaskStatus status);

  List<Task> findTop5ByUserOrderByCreatedAtDesc(User user);

  List<Task> findByUserAndDueDateBetweenAndStatusNot(User user, LocalDate from, LocalDate to, TaskStatus status);

  @EntityGraph(attributePaths = {"user"})
  List<Task> findByDueDateAndStatusNot(LocalDate dueDate, TaskStatus status);

  // @EntityGraph solves the N+1 query problem for paginated task lists.
  //
  // Without this: loading a page of 25 tasks would fire 1 query for the tasks +
  // up to 25 extra queries (one per task) to load each task's tags lazily.
  //
  // With @EntityGraph: Hibernate issues a single LEFT JOIN query across task_tags
  // to load tasks and all their tags in one round-trip.
  //
  // ⚠️  @ManyToMany + pagination caveat: the JOIN produces one row per task-tag pair,
  // so a task with 3 tags = 3 rows. SQL LIMIT is applied before deduplication, which
  // can return fewer results than the requested page size. For production systems
  // with large tag counts, prefer @BatchSize(size = 25) on Task.tags — it loads tags
  // in batches after pagination, avoiding this issue entirely.
  // Loads both tags and user in a single query to prevent N+1 on the paginated task list.
  @EntityGraph(attributePaths = {"tags", "user"})
  Page<Task> findAll(Specification<Task> spec, Pageable pageable);

}
