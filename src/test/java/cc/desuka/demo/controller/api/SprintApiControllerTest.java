package cc.desuka.demo.controller.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.Sprint;
import cc.desuka.demo.model.User;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.ProjectAccessGuard;
import cc.desuka.demo.service.SprintQueryService;
import cc.desuka.demo.service.SprintService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SprintApiControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private SprintService sprintService;
    @MockitoBean private SprintQueryService sprintQueryService;
    @MockitoBean private ProjectAccessGuard projectAccessGuard;

    private CustomUserDetails regularDetails;
    private Sprint sprint;

    @BeforeEach
    void setUp() {
        User regularUser = new User("Bob", "bob@example.com", "password", Role.USER);
        regularUser.setId(2L);
        regularDetails = new CustomUserDetails(regularUser);

        sprint = new Sprint();
        sprint.setId(1L);
        sprint.setName("Sprint 1");
        sprint.setGoal("Deliver MVP");
        sprint.setStartDate(LocalDate.of(2026, 4, 1));
        sprint.setEndDate(LocalDate.of(2026, 4, 14));
    }

    // -- GET /api/projects/{projectId}/sprints ------------------------------------

    @Test
    void listSprints_returns200() throws Exception {
        doNothing()
                .when(projectAccessGuard)
                .requireViewAccess(eq(1L), any(CustomUserDetails.class));
        when(sprintQueryService.getSprintsByProject(1L)).thenReturn(List.of(sprint));

        mockMvc.perform(get("/api/projects/1/sprints").with(user(regularDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Sprint 1"))
                .andExpect(jsonPath("$[0].goal").value("Deliver MVP"));
    }

    // -- POST /api/projects/{projectId}/sprints -----------------------------------

    @Test
    void createSprint_returns201() throws Exception {
        doNothing()
                .when(projectAccessGuard)
                .requireEditAccess(eq(1L), any(CustomUserDetails.class));
        when(sprintService.createSprint(eq(1L), any(Sprint.class))).thenReturn(sprint);

        String body =
                objectMapper.writeValueAsString(
                        Map.of(
                                "name", "Sprint 1",
                                "goal", "Deliver MVP",
                                "startDate", "2026-04-01",
                                "endDate", "2026-04-14"));

        mockMvc.perform(
                        post("/api/projects/1/sprints")
                                .with(user(regularDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sprint 1"));
    }

    @Test
    void createSprint_invalidData_returns400() throws Exception {
        doNothing()
                .when(projectAccessGuard)
                .requireEditAccess(eq(1L), any(CustomUserDetails.class));

        String body = objectMapper.writeValueAsString(Map.of("goal", "No name or dates"));

        mockMvc.perform(
                        post("/api/projects/1/sprints")
                                .with(user(regularDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    // -- PUT /api/projects/{projectId}/sprints/{id} -------------------------------

    @Test
    void updateSprint_returns200() throws Exception {
        doNothing()
                .when(projectAccessGuard)
                .requireEditAccess(eq(1L), any(CustomUserDetails.class));
        sprint.setName("Updated Sprint");
        when(sprintService.updateSprint(eq(1L), any(Sprint.class))).thenReturn(sprint);

        String body =
                objectMapper.writeValueAsString(
                        Map.of(
                                "name", "Updated Sprint",
                                "goal", "Deliver MVP",
                                "startDate", "2026-04-01",
                                "endDate", "2026-04-14"));

        mockMvc.perform(
                        put("/api/projects/1/sprints/1")
                                .with(user(regularDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Sprint"));
    }

    // -- DELETE /api/projects/{projectId}/sprints/{id} ----------------------------

    @Test
    void deleteSprint_returns204() throws Exception {
        doNothing()
                .when(projectAccessGuard)
                .requireEditAccess(eq(1L), any(CustomUserDetails.class));

        mockMvc.perform(delete("/api/projects/1/sprints/1").with(user(regularDetails)))
                .andExpect(status().isNoContent());

        verify(sprintService).deleteSprint(1L);
    }
}
