package cc.desuka.demo.repository;

import cc.desuka.demo.model.RecentView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface RecentViewRepository extends JpaRepository<RecentView, Long> {

    Optional<RecentView> findByUserIdAndEntityTypeAndEntityId(
            UUID userId, String entityType, String entityId);

    List<RecentView> findTop10ByUserIdOrderByViewedAtDesc(UUID userId);

    long countByUserId(UUID userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RecentView rv WHERE rv.user.id = :userId" + " AND rv.id NOT IN :keepIds")
    void deleteByUserIdAndIdNotIn(UUID userId, List<Long> keepIds);

    @Modifying
    @Transactional
    @Query(
            "UPDATE RecentView rv SET rv.entityTitle = :title"
                    + " WHERE rv.entityType = :entityType AND rv.entityId = :entityId")
    void updateTitle(String entityType, String entityId, String title);

    @EntityGraph(attributePaths = "user")
    List<RecentView> findByEntityTypeAndEntityId(String entityType, String entityId);

    void deleteByEntityTypeAndEntityId(String entityType, String entityId);

    void deleteByUserId(UUID userId);
}
