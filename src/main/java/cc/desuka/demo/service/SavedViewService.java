package cc.desuka.demo.service;

import cc.desuka.demo.dto.SavedViewData;
import cc.desuka.demo.model.SavedView;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.SavedViewRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saved view write operations (create, delete). Counterpart to {@link SavedViewQueryService}
 * (reads).
 */
@Service
@Transactional
public class SavedViewService {

    private final SavedViewRepository savedViewRepository;

    public SavedViewService(SavedViewRepository savedViewRepository) {
        this.savedViewRepository = savedViewRepository;
    }

    public SavedView createView(User user, String name, SavedViewData data) {
        SavedView view = new SavedView(user, name, data.toJson());
        return savedViewRepository.save(view);
    }

    public void deleteView(SavedView view) {
        savedViewRepository.delete(view);
    }

    public void deleteByUserId(UUID userId) {
        savedViewRepository.deleteByUserId(userId);
    }
}
