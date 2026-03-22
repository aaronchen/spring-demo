package cc.desuka.demo.service;

import cc.desuka.demo.dto.NotificationResponse;
import cc.desuka.demo.mapper.NotificationMapper;
import cc.desuka.demo.model.Notification;
import cc.desuka.demo.model.NotificationType;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.NotificationRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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

    @Transactional
    public void create(
            User recipient, User actor, NotificationType type, String message, String link) {
        Notification notification = new Notification(recipient, actor, type, message, link);
        Notification saved = notificationRepository.save(notification);

        NotificationResponse payload = notificationMapper.toResponse(saved);
        messagingTemplate.convertAndSendToUser(
                recipient.getEmail(), "/queue/notifications", payload);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public List<NotificationResponse> getRecentForUser(Long userId) {
        return notificationMapper.toResponseList(
                notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId));
    }

    public Page<NotificationResponse> findAllForUser(Long userId, Pageable pageable) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toResponse);
    }

    @Transactional
    public void markAsRead(Long id, Long userId) {
        notificationRepository
                .findByIdAndUserId(id, userId)
                .ifPresent(
                        n -> {
                            n.setRead(true);
                            notificationRepository.save(n);
                        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public void clearAll(Long userId) {
        notificationRepository.deleteByUserId(userId);
    }
}
