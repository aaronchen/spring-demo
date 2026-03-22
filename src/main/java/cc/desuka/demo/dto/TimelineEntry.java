package cc.desuka.demo.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * A single entry in the unified activity timeline. Represents either a comment or an audit log
 * event.
 */
public record TimelineEntry(
        String type, // "comment" or "audit"
        LocalDateTime timestamp,
        // Comment fields
        Long commentId,
        String commentText,
        String commentUserName,
        Long commentUserId,
        boolean canDelete,
        // Audit fields
        String auditAction,
        String auditPrincipal,
        Map<String, Object> auditDetails) {
    public static final String TYPE_COMMENT = "comment";
    public static final String TYPE_AUDIT = "audit";
}
