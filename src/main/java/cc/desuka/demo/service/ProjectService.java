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
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Project write operations (create, update, delete, archive). Counterpart to {@link
 * ProjectQueryService} (reads). Member operations are in {@link ProjectMemberService}.
 */
@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectQueryService projectQueryService;
    private final SprintService sprintService;
    private final RecurringTaskTemplateService recurringTaskTemplateService;
    private final RecentViewService recentViewService;
    private final PinnedItemService pinnedItemService;
    private final ApplicationEventPublisher eventPublisher;
    private final Messages messages;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectQueryService projectQueryService,
            SprintService sprintService,
            RecurringTaskTemplateService recurringTaskTemplateService,
            RecentViewService recentViewService,
            PinnedItemService pinnedItemService,
            ApplicationEventPublisher eventPublisher,
            Messages messages) {
        this.projectRepository = projectRepository;
        this.projectQueryService = projectQueryService;
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
}
