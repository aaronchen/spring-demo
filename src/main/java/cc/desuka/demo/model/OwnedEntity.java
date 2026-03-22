package cc.desuka.demo.model;

/**
 * Marker interface for entities that have an owner (a {@link User}).
 *
 * <p>Used by {@link cc.desuka.demo.util.AuthExpressions} to provide a generic {@code
 * #auth.isOwner(entity)} check in Thymeleaf templates, so ownership logic lives in one place
 * instead of being duplicated across every template.
 *
 * <h3>Adding to a new entity</h3>
 *
 * <ol>
 *   <li>Add {@code implements OwnedEntity} to the entity class.
 *   <li>Ensure {@code getUser()} returns the owning {@link User} (or {@code null} if unassigned).
 *   <li>Templates can then use {@code ${#auth.isOwner(entity)}}.
 * </ol>
 *
 * @see cc.desuka.demo.util.AuthExpressions
 * @see cc.desuka.demo.util.AuthDialect
 */
public interface OwnedEntity {

    /** Returns the owner of this entity, or {@code null} if unassigned. */
    User getUser();
}
