package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.audit.AuditField;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.ProjectMember;
import cc.desuka.demo.model.ProjectRole;
import cc.desuka.demo.model.User;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.util.Messages;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Project member write operations (add, remove, role change). Counterpart to member queries in
 * {@link ProjectQueryService}.
 */
@Service
@Transactional
public class ProjectMemberService {

    private final ProjectQueryService projectQueryService;
    private final UserQueryService userQueryService;
    private final TaskService taskService;
    private final PinnedItemService pinnedItemService;
    private final RecentViewService recentViewService;
    private final ApplicationEventPublisher eventPublisher;
    private final Messages messages;

    public ProjectMemberService(
            ProjectQueryService projectQueryService,
            UserQueryService userQueryService,
            TaskService taskService,
            PinnedItemService pinnedItemService,
            RecentViewService recentViewService,
            ApplicationEventPublisher eventPublisher,
            Messages messages) {
        this.projectQueryService = projectQueryService;
        this.userQueryService = userQueryService;
        this.taskService = taskService;
        this.pinnedItemService = pinnedItemService;
        this.recentViewService = recentViewService;
        this.eventPublisher = eventPublisher;
        this.messages = messages;
    }

    /** Adds a member to a project with the given role. */
    public ProjectMember addMember(UUID projectId, UUID userId, ProjectRole role) {
        Project project = projectQueryService.getProjectById(projectId);
        User user = userQueryService.getUserById(userId);

        if (findMember(project, userId).isPresent()) {
            throw new IllegalStateException(messages.get("project.member.alreadyExists"));
        }

        ProjectMember member = new ProjectMember(project, user, role);
        project.getMembers().add(member);

        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.PROJECT_MEMBER_ADDED,
                        Project.class,
                        projectId,
                        SecurityUtils.getCurrentPrincipal(),
                        AuditDetails.toJson(member.toAuditSnapshot())));

        return member;
    }

    /**
     * Removes a member from a project. Validates last-owner constraint, unassigns their tasks,
     * deletes their pins and recent views for the project, then removes the membership.
     */
    public void removeMember(UUID projectId, UUID userId) {
        Project project = projectQueryService.getProjectById(projectId);
        ProjectMember member = findMemberOrThrow(project, userId);

        if (member.getRole() == ProjectRole.OWNER && countOwners(project) <= 1) {
            throw new IllegalStateException(messages.get("project.member.lastOwner.remove"));
        }

        String snapshot = AuditDetails.toJson(member.toAuditSnapshot());
        taskService.unassignTasksInProject(member.getUser(), projectId);
        pinnedItemService.deleteByUserAndProject(userId, projectId);
        recentViewService.deleteByUserAndProject(userId, projectId);
        project.getMembers().remove(member);

        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.PROJECT_MEMBER_REMOVED,
                        Project.class,
                        projectId,
                        SecurityUtils.getCurrentPrincipal(),
                        snapshot));
    }

    /**
     * Updates a member's role. Validates last-owner constraint. If demoting to VIEWER, unassigns
     * their non-terminal tasks in the project first.
     */
    public void updateMemberRole(UUID projectId, UUID userId, ProjectRole newRole) {
        Project project = projectQueryService.getProjectById(projectId);
        ProjectMember member = findMemberOrThrow(project, userId);

        if (member.getRole() == ProjectRole.OWNER
                && newRole != ProjectRole.OWNER
                && countOwners(project) <= 1) {
            throw new IllegalStateException(messages.get("project.member.lastOwner.demote"));
        }

        Map<String, AuditField> before = member.toAuditSnapshot();
        if (member.getRole() != newRole) {
            member.setRole(newRole);

            if (newRole == ProjectRole.VIEWER) {
                User user = userQueryService.getUserById(userId);
                taskService.unassignTasksInProject(user, projectId);
            }

            Map<String, Object> changes = AuditDetails.diff(before, member.toAuditSnapshot());
            eventPublisher.publishEvent(
                    new AuditEvent(
                            AuditEvent.PROJECT_MEMBER_ROLE_CHANGED,
                            Project.class,
                            projectId,
                            SecurityUtils.getCurrentPrincipal(),
                            AuditDetails.toJson(changes)));
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private ProjectMember findMemberOrThrow(Project project, UUID userId) {
        return findMember(project, userId)
                .orElseThrow(() -> new EntityNotFoundException(ProjectMember.class, null));
    }

    private Optional<ProjectMember> findMember(Project project, UUID userId) {
        return project.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst();
    }

    private long countOwners(Project project) {
        return project.getMembers().stream().filter(m -> m.getRole() == ProjectRole.OWNER).count();
    }
}
