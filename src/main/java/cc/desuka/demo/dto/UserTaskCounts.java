package cc.desuka.demo.dto;

/** Aggregated task counts for a single user, used by the dashboard. */
public record UserTaskCounts(
        long open, long inProgress, long inReview, long completed, long overdue) {

    public long total() {
        return open + inProgress + inReview + completed;
    }
}
