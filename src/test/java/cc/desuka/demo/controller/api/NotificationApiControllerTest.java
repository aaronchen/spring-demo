package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.NotificationResponse;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.User;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationApiControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private NotificationService notificationService;

    private CustomUserDetails regularDetails;

    @BeforeEach
    void setUp() {
        User regularUser = new User("Bob", "bob@example.com", "password", Role.USER);
        regularUser.setId(2L);
        regularDetails = new CustomUserDetails(regularUser);
    }

    // ── GET /api/notifications/unread-count ───────────────────────────────

    @Test
    void getUnreadCount_returnsCountJson() throws Exception {
        when(notificationService.getUnreadCount(2L)).thenReturn(5L);

        mockMvc.perform(get("/api/notifications/unread-count").with(user(regularDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    // ── GET /api/notifications ────────────────────────────────────────────

    @Test
    void getNotifications_returnsPaginatedResults() throws Exception {
        NotificationResponse response = new NotificationResponse();
        response.setId(1L);
        response.setMessage("Test notification");
        when(notificationService.findAllForUser(eq(2L), any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/notifications").with(user(regularDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].message").value("Test notification"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getNotifications_customPageSize() throws Exception {
        when(notificationService.findAllForUser(eq(2L), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        mockMvc.perform(get("/api/notifications")
                        .param("page", "1")
                        .param("size", "5")
                        .with(user(regularDetails)))
                .andExpect(status().isOk());

        verify(notificationService).findAllForUser(eq(2L), eq(PageRequest.of(1, 5)));
    }

    // ── PATCH /api/notifications/{id}/read ────────────────────────────────

    @Test
    void markAsRead_returns204() throws Exception {
        mockMvc.perform(patch("/api/notifications/1/read").with(user(regularDetails)))
                .andExpect(status().isNoContent());

        verify(notificationService).markAsRead(1L, 2L);
    }

    // ── PATCH /api/notifications/read-all ─────────────────────────────────

    @Test
    void markAllAsRead_returns204() throws Exception {
        mockMvc.perform(patch("/api/notifications/read-all").with(user(regularDetails)))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead(2L);
    }

    // ── DELETE /api/notifications ──────────────────────────────────────────

    @Test
    void clearAll_returns204() throws Exception {
        mockMvc.perform(delete("/api/notifications").with(user(regularDetails)))
                .andExpect(status().isNoContent());

        verify(notificationService).clearAll(2L);
    }
}
