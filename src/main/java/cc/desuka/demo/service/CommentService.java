package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.NotificationType;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.CommentRepository;
import cc.desuka.demo.repository.TaskRepository;
import cc.desuka.demo.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageSource messageSource;

    public CommentService(CommentRepository commentRepository, TaskRepository taskRepository,
                          UserService userService, NotificationService notificationService,
                          ApplicationEventPublisher eventPublisher, MessageSource messageSource) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.userService = userService;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
        this.messageSource = messageSource;
    }

    public Comment getCommentById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Comment.class, id));
    }

    public List<Comment> getCommentsByTaskId(Long taskId) {
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    public void deleteByTaskId(Long taskId) {
        commentRepository.deleteByTaskId(taskId);
    }

    public Comment createComment(String text, Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException(Task.class, taskId));
        User user = userService.getUserById(userId);

        Comment comment = new Comment();
        comment.setText(text);
        comment.setTask(task);
        comment.setUser(user);

        Comment saved = commentRepository.save(comment);
        eventPublisher.publishEvent(new AuditEvent(
                AuditEvent.COMMENT_CREATED, Comment.class, saved.getId(),
                SecurityUtils.getCurrentPrincipal(),
                AuditDetails.toJson(saved.toAuditSnapshot())));

        // Notify task owner about new comment (unless commenter is the owner)
        User taskOwner = task.getUser();
        if (taskOwner != null && !taskOwner.getId().equals(userId)) {
            String message = messageSource.getMessage("notification.comment.added",
                    new Object[]{user.getName(), task.getTitle()}, Locale.getDefault());
            notificationService.create(taskOwner, user, NotificationType.COMMENT_ADDED,
                    message, "/tasks/" + taskId + "/edit");
        }
        return saved;
    }

    public void deleteComment(Long id) {
        Comment comment = getCommentById(id);
        String snapshot = AuditDetails.toJson(comment.toAuditSnapshot());
        commentRepository.delete(comment);
        eventPublisher.publishEvent(new AuditEvent(
                AuditEvent.COMMENT_DELETED, Comment.class, id,
                SecurityUtils.getCurrentPrincipal(),
                snapshot));
    }
}
