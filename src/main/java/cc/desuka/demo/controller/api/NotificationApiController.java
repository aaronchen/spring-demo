package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.NotificationResponse;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.service.NotificationQueryService;
import cc.desuka.demo.service.NotificationService;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private final NotificationQueryService notificationQueryService;
    private final NotificationService notificationService;

    public NotificationApiController(
            NotificationQueryService notificationQueryService,
            NotificationService notificationService) {
        this.notificationQueryService = notificationQueryService;
        this.notificationService = notificationService;
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(@AuthenticationPrincipal CustomUserDetails user) {
        return Map.of("count", notificationQueryService.getUnreadCount(user.getUser().getId()));
    }

    @GetMapping
    public Page<NotificationResponse> getNotifications(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return notificationQueryService.findAllForUser(
                user.getUser().getId(), PageRequest.of(page, size));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        notificationService.markAsRead(id, user.getUser().getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal CustomUserDetails user) {
        notificationService.markAllAsRead(user.getUser().getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearAll(@AuthenticationPrincipal CustomUserDetails user) {
        notificationService.clearAll(user.getUser().getId());
        return ResponseEntity.noContent().build();
    }
}
