package cc.desuka.demo.service;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.config.UserPreferences;
import cc.desuka.demo.dto.PinnedItemResponse;
import cc.desuka.demo.event.PinnedItemPushEvent;
import cc.desuka.demo.exception.PinLimitReachedException;
import cc.desuka.demo.model.PinnedItem;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.PinnedItemRepository;
import cc.desuka.demo.util.EntityTypes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pinned item write operations (pin, unpin, reorder, title sync, cleanup). Counterpart to {@link
 * PinnedItemQueryService} (reads).
 */
@Service
@Transactional
public class PinnedItemService {

    private final PinnedItemRepository pinnedItemRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AppRoutesProperties appRoutes;
    private final UserPreferenceQueryService userPreferenceQueryService;

    public PinnedItemService(
            PinnedItemRepository pinnedItemRepository,
            ApplicationEventPublisher eventPublisher,
            AppRoutesProperties appRoutes,
            UserPreferenceQueryService userPreferenceQueryService) {
        this.pinnedItemRepository = pinnedItemRepository;
        this.eventPublisher = eventPublisher;
        this.appRoutes = appRoutes;
        this.userPreferenceQueryService = userPreferenceQueryService;
    }

    /**
     * Pin an item. Returns the pinned item. If the item is already pinned, returns the existing
     * pin.
     *
     * @throws PinLimitReachedException if the user's pin limit is reached
     */
    public PinnedItem pin(User user, String entityType, String entityId, String entityTitle) {
        Optional<PinnedItem> existing =
                pinnedItemRepository.findByUserIdAndEntityTypeAndEntityId(
                        user.getId(), entityType, entityId);
        if (existing.isPresent()) {
            return existing.get();
        }

        UserPreferences prefs = userPreferenceQueryService.load(user.getId());
        long count = pinnedItemRepository.countByUserId(user.getId());
        if (count >= prefs.getPinnedLimit()) {
            throw new PinLimitReachedException(count, prefs.getPinnedLimit());
        }

        PinnedItem pin = new PinnedItem(user, entityType, entityId, entityTitle);
        pin.setSortOrder((int) count);
        pinnedItemRepository.save(pin);

        PinnedItemResponse payload =
                PinnedItemResponse.pinned(
                        pin.getId(),
                        entityType,
                        entityId,
                        entityTitle,
                        EntityTypes.resolveHref(appRoutes, entityType, entityId),
                        pin.getPinnedAt());
        eventPublisher.publishEvent(new PinnedItemPushEvent(user.getEmail(), payload));

        return pin;
    }

    /** Unpin an item. Pushes a deleted event via WebSocket. */
    public void unpin(PinnedItem pin) {
        PinnedItemResponse payload =
                PinnedItemResponse.deleted(pin.getId(), pin.getEntityType(), pin.getEntityId());
        eventPublisher.publishEvent(new PinnedItemPushEvent(pin.getUser().getEmail(), payload));
        pinnedItemRepository.delete(pin);
    }

    /** Update sort order for manual drag-and-drop reordering. */
    public void reorder(UUID userId, List<Long> orderedIds) {
        Map<Long, Integer> idToIndex = new HashMap<>();
        for (int i = 0; i < orderedIds.size(); i++) {
            idToIndex.put(orderedIds.get(i), i);
        }
        for (PinnedItem pin : pinnedItemRepository.findAllById(orderedIds)) {
            if (pin.getUser().getId().equals(userId)) {
                pin.setSortOrder(idToIndex.get(pin.getId()));
            }
        }
    }

    /** Update title across all users' pins when an entity is renamed. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTitle(String entityType, Object entityId, String title) {
        String idStr = entityId.toString();
        pinnedItemRepository.updateTitle(entityType, idStr, title);

        String href = EntityTypes.resolveHref(appRoutes, entityType, idStr);
        for (PinnedItem pin : pinnedItemRepository.findByEntityTypeAndEntityId(entityType, idStr)) {
            PinnedItemResponse payload =
                    PinnedItemResponse.titleUpdate(
                            pin.getId(), entityType, idStr, title, href, pin.getPinnedAt());
            eventPublisher.publishEvent(new PinnedItemPushEvent(pin.getUser().getEmail(), payload));
        }
    }

    /** Delete all pins for an entity (e.g., task deleted). Notifies affected users. */
    public void deleteByEntity(String entityType, Object entityId) {
        String idStr = entityId.toString();
        for (PinnedItem pin : pinnedItemRepository.findByEntityTypeAndEntityId(entityType, idStr)) {
            PinnedItemResponse payload =
                    PinnedItemResponse.deleted(pin.getId(), pin.getEntityType(), idStr);
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
}
