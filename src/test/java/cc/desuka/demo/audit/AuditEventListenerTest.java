package cc.desuka.demo.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cc.desuka.demo.model.AuditLog;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.AuditLogRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditEventListenerTest {

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private AuditEventListener auditEventListener;

    @Test
    void onAuditEvent_persistsAuditLog() {
        AuditEvent event =
                new AuditEvent(
                        AuditEvent.TASK_CREATED,
                        User.class,
                        ID_1,
                        "alice@example.com",
                        "{\"title\":\"Test\"}");

        auditEventListener.onAuditEvent(event);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo(AuditEvent.TASK_CREATED);
        assertThat(log.getEntityType()).isEqualTo("User");
        assertThat(log.getEntityId()).isEqualTo(ID_1.toString());
        assertThat(log.getPrincipal()).isEqualTo("alice@example.com");
        assertThat(log.getDetails()).isEqualTo("{\"title\":\"Test\"}");
        assertThat(log.getTimestamp()).isNotNull();
    }

    @Test
    void onAuditEvent_systemPrincipal_skipsLog() {
        AuditEvent event =
                new AuditEvent(AuditEvent.TASK_CREATED, User.class, ID_1, "system", null);

        auditEventListener.onAuditEvent(event);

        verify(auditLogRepository, never()).save(any());
    }
}
