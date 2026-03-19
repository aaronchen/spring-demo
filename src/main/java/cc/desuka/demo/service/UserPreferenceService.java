package cc.desuka.demo.service;

import cc.desuka.demo.config.UserPreferences;
import cc.desuka.demo.model.User;
import cc.desuka.demo.model.UserPreference;
import cc.desuka.demo.repository.UserPreferenceRepository;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserPreferenceService {

    private final UserPreferenceRepository preferenceRepository;
    private final UserService userService;

    public UserPreferenceService(UserPreferenceRepository preferenceRepository,
                                 UserService userService) {
        this.preferenceRepository = preferenceRepository;
        this.userService = userService;
    }

    /**
     * Loads all preferences for a user into a typed {@link UserPreferences} object.
     * DB keys that match a field name are set automatically via {@link BeanWrapper}.
     * Missing keys fall back to the defaults defined in {@link UserPreferences}.
     */
    public UserPreferences load(Long userId) {
        Map<String, String> db = preferenceRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserPreference::getKey,
                        p -> p.getValue() != null ? p.getValue() : ""));

        UserPreferences prefs = new UserPreferences();
        BeanWrapper wrapper = new BeanWrapperImpl(prefs);
        db.forEach((key, value) -> {
            if (wrapper.isWritableProperty(key)) {
                wrapper.setPropertyValue(key, value);
            }
        });
        return prefs;
    }

    /**
     * Creates or updates a single preference for a user.
     */
    public void save(Long userId, String key, String value) {
        UserPreference pref = preferenceRepository.findByUserIdAndKey(userId, key)
                .orElseGet(() -> {
                    User user = userService.getUserById(userId);
                    return new UserPreference(user, key, null);
                });
        pref.setValue(value);
        preferenceRepository.save(pref);
    }

    /**
     * Saves multiple preferences at once.
     */
    public void saveAll(Long userId, Map<String, String> preferences) {
        preferences.forEach((key, value) -> save(userId, key, value));
    }
}
