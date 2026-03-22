package cc.desuka.demo.security;

import cc.desuka.demo.model.User;
import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Central utility for resolving the current authenticated user.
 *
 * <p>All user-resolution logic lives here — controllers, services, template dialects, and WebSocket
 * listeners delegate to these methods instead of duplicating the SecurityContextHolder / Principal
 * pattern.
 */
public class SecurityUtils {

    /**
     * Returns the current authenticated user's email, or "system" if unauthenticated (e.g., during
     * DataLoader seeding).
     */
    public static String getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }

    /**
     * Returns the current authenticated {@link User} entity, or {@code null} if unauthenticated or
     * the principal is not a {@link CustomUserDetails}.
     */
    public static User getCurrentUser() {
        CustomUserDetails details = getCurrentUserDetails();
        return details != null ? details.getUser() : null;
    }

    /** Returns the current {@link CustomUserDetails}, or {@code null} if unauthenticated. */
    public static CustomUserDetails getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details;
        }
        return null;
    }

    /**
     * Updates the cached {@link User} in the current session's SecurityContext if the given user is
     * the currently logged-in user. Call this after any user update to keep the session in sync
     * with the database.
     */
    public static void refreshCachedUser(User updated) {
        User current = getCurrentUser();
        if (current != null && current.getId().equals(updated.getId())) {
            current.setName(updated.getName());
            current.setEmail(updated.getEmail());
            current.setRole(updated.getRole());
            current.setEnabled(updated.isEnabled());
        }
    }

    /** Returns {@code true} if the given user ID belongs to the currently logged-in user. */
    public static boolean isCurrentUser(Long userId) {
        User current = getCurrentUser();
        return current != null && current.getId().equals(userId);
    }

    /**
     * Extracts the {@link User} entity from an arbitrary {@link Principal}, such as the one
     * provided by WebSocket session events. Returns {@code null} if the principal does not wrap a
     * {@link CustomUserDetails}.
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
