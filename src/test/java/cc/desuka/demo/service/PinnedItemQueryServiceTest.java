package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cc.desuka.demo.config.UserPreferences;
import cc.desuka.demo.model.PinnedItem;
import cc.desuka.demo.repository.PinnedItemRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PinnedItemQueryServiceTest {

    @Mock private PinnedItemRepository pinnedItemRepository;
    @Mock private UserPreferenceQueryService userPreferenceQueryService;

    @InjectMocks private PinnedItemQueryService pinnedItemQueryService;

    private final UUID aliceId = UUID.randomUUID();

    @Test
    void getPinnedItems_defaultSortByPinnedDate() {
        UserPreferences prefs = new UserPreferences();
        when(userPreferenceQueryService.load(aliceId)).thenReturn(prefs);
        when(pinnedItemRepository.findByUserIdOrderByPinnedAtDesc(aliceId)).thenReturn(List.of());

        List<PinnedItem> result = pinnedItemQueryService.getPinnedItems(aliceId);

        assertThat(result).isEmpty();
        verify(pinnedItemRepository).findByUserIdOrderByPinnedAtDesc(aliceId);
    }

    @Test
    void getPinnedItems_sortByName() {
        UserPreferences prefs = new UserPreferences();
        prefs.setPinnedSortOrder(UserPreferences.SORT_NAME);
        when(userPreferenceQueryService.load(aliceId)).thenReturn(prefs);
        when(pinnedItemRepository.findByUserIdOrderByEntityTitleAsc(aliceId)).thenReturn(List.of());

        pinnedItemQueryService.getPinnedItems(aliceId);

        verify(pinnedItemRepository).findByUserIdOrderByEntityTitleAsc(aliceId);
    }
}
