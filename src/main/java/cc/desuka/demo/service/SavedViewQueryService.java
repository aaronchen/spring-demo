package cc.desuka.demo.service;

import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.SavedView;
import cc.desuka.demo.repository.SavedViewRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only saved view lookups. Counterpart to {@link SavedViewService} (writes). */
@Service
@Transactional(readOnly = true)
public class SavedViewQueryService {

    private final SavedViewRepository savedViewRepository;

    public SavedViewQueryService(SavedViewRepository savedViewRepository) {
        this.savedViewRepository = savedViewRepository;
    }

    public List<SavedView> getViewsForUser(UUID userId) {
        return savedViewRepository.findByUserIdOrderByNameAsc(userId);
    }

    public SavedView getViewById(Long id) {
        return savedViewRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(SavedView.class, id));
    }
}
