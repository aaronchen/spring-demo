package cc.desuka.demo.repository;

import cc.desuka.demo.model.Notification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByUserIdAndReadFalse(UUID userId);

    @EntityGraph(attributePaths = {"actor"})
    List<Notification> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"actor"})
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Notification> findByIdAndUserId(Long id, UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    void markAllAsReadByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    int deleteByCreatedAtBefore(LocalDateTime cutoff);
}
