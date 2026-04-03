package cc.desuka.demo.exception;

public class StaleDataException extends RuntimeException {

    public StaleDataException(Class<?> entityType, Object id) {
        super(entityType.getSimpleName() + " with id " + id + " was modified by another user");
    }
}
