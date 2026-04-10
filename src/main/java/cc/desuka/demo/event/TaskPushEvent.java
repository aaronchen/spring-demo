package cc.desuka.demo.event;

import java.util.UUID;

public record TaskPushEvent(String action, UUID taskId, UUID projectId, UUID userId) {
    public static final String ACTION_CREATED = "created";
    public static final String ACTION_UPDATED = "updated";
    public static final String ACTION_DELETED = "deleted";
}
