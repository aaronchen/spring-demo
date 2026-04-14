package cc.desuka.demo.controller;

import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.service.NotificationQueryService;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private static final int DEFAULT_PAGE_SIZE = 25;

    private final NotificationQueryService notificationQueryService;

    public NotificationController(NotificationQueryService notificationQueryService) {
        this.notificationQueryService = notificationQueryService;
    }

    @GetMapping
    public String notificationsPage(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            Model model) {
        UUID userId = user.getUser().getId();
        model.addAttribute(
                "notifications",
                notificationQueryService.findAllForUser(userId, PageRequest.of(page, size)));
        return "notifications";
    }
}
