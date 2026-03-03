package cc.desuka.demo.security;

import cc.desuka.demo.model.OwnedEntity;
import cc.desuka.demo.util.AuthExpressions;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Reusable access guard for controller methods.
 *
 * <p>Allows access if the current user is an admin or the entity's owner.
 * Throws {@link AccessDeniedException} otherwise — the 403 template falls
 * through to {@code #{error.403.message}} from {@code messages.properties}.
 *
 * <p>Delegates to {@link AuthExpressions} for both the admin and ownership checks,
 * so template-side ({@code #auth.canEdit()}) and controller-side checks share
 * identical logic.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * private final OwnershipGuard ownershipGuard;
 *
 * @GetMapping("/{id}/edit")
 * public String edit(@PathVariable Long id,
 *                    @AuthenticationPrincipal CustomUserDetails details) {
 *     Task task = taskService.getTaskById(id);
 *     ownershipGuard.requireAccess(task, details);
 *     ...
 * }
 * }</pre>
 */
@Component
public class OwnershipGuard {

    /**
     * Throws {@link AccessDeniedException} unless the authenticated user is an admin
     * or the entity's owner.
     *
     * <p>Unassigned entities ({@code entity.getUser() == null}) are editable by
     * admins only — regular users cannot claim ownership.
     *
     * @param entity         the entity to check access for
     * @param currentDetails the currently authenticated user's details
     * @throws AccessDeniedException if the user has no access
     */
    public void requireAccess(OwnedEntity entity, CustomUserDetails currentDetails) {
        if (AuthExpressions.isAdmin(currentDetails.getUser())) {
            return;
        }
        if (!AuthExpressions.isOwner(currentDetails.getUser(), entity)) {
            throw new AccessDeniedException("Access Denied");
        }
    }
}
