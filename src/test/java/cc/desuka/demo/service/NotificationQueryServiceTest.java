package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import cc.desuka.demo.dto.NotificationResponse;
import cc.desuka.demo.mapper.NotificationMapper;
import cc.desuka.demo.model.Notification;
import cc.desuka.demo.model.NotificationType;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationMapper notificationMapper;

    @InjectMocks private NotificationQueryService notificationQueryService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        alice.setId(ID_1);
        bob = new User("Bob", "bob@example.com", "password", Role.USER);
        bob.setId(ID_2);
    }

    @Test
    void getUnreadCount_delegatesToRepository() {
        when(notificationRepository.countByUserIdAndReadFalse(ID_1)).thenReturn(3L);

        assertThat(notificationQueryService.getUnreadCount(ID_1)).isEqualTo(3L);
    }

    @Test
    void getRecentForUser_returnsTop10() {
        Notification notification =
                new Notification(alice, bob, NotificationType.COMMENT_ADDED, "msg", "/link");
        NotificationResponse response = new NotificationResponse();
        when(notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(ID_1))
                .thenReturn(List.of(notification));
        when(notificationMapper.toResponseList(anyList())).thenReturn(List.of(response));

        List<NotificationResponse> result = notificationQueryService.getRecentForUser(ID_1);

        assertThat(result).hasSize(1);
    }

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

        Page<NotificationResponse> result = notificationQueryService.findAllForUser(ID_1, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}
