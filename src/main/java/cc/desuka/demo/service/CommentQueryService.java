package cc.desuka.demo.service;

import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.CommentRepository;
import cc.desuka.demo.util.MentionUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only comment lookups. Counterpart to {@link CommentService} (writes). */
@Service
@Transactional(readOnly = true)
public class CommentQueryService {

    private final CommentRepository commentRepository;

    public CommentQueryService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    // ── Single-entity lookups ────────────────────────────────────────────

    public Comment getCommentById(Long id) {
        return commentRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Comment.class, id));
    }

    public List<Comment> getCommentsByTaskId(UUID taskId) {
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    // ── Counts ───────────────────────────────────────────────────────────

    public long countByUserId(UUID userId) {
        return commentRepository.countByUserId(userId);
    }

    // ── Subscriber queries ───────────────────────────────────────────────

    /**
     * Returns the set of user IDs "subscribed" to a task via comments or @mentions. Includes all
     * users who have commented and all users @mentioned in any comment.
     */
    public Set<UUID> getSubscriberIds(UUID taskId) {
        Set<UUID> ids = new HashSet<>(getCommenterIds(taskId));
        ids.addAll(getPreviouslyMentionedUserIds(taskId));
        return ids;
    }

    /** Returns user IDs of all distinct commenters on a task. */
    public Set<UUID> getCommenterIds(UUID taskId) {
        Set<UUID> ids = new HashSet<>();
        for (User commenter : commentRepository.findDistinctUsersByTaskId(taskId)) {
            ids.add(commenter.getId());
        }
        return ids;
    }

    /**
     * Collect all user IDs @mentioned in previous comments on a task. Mentions are stored as
     * encoded tokens in comment text — parsed at read time.
     */
    public Set<UUID> getPreviouslyMentionedUserIds(UUID taskId) {
        Set<UUID> ids = new HashSet<>();
        for (String text : commentRepository.findCommentTextsByTaskId(taskId)) {
            ids.addAll(MentionUtils.extractMentionedUserIds(text));
        }
        return ids;
    }
}
