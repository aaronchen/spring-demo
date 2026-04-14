package cc.desuka.demo.service;

import cc.desuka.demo.config.Settings;
import cc.desuka.demo.model.Setting;
import cc.desuka.demo.repository.SettingRepository;
import cc.desuka.demo.util.BeanWrapperLoader;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only settings lookups. Counterpart to {@link SettingService} (writes). */
@Service
@Transactional(readOnly = true)
public class SettingQueryService {

    private final SettingRepository settingRepository;

    public SettingQueryService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    /**
     * Loads all settings from DB into a typed {@link Settings} object. Missing keys fall back to
     * the defaults defined in {@link Settings}.
     */
    public Settings load() {
        Map<String, String> db =
                settingRepository.findAll().stream()
                        .collect(
                                Collectors.toMap(
                                        Setting::getKey,
                                        s -> s.getValue() != null ? s.getValue() : ""));
        return BeanWrapperLoader.load(db, Settings::new);
    }
}
