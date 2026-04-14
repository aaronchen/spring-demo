package cc.desuka.demo.event;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.NotificationType;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.User;
import cc.desuka.demo.service.CommentQueryService;
import cc.desuka.demo.service.NotificationService;
import cc.desuka.demo.service.UserQueryService;
import cc.desuka.demo.util.MentionUtils;
import cc.desuka.demo.util.Messages;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Centralized notification logic — listens for domain events published by services and decides who
 * gets notified. Same pattern as {@code AuditEventListener}.
 */
@Component
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final CommentQueryService commentQueryService;
    private final UserQueryService userQueryService;
    private final Messages messages;
    private final AppRoutesProperties appRoutes;

    public NotificationEventListener(
            NotificationService notificationService,
            CommentQueryService commentQueryService,
            UserQueryService userQueryService,
            Messages messages,
            AppRoutesProperties appRoutes) {
        this.notificationService = notificationService;
        this.commentQueryService = commentQueryService;
        this.userQueryService = userQueryService;
        this.messages = messages;
        this.appRoutes = appRoutes;
    }

    @TransactionalEventListener
    public void onTaskAssigned(TaskAssignedEvent event) {
        Task task = event.task();
        User actor = event.actor();
        User assignee = task.getUser();

        // Don't notify if unassigned or assigning to self
        if (assignee == null || (actor != null && actor.getId().equals(assignee.getId()))) {
            return;
        }

        String message =
                messages.get(
                        "notification.task.assigned",
                        actor != null ? actor.getName() : "System",
                        task.getTitle());
        notificationService.create(
                assignee,
                actor,
                NotificationType.TASK_ASSIGNED,
                message,
                appRoutes.getTaskEdit().params("taskId", task.getId()).build());
    }

    @TransactionalEventListener
    public void onTaskUpdated(TaskUpdatedEvent event) {
        Task task = event.task();
        User actor = event.actor();
        if (actor == null) return;

        String message =
                messages.get("notification.task.updated", actor.getName(), task.getTitle());
        String link = appRoutes.getTaskDetail().params("taskId", task.getId()).build();

        Set<UUID> notifiedIds = new HashSet<>();
        notifiedIds.add(actor.getId()); // Don't notify self

        // Notify task owner
        User owner = task.getUser();
        if (owner != null && notifiedIds.add(owner.getId())) {
            notificationService.create(owner, actor, NotificationType.TASK_UPDATED, message, link);
        }

        // Notify commenters and @mentioned users (batch lookup)
        Set<UUID> subscriberIds = commentQueryService.getSubscriberIds(task.getId());
        subscriberIds.removeAll(notifiedIds);
        if (!subscriberIds.isEmpty()) {
            Map<UUID, User> subscribers = userQueryService.findAllByIds(subscriberIds);
            notifiedIds.addAll(subscribers.keySet());
            for (User subscriber : subscribers.values()) {
                notificationService.create(
                        subscriber, actor, NotificationType.TASK_UPDATED, message, link);
            }
        }
    }

    @TransactionalEventListener
    public void onCommentAdded(CommentAddedEvent event) {
        Comment comment = event.comment();
        Task task = event.task();
        User actor = event.actor();

        String message =
                messages.get("notification.comment.added", actor.getName(), task.getTitle());
        String link = appRoutes.getTaskDetail().params("taskId", task.getId()).build();

        Set<UUID> notifiedIds = new HashSet<>();
        notifiedIds.add(actor.getId()); // Don't notify self

        // Notify task owner
        User taskOwner = task.getUser();
        if (taskOwner != null && notifiedIds.add(taskOwner.getId())) {
            notificationService.create(
                    taskOwner, actor, NotificationType.COMMENT_ADDED, message, link);
        }

        // Notify subscribers (commenters + previously @mentioned) — batch lookup
        Set<UUID> subscriberIds = commentQueryService.getSubscriberIds(task.getId());
        subscriberIds.removeAll(notifiedIds);
        if (!subscriberIds.isEmpty()) {
            Map<UUID, User> subscribers = userQueryService.findAllByIds(subscriberIds);
            notifiedIds.addAll(subscribers.keySet());
            for (User subscriber : subscribers.values()) {
                notificationService.create(
                        subscriber, actor, NotificationType.COMMENT_ADDED, message, link);
            }
        }

        // Notify @mentioned users in this comment (with COMMENT_MENTIONED type) — batch lookup
        List<UUID> mentionedInThisComment = MentionUtils.extractMentionedUserIds(comment.getText());
        Set<UUID> newMentionIds = new HashSet<>(mentionedInThisComment);
        newMentionIds.removeAll(notifiedIds);
        if (!newMentionIds.isEmpty()) {
            String mentionMessage =
                    messages.get(
                            "notification.comment.mentioned", actor.getName(), task.getTitle());
            Map<UUID, User> mentionedUsers = userQueryService.findAllByIds(newMentionIds);
            notifiedIds.addAll(mentionedUsers.keySet());
            for (User mentioned : mentionedUsers.values()) {
                notificationService.create(
                        mentioned, actor, NotificationType.COMMENT_MENTIONED, mentionMessage, link);
            }
        }
    }
}
