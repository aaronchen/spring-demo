package cc.desuka.demo.dto;

import java.time.LocalDateTime;

public record PinnedItemResponse(
        Long id,
        String entityType,
        String entityId,
        String entityTitle,
        String href,
        LocalDateTime pinnedAt,
        boolean pinned,
        boolean titleOnly,
        boolean deleted) {

    /** Convenience constructor for normal list responses (no push flags). */
    public PinnedItemResponse(
            Long id,
            String entityType,
            String entityId,
            String entityTitle,
            String href,
            LocalDateTime pinnedAt) {
        this(id, entityType, entityId, entityTitle, href, pinnedAt, false, false, false);
    }
}
