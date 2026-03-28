package cc.desuka.demo.exception;

/** Thrown when adding a dependency would create a circular chain. */
public class CyclicDependencyException extends RuntimeException {

    public CyclicDependencyException(String message) {
        super(message);
    }
}
