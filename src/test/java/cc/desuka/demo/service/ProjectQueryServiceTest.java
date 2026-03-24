package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.ProjectMember;
import cc.desuka.demo.model.ProjectRole;
import cc.desuka.demo.model.ProjectStatus;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.ProjectMemberRepository;
import cc.desuka.demo.repository.ProjectRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectQueryServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository memberRepository;

    @InjectMocks private ProjectQueryService projectQueryService;

    private User alice;
    private User bob;
    private Project project;

    @BeforeEach
    void setUp() {
        alice = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        alice.setId(1L);
        bob = new User("Bob", "bob@example.com", "password", Role.USER);
        bob.setId(2L);

        project = new Project("Test Project", "Description");
        project.setId(1L);
        project.setCreatedBy(alice);
        project.setStatus(ProjectStatus.ACTIVE);
    }

    // ── getProjectById ────────────────────────────────────────────────────

    @Test
    void getProjectById_found() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        Project result = projectQueryService.getProjectById(1L);

        assertThat(result).isEqualTo(project);
    }

    @Test
    void getProjectById_notFound_throwsEntityNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectQueryService.getProjectById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── getProjectsForUser ────────────────────────────────────────────────

    @Test
    void getProjectsForUser_returnsActiveOnly() {
        Project archived = new Project("Archived", "Old project");
        archived.setId(2L);
        archived.setStatus(ProjectStatus.ARCHIVED);

        when(memberRepository.findByUserId(1L))
                .thenReturn(
                        List.of(
                                new ProjectMember(project, alice, ProjectRole.OWNER),
                                new ProjectMember(archived, alice, ProjectRole.EDITOR)));

        List<Project> result = projectQueryService.getProjectsForUser(1L);

        assertThat(result).containsExactly(project);
    }

    // ── Access checks ─────────────────────────────────────────────────────

    @Test
    void isMember_true() {
        when(memberRepository.existsByProjectIdAndUserId(1L, 1L)).thenReturn(true);

        assertThat(projectQueryService.isMember(1L, 1L)).isTrue();
    }

    @Test
    void isMember_false() {
        when(memberRepository.existsByProjectIdAndUserId(1L, 99L)).thenReturn(false);

        assertThat(projectQueryService.isMember(1L, 99L)).isFalse();
    }

    @Test
    void isOwner_true() {
        ProjectMember ownerMember = new ProjectMember(project, alice, ProjectRole.OWNER);
        when(memberRepository.findByProjectIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(ownerMember));

        assertThat(projectQueryService.isOwner(1L, 1L)).isTrue();
    }

    @Test
    void isEditor_editorCanEdit() {
        ProjectMember memberShip = new ProjectMember(project, bob, ProjectRole.EDITOR);
        when(memberRepository.findByProjectIdAndUserId(1L, 2L)).thenReturn(Optional.of(memberShip));

        assertThat(projectQueryService.isEditor(1L, 2L)).isTrue();
    }

    @Test
    void isEditor_viewerCannotEdit() {
        ProjectMember viewerMember = new ProjectMember(project, bob, ProjectRole.VIEWER);
        when(memberRepository.findByProjectIdAndUserId(1L, 2L))
                .thenReturn(Optional.of(viewerMember));

        assertThat(projectQueryService.isEditor(1L, 2L)).isFalse();
    }

    @Test
    void isEditor_nonMemberCannotEdit() {
        when(memberRepository.findByProjectIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertThat(projectQueryService.isEditor(1L, 99L)).isFalse();
    }
}
