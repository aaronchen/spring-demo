package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.audit.AuditField;
import cc.desuka.demo.event.ProjectPushEvent;
import cc.desuka.demo.event.ProjectUpdatedEvent;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.ProjectMember;
import cc.desuka.demo.model.ProjectRole;
import cc.desuka.demo.model.ProjectStatus;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.ProjectRepository;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.util.EntityTypes;
import cc.desuka.demo.util.Messages;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectQueryService projectQueryService;
    private final UserService userService;
    private final TaskCommandService taskCommandService;
    private final SprintService sprintService;
    private final RecurringTaskTemplateService recurringTaskTemplateService;
    private final RecentViewService recentViewService;
    private final PinnedItemService pinnedItemService;
    private final ApplicationEventPublisher eventPublisher;
    private final Messages messages;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectQueryService projectQueryService,
            UserService userService,
            TaskCommandService taskCommandService,
            SprintService sprintService,
            RecurringTaskTemplateService recurringTaskTemplateService,
            RecentViewService recentViewService,
            PinnedItemService pinnedItemService,
            ApplicationEventPublisher eventPublisher,
            Messages messages) {
        this.projectRepository = projectRepository;
        this.projectQueryService = projectQueryService;
        this.userService = userService;
        this.taskCommandService = taskCommandService;
        this.sprintService = sprintService;
        this.recurringTaskTemplateService = recurringTaskTemplateService;
        this.recentViewService = recentViewService;
        this.pinnedItemService = pinnedItemService;
        this.eventPublisher = eventPublisher;
        this.messages = messages;
    }

    public Project createProject(Project project, User creator) {
        project.setCreatedBy(creator);
        project.setStatus(ProjectStatus.ACTIVE);

        // Creator automatically becomes OWNER — cascaded via CascadeType.ALL
        ProjectMember ownerMember = new ProjectMember(project, creator, ProjectRole.OWNER);
        project.getMembers().add(ownerMember);

        Project saved = projectRepository.save(project);

        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.PROJECT_CREATED,
                        Project.class,
                        saved.getId(),
                        SecurityUtils.getCurrentPrincipal(),
                        AuditDetails.toJson(saved.toAuditSnapshot())));

        return saved;
    }

    public Project updateProject(UUID id, Project projectDetails) {
        Project project = projectQueryService.getProjectById(id);
        Map<String, AuditField> before = project.toAuditSnapshot();

        project.setName(projectDetails.getName());
        project.setDescription(projectDetails.getDescription());

        // When disabling sprints, unassign all tasks from sprints in this project
        boolean disablingSprints = project.isSprintEnabled() && !projectDetails.isSprintEnabled();
        // When enabling sprints, disable all recurring templates (mutually exclusive)
        boolean enablingSprints = !project.isSprintEnabled() && projectDetails.isSprintEnabled();
        project.setSprintEnabled(projectDetails.isSprintEnabled());
        if (disablingSprints) {
            sprintService.clearSprintAssignments(id);
        }
        if (enablingSprints) {
            recurringTaskTemplateService.disableAllForProject(id);
        }

        Project saved = projectRepository.save(project);

        Map<String, Object> changes = AuditDetails.diff(before, saved.toAuditSnapshot());
        if (!changes.isEmpty()) {
            eventPublisher.publishEvent(
                    new AuditEvent(
                            AuditEvent.PROJECT_UPDATED,
                            Project.class,
                            saved.getId(),
                            SecurityUtils.getCurrentPrincipal(),
                            AuditDetails.toJson(changes)));
            eventPublisher.publishEvent(
                    new ProjectUpdatedEvent(saved, SecurityUtils.getCurrentUser()));
            eventPublisher.publishEvent(
                    new ProjectPushEvent(
                            ProjectPushEvent.ACTION_UPDATED,
                            saved.getId(),
                            SecurityUtils.getCurrentUser().getId()));
        }

        return saved;
    }

    public void archiveProject(UUID id) {
        Project project = projectQueryService.getProjectById(id);
        project.setStatus(ProjectStatus.ARCHIVED);
        projectRepository.save(project);

        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.PROJECT_ARCHIVED,
                        Project.class,
                        id,
                        SecurityUtils.getCurrentPrincipal(),
                        null));
        eventPublisher.publishEvent(
                new ProjectPushEvent(
                        ProjectPushEvent.ACTION_ARCHIVED,
                        id,
                        SecurityUtils.getCurrentUser().getId()));
    }

    public void unarchiveProject(UUID id) {
        Project project = projectQueryService.getProjectById(id);
        project.setStatus(ProjectStatus.ACTIVE);
        projectRepository.save(project);

        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.PROJECT_UNARCHIVED,
                        Project.class,
                        id,
                        SecurityUtils.getCurrentPrincipal(),
                        null));
        eventPublisher.publishEvent(
                new ProjectPushEvent(
                        ProjectPushEvent.ACTION_UNARCHIVED,
                        id,
                        SecurityUtils.getCurrentUser().getId()));
    }

    /**
     * Deletes a project if it has no completed tasks. Projects with completed work should be
     * archived instead. Cancelled tasks don't block deletion — they represent abandoned work.
     */
    public void deleteProject(UUID id) {
        Project project = projectQueryService.getProjectById(id);

        boolean hasCompletedTasks =
                project.getTasks().stream().anyMatch(t -> t.getStatus() == TaskStatus.COMPLETED);
        if (hasCompletedTasks) {
            throw new IllegalStateException(messages.get("project.delete.hasCompletedTasks"));
        }

        String snapshot = AuditDetails.toJson(project.toAuditSnapshot());
        recentViewService.deleteByEntity(EntityTypes.PROJECT, id);
        pinnedItemService.deleteByEntity(EntityTypes.PROJECT, id);
        projectRepository.delete(project);

        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.PROJECT_DELETED,
                        Project.class,
                        id,
                        SecurityUtils.getCurrentPrincipal(),
                        snapshot));
    }

    // ── Member management ─────────────────────────────────────────────────

    public ProjectMember addMember(UUID projectId, UUID userId, ProjectRole role) {
        Project project = projectQueryService.getProjectById(projectId);
        User user = userService.getUserById(userId);

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

    public void removeMember(UUID projectId, UUID userId) {
        Project project = projectQueryService.getProjectById(projectId);
        ProjectMember member =
                findMember(project, userId)
                        .orElseThrow(
                                () ->
                                        new cc.desuka.demo.exception.EntityNotFoundException(
                                                ProjectMember.class, null));

        if (member.getRole() == ProjectRole.OWNER && countOwners(project) <= 1) {
            throw new IllegalStateException(messages.get("project.member.lastOwner.remove"));
        }

        String snapshot = AuditDetails.toJson(member.toAuditSnapshot());
        taskCommandService.unassignTasksInProject(member.getUser(), projectId);
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

    public void updateMemberRole(UUID projectId, UUID userId, ProjectRole newRole) {
        Project project = projectQueryService.getProjectById(projectId);
        ProjectMember member =
                findMember(project, userId)
                        .orElseThrow(
                                () ->
                                        new cc.desuka.demo.exception.EntityNotFoundException(
                                                ProjectMember.class, null));

        if (member.getRole() == ProjectRole.OWNER
                && newRole != ProjectRole.OWNER
                && countOwners(project) <= 1) {
            throw new IllegalStateException(messages.get("project.member.lastOwner.demote"));
        }

        Map<String, AuditField> before = member.toAuditSnapshot();
        if (member.getRole() != newRole) {
            member.setRole(newRole);

            // Demoting to VIEWER — unassign non-terminal tasks in this project
            if (newRole == ProjectRole.VIEWER) {
                User user = userService.getUserById(userId);
                taskCommandService.unassignTasksInProject(user, projectId);
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

    // ── Private helpers ─────────────────────────────────────────────────

    private Optional<ProjectMember> findMember(Project project, UUID userId) {
        return project.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst();
    }

    private long countOwners(Project project) {
        return project.getMembers().stream().filter(m -> m.getRole() == ProjectRole.OWNER).count();
    }
}
