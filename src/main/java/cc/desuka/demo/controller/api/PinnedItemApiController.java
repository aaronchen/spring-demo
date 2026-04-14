package cc.desuka.demo.controller.api;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.dto.PinnedItemRequest;
import cc.desuka.demo.dto.PinnedItemResponse;
import cc.desuka.demo.mapper.PinnedItemMapper;
import cc.desuka.demo.model.PinnedItem;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.OwnershipGuard;
import cc.desuka.demo.service.PinnedItemQueryService;
import cc.desuka.demo.service.PinnedItemService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pins")
public class PinnedItemApiController {

    private final PinnedItemQueryService pinnedItemQueryService;
    private final PinnedItemService pinnedItemService;
    private final PinnedItemMapper pinnedItemMapper;
    private final OwnershipGuard ownershipGuard;
    private final AppRoutesProperties appRoutes;

    public PinnedItemApiController(
            PinnedItemQueryService pinnedItemQueryService,
            PinnedItemService pinnedItemService,
            PinnedItemMapper pinnedItemMapper,
            OwnershipGuard ownershipGuard,
            AppRoutesProperties appRoutes) {
        this.pinnedItemQueryService = pinnedItemQueryService;
        this.pinnedItemService = pinnedItemService;
        this.pinnedItemMapper = pinnedItemMapper;
        this.ownershipGuard = ownershipGuard;
        this.appRoutes = appRoutes;
    }

    @GetMapping
    public List<PinnedItemResponse> getPins(
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        return pinnedItemMapper.toResponseList(
                pinnedItemQueryService.getPinnedItems(currentDetails.getUser().getId()), appRoutes);
    }

    @PostMapping
    public ResponseEntity<PinnedItemResponse> pin(
            @Valid @RequestBody PinnedItemRequest request,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        PinnedItem pin =
                pinnedItemService.pin(
                        currentDetails.getUser(),
                        request.entityType(),
                        request.entityId(),
                        request.entityTitle());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pinnedItemMapper.toResponse(pin, appRoutes));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unpin(
            @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentDetails) {
        PinnedItem pin = pinnedItemQueryService.getPinById(id);
        ownershipGuard.requireAccess(pin, currentDetails);
        pinnedItemService.unpin(pin);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reorder")
    public ResponseEntity<Void> reorder(
            @RequestBody List<Long> orderedIds,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        pinnedItemService.reorder(currentDetails.getUser().getId(), orderedIds);
        return ResponseEntity.noContent().build();
    }
}
