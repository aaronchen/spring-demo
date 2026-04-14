package cc.desuka.demo.service;

import cc.desuka.demo.model.RecentView;
import cc.desuka.demo.repository.RecentViewRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only recent view lookups. Counterpart to {@link RecentViewService} (writes). */
@Service
@Transactional(readOnly = true)
public class RecentViewQueryService {

    private final RecentViewRepository recentViewRepository;

    public RecentViewQueryService(RecentViewRepository recentViewRepository) {
        this.recentViewRepository = recentViewRepository;
    }

    public List<RecentView> getRecentViews(UUID userId) {
        return recentViewRepository.findTop10ByUserIdOrderByViewedAtDesc(userId);
    }
}
