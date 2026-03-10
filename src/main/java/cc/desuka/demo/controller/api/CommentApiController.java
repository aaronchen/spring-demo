package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.CommentResponse;
import cc.desuka.demo.mapper.CommentMapper;
import cc.desuka.demo.model.Comment;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.OwnershipGuard;
import cc.desuka.demo.service.CommentService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
public class CommentApiController {

    private final CommentService commentService;
    private final CommentMapper commentMapper;
    private final OwnershipGuard ownershipGuard;

    public CommentApiController(CommentService commentService, CommentMapper commentMapper,
                                OwnershipGuard ownershipGuard) {
        this.commentService = commentService;
        this.commentMapper = commentMapper;
        this.ownershipGuard = ownershipGuard;
    }

    // GET /api/tasks/1/comments
    @GetMapping
    public List<CommentResponse> getComments(@PathVariable Long taskId) {
        return commentMapper.toResponseList(commentService.getCommentsByTaskId(taskId));
    }

    // POST /api/tasks/1/comments
    // Any authenticated user may comment on any task.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(@PathVariable Long taskId,
                                         @RequestBody Map<String, String> body,
                                         @AuthenticationPrincipal CustomUserDetails currentDetails) {
        String text = body.get("text");
        return commentMapper.toResponse(
                commentService.createComment(text, taskId, currentDetails.getUser().getId()));
    }

    // DELETE /api/tasks/1/comments/5
    // Comment owner or admin only.
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long taskId, @PathVariable Long commentId,
                              @AuthenticationPrincipal CustomUserDetails currentDetails) {
        Comment comment = commentService.getCommentById(commentId);
        ownershipGuard.requireAccess(comment, currentDetails);
        commentService.deleteComment(commentId);
    }
}
