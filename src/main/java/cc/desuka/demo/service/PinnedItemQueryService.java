package cc.desuka.demo.service;

import cc.desuka.demo.config.UserPreferences;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.PinnedItem;
import cc.desuka.demo.repository.PinnedItemRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only pinned item lookups. Counterpart to {@link PinnedItemService} (writes). */
@Service
@Transactional(readOnly = true)
public class PinnedItemQueryService {

    private final PinnedItemRepository pinnedItemRepository;
    private final UserPreferenceQueryService userPreferenceQueryService;

    public PinnedItemQueryService(
            PinnedItemRepository pinnedItemRepository,
            UserPreferenceQueryService userPreferenceQueryService) {
        this.pinnedItemRepository = pinnedItemRepository;
        this.userPreferenceQueryService = userPreferenceQueryService;
    }

    /** Get a pin by ID with user eagerly loaded (for ownership checks). */
    public PinnedItem getPinById(Long id) {
        return pinnedItemRepository
                .findWithUserById(id)
                .orElseThrow(() -> new EntityNotFoundException(PinnedItem.class, id));
    }

    /** Get user's pinned items, sorted per their preference. */
    public List<PinnedItem> getPinnedItems(UUID userId) {
        UserPreferences prefs = userPreferenceQueryService.load(userId);
        String sortOrder = prefs.getPinnedSortOrder();

        return switch (sortOrder) {
            case UserPreferences.SORT_NAME ->
                    pinnedItemRepository.findByUserIdOrderByEntityTitleAsc(userId);
            case UserPreferences.SORT_MANUAL ->
                    pinnedItemRepository.findByUserIdOrderBySortOrderAsc(userId);
            default -> pinnedItemRepository.findByUserIdOrderByPinnedAtDesc(userId);
        };
    }
}
