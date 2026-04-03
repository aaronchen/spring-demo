package cc.desuka.demo.event;

public record TaskChangeEvent(String action, long taskId, long projectId, long userId) {
    public static final String ACTION_CREATED = "created";
    public static final String ACTION_UPDATED = "updated";
    public static final String ACTION_DELETED = "deleted";
}
