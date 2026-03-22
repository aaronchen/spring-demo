package cc.desuka.demo.security;

import cc.desuka.demo.service.ProjectService;
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

    private final ProjectService projectService;

    public ProjectAccessGuard(ProjectService projectService) {
        this.projectService = projectService;
    }

    /** Throws {@link AccessDeniedException} unless the user has project access or is admin. */
    public void requireViewAccess(Long projectId, CustomUserDetails currentDetails) {
        if (AuthExpressions.isAdmin(currentDetails.getUser())) {
            return;
        }
        if (!projectService.isMember(projectId, currentDetails.getUser().getId())) {
            throw new AccessDeniedException("Access Denied");
        }
    }

    /**
     * Throws {@link AccessDeniedException} unless the user can edit within the project (EDITOR or
     * OWNER role, or system admin).
     */
    public void requireEditAccess(Long projectId, CustomUserDetails currentDetails) {
        if (AuthExpressions.isAdmin(currentDetails.getUser())) {
            return;
        }
        if (!projectService.isEditor(projectId, currentDetails.getUser().getId())) {
            throw new AccessDeniedException("Access Denied");
        }
    }

    /** Throws {@link AccessDeniedException} unless the user is a project OWNER or system admin. */
    public void requireOwnerAccess(Long projectId, CustomUserDetails currentDetails) {
        if (AuthExpressions.isAdmin(currentDetails.getUser())) {
            return;
        }
        if (!projectService.isOwner(projectId, currentDetails.getUser().getId())) {
            throw new AccessDeniedException("Access Denied");
        }
    }
}
