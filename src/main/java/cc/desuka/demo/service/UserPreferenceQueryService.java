package cc.desuka.demo.service;

import cc.desuka.demo.config.UserPreferences;
import cc.desuka.demo.model.UserPreference;
import cc.desuka.demo.repository.UserPreferenceRepository;
import cc.desuka.demo.util.BeanWrapperLoader;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only user preference lookups. Counterpart to {@link UserPreferenceService} (writes). */
@Service
@Transactional(readOnly = true)
public class UserPreferenceQueryService {

    private final UserPreferenceRepository preferenceRepository;

    public UserPreferenceQueryService(UserPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    /**
     * Loads all preferences for a user into a typed {@link UserPreferences} object. Missing keys
     * fall back to the defaults defined in {@link UserPreferences}.
     */
    public UserPreferences load(UUID userId) {
        Map<String, String> db =
                preferenceRepository.findByUserId(userId).stream()
                        .collect(
                                Collectors.toMap(
                                        UserPreference::getKey,
                                        p -> p.getValue() != null ? p.getValue() : ""));
        return BeanWrapperLoader.load(db, UserPreferences::new);
    }
}
