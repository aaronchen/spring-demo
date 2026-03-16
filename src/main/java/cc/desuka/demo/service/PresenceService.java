package cc.desuka.demo.service;

import cc.desuka.demo.model.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PresenceService {

    private final ConcurrentHashMap<String, Long> onlineSessions = new ConcurrentHashMap<>();
    private final UserService userService;

    public PresenceService(UserService userService) {
        this.userService = userService;
    }

    public void userConnected(String sessionId, Long userId) {
        onlineSessions.put(sessionId, userId);
    }

    public void userDisconnected(String sessionId) {
        onlineSessions.remove(sessionId);
    }

    public List<String> getOnlineUsers() {
        Set<Long> uniqueIds = onlineSessions.values().stream()
                .collect(Collectors.toSet());
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
