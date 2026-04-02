package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.event.CommentAddedEvent;
import cc.desuka.demo.event.CommentChangeEvent;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.CommentRepository;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.util.MentionUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskQueryService taskQueryService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public CommentService(
            CommentRepository commentRepository,
            TaskQueryService taskQueryService,
            UserService userService,
            ApplicationEventPublisher eventPublisher) {
        this.commentRepository = commentRepository;
        this.taskQueryService = taskQueryService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    public Comment getCommentById(Long id) {
        return commentRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Comment.class, id));
    }

    public List<Comment> getCommentsByTaskId(Long taskId) {
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    public void deleteByTaskId(Long taskId) {
        commentRepository.deleteByTaskId(taskId);
    }

    public Comment createComment(String text, Long taskId, Long userId) {
        Task task = taskQueryService.getTaskById(taskId);
        User user = userService.getUserById(userId);

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
        Comment comment = getCommentById(id);
        Long taskId = comment.getTask().getId();
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
        long actorId = current != null ? current.getId() : 0L;
        eventPublisher.publishEvent(new CommentChangeEvent("deleted", taskId, id, actorId));
    }

    /**
     * Returns the set of user IDs "subscribed" to a task via comments or @mentions. Includes all
     * users who have commented and all users @mentioned in any comment.
     */
    public Set<Long> getSubscriberIds(Long taskId) {
        Set<Long> ids = new HashSet<>(getCommenterIds(taskId));
        ids.addAll(getPreviouslyMentionedUserIds(taskId));
        return ids;
    }

    /** Returns user IDs of all distinct commenters on a task. */
    public Set<Long> getCommenterIds(Long taskId) {
        Set<Long> ids = new HashSet<>();
        for (User commenter : commentRepository.findDistinctUsersByTaskId(taskId)) {
            ids.add(commenter.getId());
        }
        return ids;
    }

    /**
     * Collect all user IDs @mentioned in previous comments on a task. Mentions are stored as
     * encoded tokens in comment text — parsed at read time.
     */
    public Set<Long> getPreviouslyMentionedUserIds(Long taskId) {
        Set<Long> ids = new HashSet<>();
        for (String text : commentRepository.findCommentTextsByTaskId(taskId)) {
            ids.addAll(MentionUtils.extractMentionedUserIds(text));
        }
        return ids;
    }
}
