package cc.desuka.demo.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cc.desuka.demo.exception.PinLimitReachedException;
import cc.desuka.demo.model.PinnedItem;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.User;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.OwnershipGuard;
import cc.desuka.demo.service.PinnedItemService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PinnedItemApiControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PinnedItemService pinnedItemService;
    @MockitoBean private OwnershipGuard ownershipGuard;

    private User alice;
    private CustomUserDetails aliceDetails;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId(UUID.randomUUID());
        alice.setName("Alice");
        alice.setEmail("alice@example.com");
        alice.setRole(Role.ADMIN);
        alice.setEnabled(true);
        aliceDetails = new CustomUserDetails(alice);
    }

    @Test
    void getPins_returnsEmptyList() throws Exception {
        when(pinnedItemService.getPinnedItems(alice.getId())).thenReturn(List.of());

        mockMvc.perform(get("/api/pins").with(user(aliceDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getPins_returnsPinnedItems() throws Exception {
        PinnedItem pin = new PinnedItem(alice, "TASK", "task-id", "My Task");
        pin.setId(1L);
        when(pinnedItemService.getPinnedItems(alice.getId())).thenReturn(List.of(pin));

        mockMvc.perform(get("/api/pins").with(user(aliceDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entityType").value("TASK"))
                .andExpect(jsonPath("$[0].entityTitle").value("My Task"));
    }

    @Test
    void pin_createsAndReturns201() throws Exception {
        PinnedItem pin = new PinnedItem(alice, "TASK", "task-id", "My Task");
        pin.setId(1L);
        when(pinnedItemService.pin(any(), eq("TASK"), eq("task-id"), eq("My Task")))
                .thenReturn(pin);

        mockMvc.perform(
                        post("/api/pins")
                                .with(user(aliceDetails))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"entityType\":\"TASK\",\"entityId\":\"task-id\",\"entityTitle\":\"My Task\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entityType").value("TASK"));
    }

    @Test
    void pin_returns409WhenLimitReached() throws Exception {
        when(pinnedItemService.pin(any(), any(), any(), any()))
                .thenThrow(new PinLimitReachedException(20, 20));

        mockMvc.perform(
                        post("/api/pins")
                                .with(user(aliceDetails))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"entityType\":\"TASK\",\"entityId\":\"task-id\",\"entityTitle\":\"My Task\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.limit").value(20));
    }

    @Test
    void unpin_returns204() throws Exception {
        PinnedItem pin = new PinnedItem(alice, "TASK", "task-id", "My Task");
        pin.setId(1L);
        when(pinnedItemService.getPinById(1L)).thenReturn(pin);

        mockMvc.perform(delete("/api/pins/1").with(user(aliceDetails)).with(csrf()))
                .andExpect(status().isNoContent());

        verify(ownershipGuard).requireAccess(pin, aliceDetails);
        verify(pinnedItemService).unpin(pin);
    }
}
