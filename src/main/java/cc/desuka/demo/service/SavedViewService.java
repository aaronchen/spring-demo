package cc.desuka.demo.service;

import cc.desuka.demo.dto.SavedViewData;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.SavedView;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.SavedViewRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SavedViewService {

    private final SavedViewRepository savedViewRepository;

    public SavedViewService(SavedViewRepository savedViewRepository) {
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
