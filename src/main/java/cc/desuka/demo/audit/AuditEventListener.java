package cc.desuka.demo.audit;

import cc.desuka.demo.model.AuditLog;
import cc.desuka.demo.repository.AuditLogRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;

    public AuditEventListener(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // Synchronous listener — saves audit log inline.
    // For production, consider @TransactionalEventListener(phase = AFTER_COMMIT)
    // to decouple audit persistence from the business transaction.
    @EventListener
    public void onAuditEvent(AuditEvent event) {
        // Skip events generated during DataLoader seeding
        if ("system".equals(event.getPrincipal())) {
            return;
        }

        AuditLog log = new AuditLog();
        log.setAction(event.getAction());
        log.setEntityType(event.getEntityType());
        log.setEntityId(event.getEntityId());
        log.setPrincipal(event.getPrincipal());
        log.setDetails(event.getDetails());
        log.setTimestamp(Instant.now());
        auditLogRepository.save(log);
    }
}
