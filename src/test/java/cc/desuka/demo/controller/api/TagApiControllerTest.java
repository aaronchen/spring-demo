package cc.desuka.demo.controller.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cc.desuka.demo.dto.TagResponse;
import cc.desuka.demo.mapper.TagMapper;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.Tag;
import cc.desuka.demo.model.User;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.service.TagQueryService;
import cc.desuka.demo.service.TagService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
class TagApiControllerTest {

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private TagQueryService tagQueryService;
    @MockitoBean private TagService tagService;
    @MockitoBean private TagMapper tagMapper;

    private CustomUserDetails regularDetails;
    private CustomUserDetails adminDetails;

    @BeforeEach
    void setUp() {
        User regularUser = new User("Bob", "bob@example.com", "password", Role.USER);
        regularUser.setId(ID_2);
        User adminUser = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        adminUser.setId(ID_1);
        regularDetails = new CustomUserDetails(regularUser);
        adminDetails = new CustomUserDetails(adminUser);
    }

    // ── GET /api/tags ────────────────────────────────────────────────────

    @Test
    void getAllTags_returnsJsonList() throws Exception {
        Tag tag = new Tag("Work");
        tag.setId(1L);
        when(tagQueryService.getAllTags()).thenReturn(List.of(tag));
        when(tagMapper.toResponseList(anyCollection()))
                .thenReturn(List.of(new TagResponse(1L, "Work")));

        mockMvc.perform(get("/api/tags").with(user(regularDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Work"));
    }

    // ── GET /api/tags/{id} ───────────────────────────────────────────────

    @Test
    void getTagById_returnsJson() throws Exception {
        Tag tag = new Tag("Work");
        tag.setId(1L);
        when(tagQueryService.getTagById(1L)).thenReturn(tag);
        when(tagMapper.toResponse(tag)).thenReturn(new TagResponse(1L, "Work"));

        mockMvc.perform(get("/api/tags/1").with(user(regularDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Work"));
    }

    // ── POST /api/tags ───────────────────────────────────────────────────

    @Test
    void createTag_admin_returns201() throws Exception {
        Tag tag = new Tag("New");
        tag.setId(2L);
        when(tagMapper.toEntity(any())).thenReturn(tag);
        when(tagService.createTag(any(Tag.class))).thenReturn(tag);
        when(tagMapper.toResponse(any(Tag.class))).thenReturn(new TagResponse(2L, "New"));

        String body = objectMapper.writeValueAsString(Map.of("name", "New"));

        mockMvc.perform(
                        post("/api/tags")
                                .with(user(adminDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New"));
    }

    @Test
    void createTag_regularUser_returns403() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", "New"));

        mockMvc.perform(
                        post("/api/tags")
                                .with(user(regularDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/tags/{id} ────────────────────────────────────────────

    @Test
    void deleteTag_admin_returns204() throws Exception {
        mockMvc.perform(delete("/api/tags/1").with(user(adminDetails)))
                .andExpect(status().isNoContent());

        verify(tagService).deleteTag(1L);
    }

    @Test
    void deleteTag_regularUser_returns403() throws Exception {
        mockMvc.perform(delete("/api/tags/1").with(user(regularDetails)))
                .andExpect(status().isForbidden());

        verify(tagService, never()).deleteTag(any());
    }
}
