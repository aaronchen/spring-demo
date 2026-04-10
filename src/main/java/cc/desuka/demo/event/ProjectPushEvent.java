package cc.desuka.demo.event;

import java.util.UUID;

public record ProjectPushEvent(String action, UUID projectId, UUID userId) {
    public static final String ACTION_UPDATED = "updated";
    public static final String ACTION_ARCHIVED = "archived";
    public static final String ACTION_UNARCHIVED = "unarchived";
}
