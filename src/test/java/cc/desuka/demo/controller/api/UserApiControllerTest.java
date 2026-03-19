package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.UserResponse;
import cc.desuka.demo.mapper.UserMapper;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.User;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.service.UserService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserApiControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserService userService;
    @MockitoBean private UserMapper userMapper;

    private CustomUserDetails regularDetails;
    private CustomUserDetails adminDetails;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        User regularUser = new User("Bob", "bob@example.com", "password", Role.USER);
        regularUser.setId(2L);
        User adminUser = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        adminUser.setId(1L);
        regularDetails = new CustomUserDetails(regularUser);
        adminDetails = new CustomUserDetails(adminUser);

        userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setName("Alice");
        userResponse.setEmail("alice@example.com");
    }

    // ── GET /api/users ───────────────────────────────────────────────────

    @Test
    void getAllUsers_returnsJsonList() throws Exception {
        when(userService.searchEnabledUsers(null)).thenReturn(List.of());
        when(userMapper.toResponseList(anyList())).thenReturn(List.of(userResponse));

        mockMvc.perform(get("/api/users").with(user(regularDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alice"));
    }

    @Test
    void getAllUsers_withQuery_passesQueryToService() throws Exception {
        when(userService.searchEnabledUsers("ali")).thenReturn(List.of());
        when(userMapper.toResponseList(anyList())).thenReturn(List.of(userResponse));

        mockMvc.perform(get("/api/users").param("q", "ali").with(user(regularDetails)))
                .andExpect(status().isOk());

        verify(userService).searchEnabledUsers("ali");
    }

    // ── GET /api/users/{id} ──────────────────────────────────────────────

    @Test
    void getUserById_returnsJson() throws Exception {
        User alice = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        alice.setId(1L);
        when(userService.getUserById(1L)).thenReturn(alice);
        when(userMapper.toResponse(alice)).thenReturn(userResponse);

        mockMvc.perform(get("/api/users/1").with(user(regularDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    // ── POST /api/users ──────────────────────────────────────────────────

    @Test
    void createUser_admin_returns201() throws Exception {
        User newUser = new User("Charlie", "charlie@example.com", null, Role.USER);
        newUser.setId(3L);
        UserResponse newResponse = new UserResponse();
        newResponse.setId(3L);
        newResponse.setName("Charlie");
        when(userMapper.toEntity(any())).thenReturn(newUser);
        when(userService.createUser(any(User.class))).thenReturn(newUser);
        when(userMapper.toResponse(any(User.class))).thenReturn(newResponse);

        String body = objectMapper.writeValueAsString(
                Map.of("name", "Charlie", "email", "charlie@example.com"));

        mockMvc.perform(post("/api/users")
                        .with(user(adminDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Charlie"));
    }

    @Test
    void createUser_regularUser_returns403() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("name", "Charlie", "email", "charlie@example.com"));

        mockMvc.perform(post("/api/users")
                        .with(user(regularDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/users/{id} ───────────────────────────────────────────

    @Test
    void deleteUser_admin_returns204() throws Exception {
        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(() -> SecurityUtils.isCurrentUser(2L)).thenReturn(false);

            mockMvc.perform(delete("/api/users/2").with(user(adminDetails)))
                    .andExpect(status().isNoContent());

            verify(userService).deleteUser(2L);
        }
    }

    @Test
    void deleteUser_self_returns400() throws Exception {
        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(() -> SecurityUtils.isCurrentUser(1L)).thenReturn(true);

            mockMvc.perform(delete("/api/users/1").with(user(adminDetails)))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).deleteUser(any());
        }
    }

    @Test
    void deleteUser_regularUser_returns403() throws Exception {
        mockMvc.perform(delete("/api/users/2").with(user(regularDetails)))
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteUser(any());
    }
}
