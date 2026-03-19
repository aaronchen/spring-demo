package cc.desuka.demo.audit;

import cc.desuka.demo.model.AuditLog;
import cc.desuka.demo.repository.AuditLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Component
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;

    public AuditEventListener(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
