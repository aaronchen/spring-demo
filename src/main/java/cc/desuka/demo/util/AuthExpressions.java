package cc.desuka.demo.util;

import cc.desuka.demo.model.OwnedEntity;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.User;

/**
 * Custom Thymeleaf expression object exposed as {@code #auth} in templates.
 *
 * <p>Constructed per-request by {@link AuthDialect} with the currently authenticated
 * {@link User} (or {@code null} for anonymous requests).
 *
 * <h3>Template usage</h3>
 * <pre>{@code
 * <!-- Replaces the verbose inline ownership check -->
 * <div th:with="isOwner=${#auth.isOwner(task)}">
 *     <button th:if="${isOwner}" ...>Delete</button>
 * </div>
 * }</pre>
 *
 * <h3>Available methods</h3>
 * <ul>
 *   <li>{@code ${#auth.isOwner(task)}} — true if current user owns the entity</li>
 *   <li>{@code ${#auth.isAdmin()}} — true if current user has ADMIN role</li>
 *   <li>{@code ${#auth.canEdit(task)}} — true if admin or owner (use for UI visibility)</li>
 * </ul>
 *
 * @see AuthDialect
 * @see OwnedEntity
 */
public class AuthExpressions {

    private final User currentUser;

    public AuthExpressions(User currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Returns {@code true} if the currently authenticated user owns the given entity.
     *
     * <p>This instance method exists so Thymeleaf can call {@code ${#auth.isOwner(task)}}
     * — expression objects must be instances, not static method holders. It delegates to
     * the static overload which holds the actual comparison logic.
     *
     * @param entity any entity implementing {@link OwnedEntity}
     * @return {@code true} if the current user is the entity's owner
     */
    public boolean isOwner(OwnedEntity entity) {
        return isOwner(this.currentUser, entity);
    }

    /**
     * Returns {@code true} if the current user has the {@link Role#ADMIN} role.
     */
    public boolean isAdmin() {
        return isAdmin(this.currentUser);
    }

    /**
     * Returns {@code true} if the current user can edit the given entity —
     * either as its owner or as an admin. Use in templates to control
     * edit/delete button visibility.
     */
    public boolean canEdit(OwnedEntity entity) {
        return isAdmin() || isOwner(entity);
    }

    /**
     * Returns {@code true} if {@code currentUser} owns the given entity.
     *
     * <p>Ownership requires both a non-null user and an assigned entity owner,
     * compared via {@link User#equals(Object)} (which checks {@code id} only).
     *
     * <p>This static overload exists so server-side code (e.g. controller guard methods)
     * can reuse the same ownership logic without constructing an {@code AuthExpressions}
     * instance or depending on the Thymeleaf dialect.
     *
     * @param currentUser the user to test (may be {@code null} for anonymous)
     * @param entity      any entity implementing {@link OwnedEntity}
     * @return {@code true} if {@code currentUser} is the entity's owner
     */
    public static boolean isOwner(User currentUser, OwnedEntity entity) {
        if (currentUser == null || entity == null || entity.getUser() == null) {
            return false;
        }
        return currentUser.equals(entity.getUser());
    }

    /**
     * Static overload for server-side admin checks (e.g. {@link cc.desuka.demo.security.OwnershipGuard}).
     */
    public static boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }
}
