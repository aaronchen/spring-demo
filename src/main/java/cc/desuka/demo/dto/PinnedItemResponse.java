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

    /** WebSocket push: new pin added. */
    public static PinnedItemResponse pinned(
            Long id,
            String entityType,
            String entityId,
            String entityTitle,
            String href,
            LocalDateTime pinnedAt) {
        return new PinnedItemResponse(
                id, entityType, entityId, entityTitle, href, pinnedAt, true, false, false);
    }

    /** WebSocket push: title updated for an existing pin. */
    public static PinnedItemResponse titleUpdate(
            Long id,
            String entityType,
            String entityId,
            String entityTitle,
            String href,
            LocalDateTime pinnedAt) {
        return new PinnedItemResponse(
                id, entityType, entityId, entityTitle, href, pinnedAt, false, true, false);
    }

    /** WebSocket push: pin removed. */
    public static PinnedItemResponse deleted(Long id, String entityType, String entityId) {
        return new PinnedItemResponse(
                id, entityType, entityId, null, null, null, false, false, true);
    }
}
