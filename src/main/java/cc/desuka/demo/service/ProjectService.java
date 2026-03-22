package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.ProjectMember;
import cc.desuka.demo.model.ProjectRole;
import cc.desuka.demo.model.ProjectStatus;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.ProjectMemberRepository;
import cc.desuka.demo.repository.ProjectRepository;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.util.Messages;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final Messages messages;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository memberRepository,
            UserService userService,
            ApplicationEventPublisher eventPublisher,
            Messages messages) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.messages = messages;
    }

    public Project getProjectById(Long id) {
        return projectRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Project.class, id));
    }

    public List<Project> getActiveProjects() {
        return projectRepository.findByStatusOrderByNameAsc(ProjectStatus.ACTIVE);
    }

    public List<Project> getActiveProjectsByNewest() {
        return projectRepository.findByStatusOrderByCreatedAtDesc(ProjectStatus.ACTIVE);
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAllByOrderByNameAsc();
    }

    public List<Project> getAllProjectsByNewest() {
        return projectRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Project> getProjectsForUser(Long userId) {
        return memberRepository.findByUserId(userId).stream()
                .map(ProjectMember::getProject)
                .filter(p -> p.getStatus() == ProjectStatus.ACTIVE)
                .toList();
    }

    public List<Project> getProjectsForUser(Long userId, boolean includeArchived, String sort) {
        java.util.Comparator<Project> comparator =
                "newest".equals(sort)
                        ? java.util.Comparator.comparing(
                                Project::getCreatedAt, java.util.Comparator.reverseOrder())
                        : java.util.Comparator.comparing(
                                Project::getName, String.CASE_INSENSITIVE_ORDER);
        return memberRepository.findByUserId(userId).stream()
                .map(ProjectMember::getProject)
                .filter(p -> includeArchived || p.getStatus() == ProjectStatus.ACTIVE)
                .sorted(comparator)
                .toList();
    }

    public List<Long> getAccessibleProjectIds(Long userId) {
        return memberRepository.findByUserId(userId).stream()
                .map(ProjectMember::getProject)
                .filter(p -> p.getStatus() == ProjectStatus.ACTIVE)
                .map(Project::getId)
                .toList();
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

    public Project updateProject(Long id, Project projectDetails) {
        Project project = getProjectById(id);
        Map<String, Object> before = project.toAuditSnapshot();

        project.setName(projectDetails.getName());
        project.setDescription(projectDetails.getDescription());

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
        }

        return saved;
    }

    public void archiveProject(Long id) {
        Project project = getProjectById(id);
        project.setStatus(ProjectStatus.ARCHIVED);
        projectRepository.save(project);

        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.PROJECT_ARCHIVED,
                        Project.class,
                        id,
                        SecurityUtils.getCurrentPrincipal(),
                        null));
    }

    public void unarchiveProject(Long id) {
        Project project = getProjectById(id);
        project.setStatus(ProjectStatus.ACTIVE);
        projectRepository.save(project);

        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.PROJECT_UNARCHIVED,
                        Project.class,
                        id,
                        SecurityUtils.getCurrentPrincipal(),
                        null));
    }

    /**
     * Deletes a project if it has no completed tasks. Projects with completed work should be
     * archived instead. Cancelled tasks don't block deletion — they represent abandoned work.
     */
    public void deleteProject(Long id) {
        Project project = getProjectById(id);

        boolean hasCompletedTasks =
                project.getTasks().stream().anyMatch(t -> t.getStatus() == TaskStatus.COMPLETED);
        if (hasCompletedTasks) {
            throw new IllegalStateException(messages.get("project.delete.hasCompletedTasks"));
        }

        String snapshot = AuditDetails.toJson(project.toAuditSnapshot());
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

    public List<ProjectMember> getMembers(Long projectId) {
        Project project = getProjectById(projectId);
        return project.getMembers();
    }

    public ProjectMember addMember(Long projectId, Long userId, ProjectRole role) {
        Project project = getProjectById(projectId);
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

    public void removeMember(Long projectId, Long userId) {
        Project project = getProjectById(projectId);
        ProjectMember member =
                findMember(project, userId)
                        .orElseThrow(() -> new EntityNotFoundException(ProjectMember.class, null));

        if (member.getRole() == ProjectRole.OWNER && countOwners(project) <= 1) {
            throw new IllegalStateException(messages.get("project.member.lastOwner.remove"));
        }

        String snapshot = AuditDetails.toJson(member.toAuditSnapshot());
        project.getMembers().remove(member);

        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.PROJECT_MEMBER_REMOVED,
                        Project.class,
                        projectId,
                        SecurityUtils.getCurrentPrincipal(),
                        snapshot));
    }

    public void updateMemberRole(Long projectId, Long userId, ProjectRole newRole) {
        Project project = getProjectById(projectId);
        ProjectMember member =
                findMember(project, userId)
                        .orElseThrow(() -> new EntityNotFoundException(ProjectMember.class, null));

        if (member.getRole() == ProjectRole.OWNER
                && newRole != ProjectRole.OWNER
                && countOwners(project) <= 1) {
            throw new IllegalStateException(messages.get("project.member.lastOwner.demote"));
        }

        Map<String, Object> before = member.toAuditSnapshot();
        if (member.getRole() != newRole) {
            member.setRole(newRole);
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

    // ── Access checks ─────────────────────────────────────────────────────

    public boolean isMember(Long projectId, Long userId) {
        return memberRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    public Optional<ProjectRole> getMemberRole(Long projectId, Long userId) {
        return memberRepository
                .findByProjectIdAndUserId(projectId, userId)
                .map(ProjectMember::getRole);
    }

    public boolean isOwner(Long projectId, Long userId) {
        return getMemberRole(projectId, userId)
                .map(role -> role == ProjectRole.OWNER)
                .orElse(false);
    }

    public boolean isEditor(Long projectId, Long userId) {
        return getMemberRole(projectId, userId)
                .map(role -> role == ProjectRole.OWNER || role == ProjectRole.EDITOR)
                .orElse(false);
    }

    // ── Private helpers ─────────────────────────────────────────────────

    private Optional<ProjectMember> findMember(Project project, Long userId) {
        return project.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst();
    }

    private long countOwners(Project project) {
        return project.getMembers().stream().filter(m -> m.getRole() == ProjectRole.OWNER).count();
    }
}
