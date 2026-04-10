package cc.desuka.demo.repository;

import cc.desuka.demo.model.SavedView;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SavedViewRepository extends JpaRepository<SavedView, Long> {
    List<SavedView> findByUserIdOrderByNameAsc(UUID userId);

    void deleteByUserId(UUID userId);
}
