package cc.desuka.demo.controller.api;

import cc.desuka.demo.service.PresenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PresenceApiController {

    private final PresenceService presenceService;

    public PresenceApiController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @GetMapping("/api/presence")
    public Map<String, Object> getPresence() {
        return Map.of(
                "users", presenceService.getOnlineUsers(),
                "count", presenceService.getOnlineCount()
        );
    }
}
