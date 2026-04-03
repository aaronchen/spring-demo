package cc.desuka.demo.presence;

import cc.desuka.demo.model.User;
import cc.desuka.demo.service.UserService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PresenceService {

    private final ConcurrentHashMap<String, UUID> onlineSessions = new ConcurrentHashMap<>();
    private final UserService userService;

    public PresenceService(UserService userService) {
        this.userService = userService;
    }

    public void userConnected(String sessionId, UUID userId) {
        onlineSessions.put(sessionId, userId);
    }

    public void userDisconnected(String sessionId) {
        onlineSessions.remove(sessionId);
    }

    public List<String> getOnlineUsers() {
        Set<UUID> uniqueIds = onlineSessions.values().stream().collect(Collectors.toSet());
        return uniqueIds.stream()
                .map(userService::findUserById)
                .filter(user -> user != null)
                .map(User::getName)
                .sorted()
                .toList();
    }

    public int getOnlineCount() {
        return (int) onlineSessions.values().stream().distinct().count();
    }
}
