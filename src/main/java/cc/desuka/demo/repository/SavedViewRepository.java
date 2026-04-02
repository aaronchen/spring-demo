package cc.desuka.demo.repository;

import cc.desuka.demo.model.SavedView;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SavedViewRepository extends JpaRepository<SavedView, Long> {
    List<SavedView> findByUserIdOrderByNameAsc(Long userId);
}
