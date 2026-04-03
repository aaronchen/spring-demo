package cc.desuka.demo.presence;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.dto.PresenceResponse;
import cc.desuka.demo.model.User;
import cc.desuka.demo.security.SecurityUtils;
import java.security.Principal;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class PresenceEventListener {

    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AppRoutesProperties appRoutes;

    public PresenceEventListener(
            PresenceService presenceService,
            SimpMessagingTemplate messagingTemplate,
            AppRoutesProperties appRoutes) {
        this.presenceService = presenceService;
        this.messagingTemplate = messagingTemplate;
        this.appRoutes = appRoutes;
    }

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        String sessionId = accessor.getSessionId();
        if (principal != null && sessionId != null) {
            User user = SecurityUtils.getUserFrom(principal);
            if (user != null) {
                presenceService.userConnected(sessionId, user.getId());
                broadcastPresence();
            }
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        presenceService.userDisconnected(event.getSessionId());
        broadcastPresence();
    }

    private void broadcastPresence() {
        PresenceResponse payload =
                new PresenceResponse(
                        presenceService.getOnlineUsers(), presenceService.getOnlineCount());
        messagingTemplate.convertAndSend(appRoutes.getTopicPresence().toString(), payload);
    }
}
