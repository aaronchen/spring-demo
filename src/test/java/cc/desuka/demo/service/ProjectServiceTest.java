package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.ProjectMember;
import cc.desuka.demo.model.ProjectRole;
import cc.desuka.demo.model.ProjectStatus;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.ProjectRepository;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.util.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectQueryService projectQueryService;
    @Mock private UserService userService;
    @Mock private TaskQueryService taskQueryService;
    @Mock private SprintService sprintService;
    @Mock private RecurringTaskTemplateService recurringTaskTemplateService;
    @Mock private RecentViewService recentViewService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private Messages messages;

    @InjectMocks private ProjectService projectService;

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

    // ── createProject ─────────────────────────────────────────────────────

    @Test
    void createProject_setsCreatorAsOwner_publishesAuditEvent() {
        when(projectRepository.save(any(Project.class)))
                .thenAnswer(
                        inv -> {
                            Project p = inv.getArgument(0);
                            p.setId(1L);
                            return p;
                        });

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            Project newProject = new Project("New", "Desc");
            Project result = projectService.createProject(newProject, alice);

            assertThat(result.getCreatedBy()).isEqualTo(alice);
            assertThat(result.getStatus()).isEqualTo(ProjectStatus.ACTIVE);

            // Creator added as OWNER via cascade
            assertThat(result.getMembers()).hasSize(1);
            ProjectMember owner = result.getMembers().iterator().next();
            assertThat(owner.getRole()).isEqualTo(ProjectRole.OWNER);
            assertThat(owner.getUser()).isEqualTo(alice);

            // Audit event published
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    // ── updateProject ─────────────────────────────────────────────────────

    @Test
    void updateProject_changesNameAndDescription() {
        when(projectQueryService.getProjectById(1L)).thenReturn(project);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            Project projectDetails = new Project("Updated Name", "Updated Description");
            Project result = projectService.updateProject(1L, projectDetails);

            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(result.getDescription()).isEqualTo("Updated Description");
        }
    }

    // ── archiveProject ────────────────────────────────────────────────────

    @Test
    void archiveProject_setsStatusToArchived() {
        when(projectQueryService.getProjectById(1L)).thenReturn(project);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            projectService.archiveProject(1L);

            assertThat(project.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    // ── deleteProject ─────────────────────────────────────────────────────

    @Test
    void deleteProject_deletesAndPublishesEvent() {
        when(projectQueryService.getProjectById(1L)).thenReturn(project);

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            projectService.deleteProject(1L);

            verify(projectRepository).delete(project);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    @Test
    void deleteProject_withCompletedTasks_throwsException() {
        Task completedTask = new Task();
        completedTask.setStatus(TaskStatus.COMPLETED);
        project.getTasks().add(completedTask);
        when(projectQueryService.getProjectById(1L)).thenReturn(project);
        when(messages.get("project.delete.hasCompletedTasks"))
                .thenReturn("Cannot delete a project with completed tasks.");

        assertThatThrownBy(() -> projectService.deleteProject(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deleteProject_withOpenTasks_succeeds() {
        Task openTask = new Task();
        openTask.setStatus(TaskStatus.OPEN);
        project.getTasks().add(openTask);
        when(projectQueryService.getProjectById(1L)).thenReturn(project);

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            projectService.deleteProject(1L);

            verify(projectRepository).delete(project);
        }
    }

    // ── Member management ─────────────────────────────────────────────────

    @Test
    void addMember_newMember_addedToCollection() {
        when(projectQueryService.getProjectById(1L)).thenReturn(project);
        when(userService.getUserById(2L)).thenReturn(bob);

        ProjectMember result = projectService.addMember(1L, 2L, ProjectRole.EDITOR);

        assertThat(result.getUser()).isEqualTo(bob);
        assertThat(result.getRole()).isEqualTo(ProjectRole.EDITOR);
        assertThat(project.getMembers()).contains(result);
    }

    @Test
    void addMember_existingMember_throwsException() {
        ProjectMember existing = new ProjectMember(project, bob, ProjectRole.VIEWER);
        project.getMembers().add(existing);
        when(projectQueryService.getProjectById(1L)).thenReturn(project);
        when(userService.getUserById(2L)).thenReturn(bob);
        when(messages.get("project.member.alreadyExists")).thenReturn("Already a member");

        assertThatThrownBy(() -> projectService.addMember(1L, 2L, ProjectRole.EDITOR))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void removeMember_lastOwner_throwsException() {
        ProjectMember ownerMember = new ProjectMember(project, alice, ProjectRole.OWNER);
        project.getMembers().add(ownerMember);
        when(projectQueryService.getProjectById(1L)).thenReturn(project);
        when(messages.get("project.member.lastOwner.remove"))
                .thenReturn("Cannot remove the last owner.");

        assertThatThrownBy(() -> projectService.removeMember(1L, 1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void removeMember_nonOwner_succeeds() {
        ProjectMember memberShip = new ProjectMember(project, bob, ProjectRole.EDITOR);
        project.getMembers().add(memberShip);
        when(projectQueryService.getProjectById(1L)).thenReturn(project);

        projectService.removeMember(1L, 2L);

        assertThat(project.getMembers()).doesNotContain(memberShip);
    }

    @Test
    void updateMemberRole_lastOwnerDemoted_throwsException() {
        ProjectMember ownerMember = new ProjectMember(project, alice, ProjectRole.OWNER);
        project.getMembers().add(ownerMember);
        when(projectQueryService.getProjectById(1L)).thenReturn(project);
        when(messages.get("project.member.lastOwner.demote"))
                .thenReturn("Cannot demote the last owner.");

        assertThatThrownBy(() -> projectService.updateMemberRole(1L, 1L, ProjectRole.EDITOR))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateMemberRole_succeeds() {
        ProjectMember memberShip = new ProjectMember(project, bob, ProjectRole.VIEWER);
        project.getMembers().add(memberShip);
        when(projectQueryService.getProjectById(1L)).thenReturn(project);

        projectService.updateMemberRole(1L, 2L, ProjectRole.EDITOR);

        assertThat(memberShip.getRole()).isEqualTo(ProjectRole.EDITOR);
        verify(taskQueryService, never()).unassignTasksInProject(any(), any());
    }

    @Test
    void updateMemberRole_demoteToViewer_unassignsTasks() {
        ProjectMember memberShip = new ProjectMember(project, bob, ProjectRole.EDITOR);
        project.getMembers().add(memberShip);
        when(projectQueryService.getProjectById(1L)).thenReturn(project);
        when(userService.getUserById(2L)).thenReturn(bob);

        projectService.updateMemberRole(1L, 2L, ProjectRole.VIEWER);

        assertThat(memberShip.getRole()).isEqualTo(ProjectRole.VIEWER);
        verify(taskQueryService).unassignTasksInProject(bob, 1L);
    }
}
