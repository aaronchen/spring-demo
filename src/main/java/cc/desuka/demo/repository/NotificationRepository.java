package cc.desuka.demo.repository;

import cc.desuka.demo.model.Notification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByUserIdAndReadFalse(Long userId);

    @EntityGraph(attributePaths = {"actor"})
    List<Notification> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"actor"})
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    void markAllAsReadByUserId(Long userId);

    void deleteByUserId(Long userId);

    int deleteByCreatedAtBefore(LocalDateTime cutoff);
}
