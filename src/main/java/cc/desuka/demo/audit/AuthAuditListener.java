package cc.desuka.demo.audit;

import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthAuditListener {

    private final ApplicationEventPublisher eventPublisher;

    public AuthAuditListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String principal = event.getAuthentication().getName();
        eventPublisher.publishEvent(
                new AuditEvent(AuditEvent.AUTH_SUCCESS, null, null, principal, null));
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String principal = event.getAuthentication().getName();
        String reason = event.getException().getMessage();
        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.AUTH_FAILURE,
                        null,
                        null,
                        principal,
                        AuditDetails.toJson(Map.of("reason", reason))));
    }
}
