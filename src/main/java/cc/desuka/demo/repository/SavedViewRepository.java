package cc.desuka.demo.repository;

import cc.desuka.demo.model.SavedView;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedViewRepository extends JpaRepository<SavedView, Long> {
    List<SavedView> findByUserIdOrderByNameAsc(Long userId);
}
