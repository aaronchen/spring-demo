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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectQueryServiceTest {

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ID_99 = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository memberRepository;

    @InjectMocks private ProjectQueryService projectQueryService;

    private User alice;
    private User bob;
    private Project project;

    @BeforeEach
    void setUp() {
        alice = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        alice.setId(ID_1);
        bob = new User("Bob", "bob@example.com", "password", Role.USER);
        bob.setId(ID_2);

        project = new Project("Test Project", "Description");
        project.setId(ID_1);
        project.setCreatedBy(alice);
        project.setStatus(ProjectStatus.ACTIVE);
    }

    // ── getProjectById ────────────────────────────────────────────────────

    @Test
    void getProjectById_found() {
        when(projectRepository.findById(ID_1)).thenReturn(Optional.of(project));

        Project result = projectQueryService.getProjectById(ID_1);

        assertThat(result).isEqualTo(project);
    }

    @Test
    void getProjectById_notFound_throwsEntityNotFoundException() {
        when(projectRepository.findById(ID_99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectQueryService.getProjectById(ID_99))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── getProjectsForUser ────────────────────────────────────────────────

    @Test
    void getProjectsForUser_returnsActiveOnly() {
        Project archived = new Project("Archived", "Old project");
        archived.setId(ID_2);
        archived.setStatus(ProjectStatus.ARCHIVED);

        when(memberRepository.findByUserId(ID_1))
                .thenReturn(
                        List.of(
                                new ProjectMember(project, alice, ProjectRole.OWNER),
                                new ProjectMember(archived, alice, ProjectRole.EDITOR)));

        List<Project> result = projectQueryService.getProjectsForUser(ID_1);

        assertThat(result).containsExactly(project);
    }

    // ── Access checks ─────────────────────────────────────────────────────

    @Test
    void isMember_true() {
        when(memberRepository.existsByProjectIdAndUserId(ID_1, ID_1)).thenReturn(true);

        assertThat(projectQueryService.isMember(ID_1, ID_1)).isTrue();
    }

    @Test
    void isMember_false() {
        when(memberRepository.existsByProjectIdAndUserId(ID_1, ID_99)).thenReturn(false);

        assertThat(projectQueryService.isMember(ID_1, ID_99)).isFalse();
    }

    @Test
    void isOwner_true() {
        ProjectMember ownerMember = new ProjectMember(project, alice, ProjectRole.OWNER);
        when(memberRepository.findByProjectIdAndUserId(ID_1, ID_1))
                .thenReturn(Optional.of(ownerMember));

        assertThat(projectQueryService.isOwner(ID_1, ID_1)).isTrue();
    }

    @Test
    void isEditor_editorCanEdit() {
        ProjectMember memberShip = new ProjectMember(project, bob, ProjectRole.EDITOR);
        when(memberRepository.findByProjectIdAndUserId(ID_1, ID_2))
                .thenReturn(Optional.of(memberShip));

        assertThat(projectQueryService.isEditor(ID_1, ID_2)).isTrue();
    }

    @Test
    void isEditor_viewerCannotEdit() {
        ProjectMember viewerMember = new ProjectMember(project, bob, ProjectRole.VIEWER);
        when(memberRepository.findByProjectIdAndUserId(ID_1, ID_2))
                .thenReturn(Optional.of(viewerMember));

        assertThat(projectQueryService.isEditor(ID_1, ID_2)).isFalse();
    }

    @Test
    void isEditor_nonMemberCannotEdit() {
        when(memberRepository.findByProjectIdAndUserId(ID_1, ID_99)).thenReturn(Optional.empty());

        assertThat(projectQueryService.isEditor(ID_1, ID_99)).isFalse();
    }
}
