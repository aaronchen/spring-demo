package cc.desuka.demo.audit;

import cc.desuka.demo.model.AuditLog;
import cc.desuka.demo.repository.AuditLogRepository;
import cc.desuka.demo.repository.AuditLogSpecifications;
import java.time.Instant;
import java.util.List;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only audit log lookups and search. */
@Service
@Transactional(readOnly = true)
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final MessageSource messageSource;

    public AuditLogService(AuditLogRepository auditLogRepository, MessageSource messageSource) {
        this.auditLogRepository = auditLogRepository;
        this.messageSource = messageSource;
    }

    public Page<AuditLog> getAuditPage(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    public Page<AuditLog> searchAuditLogs(
            String category, String search, Instant from, Instant to, Pageable pageable) {
        Page<AuditLog> page =
                auditLogRepository.findAll(
                        AuditLogSpecifications.build(category, search, from, to), pageable);
        resolveDisplayNames(page.getContent());
        return page;
    }

    public List<AuditLog> getRecentByActions(List<String> actions) {
        return auditLogRepository.findTop10ByActionInOrderByTimestampDesc(actions);
    }

    public List<AuditLog> getEntityHistory(Class<?> entityType, String entityId) {
        List<AuditLog> entries =
                auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(
                        entityType.getSimpleName(), entityId);
        resolveDisplayNames(entries);
        return entries;
    }

    private void resolveDisplayNames(List<AuditLog> entries) {
        var locale = LocaleContextHolder.getLocale();
        for (AuditLog entry : entries) {
            entry.setDetailsMap(
                    AuditDetails.resolveDisplayNames(entry.getDetailsMap(), messageSource, locale));
        }
    }
}
