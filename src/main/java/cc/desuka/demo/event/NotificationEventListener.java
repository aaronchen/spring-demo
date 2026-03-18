package cc.desuka.demo.event;

import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.NotificationType;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.User;
import cc.desuka.demo.service.CommentService;
import cc.desuka.demo.service.NotificationService;
import cc.desuka.demo.service.UserService;
import cc.desuka.demo.util.MentionUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Centralized notification logic — listens for domain events published by
 * services and decides who gets notified. Same pattern as {@code AuditEventListener}.
 */
@Component
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final CommentService commentService;
    private final UserService userService;
    private final MessageSource messageSource;

    public NotificationEventListener(NotificationService notificationService,
                                     CommentService commentService,
                                     UserService userService,
                                     MessageSource messageSource) {
        this.notificationService = notificationService;
        this.commentService = commentService;
        this.userService = userService;
        this.messageSource = messageSource;
    }

    @EventListener
    public void onTaskAssigned(TaskAssignedEvent event) {
        Task task = event.task();
        User actor = event.actor();
        User assignee = task.getUser();

        // Don't notify if unassigned or assigning to self
        if (assignee == null || (actor != null && actor.getId().equals(assignee.getId()))) {
            return;
        }

        String message = messageSource.getMessage("notification.task.assigned",
                new Object[]{actor != null ? actor.getName() : "System", task.getTitle()},
                Locale.getDefault());
        notificationService.create(assignee, actor, NotificationType.TASK_ASSIGNED,
                message, "/tasks/" + task.getId() + "/edit");
    }

    @EventListener
    public void onTaskUpdated(TaskUpdatedEvent event) {
        Task task = event.task();
        User actor = event.actor();
        if (actor == null) return;

        String message = messageSource.getMessage("notification.task.updated",
                new Object[]{actor.getName(), task.getTitle()}, Locale.getDefault());
        String link = "/tasks/" + task.getId();

        Set<Long> notifiedIds = new HashSet<>();
        notifiedIds.add(actor.getId()); // Don't notify self

        // Notify task owner
        User owner = task.getUser();
        if (owner != null && notifiedIds.add(owner.getId())) {
            notificationService.create(owner, actor, NotificationType.TASK_UPDATED, message, link);
        }

        // Notify commenters and @mentioned users
        for (Long subscriberId : commentService.getSubscriberIds(task.getId())) {
            if (notifiedIds.add(subscriberId)) {
                User subscriber = userService.findUserById(subscriberId);
                if (subscriber != null) {
                    notificationService.create(subscriber, actor, NotificationType.TASK_UPDATED, message, link);
                }
            }
        }
    }

    @EventListener
    public void onCommentAdded(CommentAddedEvent event) {
        Comment comment = event.comment();
        Task task = event.task();
        User actor = event.actor();

        String message = messageSource.getMessage("notification.comment.added",
                new Object[]{actor.getName(), task.getTitle()}, Locale.getDefault());
        String link = "/tasks/" + task.getId();

        Set<Long> notifiedIds = new HashSet<>();
        notifiedIds.add(actor.getId()); // Don't notify self

        // Notify task owner
        User taskOwner = task.getUser();
        if (taskOwner != null && notifiedIds.add(taskOwner.getId())) {
            notificationService.create(taskOwner, actor, NotificationType.COMMENT_ADDED, message, link);
        }

        // Notify previous commenters
        for (Long commenterId : commentService.getCommenterIds(task.getId())) {
            if (notifiedIds.add(commenterId)) {
                User commenter = userService.findUserById(commenterId);
                if (commenter != null) {
                    notificationService.create(commenter, actor, NotificationType.COMMENT_ADDED, message, link);
                }
            }
        }

        // Notify previously @mentioned users (subscribed to conversation)
        for (Long mentionedId : commentService.getPreviouslyMentionedUserIds(task.getId())) {
            if (notifiedIds.add(mentionedId)) {
                User mentioned = userService.findUserById(mentionedId);
                if (mentioned != null) {
                    notificationService.create(mentioned, actor, NotificationType.COMMENT_ADDED, message, link);
                }
            }
        }

        // Notify @mentioned users in this comment (with COMMENT_MENTIONED type)
        List<Long> mentionedInThisComment = MentionUtils.extractMentionedUserIds(comment.getText());
        if (!mentionedInThisComment.isEmpty()) {
            String mentionMessage = messageSource.getMessage("notification.comment.mentioned",
                    new Object[]{actor.getName(), task.getTitle()}, Locale.getDefault());
            for (Long mentionedId : mentionedInThisComment) {
                if (notifiedIds.add(mentionedId)) {
                    User mentioned = userService.findUserById(mentionedId);
                    if (mentioned != null) {
                        notificationService.create(mentioned, actor, NotificationType.COMMENT_MENTIONED,
                                mentionMessage, link);
                    }
                }
            }
        }
    }
}
