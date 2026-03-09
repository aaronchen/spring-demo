package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.model.Setting;
import cc.desuka.demo.config.Settings;
import cc.desuka.demo.repository.SettingRepository;
import cc.desuka.demo.security.SecurityUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SettingService {

    private final SettingRepository settingRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SettingService(SettingRepository settingRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.settingRepository = settingRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Loads all settings from DB into a typed {@link Settings} object.
     * DB keys that match a {@link Settings} field name are set automatically
     * via {@link BeanWrapper} (handles String → boolean, int, etc.).
     * Missing keys fall back to the defaults defined in {@link Settings}.
     */
    public Settings load() {
        Map<String, String> db = settingRepository.findAll().stream()
                .collect(Collectors.toMap(Setting::getKey,
                        s -> s.getValue() != null ? s.getValue() : ""));

        Settings settings = new Settings();
        BeanWrapper wrapper = new BeanWrapperImpl(settings);
        db.forEach((key, value) -> {
            if (wrapper.isWritableProperty(key)) {
                wrapper.setPropertyValue(key, value);
            }
        });
        return settings;
    }

    public void updateValue(String key, String value) {
        Setting setting = settingRepository.findByKey(key)
                .orElseGet(() -> new Setting(key, null));

        Map<String, Object> before = setting.getId() != null ? setting.toAuditSnapshot() : null;
        setting.setValue(value);
        Setting saved = settingRepository.save(setting);

        if (before != null) {
            Map<String, Object> after = saved.toAuditSnapshot();
            Map<String, Object> diff = AuditDetails.diff(before, after);
            if (!diff.isEmpty()) {
                eventPublisher.publishEvent(new AuditEvent(
                        AuditEvent.SETTING_UPDATED, Setting.class, saved.getId(),
                        SecurityUtils.getCurrentPrincipal(),
                        AuditDetails.toJson(diff)));
            }
        }
    }
}
