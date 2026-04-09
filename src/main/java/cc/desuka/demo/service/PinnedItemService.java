package cc.desuka.demo.service;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.config.UserPreferences;
import cc.desuka.demo.dto.PinnedItemResponse;
import cc.desuka.demo.event.PinnedItemPushEvent;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.PinnedItem;
import cc.desuka.demo.model.RecentView;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.PinnedItemRepository;
import cc.desuka.demo.repository.RecentViewRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PinnedItemService {

    private final PinnedItemRepository pinnedItemRepository;
    private final RecentViewRepository recentViewRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AppRoutesProperties appRoutes;
    private final ApplicationContext applicationContext;

    public PinnedItemService(
            PinnedItemRepository pinnedItemRepository,
            RecentViewRepository recentViewRepository,
            ApplicationEventPublisher eventPublisher,
            AppRoutesProperties appRoutes,
            ApplicationContext applicationContext) {
        this.pinnedItemRepository = pinnedItemRepository;
        this.recentViewRepository = recentViewRepository;
        this.eventPublisher = eventPublisher;
        this.appRoutes = appRoutes;
        this.applicationContext = applicationContext;
    }

    private UserPreferenceService userPreferenceService() {
        return applicationContext.getBean(UserPreferenceService.class);
    }

    /**
     * Pin an item. Returns the pinned item, or null if the limit is reached. If the item is already
     * pinned, returns the existing pin.
     */
    public PinnedItem pin(User user, String entityType, String entityId, String entityTitle) {
        String idStr = entityId.toString();
        Optional<PinnedItem> existing =
                pinnedItemRepository.findByUserIdAndEntityTypeAndEntityId(
                        user.getId(), entityType, idStr);
        if (existing.isPresent()) {
            return existing.get();
        }

        UserPreferences prefs = userPreferenceService().load(user.getId());
        long count = pinnedItemRepository.countByUserId(user.getId());
        if (count >= prefs.getPinnedLimit()) {
            return null;
        }

        PinnedItem pin = new PinnedItem(user, entityType, idStr, entityTitle);
        pin.setSortOrder((int) count);
        pinnedItemRepository.save(pin);

        String href = resolveHref(entityType, idStr);
        PinnedItemResponse payload =
                new PinnedItemResponse(
                        pin.getId(),
                        entityType,
                        idStr,
                        entityTitle,
                        href,
                        pin.getPinnedAt(),
                        true,
                        false,
                        false);
        eventPublisher.publishEvent(new PinnedItemPushEvent(user.getEmail(), payload));

        return pin;
    }

    /** Unpin an item by ID. Pushes a deleted event via WebSocket. */
    public void unpin(Long id) {
        PinnedItem pin =
                pinnedItemRepository
                        .findById(id)
                        .orElseThrow(() -> new EntityNotFoundException(PinnedItem.class, id));
        PinnedItemResponse payload =
                new PinnedItemResponse(
                        pin.getId(),
                        pin.getEntityType(),
                        pin.getEntityId(),
                        null,
                        null,
                        null,
                        false,
                        false,
                        true);
        eventPublisher.publishEvent(new PinnedItemPushEvent(pin.getUser().getEmail(), payload));
        pinnedItemRepository.delete(pin);
    }

    /** Get a pin by ID with user eagerly loaded (for ownership checks). */
    @Transactional(readOnly = true)
    public PinnedItem getPinById(Long id) {
        return pinnedItemRepository
                .findWithUserById(id)
                .orElseThrow(() -> new EntityNotFoundException(PinnedItem.class, id));
    }

    /** Get user's pinned items, sorted per their preference. */
    @Transactional(readOnly = true)
    public List<PinnedItem> getPinnedItems(UUID userId) {
        UserPreferences prefs = userPreferenceService().load(userId);
        String sortOrder = prefs.getPinnedSortOrder();

        return switch (sortOrder) {
            case UserPreferences.SORT_NAME ->
                    pinnedItemRepository.findByUserIdOrderByEntityTitleAsc(userId);
            case UserPreferences.SORT_MANUAL ->
                    pinnedItemRepository.findByUserIdOrderBySortOrderAsc(userId);
            case UserPreferences.SORT_LAST_VIEWED -> sortByLastViewed(userId);
            default -> pinnedItemRepository.findByUserIdOrderByPinnedAtDesc(userId);
        };
    }

    /** Update sort order for manual drag-and-drop reordering. */
    public void reorder(UUID userId, List<Long> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            pinnedItemRepository
                    .findById(orderedIds.get(i))
                    .ifPresent(
                            pin -> {
                                if (pin.getUser().getId().equals(userId)) {
                                    pin.setSortOrder(orderedIds.indexOf(pin.getId()));
                                }
                            });
        }
    }

    /** Update title across all users' pins when an entity is renamed. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTitle(String entityType, Object entityId, String title) {
        String idStr = entityId.toString();
        pinnedItemRepository.updateTitle(entityType, idStr, title);

        String href = resolveHref(entityType, idStr);
        for (PinnedItem pin : pinnedItemRepository.findByEntityTypeAndEntityId(entityType, idStr)) {
            PinnedItemResponse payload =
                    new PinnedItemResponse(
                            pin.getId(),
                            entityType,
                            idStr,
                            title,
                            href,
                            pin.getPinnedAt(),
                            false,
                            true,
                            false);
            eventPublisher.publishEvent(new PinnedItemPushEvent(pin.getUser().getEmail(), payload));
        }
    }

    /** Delete all pins for an entity (e.g., task deleted). Notifies affected users. */
    public void deleteByEntity(String entityType, Object entityId) {
        String idStr = entityId.toString();
        for (PinnedItem pin : pinnedItemRepository.findByEntityTypeAndEntityId(entityType, idStr)) {
            PinnedItemResponse payload =
                    new PinnedItemResponse(
                            pin.getId(),
                            pin.getEntityType(),
                            idStr,
                            null,
                            null,
                            null,
                            false,
                            false,
                            true);
            eventPublisher.publishEvent(new PinnedItemPushEvent(pin.getUser().getEmail(), payload));
        }
        pinnedItemRepository.deleteByEntityTypeAndEntityId(entityType, idStr);
    }

    /** Delete all pins for a user (user deletion cleanup). */
    public void deleteByUserId(UUID userId) {
        pinnedItemRepository.deleteByUserId(userId);
    }

    /** Delete a user's pins for a specific project and all its tasks (member removal cleanup). */
    public void deleteByUserAndProject(UUID userId, UUID projectId) {
        pinnedItemRepository.deleteByUserAndProject(userId, projectId);
    }

    private List<PinnedItem> sortByLastViewed(UUID userId) {
        List<PinnedItem> pins = pinnedItemRepository.findByUserIdOrderByPinnedAtDesc(userId);
        List<RecentView> recentViews =
                recentViewRepository.findTop10ByUserIdOrderByViewedAtDesc(userId);

        Map<String, LocalDateTime> viewedAtMap =
                recentViews.stream()
                        .collect(
                                Collectors.toMap(
                                        rv -> rv.getEntityType() + ":" + rv.getEntityId(),
                                        RecentView::getViewedAt,
                                        (a, b) -> a));

        pins.sort(
                Comparator.comparing(
                                (PinnedItem pin) ->
                                        viewedAtMap.getOrDefault(
                                                pin.getEntityType() + ":" + pin.getEntityId(),
                                                LocalDateTime.MIN))
                        .reversed());

        return pins;
    }

    private String resolveHref(String entityType, String entityId) {
        if (PinnedItem.TYPE_TASK.equals(entityType)) {
            return appRoutes.getTaskDetail().params("taskId", entityId).build();
        }
        return appRoutes.getProjectDetail().params("projectId", entityId).build();
    }
}
