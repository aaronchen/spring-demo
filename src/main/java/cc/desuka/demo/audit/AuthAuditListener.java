package cc.desuka.demo.audit;

import cc.desuka.demo.model.AuditLog;
import cc.desuka.demo.repository.AuditLogRepository;
import java.time.Instant;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists audit logs for authentication events directly. Spring Security publishes auth events
 * outside a Spring-managed transaction, so the normal {@code @TransactionalEventListener} path in
 * {@link AuditEventListener} would never fire.
 */
@Component
public class AuthAuditListener {

    private final AuditLogRepository auditLogRepository;

    public AuthAuditListener(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @EventListener
    @Transactional
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String principal = event.getAuthentication().getName();
        saveAuditLog(AuditEvent.AUTH_SUCCESS, principal, null);
    }

    @EventListener
    @Transactional
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String principal = event.getAuthentication().getName();
        String reason = event.getException().getMessage();
        saveAuditLog(
                AuditEvent.AUTH_FAILURE, principal, AuditDetails.toJson(Map.of("reason", reason)));
    }

    private void saveAuditLog(String action, String principal, String details) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setPrincipal(principal);
        log.setDetails(details);
        log.setTimestamp(Instant.now());
        auditLogRepository.save(log);
    }
}
