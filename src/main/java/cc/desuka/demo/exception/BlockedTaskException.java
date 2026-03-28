package cc.desuka.demo.exception;

import java.util.List;

/** Thrown when a status transition is blocked because the task has unresolved dependencies. */
public class BlockedTaskException extends RuntimeException {

    private final List<String> blockerNames;

    public BlockedTaskException(String message, List<String> blockerNames) {
        super(message);
        this.blockerNames = blockerNames;
    }

    public List<String> getBlockerNames() {
        return blockerNames;
    }
}
