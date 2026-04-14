package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.RecurringTaskTemplateRequest;
import cc.desuka.demo.dto.RecurringTaskTemplateResponse;
import cc.desuka.demo.mapper.RecurringTaskTemplateMapper;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.ProjectAccessGuard;
import cc.desuka.demo.service.RecurringTaskGenerationService;
import cc.desuka.demo.service.RecurringTaskTemplateQueryService;
import cc.desuka.demo.service.RecurringTaskTemplateService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/recurring-templates")
public class RecurringTaskTemplateApiController {

    private final RecurringTaskTemplateQueryService templateQueryService;
    private final RecurringTaskTemplateService templateService;
    private final RecurringTaskGenerationService generationService;
    private final ProjectAccessGuard projectAccessGuard;
    private final RecurringTaskTemplateMapper recurringTaskTemplateMapper;

    public RecurringTaskTemplateApiController(
            RecurringTaskTemplateQueryService templateQueryService,
            RecurringTaskTemplateService templateService,
            RecurringTaskGenerationService generationService,
            ProjectAccessGuard projectAccessGuard,
            RecurringTaskTemplateMapper recurringTaskTemplateMapper) {
        this.templateQueryService = templateQueryService;
        this.templateService = templateService;
        this.generationService = generationService;
        this.projectAccessGuard = projectAccessGuard;
        this.recurringTaskTemplateMapper = recurringTaskTemplateMapper;
    }

    @GetMapping
    public List<RecurringTaskTemplateResponse> listTemplates(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireViewAccess(projectId, currentDetails);
        return recurringTaskTemplateMapper.toResponseList(
                templateQueryService.getTemplatesByProject(projectId));
    }

    @GetMapping("/{id}")
    public RecurringTaskTemplateResponse getTemplate(
            @PathVariable UUID projectId,
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireViewAccess(projectId, currentDetails);
        return recurringTaskTemplateMapper.toResponse(templateQueryService.getTemplateById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringTaskTemplateResponse createTemplate(
            @PathVariable UUID projectId,
            @Valid @RequestBody RecurringTaskTemplateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireEditAccess(projectId, currentDetails);
        return recurringTaskTemplateMapper.toResponse(
                templateService.createTemplate(projectId, request));
    }

    @PutMapping("/{id}")
    public RecurringTaskTemplateResponse updateTemplate(
            @PathVariable UUID projectId,
            @PathVariable Long id,
            @Valid @RequestBody RecurringTaskTemplateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireEditAccess(projectId, currentDetails);
        return recurringTaskTemplateMapper.toResponse(templateService.updateTemplate(id, request));
    }

    @PostMapping("/{id}/toggle")
    public RecurringTaskTemplateResponse toggleEnabled(
            @PathVariable UUID projectId,
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireEditAccess(projectId, currentDetails);
        templateService.toggleEnabled(id);
        return recurringTaskTemplateMapper.toResponse(templateQueryService.getTemplateById(id));
    }

    @PostMapping("/{id}/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public void generateFromTemplate(
            @PathVariable UUID projectId,
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireEditAccess(projectId, currentDetails);
        generationService.generateFromTemplate(
                templateQueryService.getTemplateById(id), currentDetails.getUsername());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(
            @PathVariable UUID projectId,
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireEditAccess(projectId, currentDetails);
        templateService.deleteTemplate(id);
    }
}
