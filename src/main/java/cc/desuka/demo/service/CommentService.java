package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.event.CommentAddedEvent;
import cc.desuka.demo.event.CommentChangeEvent;
import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.CommentRepository;
import cc.desuka.demo.security.SecurityUtils;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comment write operations (create, delete). Counterpart to {@link CommentQueryService} (reads).
 */
@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentQueryService commentQueryService;
    private final TaskQueryService taskQueryService;
    private final UserQueryService userQueryService;
    private final ApplicationEventPublisher eventPublisher;

    public CommentService(
            CommentRepository commentRepository,
            CommentQueryService commentQueryService,
            TaskQueryService taskQueryService,
            UserQueryService userQueryService,
            ApplicationEventPublisher eventPublisher) {
        this.commentRepository = commentRepository;
        this.commentQueryService = commentQueryService;
        this.taskQueryService = taskQueryService;
        this.userQueryService = userQueryService;
        this.eventPublisher = eventPublisher;
    }

    public Comment createComment(String text, UUID taskId, UUID userId) {
        Task task = taskQueryService.getTaskById(taskId);
        User user = userQueryService.getUserById(userId);

        Comment comment = new Comment();
        comment.setText(text);
        comment.setTask(task);
        comment.setUser(user);

        Comment saved = commentRepository.save(comment);
        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.COMMENT_CREATED,
                        Comment.class,
                        saved.getId(),
                        SecurityUtils.getCurrentPrincipal(),
                        AuditDetails.toJson(saved.toAuditSnapshot())));
        eventPublisher.publishEvent(new CommentAddedEvent(saved, task, user));
        eventPublisher.publishEvent(
                new CommentChangeEvent("created", taskId, saved.getId(), user.getId()));
        return saved;
    }

    public void deleteComment(Long id) {
        Comment comment = commentQueryService.getCommentById(id);
        UUID taskId = comment.getTask().getId();
        String snapshot = AuditDetails.toJson(comment.toAuditSnapshot());
        commentRepository.delete(comment);
        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.COMMENT_DELETED,
                        Comment.class,
                        id,
                        SecurityUtils.getCurrentPrincipal(),
                        snapshot));
        User current = SecurityUtils.getCurrentUser();
        UUID actorId = current != null ? current.getId() : null;
        eventPublisher.publishEvent(new CommentChangeEvent("deleted", taskId, id, actorId));
    }

    public void deleteByTaskId(UUID taskId) {
        commentRepository.deleteByTaskId(taskId);
    }
}
