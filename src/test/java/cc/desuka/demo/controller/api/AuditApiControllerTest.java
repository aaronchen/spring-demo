package cc.desuka.demo.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cc.desuka.demo.audit.AuditLogService;
import cc.desuka.demo.model.AuditLog;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.User;
import cc.desuka.demo.security.CustomUserDetails;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditApiControllerTest {

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuditLogService auditLogService;

    private CustomUserDetails adminDetails;
    private CustomUserDetails regularDetails;

    @BeforeEach
    void setUp() {
        User adminUser = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        adminUser.setId(ID_1);
        adminDetails = new CustomUserDetails(adminUser);

        User regularUser = new User("Bob", "bob@example.com", "password", Role.USER);
        regularUser.setId(ID_2);
        regularDetails = new CustomUserDetails(regularUser);
    }

    @Test
    void getAuditLogs_admin_returnsPage() throws Exception {
        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setAction("TASK_CREATED");
        log.setPrincipal("alice@example.com");
        log.setTimestamp(Instant.now());
        when(auditLogService.getAuditPage(any()))
                .thenReturn(
                        new PageImpl<>(
                                List.of(log),
                                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "timestamp")),
                                1));

        mockMvc.perform(get("/api/audit").with(user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("TASK_CREATED"));
    }

    @Test
    void getAuditLogs_regularUser_returns403() throws Exception {
        mockMvc.perform(get("/api/audit").with(user(regularDetails)))
                .andExpect(status().isForbidden());
    }
}
