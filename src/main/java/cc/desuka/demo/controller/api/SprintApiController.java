package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.SprintRequest;
import cc.desuka.demo.dto.SprintResponse;
import cc.desuka.demo.model.Sprint;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.ProjectAccessGuard;
import cc.desuka.demo.service.SprintQueryService;
import cc.desuka.demo.service.SprintService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/sprints")
public class SprintApiController {

    private final SprintService sprintService;
    private final SprintQueryService sprintQueryService;
    private final ProjectAccessGuard projectAccessGuard;

    public SprintApiController(
            SprintService sprintService,
            SprintQueryService sprintQueryService,
            ProjectAccessGuard projectAccessGuard) {
        this.sprintService = sprintService;
        this.sprintQueryService = sprintQueryService;
        this.projectAccessGuard = projectAccessGuard;
    }

    @GetMapping
    public List<SprintResponse> listSprints(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireViewAccess(projectId, currentDetails);
        return sprintQueryService.getSprintsByProject(projectId).stream()
                .map(SprintResponse::fromEntity)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SprintResponse createSprint(
            @PathVariable Long projectId,
            @Valid @RequestBody SprintRequest request,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireEditAccess(projectId, currentDetails);
        Sprint sprint = sprintService.createSprint(projectId, request.toEntity());
        return SprintResponse.fromEntity(sprint);
    }

    @PutMapping("/{id}")
    public SprintResponse updateSprint(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @Valid @RequestBody SprintRequest request,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireEditAccess(projectId, currentDetails);
        Sprint sprint = sprintService.updateSprint(id, request.toEntity());
        return SprintResponse.fromEntity(sprint);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSprint(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireEditAccess(projectId, currentDetails);
        sprintService.deleteSprint(id);
    }
}
