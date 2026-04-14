package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.audit.AuditField;
import cc.desuka.demo.model.Setting;
import cc.desuka.demo.repository.SettingRepository;
import cc.desuka.demo.security.SecurityUtils;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Site setting write operations (update). Counterpart to {@link SettingQueryService} (reads). */
@Service
@Transactional
public class SettingService {

    private final SettingRepository settingRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SettingService(
            SettingRepository settingRepository, ApplicationEventPublisher eventPublisher) {
        this.settingRepository = settingRepository;
        this.eventPublisher = eventPublisher;
    }

    public void updateValue(String key, String value) {
        Setting setting = settingRepository.findByKey(key).orElseGet(() -> new Setting(key, null));

        Map<String, AuditField> before = setting.getId() != null ? setting.toAuditSnapshot() : null;
        setting.setValue(value);
        Setting saved = settingRepository.save(setting);

        if (before != null) {
            Map<String, AuditField> after = saved.toAuditSnapshot();
            Map<String, Object> diff = AuditDetails.diff(before, after);
            if (!diff.isEmpty()) {
                eventPublisher.publishEvent(
                        new AuditEvent(
                                AuditEvent.SETTING_UPDATED,
                                Setting.class,
                                saved.getId(),
                                SecurityUtils.getCurrentPrincipal(),
                                AuditDetails.toJson(diff)));
            }
        }
    }
}
