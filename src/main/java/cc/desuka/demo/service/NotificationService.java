package cc.desuka.demo.service;

import cc.desuka.demo.dto.NotificationResponse;
import cc.desuka.demo.mapper.NotificationMapper;
import cc.desuka.demo.model.Notification;
import cc.desuka.demo.model.NotificationType;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentForUser(UUID userId) {
        return notificationMapper.toResponseList(
                notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId));
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> findAllForUser(UUID userId, Pageable pageable) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toResponse);
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
