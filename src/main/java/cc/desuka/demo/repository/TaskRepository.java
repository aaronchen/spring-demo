package cc.desuka.demo.repository;

import cc.desuka.demo.model.Task;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

  // Spring Data JPA generates implementations automatically!

  // Find by exact title
  List<Task> findByTitle(String title);

  // Find where title contains (LIKE %keyword%)
  List<Task> findByTitleContaining(String keyword);

  // Find all completed or incomplete tasks
  List<Task> findByCompleted(boolean completed);

  // Find all completed or incomplete tasks with sorting
  List<Task> findByCompleted(boolean completed, Sort sort);

  // Find by title containing AND not completed
  List<Task> findByTitleContainingAndCompleted(String keyword, boolean completed);

  // Find by title OR description containing (for search)
  List<Task> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);

  // Find by title OR description containing with sorting
  List<Task> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
      String title, String description, Sort sort);

}