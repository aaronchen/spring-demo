package cc.desuka.demo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired private MockMvc mockMvc;

    private CustomUserDetails regularUser() {
        User user = new User("Bob", "bob@example.com", "password", Role.USER);
        user.setId(ID_2);
        return new CustomUserDetails(user);
    }

    private CustomUserDetails adminUser() {
        User user = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        user.setId(ID_1);
        return new CustomUserDetails(user);
    }

    // ── Public access ───────────────────────────────────────────────────

    @Test
    void loginPage_accessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void registerPage_accessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    void staticAssets_accessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/css/base.css")).andExpect(status().isOk());
    }

    @Test
    void actuatorHealth_accessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void actuatorInfo_accessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
    }

    // ── Authenticated access ────────────────────────────────────────────

    @Test
    void tasksPage_requiresAuth() throws Exception {
        mockMvc.perform(get("/tasks")).andExpect(status().is3xxRedirection());
    }

    @Test
    void tasksPage_authenticatedUser_allowed() throws Exception {
        mockMvc.perform(get("/tasks").with(user(regularUser()))).andExpect(status().isOk());
    }

    @Test
    void apiTasks_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
    }

    // ── Admin-only access ───────────────────────────────────────────────

    @Test
    void adminPages_regularUser_returns403() throws Exception {
        mockMvc.perform(get("/admin/users").with(user(regularUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPages_adminUser_allowed() throws Exception {
        mockMvc.perform(get("/admin/users").with(user(adminUser()))).andExpect(status().isOk());
    }

    @Test
    void adminAudit_regularUser_returns403() throws Exception {
        mockMvc.perform(get("/admin/audit").with(user(regularUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminSettings_regularUser_returns403() throws Exception {
        mockMvc.perform(get("/admin/settings").with(user(regularUser())))
                .andExpect(status().isForbidden());
    }

    // ── API admin mutations ─────────────────────────────────────────────

    @Test
    void apiCreateTag_regularUser_returns403() throws Exception {
        mockMvc.perform(
                        post("/api/tags")
                                .with(user(regularUser()))
                                .contentType("application/json")
                                .content("{\"name\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiDeleteTag_regularUser_returns403() throws Exception {
        mockMvc.perform(delete("/api/tags/1").with(user(regularUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiCreateUser_regularUser_returns403() throws Exception {
        mockMvc.perform(
                        post("/api/users")
                                .with(user(regularUser()))
                                .contentType("application/json")
                                .content("{\"name\":\"test\",\"email\":\"test@test.com\"}"))
                .andExpect(status().isForbidden());
    }

    // ── CSRF ────────────────────────────────────────────────────────────

    @Test
    void apiEndpoints_csrfExempt() throws Exception {
        mockMvc.perform(get("/api/tasks").with(user(regularUser()))).andExpect(status().isOk());
    }

    @Test
    void webForms_requireCsrf() throws Exception {
        mockMvc.perform(post("/projects").with(user(regularUser())).param("name", "Test"))
                .andExpect(status().isForbidden());
    }

    @Test
    void webForms_withCsrf_allowed() throws Exception {
        // CSRF token present → request is not rejected as 403 (Forbidden).
        // The actual response depends on the endpoint logic; we only care that CSRF passed.
        int status =
                mockMvc.perform(
                                post("/projects")
                                        .with(user(regularUser()))
                                        .with(csrf())
                                        .param("name", "Test"))
                        .andReturn()
                        .getResponse()
                        .getStatus();
        assertThat(status).isNotEqualTo(403);
    }
}
