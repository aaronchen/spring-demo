package cc.desuka.demo.security;

import cc.desuka.demo.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;

/**
 * Central utility for resolving the current authenticated user.
 *
 * All user-resolution logic lives here — controllers, services, template
 * dialects, and WebSocket listeners delegate to these methods instead of
 * duplicating the SecurityContextHolder / Principal pattern.
 */
public class SecurityUtils {

    /**
     * Returns the current authenticated user's email, or "system" if
     * unauthenticated (e.g., during DataLoader seeding).
     */
    public static String getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }

    /**
     * Returns the current authenticated {@link User} entity, or {@code null}
     * if unauthenticated or the principal is not a {@link CustomUserDetails}.
     */
    public static User getCurrentUser() {
        CustomUserDetails details = getCurrentUserDetails();
        return details != null ? details.getUser() : null;
    }

    /**
     * Returns the current {@link CustomUserDetails}, or {@code null}
     * if unauthenticated.
     */
    public static CustomUserDetails getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details;
        }
        return null;
    }

    /**
     * Extracts the {@link User} entity from an arbitrary {@link Principal},
     * such as the one provided by WebSocket session events.
     * Returns {@code null} if the principal does not wrap a {@link CustomUserDetails}.
     */
    public static User getUserFrom(Principal principal) {
        if (principal instanceof Authentication auth
                && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUser();
        }
        return null;
    }

    private SecurityUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
}
