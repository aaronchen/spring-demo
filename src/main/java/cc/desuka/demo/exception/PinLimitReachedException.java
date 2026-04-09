package cc.desuka.demo.exception;

/** Thrown when a user attempts to pin an item but has reached their configured pin limit. */
public class PinLimitReachedException extends RuntimeException {

    private final long currentCount;
    private final int limit;

    public PinLimitReachedException(long currentCount, int limit) {
        super("Pin limit reached (%d/%d)".formatted(currentCount, limit));
        this.currentCount = currentCount;
        this.limit = limit;
    }

    public long getCurrentCount() {
        return currentCount;
    }

    public int getLimit() {
        return limit;
    }
}
