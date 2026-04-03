package cc.desuka.demo.exception;

/**
 * Thrown when a requested entity (Task, User, Tag, etc.) cannot be found by ID.
 *
 * <p>Handled by:
 *
 * <ul>
 *   <li>{@link GlobalExceptionHandler} → 404 JSON response for REST API requests
 *   <li>{@link WebExceptionHandler} → 404 Thymeleaf error page for web UI requests
 * </ul>
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(Class<?> entityType, Object id) {
        super(entityType.getSimpleName() + " not found with id: " + id);
    }
}
