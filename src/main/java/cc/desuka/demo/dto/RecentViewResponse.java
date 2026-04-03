package cc.desuka.demo.dto;

import java.time.LocalDateTime;

public record RecentViewResponse(
        String entityType,
        String entityId,
        String entityTitle,
        String href,
        LocalDateTime viewedAt,
        boolean titleOnly) {}
