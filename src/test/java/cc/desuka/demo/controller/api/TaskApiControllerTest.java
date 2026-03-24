package cc.desuka.demo.controller.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cc.desuka.demo.dto.TaskResponse;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.exception.StaleDataException;
import cc.desuka.demo.mapper.TaskMapper;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.User;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.ProjectAccessGuard;
import cc.desuka.demo.service.ProjectQueryService;
import cc.desuka.demo.service.TaskQueryService;
import cc.desuka.demo.service.TaskService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskApiControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private TaskService taskService;
    @MockitoBean private TaskQueryService taskQueryService;
    @MockitoBean private ProjectQueryService projectQueryService;
    @MockitoBean private TaskMapper taskMapper;
    @MockitoBean private ProjectAccessGuard projectAccessGuard;

    private CustomUserDetails regularDetails;
    private CustomUserDetails adminDetails;
    private Task task;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        User regularUser = new User("Bob", "bob@example.com", "password", Role.USER);
        regularUser.setId(2L);
        User adminUser = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        adminUser.setId(1L);
        regularDetails = new CustomUserDetails(regularUser);
        adminDetails = new CustomUserDetails(adminUser);

        Project project = new Project("Test Project", "Desc");
        project.setId(1L);

        task = new Task("Test Task", "Description");
        task.setId(1L);
        task.setVersion(0L);
        task.setUser(regularUser);
        task.setProject(project);
        task.setStatus(TaskStatus.OPEN);
        task.setPriority(Priority.MEDIUM);

        taskResponse = new TaskResponse();
        taskResponse.setId(1L);
        taskResponse.setTitle("Test Task");
        taskResponse.setStatus(TaskStatus.OPEN);
        taskResponse.setPriority(Priority.MEDIUM);
        taskResponse.setTags(List.of());
        taskResponse.setVersion(0L);
    }

    // ── GET /api/tasks ──────────────────────────────────────────────────

    @Test
    void getTasks_returnsPaginatedResults() throws Exception {
        when(projectQueryService.getAccessibleProjectIds(2L)).thenReturn(List.of(1L));
        when(taskQueryService.searchTasks(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));
        when(taskMapper.toResponse(any(Task.class))).thenReturn(taskResponse);

        mockMvc.perform(get("/api/tasks").with(user(regularDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Test Task"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void getAllTasks_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
    }

    // ── GET /api/tasks/{id} ─────────────────────────────────────────────

    @Test
    void getTaskById_found_returnsJson() throws Exception {
        when(taskQueryService.getTaskById(1L)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        mockMvc.perform(get("/api/tasks/1").with(user(regularDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Task"));
    }

    @Test
    void getTaskById_notFound_returns404() throws Exception {
        when(taskQueryService.getTaskById(99L))
                .thenThrow(new EntityNotFoundException(Task.class, 99L));

        mockMvc.perform(get("/api/tasks/99").with(user(regularDetails)))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/tasks ─────────────────────────────────────────────────

    @Test
    void createTask_validRequest_returns201() throws Exception {
        when(projectQueryService.getProjectById(1L)).thenReturn(task.getProject());
        when(taskMapper.toEntity(any())).thenReturn(new Task("New Task", null));
        when(taskService.createTask(any(Task.class), any(), eq(2L))).thenReturn(task);
        when(taskMapper.toResponse(any(Task.class))).thenReturn(taskResponse);

        String body = objectMapper.writeValueAsString(Map.of("title", "New Task", "projectId", 1));

        mockMvc.perform(
                        post("/api/tasks")
                                .with(user(regularDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createTask_blankTitle_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("title", ""));

        mockMvc.perform(
                        post("/api/tasks")
                                .with(user(regularDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTask_adminCanAssignToOtherUser() throws Exception {
        when(projectQueryService.getProjectById(1L)).thenReturn(task.getProject());
        when(taskMapper.toEntity(any())).thenReturn(new Task("Admin Task", null));
        when(taskService.createTask(any(), any(), eq(2L))).thenReturn(task);
        when(taskMapper.toResponse(any(Task.class))).thenReturn(taskResponse);

        String body =
                objectMapper.writeValueAsString(
                        Map.of("title", "Admin Task", "userId", 2, "projectId", 1));

        mockMvc.perform(
                        post("/api/tasks")
                                .with(user(adminDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated());

        verify(taskService).createTask(any(), any(), eq(2L));
    }

    @Test
    void createTask_regularUserCannotAssignToOtherUser() throws Exception {
        when(projectQueryService.getProjectById(1L)).thenReturn(task.getProject());
        when(taskMapper.toEntity(any())).thenReturn(new Task("My Task", null));
        when(taskService.createTask(any(), any(), eq(2L))).thenReturn(task);
        when(taskMapper.toResponse(any(Task.class))).thenReturn(taskResponse);

        String body =
                objectMapper.writeValueAsString(
                        Map.of("title", "My Task", "userId", 1, "projectId", 1));

        mockMvc.perform(
                        post("/api/tasks")
                                .with(user(regularDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated());

        // Should be assigned to the caller (2), not the requested user (1)
        verify(taskService).createTask(any(), any(), eq(2L));
    }

    // ── PUT /api/tasks/{id} ─────────────────────────────────────────────

    @Test
    void updateTask_owner_succeeds() throws Exception {
        when(taskQueryService.getTaskById(1L)).thenReturn(task);
        when(taskMapper.toEntity(any())).thenReturn(new Task("Updated", null));
        when(taskService.updateTask(eq(1L), any(), any(), any(), any())).thenReturn(task);
        when(taskMapper.toResponse(any(Task.class))).thenReturn(taskResponse);

        String body = objectMapper.writeValueAsString(Map.of("title", "Updated", "version", 0));

        mockMvc.perform(
                        put("/api/tasks/1")
                                .with(user(regularDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void updateTask_notEditor_returns403() throws Exception {
        when(taskQueryService.getTaskById(1L)).thenReturn(task);
        doThrow(new AccessDeniedException("Access denied"))
                .when(projectAccessGuard)
                .requireEditAccess(eq(1L), any(CustomUserDetails.class));

        String body = objectMapper.writeValueAsString(Map.of("title", "Hacked", "version", 0));

        mockMvc.perform(
                        put("/api/tasks/1")
                                .with(user(regularDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTask_staleVersion_returns409() throws Exception {
        when(taskQueryService.getTaskById(1L)).thenReturn(task);
        when(taskMapper.toEntity(any())).thenReturn(new Task("Updated", null));
        when(taskService.updateTask(eq(1L), any(), any(), any(), any()))
                .thenThrow(new StaleDataException(Task.class, 1L));

        String body = objectMapper.writeValueAsString(Map.of("title", "Updated", "version", 999));

        mockMvc.perform(
                        put("/api/tasks/1")
                                .with(user(regularDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isConflict());
    }

    // ── DELETE /api/tasks/{id} ──────────────────────────────────────────

    @Test
    void deleteTask_owner_returns204() throws Exception {
        when(taskQueryService.getTaskById(1L)).thenReturn(task);

        mockMvc.perform(delete("/api/tasks/1").with(user(regularDetails)))
                .andExpect(status().isNoContent());

        verify(taskService).deleteTask(1L);
    }

    @Test
    void deleteTask_notCreatorNotProjectOwnerNotAdmin_returns403() throws Exception {
        // Task created by admin (userId=1), caller is regular user (userId=2)
        // Regular user is not admin, not the task creator, and not a project owner
        User adminUser = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        adminUser.setId(1L);
        task.setUser(adminUser);
        when(taskQueryService.getTaskById(1L)).thenReturn(task);
        doThrow(new AccessDeniedException("Access denied"))
                .when(projectAccessGuard)
                .requireOwnerAccess(eq(1L), any(CustomUserDetails.class));

        mockMvc.perform(delete("/api/tasks/1").with(user(regularDetails)))
                .andExpect(status().isForbidden());

        verify(taskService, never()).deleteTask(any());
    }

    // ── PATCH /api/tasks/{id}/toggle ────────────────────────────────────

    @Test
    void advanceStatus_returnsUpdatedTask() throws Exception {
        when(taskQueryService.getTaskById(1L)).thenReturn(task);
        task.setStatus(TaskStatus.IN_PROGRESS);
        when(taskService.advanceStatus(1L)).thenReturn(task);
        taskResponse.setStatus(TaskStatus.IN_PROGRESS);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        mockMvc.perform(patch("/api/tasks/1/toggle").with(user(regularDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    // ── GET /api/tasks/search ───────────────────────────────────────────

    @Test
    void searchTasks_returnsFilteredResults() throws Exception {
        when(taskQueryService.searchTasks("test")).thenReturn(List.of(task));
        when(taskMapper.toResponseList(anyList())).thenReturn(List.of(taskResponse));

        mockMvc.perform(
                        get("/api/tasks/search")
                                .param("keyword", "test")
                                .with(user(regularDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Task"));
    }
}
