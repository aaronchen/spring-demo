package cc.desuka.demo.service;

import cc.desuka.demo.model.User;
import cc.desuka.demo.model.UserPreference;
import cc.desuka.demo.repository.UserPreferenceRepository;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User preference write operations (save, delete). Counterpart to {@link
 * UserPreferenceQueryService} (reads).
 */
@Service
@Transactional
public class UserPreferenceService {

    private final UserPreferenceRepository preferenceRepository;
    private final UserQueryService userQueryService;

    public UserPreferenceService(
            UserPreferenceRepository preferenceRepository, UserQueryService userQueryService) {
        this.preferenceRepository = preferenceRepository;
        this.userQueryService = userQueryService;
    }

    /** Creates or updates a single preference for a user. */
    public void save(UUID userId, String key, String value) {
        UserPreference pref =
                preferenceRepository
                        .findByUserIdAndKey(userId, key)
                        .orElseGet(
                                () -> {
                                    User user = userQueryService.getUserById(userId);
                                    return new UserPreference(user, key, null);
                                });
        pref.setValue(value);
        preferenceRepository.save(pref);
    }

    /** Saves multiple preferences at once. */
    public void saveAll(UUID userId, Map<String, String> preferences) {
        Map<String, UserPreference> existing =
                preferenceRepository.findByUserId(userId).stream()
                        .collect(Collectors.toMap(UserPreference::getKey, p -> p));
        User user = null;

        for (Map.Entry<String, String> entry : preferences.entrySet()) {
            UserPreference pref = existing.get(entry.getKey());
            if (pref == null) {
                if (user == null) {
                    user = userQueryService.getUserById(userId);
                }
                pref = new UserPreference(user, entry.getKey(), null);
            }
            pref.setValue(entry.getValue());
            preferenceRepository.save(pref);
        }
    }

    public void deleteByUserId(UUID userId) {
        preferenceRepository.deleteByUserId(userId);
    }
}
