package cc.desuka.demo.exception;

public class StaleDataException extends RuntimeException {

    public StaleDataException(Class<?> entityType, Long id) {
        super(entityType.getSimpleName() + " with id " + id + " was modified by another user");
    }
}
