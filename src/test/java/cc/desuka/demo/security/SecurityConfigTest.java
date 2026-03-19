package cc.desuka.demo.security;

import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;

    private CustomUserDetails regularUser() {
        User user = new User("Bob", "bob@example.com", "password", Role.USER);
        user.setId(2L);
        return new CustomUserDetails(user);
    }

    private CustomUserDetails adminUser() {
        User user = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        user.setId(1L);
        return new CustomUserDetails(user);
    }

    // ── Public access ───────────────────────────────────────────────────

    @Test
    void loginPage_accessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void registerPage_accessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    void staticAssets_accessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/css/base.css"))
                .andExpect(status().isOk());
    }

    // ── Authenticated access ────────────────────────────────────────────

    @Test
    void tasksPage_requiresAuth() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void tasksPage_authenticatedUser_allowed() throws Exception {
        mockMvc.perform(get("/tasks").with(user(regularUser())))
                .andExpect(status().isOk());
    }

    @Test
    void apiTasks_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().is3xxRedirection());
    }

    // ── Admin-only access ───────────────────────────────────────────────

    @Test
    void adminPages_regularUser_returns403() throws Exception {
        mockMvc.perform(get("/admin/users").with(user(regularUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPages_adminUser_allowed() throws Exception {
        mockMvc.perform(get("/admin/users").with(user(adminUser())))
                .andExpect(status().isOk());
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
        mockMvc.perform(post("/api/tags")
                        .with(user(regularUser()))
                        .contentType("application/json")
                        .content("{\"name\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiDeleteTag_regularUser_returns403() throws Exception {
        mockMvc.perform(delete("/api/tags/1")
                        .with(user(regularUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiCreateUser_regularUser_returns403() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(user(regularUser()))
                        .contentType("application/json")
                        .content("{\"name\":\"test\",\"email\":\"test@test.com\"}"))
                .andExpect(status().isForbidden());
    }

    // ── CSRF ────────────────────────────────────────────────────────────

    @Test
    void apiEndpoints_csrfExempt() throws Exception {
        mockMvc.perform(get("/api/tasks").with(user(regularUser())))
                .andExpect(status().isOk());
    }

    @Test
    void webForms_requireCsrf() throws Exception {
        mockMvc.perform(post("/tasks")
                        .with(user(regularUser()))
                        .param("title", "Test"))
                .andExpect(status().isForbidden());
    }

    @Test
    void webForms_withCsrf_allowed() throws Exception {
        mockMvc.perform(post("/tasks")
                        .with(user(regularUser()))
                        .with(csrf())
                        .param("title", "Test"))
                .andExpect(status().is3xxRedirection());
    }
}
