package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.SavedViewRequest;
import cc.desuka.demo.dto.SavedViewResponse;
import cc.desuka.demo.model.SavedView;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.OwnershipGuard;
import cc.desuka.demo.service.SavedViewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/views")
public class SavedViewController {

    private final SavedViewService savedViewService;
    private final OwnershipGuard ownershipGuard;

    public SavedViewController(SavedViewService savedViewService, OwnershipGuard ownershipGuard) {
        this.savedViewService = savedViewService;
        this.ownershipGuard = ownershipGuard;
    }

    @GetMapping
    public List<SavedViewResponse> listViews(
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        return savedViewService.getViewsForUser(currentDetails.getUser().getId()).stream()
                .map(SavedViewResponse::fromEntity)
                .toList();
    }

    @PostMapping
    public ResponseEntity<SavedViewResponse> createView(
            @Valid @RequestBody SavedViewRequest request,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        SavedView view =
                savedViewService.createView(
                        currentDetails.getUser(), request.name(), request.data());
        return ResponseEntity.status(HttpStatus.CREATED).body(SavedViewResponse.fromEntity(view));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteView(
            @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentDetails) {
        SavedView view = savedViewService.getViewById(id);
        ownershipGuard.requireAccess(view, currentDetails);
        savedViewService.deleteView(view);
        return ResponseEntity.noContent().build();
    }
}
