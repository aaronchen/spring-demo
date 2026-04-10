package cc.desuka.demo.service;

import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.ProjectMember;
import cc.desuka.demo.model.ProjectRole;
import cc.desuka.demo.model.ProjectStatus;
import cc.desuka.demo.repository.ProjectMemberRepository;
import cc.desuka.demo.repository.ProjectRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProjectQueryService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;

    public ProjectQueryService(
            ProjectRepository projectRepository, ProjectMemberRepository memberRepository) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
    }

    public Project getProjectById(UUID id) {
        return projectRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Project.class, id));
    }

    public List<Project> getActiveProjects() {
        return projectRepository.findByStatusOrderByNameAsc(ProjectStatus.ACTIVE);
    }

    public List<Project> getAdminProjects(boolean includeArchived, String sort) {
        if (includeArchived) {
            return "newest".equals(sort)
                    ? projectRepository.findAllByOrderByCreatedAtDesc()
                    : projectRepository.findAllByOrderByNameAsc();
        }
        return "newest".equals(sort)
                ? projectRepository.findByStatusOrderByCreatedAtDesc(ProjectStatus.ACTIVE)
                : projectRepository.findByStatusOrderByNameAsc(ProjectStatus.ACTIVE);
    }

    public List<Project> getProjectsForUser(UUID userId) {
        return memberRepository.findByUserId(userId).stream()
                .map(ProjectMember::getProject)
                .filter(p -> p.getStatus() == ProjectStatus.ACTIVE)
                .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<Project> getProjectsForUser(UUID userId, boolean includeArchived, String sort) {
        Comparator<Project> comparator =
                "newest".equals(sort)
                        ? Comparator.comparing(Project::getCreatedAt, Comparator.reverseOrder())
                        : Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER);
        return memberRepository.findByUserId(userId).stream()
                .map(ProjectMember::getProject)
                .filter(p -> includeArchived || p.getStatus() == ProjectStatus.ACTIVE)
                .sorted(comparator)
                .toList();
    }

    public List<UUID> getAccessibleProjectIds(UUID userId) {
        return memberRepository.findByUserId(userId).stream()
                .map(ProjectMember::getProject)
                .filter(p -> p.getStatus() == ProjectStatus.ACTIVE)
                .map(Project::getId)
                .toList();
    }

    public List<UUID> getAllActiveProjectIds() {
        return projectRepository.findIdsByStatus(ProjectStatus.ACTIVE);
    }

    public List<Project> getEditableProjectsForUser(UUID userId) {
        return memberRepository.findByUserId(userId).stream()
                .filter(
                        m ->
                                m.getProject().getStatus() == ProjectStatus.ACTIVE
                                        && (m.getRole() == ProjectRole.EDITOR
                                                || m.getRole() == ProjectRole.OWNER))
                .map(ProjectMember::getProject)
                .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    // ── Member queries ────────────────────────────────────────────────────

    public Set<ProjectMember> getMembers(UUID projectId) {
        Project project = getProjectById(projectId);
        return project.getMembers();
    }

    public boolean isMember(UUID projectId, UUID userId) {
        return memberRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    public Optional<ProjectRole> getMemberRole(UUID projectId, UUID userId) {
        return memberRepository
                .findByProjectIdAndUserId(projectId, userId)
                .map(ProjectMember::getRole);
    }

    public boolean isOwner(UUID projectId, UUID userId) {
        return getMemberRole(projectId, userId)
                .map(role -> role == ProjectRole.OWNER)
                .orElse(false);
    }

    public boolean isEditor(UUID projectId, UUID userId) {
        return getMemberRole(projectId, userId)
                .map(role -> role == ProjectRole.OWNER || role == ProjectRole.EDITOR)
                .orElse(false);
    }

    public boolean isSoleOwnerOfAnyProject(UUID userId) {
        return memberRepository.isSoleOwnerOfAnyProject(userId);
    }
}
