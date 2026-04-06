package cc.desuka.demo.dto;

import java.time.LocalDateTime;

public record RecentViewResponse(
        String entityType,
        String entityId,
        String entityTitle,
        String href,
        LocalDateTime viewedAt,
        boolean titleOnly,
        boolean deleted) {

    public RecentViewResponse(
            String entityType,
            String entityId,
            String entityTitle,
            String href,
            LocalDateTime viewedAt,
            boolean titleOnly) {
        this(entityType, entityId, entityTitle, href, viewedAt, titleOnly, false);
    }
}
