package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.dto.CommentChangeEvent;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.NotificationType;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.CommentRepository;
import cc.desuka.demo.repository.TaskRepository;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.util.MentionUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageSource messageSource;
    private final SimpMessagingTemplate messagingTemplate;

    public CommentService(CommentRepository commentRepository, TaskRepository taskRepository,
                          UserService userService, NotificationService notificationService,
                          ApplicationEventPublisher eventPublisher, MessageSource messageSource,
                          SimpMessagingTemplate messagingTemplate) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.userService = userService;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
        this.messageSource = messageSource;
        this.messagingTemplate = messagingTemplate;
    }

    public Comment getCommentById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Comment.class, id));
    }

    public List<Comment> getCommentsByTaskId(Long taskId) {
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    public long countByUserId(Long userId) {
        return commentRepository.countByUserId(userId);
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

        // Notify task owner, previous commenters, and previously-mentioned users
        // (excluding the commenter themselves). Being @mentioned subscribes you
        // to the conversation — you'll see all future comments on that task.
        String message = messageSource.getMessage("notification.comment.added",
                new Object[]{user.getName(), task.getTitle()}, Locale.getDefault());
        String link = "/tasks/" + taskId;

        Set<Long> notifiedIds = new HashSet<>();
        notifiedIds.add(userId); // Don't notify self

        User taskOwner = task.getUser();
        if (taskOwner != null && notifiedIds.add(taskOwner.getId())) {
            notificationService.create(taskOwner, user, NotificationType.COMMENT_ADDED, message, link);
        }
        for (User commenter : commentRepository.findDistinctUsersByTaskId(taskId)) {
            if (notifiedIds.add(commenter.getId())) {
                notificationService.create(commenter, user, NotificationType.COMMENT_ADDED, message, link);
            }
        }
        for (Long previouslyMentionedId : findPreviouslyMentionedUserIds(taskId)) {
            if (notifiedIds.add(previouslyMentionedId)) {
                User mentioned = userService.findUserById(previouslyMentionedId);
                if (mentioned != null) {
                    notificationService.create(mentioned, user, NotificationType.COMMENT_ADDED, message, link);
                }
            }
        }

        // Notify @mentioned users in this comment (who haven't already been notified)
        List<Long> mentionedIds = MentionUtils.extractMentionedUserIds(text);
        if (!mentionedIds.isEmpty()) {
            String mentionMessage = messageSource.getMessage("notification.comment.mentioned",
                    new Object[]{user.getName(), task.getTitle()}, Locale.getDefault());
            for (Long mentionedId : mentionedIds) {
                if (notifiedIds.add(mentionedId)) {
                    User mentioned = userService.findUserById(mentionedId);
                    if (mentioned != null) {
                        notificationService.create(mentioned, user, NotificationType.COMMENT_MENTIONED,
                                mentionMessage, link);
                    }
                }
            }
        }
        broadcastCommentChange("created", taskId, saved.getId());
        return saved;
    }

    public void deleteComment(Long id) {
        Comment comment = getCommentById(id);
        Long taskId = comment.getTask().getId();
        String snapshot = AuditDetails.toJson(comment.toAuditSnapshot());
        commentRepository.delete(comment);
        eventPublisher.publishEvent(new AuditEvent(
                AuditEvent.COMMENT_DELETED, Comment.class, id,
                SecurityUtils.getCurrentPrincipal(),
                snapshot));
        broadcastCommentChange("deleted", taskId, id);
    }

    /**
     * Collect all user IDs @mentioned in previous comments on a task.
     * Mentions are stored as encoded tokens in comment text — parsed at read time.
     */
    private Set<Long> findPreviouslyMentionedUserIds(Long taskId) {
        Set<Long> ids = new HashSet<>();
        for (String text : commentRepository.findCommentTextsByTaskId(taskId)) {
            ids.addAll(MentionUtils.extractMentionedUserIds(text));
        }
        return ids;
    }

    private void broadcastCommentChange(String action, Long taskId, Long commentId) {
        User current = SecurityUtils.getCurrentUser();
        long actorId = current != null ? current.getId() : 0L;
        messagingTemplate.convertAndSend(
                "/topic/tasks/" + taskId + "/comments",
                new CommentChangeEvent(action, taskId, commentId, actorId));
    }
}
