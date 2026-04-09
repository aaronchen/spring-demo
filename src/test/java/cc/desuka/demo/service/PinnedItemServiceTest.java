package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.config.UserPreferences;
import cc.desuka.demo.event.PinnedItemPushEvent;
import cc.desuka.demo.model.PinnedItem;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.PinnedItemRepository;
import cc.desuka.demo.repository.RecentViewRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PinnedItemServiceTest {

    @Mock private PinnedItemRepository pinnedItemRepository;
    @Mock private RecentViewRepository recentViewRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AppRoutesProperties appRoutes;
    @Mock private ApplicationContext applicationContext;
    @Mock private UserPreferenceService userPreferenceService;

    private PinnedItemService pinnedItemService;

    private User alice;
    private UUID aliceId;

    @BeforeEach
    void setUp() {
        pinnedItemService =
                new PinnedItemService(
                        pinnedItemRepository,
                        recentViewRepository,
                        eventPublisher,
                        appRoutes,
                        applicationContext);

        aliceId = UUID.randomUUID();
        alice = new User();
        alice.setId(aliceId);
        alice.setName("Alice");
        alice.setEmail("alice@example.com");

        lenient()
                .when(applicationContext.getBean(UserPreferenceService.class))
                .thenReturn(userPreferenceService);
        lenient()
                .when(appRoutes.getTaskDetail())
                .thenReturn(new cc.desuka.demo.util.RouteTemplate("/tasks/{taskId}"));
        lenient()
                .when(appRoutes.getProjectDetail())
                .thenReturn(new cc.desuka.demo.util.RouteTemplate("/projects/{projectId}"));
    }

    @Test
    void pin_createsNewPinAndPublishesEvent() {
        UserPreferences prefs = new UserPreferences();
        when(userPreferenceService.load(aliceId)).thenReturn(prefs);
        when(pinnedItemRepository.findByUserIdAndEntityTypeAndEntityId(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(pinnedItemRepository.countByUserId(aliceId)).thenReturn(0L);
        when(pinnedItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PinnedItem pin = pinnedItemService.pin(alice, "TASK", "task-123", "My Task");

        assertThat(pin).isNotNull();
        assertThat(pin.getEntityType()).isEqualTo("TASK");
        assertThat(pin.getEntityId()).isEqualTo("task-123");
        verify(pinnedItemRepository).save(any());
        verify(eventPublisher).publishEvent(any(PinnedItemPushEvent.class));
    }

    @Test
    void pin_returnsExistingWhenAlreadyPinned() {
        PinnedItem existing = new PinnedItem(alice, "TASK", "task-123", "My Task");
        when(pinnedItemRepository.findByUserIdAndEntityTypeAndEntityId(aliceId, "TASK", "task-123"))
                .thenReturn(Optional.of(existing));

        PinnedItem result = pinnedItemService.pin(alice, "TASK", "task-123", "My Task");

        assertThat(result).isSameAs(existing);
        verify(pinnedItemRepository, never()).save(any());
    }

    @Test
    void pin_returnsNullWhenLimitReached() {
        UserPreferences prefs = new UserPreferences();
        prefs.setPinnedLimit(10);
        when(userPreferenceService.load(aliceId)).thenReturn(prefs);
        when(pinnedItemRepository.findByUserIdAndEntityTypeAndEntityId(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(pinnedItemRepository.countByUserId(aliceId)).thenReturn(10L);

        PinnedItem result = pinnedItemService.pin(alice, "TASK", "task-123", "My Task");

        assertThat(result).isNull();
        verify(pinnedItemRepository, never()).save(any());
    }

    @Test
    void unpin_deletesAndPublishesEvent() {
        PinnedItem pin = new PinnedItem(alice, "TASK", "task-123", "My Task");
        pin.setId(1L);
        when(pinnedItemRepository.findById(1L)).thenReturn(Optional.of(pin));

        pinnedItemService.unpin(1L);

        verify(pinnedItemRepository).delete(pin);
        verify(eventPublisher).publishEvent(any(PinnedItemPushEvent.class));
    }

    @Test
    void getPinnedItems_defaultSortByPinnedDate() {
        UserPreferences prefs = new UserPreferences();
        when(userPreferenceService.load(aliceId)).thenReturn(prefs);
        when(pinnedItemRepository.findByUserIdOrderByPinnedAtDesc(aliceId)).thenReturn(List.of());

        List<PinnedItem> result = pinnedItemService.getPinnedItems(aliceId);

        assertThat(result).isEmpty();
        verify(pinnedItemRepository).findByUserIdOrderByPinnedAtDesc(aliceId);
    }

    @Test
    void getPinnedItems_sortByName() {
        UserPreferences prefs = new UserPreferences();
        prefs.setPinnedSortOrder(UserPreferences.SORT_NAME);
        when(userPreferenceService.load(aliceId)).thenReturn(prefs);
        when(pinnedItemRepository.findByUserIdOrderByEntityTitleAsc(aliceId)).thenReturn(List.of());

        pinnedItemService.getPinnedItems(aliceId);

        verify(pinnedItemRepository).findByUserIdOrderByEntityTitleAsc(aliceId);
    }

    @Test
    void deleteByEntity_notifiesUsersAndDeletes() {
        PinnedItem pin = new PinnedItem(alice, "TASK", "task-123", "My Task");
        pin.setId(1L);
        when(pinnedItemRepository.findByEntityTypeAndEntityId("TASK", "task-123"))
                .thenReturn(List.of(pin));

        pinnedItemService.deleteByEntity("TASK", "task-123");

        verify(eventPublisher).publishEvent(any(PinnedItemPushEvent.class));
        verify(pinnedItemRepository).deleteByEntityTypeAndEntityId("TASK", "task-123");
    }

    @Test
    void deleteByUserId_deletesAll() {
        pinnedItemService.deleteByUserId(aliceId);

        verify(pinnedItemRepository).deleteByUserId(aliceId);
    }
}
