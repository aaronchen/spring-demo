package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.PresenceResponse;
import cc.desuka.demo.presence.PresenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PresenceApiController {

    private final PresenceService presenceService;

    public PresenceApiController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @GetMapping("/api/presence")
    public PresenceResponse getPresence() {
        return new PresenceResponse(
                presenceService.getOnlineUsers(), presenceService.getOnlineCount());
    }
}
