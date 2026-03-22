package cc.desuka.demo.controller.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.User;
import cc.desuka.demo.presence.PresenceService;
import cc.desuka.demo.security.CustomUserDetails;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PresenceApiControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PresenceService presenceService;

    private CustomUserDetails regularDetails;

    @BeforeEach
    void setUp() {
        User regularUser = new User("Bob", "bob@example.com", "password", Role.USER);
        regularUser.setId(2L);
        regularDetails = new CustomUserDetails(regularUser);
    }

    @Test
    void getPresence_returnsUsersAndCount() throws Exception {
        when(presenceService.getOnlineUsers()).thenReturn(List.of("Alice", "Bob"));
        when(presenceService.getOnlineCount()).thenReturn(2);

        mockMvc.perform(get("/api/presence").with(user(regularDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0]").value("Alice"))
                .andExpect(jsonPath("$.users[1]").value("Bob"))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void getPresence_emptyList() throws Exception {
        when(presenceService.getOnlineUsers()).thenReturn(List.of());
        when(presenceService.getOnlineCount()).thenReturn(0);

        mockMvc.perform(get("/api/presence").with(user(regularDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.users").isEmpty());
    }
}
