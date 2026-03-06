package cc.desuka.demo.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class for security-related operations.
 */
public class SecurityUtils {

    /**
     * Returns the current authenticated user's name, or "system" if unauthenticated
     * (e.g., during DataLoader seeding).
     */
    public static String getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }

    private SecurityUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
}
