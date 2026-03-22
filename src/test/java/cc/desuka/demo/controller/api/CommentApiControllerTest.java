package cc.desuka.demo.controller.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cc.desuka.demo.dto.CommentResponse;
import cc.desuka.demo.dto.UserResponse;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.mapper.CommentMapper;
import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.User;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.OwnershipGuard;
import cc.desuka.demo.security.ProjectAccessGuard;
import cc.desuka.demo.service.CommentService;
import cc.desuka.demo.service.TaskService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentApiControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CommentService commentService;
    @MockitoBean private CommentMapper commentMapper;
    @MockitoBean private OwnershipGuard ownershipGuard;
    @MockitoBean private ProjectAccessGuard projectAccessGuard;
    @MockitoBean private TaskService taskService;

    private CustomUserDetails regularDetails;
    private CustomUserDetails adminDetails;
    private Comment comment;
    private CommentResponse commentResponse;

    @BeforeEach
    void setUp() {
        User regularUser = new User("Bob", "bob@example.com", "password", Role.USER);
        regularUser.setId(2L);
        User adminUser = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        adminUser.setId(1L);
        regularDetails = new CustomUserDetails(regularUser);
        adminDetails = new CustomUserDetails(adminUser);

        Project project = new Project();
        project.setId(1L);
        project.setName("Test Project");

        Task task = new Task("Test Task", "Description");
        task.setId(1L);
        task.setProject(project);

        when(taskService.getTaskById(1L)).thenReturn(task);

        comment = new Comment();
        comment.setId(1L);
        comment.setText("A comment");
        comment.setTask(task);
        comment.setUser(regularUser);

        commentResponse = new CommentResponse();
        commentResponse.setId(1L);
        commentResponse.setText("A comment");
        commentResponse.setTaskId(1L);
        commentResponse.setCreatedAt(LocalDateTime.now());
        UserResponse userResponse = new UserResponse();
        userResponse.setId(2L);
        userResponse.setName("Bob");
        commentResponse.setUser(userResponse);
    }

    // ── GET /api/tasks/{taskId}/comments ─────────────────────────────────

    @Test
    void getComments_returnsJsonList() throws Exception {
        when(commentService.getCommentsByTaskId(1L)).thenReturn(List.of(comment));
        when(commentMapper.toResponseList(anyList())).thenReturn(List.of(commentResponse));

        mockMvc.perform(get("/api/tasks/1/comments").with(user(regularDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].text").value("A comment"))
                .andExpect(jsonPath("$[0].taskId").value(1));
    }

    @Test
    void getComments_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/tasks/1/comments")).andExpect(status().isUnauthorized());
    }

    // ── POST /api/tasks/{taskId}/comments ────────────────────────────────

    @Test
    void createComment_validRequest_returns201() throws Exception {
        when(commentService.createComment(eq("New comment"), eq(1L), eq(2L))).thenReturn(comment);
        when(commentMapper.toResponse(any(Comment.class))).thenReturn(commentResponse);

        String body = objectMapper.writeValueAsString(Map.of("text", "New comment"));

        mockMvc.perform(
                        post("/api/tasks/1/comments")
                                .with(user(regularDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(commentService).createComment("New comment", 1L, 2L);
    }

    @Test
    void createComment_anyUserCanComment() throws Exception {
        // Even non-owners can comment on any task
        when(commentService.createComment(anyString(), eq(1L), eq(1L))).thenReturn(comment);
        when(commentMapper.toResponse(any(Comment.class))).thenReturn(commentResponse);

        String body = objectMapper.writeValueAsString(Map.of("text", "Admin comment"));

        mockMvc.perform(
                        post("/api/tasks/1/comments")
                                .with(user(adminDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated());
    }

    // ── DELETE /api/tasks/{taskId}/comments/{commentId} ──────────────────

    @Test
    void deleteComment_owner_returns204() throws Exception {
        when(commentService.getCommentById(1L)).thenReturn(comment);

        mockMvc.perform(delete("/api/tasks/1/comments/1").with(user(regularDetails)))
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(1L);
    }

    @Test
    void deleteComment_admin_returns204() throws Exception {
        when(commentService.getCommentById(1L)).thenReturn(comment);

        mockMvc.perform(delete("/api/tasks/1/comments/1").with(user(adminDetails)))
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(1L);
    }

    @Test
    void deleteComment_notOwnerNotAdmin_returns403() throws Exception {
        when(commentService.getCommentById(1L)).thenReturn(comment);
        doThrow(new AccessDeniedException("Access denied"))
                .when(ownershipGuard)
                .requireAccess(any(Comment.class), any(CustomUserDetails.class));

        mockMvc.perform(delete("/api/tasks/1/comments/1").with(user(regularDetails)))
                .andExpect(status().isForbidden());

        verify(commentService, never()).deleteComment(any());
    }

    @Test
    void deleteComment_notFound_returns404() throws Exception {
        when(commentService.getCommentById(99L))
                .thenThrow(new EntityNotFoundException(Comment.class, 99L));

        mockMvc.perform(delete("/api/tasks/1/comments/99").with(user(regularDetails)))
                .andExpect(status().isNotFound());
    }
}
