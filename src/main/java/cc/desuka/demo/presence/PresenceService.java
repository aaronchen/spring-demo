package cc.desuka.demo.presence;

import cc.desuka.demo.service.UserQueryService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class PresenceService {

    private final ConcurrentHashMap<String, UUID> onlineSessions = new ConcurrentHashMap<>();
    private final UserQueryService userQueryService;

    public PresenceService(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    public void userConnected(String sessionId, UUID userId) {
        onlineSessions.put(sessionId, userId);
    }

    public void userDisconnected(String sessionId) {
        onlineSessions.remove(sessionId);
    }

    public List<String> getOnlineUsers() {
        Set<UUID> uniqueIds = new HashSet<>(onlineSessions.values());
        Map<UUID, String> names = userQueryService.getNamesByIds(uniqueIds);
        return names.values().stream().sorted().toList();
    }

    public int getOnlineCount() {
        return (int) onlineSessions.values().stream().distinct().count();
    }
}
