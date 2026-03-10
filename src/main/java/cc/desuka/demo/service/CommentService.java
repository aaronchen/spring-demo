package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.CommentRepository;
import cc.desuka.demo.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskService taskService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public CommentService(CommentRepository commentRepository, TaskService taskService,
                          UserService userService, ApplicationEventPublisher eventPublisher) {
        this.commentRepository = commentRepository;
        this.taskService = taskService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    public Comment getCommentById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Comment.class, id));
    }

    public List<Comment> getCommentsByTaskId(Long taskId) {
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    public Comment createComment(String text, Long taskId, Long userId) {
        Task task = taskService.getTaskById(taskId);
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
