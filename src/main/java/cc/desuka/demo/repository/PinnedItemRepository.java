package cc.desuka.demo.repository;

import cc.desuka.demo.model.PinnedItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PinnedItemRepository extends JpaRepository<PinnedItem, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<PinnedItem> findWithUserById(Long id);

    Optional<PinnedItem> findByUserIdAndEntityTypeAndEntityId(
            UUID userId, String entityType, String entityId);

    List<PinnedItem> findByUserIdOrderByPinnedAtDesc(UUID userId);

    List<PinnedItem> findByUserIdOrderByEntityTitleAsc(UUID userId);

    List<PinnedItem> findByUserIdOrderBySortOrderAsc(UUID userId);

    long countByUserId(UUID userId);

    @EntityGraph(attributePaths = "user")
    List<PinnedItem> findByEntityTypeAndEntityId(String entityType, String entityId);

    @Modifying
    @Query(
            "UPDATE PinnedItem p SET p.entityTitle = :title "
                    + "WHERE p.entityType = :entityType AND p.entityId = :entityId")
    void updateTitle(String entityType, String entityId, String title);

    void deleteByEntityTypeAndEntityId(String entityType, String entityId);

    void deleteByUserId(UUID userId);

    @Modifying
    @Query(
            "DELETE FROM PinnedItem p WHERE p.user.id = :userId "
                    + "AND ((p.entityType = 'PROJECT' AND p.entityId = CAST(:projectId AS string)) "
                    + "OR (p.entityType = 'TASK' AND p.entityId IN "
                    + "(SELECT CAST(t.id AS string) FROM Task t WHERE t.project.id = :projectId)))")
    void deleteByUserAndProject(UUID userId, UUID projectId);
}
