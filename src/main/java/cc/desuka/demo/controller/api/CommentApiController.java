package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.CommentRequest;
import cc.desuka.demo.dto.CommentResponse;
import cc.desuka.demo.mapper.CommentMapper;
import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.OwnershipGuard;
import cc.desuka.demo.security.ProjectAccessGuard;
import cc.desuka.demo.service.CommentQueryService;
import cc.desuka.demo.service.CommentService;
import cc.desuka.demo.service.TaskQueryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
public class CommentApiController {

    private final CommentQueryService commentQueryService;
    private final CommentService commentService;
    private final CommentMapper commentMapper;
    private final OwnershipGuard ownershipGuard;
    private final ProjectAccessGuard projectAccessGuard;
    private final TaskQueryService taskQueryService;

    public CommentApiController(
            CommentQueryService commentQueryService,
            CommentService commentService,
            CommentMapper commentMapper,
            OwnershipGuard ownershipGuard,
            ProjectAccessGuard projectAccessGuard,
            TaskQueryService taskQueryService) {
        this.commentQueryService = commentQueryService;
        this.commentService = commentService;
        this.commentMapper = commentMapper;
        this.ownershipGuard = ownershipGuard;
        this.projectAccessGuard = projectAccessGuard;
        this.taskQueryService = taskQueryService;
    }

    // GET /api/tasks/1/comments
    // Project members only.
    @GetMapping
    public List<CommentResponse> getComments(
            @PathVariable UUID taskId, @AuthenticationPrincipal CustomUserDetails currentDetails) {
        Task task = taskQueryService.getTaskById(taskId);
        projectAccessGuard.requireViewAccess(task.getProject().getId(), currentDetails);
        return commentMapper.toResponseList(commentQueryService.getCommentsByTaskId(taskId));
    }

    // POST /api/tasks/1/comments
    // Project members only.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(
            @PathVariable UUID taskId,
            @Valid @RequestBody CommentRequest commentRequest,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        Task task = taskQueryService.getTaskById(taskId);
        projectAccessGuard.requireViewAccess(task.getProject().getId(), currentDetails);
        return commentMapper.toResponse(
                commentService.createComment(
                        commentRequest.getText(), taskId, currentDetails.getUser().getId()));
    }

    // DELETE /api/tasks/1/comments/5
    // Comment owner or admin only.
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable UUID taskId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        Task task = taskQueryService.getTaskById(taskId);
        projectAccessGuard.requireViewAccess(task.getProject().getId(), currentDetails);
        Comment comment = commentQueryService.getCommentById(commentId);
        ownershipGuard.requireAccess(comment, currentDetails);
        commentService.deleteComment(commentId);
    }
}
