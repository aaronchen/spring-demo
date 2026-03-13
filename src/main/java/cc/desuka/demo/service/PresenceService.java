package cc.desuka.demo.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    private final ConcurrentHashMap<String, String> onlineSessions = new ConcurrentHashMap<>();

    public void userConnected(String sessionId, String userName) {
        onlineSessions.put(sessionId, userName);
    }

    public void userDisconnected(String sessionId) {
        onlineSessions.remove(sessionId);
    }

    public List<String> getOnlineUsers() {
        return onlineSessions.values().stream()
                .distinct()
                .sorted()
                .toList();
    }

    public int getOnlineCount() {
        return (int) onlineSessions.values().stream().distinct().count();
    }
}
