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

    public Project getProjectById(Long id) {
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

    public List<Project> getProjectsForUser(Long userId) {
        return memberRepository.findByUserId(userId).stream()
                .map(ProjectMember::getProject)
                .filter(p -> p.getStatus() == ProjectStatus.ACTIVE)
                .toList();
    }

    public List<Project> getProjectsForUser(Long userId, boolean includeArchived, String sort) {
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

    public List<Long> getAccessibleProjectIds(Long userId) {
        return memberRepository.findByUserId(userId).stream()
                .map(ProjectMember::getProject)
                .filter(p -> p.getStatus() == ProjectStatus.ACTIVE)
                .map(Project::getId)
                .toList();
    }

    public List<Project> getEditableProjectsForUser(Long userId) {
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

    public Set<ProjectMember> getMembers(Long projectId) {
        Project project = getProjectById(projectId);
        return project.getMembers();
    }

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
}
