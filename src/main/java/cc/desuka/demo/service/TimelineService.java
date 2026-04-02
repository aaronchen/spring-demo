package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditLogService;
import cc.desuka.demo.dto.TimelineEntry;
import cc.desuka.demo.model.AuditLog;
import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.User;
import cc.desuka.demo.security.AuthExpressions;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TimelineService {

    private final CommentService commentService;
    private final AuditLogService auditLogService;

    public TimelineService(CommentService commentService, AuditLogService auditLogService) {
        this.commentService = commentService;
        this.auditLogService = auditLogService;
    }

    public List<TimelineEntry> getTimeline(Long taskId, User currentUser) {
        List<Comment> comments = commentService.getCommentsByTaskId(taskId);
        List<AuditLog> auditEntries = auditLogService.getEntityHistory(Task.class, taskId);

        List<TimelineEntry> timeline = new ArrayList<>();

        for (Comment c : comments) {
            boolean canDelete =
                    currentUser != null
                            && (AuthExpressions.isAdmin(currentUser)
                                    || (c.getUser() != null
                                            && c.getUser().getId().equals(currentUser.getId())));
            timeline.add(
                    new TimelineEntry(
                            TimelineEntry.TYPE_COMMENT,
                            c.getCreatedAt(),
                            c.getId(),
                            c.getText(),
                            c.getUser() != null ? c.getUser().getName() : null,
                            c.getUser() != null ? c.getUser().getId() : null,
                            canDelete,
                            null,
                            null,
                            null));
        }

        for (AuditLog a : auditEntries) {
            LocalDateTime ldt = LocalDateTime.ofInstant(a.getTimestamp(), ZoneId.systemDefault());
            timeline.add(
                    new TimelineEntry(
                            TimelineEntry.TYPE_AUDIT,
                            ldt,
                            null,
                            null,
                            null,
                            null,
                            false,
                            a.getAction(),
                            a.getPrincipal(),
                            a.getDetailsMap()));
        }

        timeline.sort(Comparator.comparing(TimelineEntry::timestamp).reversed());
        return timeline;
    }
}
