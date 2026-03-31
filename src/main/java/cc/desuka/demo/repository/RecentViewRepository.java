package cc.desuka.demo.repository;

import cc.desuka.demo.model.RecentView;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RecentViewRepository extends JpaRepository<RecentView, Long> {

    Optional<RecentView> findByUserIdAndEntityTypeAndEntityId(
            Long userId, String entityType, Long entityId);

    List<RecentView> findTop10ByUserIdOrderByViewedAtDesc(Long userId);

    long countByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM RecentView rv WHERE rv.user.id = :userId" + " AND rv.id NOT IN :keepIds")
    void deleteByUserIdAndIdNotIn(Long userId, List<Long> keepIds);

    @Modifying
    @Query(
            "UPDATE RecentView rv SET rv.entityTitle = :title"
                    + " WHERE rv.entityType = :entityType AND rv.entityId = :entityId")
    void updateTitle(String entityType, Long entityId, String title);

    @EntityGraph(attributePaths = "user")
    List<RecentView> findByEntityTypeAndEntityId(String entityType, Long entityId);

    void deleteByEntityTypeAndEntityId(String entityType, Long entityId);

    void deleteByUserId(Long userId);
}
