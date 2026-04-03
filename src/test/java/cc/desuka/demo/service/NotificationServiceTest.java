package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import cc.desuka.demo.dto.NotificationResponse;
import cc.desuka.demo.mapper.NotificationMapper;
import cc.desuka.demo.model.Notification;
import cc.desuka.demo.model.NotificationType;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.NotificationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationMapper notificationMapper;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private NotificationService notificationService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        alice.setId(ID_1);
        bob = new User("Bob", "bob@example.com", "password", Role.USER);
        bob.setId(ID_2);
    }

    // ── create ───────────────────────────────────────────────────────────

    @Test
    void create_savesAndPushesViaWebSocket() {
        NotificationResponse response = new NotificationResponse();
        response.setId(1L);
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(
                        inv -> {
                            Notification n = inv.getArgument(0);
                            n.setId(1L);
                            return n;
                        });
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(response);

        notificationService.create(
                alice,
                bob,
                NotificationType.TASK_ASSIGNED,
                "Bob assigned you a task",
                "/tasks/1/edit");

        // Verify saved to DB
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(alice);
        assertThat(saved.getActor()).isEqualTo(bob);
        assertThat(saved.getType()).isEqualTo(NotificationType.TASK_ASSIGNED);
        assertThat(saved.getMessage()).isEqualTo("Bob assigned you a task");
        assertThat(saved.getLink()).isEqualTo("/tasks/1/edit");

        // Verify pushed via WebSocket to recipient's email
        verify(messagingTemplate)
                .convertAndSendToUser(
                        eq("alice@example.com"), eq("/queue/notifications"), eq(response));
    }

    // ── getUnreadCount ───────────────────────────────────────────────────

    @Test
    void getUnreadCount_delegatesToRepository() {
        when(notificationRepository.countByUserIdAndReadFalse(ID_1)).thenReturn(3L);

        assertThat(notificationService.getUnreadCount(ID_1)).isEqualTo(3L);
    }

    // ── getRecentForUser ─────────────────────────────────────────────────

    @Test
    void getRecentForUser_returnsTop10() {
        Notification notification =
                new Notification(alice, bob, NotificationType.COMMENT_ADDED, "msg", "/link");
        NotificationResponse response = new NotificationResponse();
        when(notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(ID_1))
                .thenReturn(List.of(notification));
        when(notificationMapper.toResponseList(anyList())).thenReturn(List.of(response));

        List<NotificationResponse> result = notificationService.getRecentForUser(ID_1);

        assertThat(result).hasSize(1);
    }

    // ── findAllForUser ───────────────────────────────────────────────────

    @Test
    void findAllForUser_returnsPaginatedResults() {
        Notification notification =
                new Notification(alice, bob, NotificationType.TASK_UPDATED, "msg", "/link");
        NotificationResponse response = new NotificationResponse();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(ID_1, pageable))
                .thenReturn(page);
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(response);

        Page<NotificationResponse> result = notificationService.findAllForUser(ID_1, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ── markAsRead ───────────────────────────────────────────────────────

    @Test
    void markAsRead_found_setsReadTrue() {
        Notification notification =
                new Notification(alice, bob, NotificationType.TASK_ASSIGNED, "msg", "/link");
        notification.setId(1L);
        when(notificationRepository.findByIdAndUserId(1L, ID_1))
                .thenReturn(Optional.of(notification));

        notificationService.markAsRead(1L, ID_1);

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_notFound_doesNothing() {
        when(notificationRepository.findByIdAndUserId(99L, ID_1)).thenReturn(Optional.empty());

        notificationService.markAsRead(99L, ID_1);

        verify(notificationRepository, never()).save(any());
    }

    // ── markAllAsRead ────────────────────────────────────────────────────

    @Test
    void markAllAsRead_delegatesToRepository() {
        notificationService.markAllAsRead(ID_1);

        verify(notificationRepository).markAllAsReadByUserId(ID_1);
    }

    // ── clearAll ─────────────────────────────────────────────────────────

    @Test
    void clearAll_delegatesToRepository() {
        notificationService.clearAll(ID_1);

        verify(notificationRepository).deleteByUserId(ID_1);
    }
}
