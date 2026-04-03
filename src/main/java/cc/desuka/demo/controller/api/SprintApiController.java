package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.SprintRequest;
import cc.desuka.demo.dto.SprintResponse;
import cc.desuka.demo.mapper.SprintMapper;
import cc.desuka.demo.model.Sprint;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.ProjectAccessGuard;
import cc.desuka.demo.service.SprintQueryService;
import cc.desuka.demo.service.SprintService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/sprints")
public class SprintApiController {

    private final SprintService sprintService;
    private final SprintQueryService sprintQueryService;
    private final ProjectAccessGuard projectAccessGuard;
    private final SprintMapper sprintMapper;

    public SprintApiController(
            SprintService sprintService,
            SprintQueryService sprintQueryService,
            ProjectAccessGuard projectAccessGuard,
            SprintMapper sprintMapper) {
        this.sprintService = sprintService;
        this.sprintQueryService = sprintQueryService;
        this.projectAccessGuard = projectAccessGuard;
        this.sprintMapper = sprintMapper;
    }

    @GetMapping
    public List<SprintResponse> listSprints(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireViewAccess(projectId, currentDetails);
        return sprintMapper.toResponseList(sprintQueryService.getSprintsByProject(projectId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SprintResponse createSprint(
            @PathVariable UUID projectId,
            @Valid @RequestBody SprintRequest request,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireEditAccess(projectId, currentDetails);
        Sprint sprint = sprintService.createSprint(projectId, sprintMapper.toEntity(request));
        return sprintMapper.toResponse(sprint);
    }

    @PutMapping("/{id}")
    public SprintResponse updateSprint(
            @PathVariable UUID projectId,
            @PathVariable Long id,
            @Valid @RequestBody SprintRequest request,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireEditAccess(projectId, currentDetails);
        Sprint sprint = sprintService.updateSprint(id, sprintMapper.toEntity(request));
        return sprintMapper.toResponse(sprint);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSprint(
            @PathVariable UUID projectId,
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireEditAccess(projectId, currentDetails);
        sprintService.deleteSprint(id);
    }
}
