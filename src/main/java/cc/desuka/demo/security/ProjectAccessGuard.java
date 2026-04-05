package cc.desuka.demo.security;

import cc.desuka.demo.model.OwnedEntity;
import cc.desuka.demo.service.ProjectQueryService;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Project-scoped access guard for controller methods.
 *
 * <p>Checks that the current user has access to the specified project, or is a system admin (who
 * can see all projects).
 *
 * <p>For write operations, use {@link #requireEditAccess(Long, CustomUserDetails)} which
 * additionally checks that the user has EDITOR or OWNER role (not VIEWER).
 */
@Component
public class ProjectAccessGuard {

    private final ProjectQueryService projectQueryService;

    public ProjectAccessGuard(ProjectQueryService projectQueryService) {
        this.projectQueryService = projectQueryService;
    }

    /** Throws {@link AccessDeniedException} unless the user has project access or is admin. */
    public void requireViewAccess(UUID projectId, CustomUserDetails currentDetails) {
        if (AuthExpressions.isAdmin(currentDetails.getUser())) {
            return;
        }
        if (!projectQueryService.isMember(projectId, currentDetails.getUser().getId())) {
            throw new AccessDeniedException("Access Denied");
        }
    }

    /**
     * Throws {@link AccessDeniedException} unless the user can edit within the project (EDITOR or
     * OWNER role, or system admin).
     */
    public void requireEditAccess(UUID projectId, CustomUserDetails currentDetails) {
        if (AuthExpressions.isAdmin(currentDetails.getUser())) {
            return;
        }
        if (!projectQueryService.isEditor(projectId, currentDetails.getUser().getId())) {
            throw new AccessDeniedException("Access Denied");
        }
    }

    /** Returns true if the user can edit within the project (EDITOR/OWNER or admin). */
    public boolean canEdit(UUID projectId, CustomUserDetails currentDetails) {
        if (AuthExpressions.isAdmin(currentDetails.getUser())) {
            return true;
        }
        return projectQueryService.isEditor(projectId, currentDetails.getUser().getId());
    }

    /**
     * Throws {@link AccessDeniedException} unless the user is the entity owner, a project OWNER, or
     * a system admin.
     */
    public void requireDeleteAccess(
            OwnedEntity entity, UUID projectId, CustomUserDetails currentDetails) {
        if (AuthExpressions.canDelete(currentDetails.getUser(), entity)) {
            return;
        }
        requireOwnerAccess(projectId, currentDetails);
    }

    /** Throws {@link AccessDeniedException} unless the user is a project OWNER or system admin. */
    public void requireOwnerAccess(UUID projectId, CustomUserDetails currentDetails) {
        if (AuthExpressions.isAdmin(currentDetails.getUser())) {
            return;
        }
        if (!projectQueryService.isOwner(projectId, currentDetails.getUser().getId())) {
            throw new AccessDeniedException("Access Denied");
        }
    }
}
