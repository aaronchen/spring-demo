package cc.desuka.demo.service;

import cc.desuka.demo.dto.NotificationResponse;
import cc.desuka.demo.mapper.NotificationMapper;
import cc.desuka.demo.model.Notification;
import cc.desuka.demo.model.NotificationType;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.NotificationRepository;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notification write operations (create, mark read, clear). Counterpart to {@link
 * NotificationQueryService} (reads).
 */
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationMapper notificationMapper,
            SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.messagingTemplate = messagingTemplate;
    }

    public void create(
            User recipient, User actor, NotificationType type, String message, String link) {
        Notification notification = new Notification(recipient, actor, type, message, link);
        Notification saved = notificationRepository.save(notification);

        NotificationResponse payload = notificationMapper.toResponse(saved);
        messagingTemplate.convertAndSendToUser(
                recipient.getEmail(), "/queue/notifications", payload);
    }

    public void markAsRead(Long id, UUID userId) {
        notificationRepository
                .findByIdAndUserId(id, userId)
                .ifPresent(
                        n -> {
                            n.setRead(true);
                            notificationRepository.save(n);
                        });
    }

    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    public void clearAll(UUID userId) {
        notificationRepository.deleteByUserId(userId);
    }

    public void nullActorByUserId(UUID userId) {
        notificationRepository.nullActorByUserId(userId);
    }
}
