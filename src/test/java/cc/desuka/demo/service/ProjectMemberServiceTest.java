package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.ProjectMember;
import cc.desuka.demo.model.ProjectRole;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.User;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.util.Messages;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ALICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID BOB_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    @Mock private ProjectQueryService projectQueryService;
    @Mock private UserQueryService userQueryService;
    @Mock private TaskService taskService;
    @Mock private PinnedItemService pinnedItemService;
    @Mock private RecentViewService recentViewService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private Messages messages;

    @InjectMocks private ProjectMemberService projectMemberService;

    private Project project;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        alice.setId(ALICE_ID);
        bob = new User("Bob", "bob@example.com", "password", Role.USER);
        bob.setId(BOB_ID);

        project = new Project("Test Project", "Description");
        project.setId(PROJECT_ID);
    }

    // ── addMember ────────────────────────────────────────────────────────

    @Test
    void addMember_newMember_addedToCollection() {
        when(projectQueryService.getProjectById(PROJECT_ID)).thenReturn(project);
        when(userQueryService.getUserById(BOB_ID)).thenReturn(bob);

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            ProjectMember result =
                    projectMemberService.addMember(PROJECT_ID, BOB_ID, ProjectRole.EDITOR);

            assertThat(result.getUser()).isEqualTo(bob);
            assertThat(result.getRole()).isEqualTo(ProjectRole.EDITOR);
            assertThat(project.getMembers()).contains(result);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    @Test
    void addMember_existingMember_throwsException() {
        ProjectMember existing = new ProjectMember(project, bob, ProjectRole.VIEWER);
        project.getMembers().add(existing);
        when(projectQueryService.getProjectById(PROJECT_ID)).thenReturn(project);
        when(userQueryService.getUserById(BOB_ID)).thenReturn(bob);
        when(messages.get("project.member.alreadyExists")).thenReturn("Already a member");

        assertThatThrownBy(
                        () ->
                                projectMemberService.addMember(
                                        PROJECT_ID, BOB_ID, ProjectRole.EDITOR))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── removeMember ─────────────────────────────────────────────────────

    @Test
    void removeMember_lastOwner_throwsException() {
        ProjectMember ownerMember = new ProjectMember(project, alice, ProjectRole.OWNER);
        project.getMembers().add(ownerMember);
        when(projectQueryService.getProjectById(PROJECT_ID)).thenReturn(project);
        when(messages.get("project.member.lastOwner.remove"))
                .thenReturn("Cannot remove the last owner.");

        assertThatThrownBy(() -> projectMemberService.removeMember(PROJECT_ID, ALICE_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void removeMember_nonOwner_succeeds() {
        ProjectMember memberShip = new ProjectMember(project, bob, ProjectRole.EDITOR);
        project.getMembers().add(memberShip);
        when(projectQueryService.getProjectById(PROJECT_ID)).thenReturn(project);

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            projectMemberService.removeMember(PROJECT_ID, BOB_ID);

            assertThat(project.getMembers()).doesNotContain(memberShip);
            verify(taskService).unassignTasksInProject(bob, PROJECT_ID);
            verify(pinnedItemService).deleteByUserAndProject(BOB_ID, PROJECT_ID);
            verify(recentViewService).deleteByUserAndProject(BOB_ID, PROJECT_ID);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    // ── updateMemberRole ─────────────────────────────────────────────────

    @Test
    void updateMemberRole_lastOwnerDemoted_throwsException() {
        ProjectMember ownerMember = new ProjectMember(project, alice, ProjectRole.OWNER);
        project.getMembers().add(ownerMember);
        when(projectQueryService.getProjectById(PROJECT_ID)).thenReturn(project);
        when(messages.get("project.member.lastOwner.demote"))
                .thenReturn("Cannot demote the last owner.");

        assertThatThrownBy(
                        () ->
                                projectMemberService.updateMemberRole(
                                        PROJECT_ID, ALICE_ID, ProjectRole.EDITOR))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateMemberRole_succeeds() {
        ProjectMember memberShip = new ProjectMember(project, bob, ProjectRole.VIEWER);
        project.getMembers().add(memberShip);
        when(projectQueryService.getProjectById(PROJECT_ID)).thenReturn(project);

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            projectMemberService.updateMemberRole(PROJECT_ID, BOB_ID, ProjectRole.EDITOR);

            assertThat(memberShip.getRole()).isEqualTo(ProjectRole.EDITOR);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    @Test
    void updateMemberRole_demoteToViewer_unassignsTasks() {
        ProjectMember memberShip = new ProjectMember(project, bob, ProjectRole.EDITOR);
        project.getMembers().add(memberShip);
        when(projectQueryService.getProjectById(PROJECT_ID)).thenReturn(project);
        when(userQueryService.getUserById(BOB_ID)).thenReturn(bob);

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            projectMemberService.updateMemberRole(PROJECT_ID, BOB_ID, ProjectRole.VIEWER);

            assertThat(memberShip.getRole()).isEqualTo(ProjectRole.VIEWER);
            verify(taskService).unassignTasksInProject(bob, PROJECT_ID);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }
}
