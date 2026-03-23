package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.audit.AuditLogService;
import cc.desuka.demo.dto.DashboardStats;
import cc.desuka.demo.dto.ProjectSummary;
import cc.desuka.demo.model.AuditLog;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.User;
import cc.desuka.demo.presence.PresenceService;
import cc.desuka.demo.security.AuthExpressions;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private static final List<String> TASK_ACTIONS =
            List.of(AuditEvent.TASK_CREATED, AuditEvent.TASK_UPDATED, AuditEvent.TASK_DELETED);

    private final TaskService taskService;
    private final ProjectService projectService;
    private final AuditLogService auditLogService;
    private final PresenceService presenceService;

    public DashboardService(
            TaskService taskService,
            ProjectService projectService,
            AuditLogService auditLogService,
            PresenceService presenceService) {
        this.taskService = taskService;
        this.projectService = projectService;
        this.auditLogService = auditLogService;
        this.presenceService = presenceService;
    }

    /**
     * @param accessibleProjectIds null = admin (show all); non-null = scoped to these projects
     */
    public DashboardStats buildStats(User user, List<Long> accessibleProjectIds) {
        long myOpen = taskService.countByUserAndStatus(user, TaskStatus.OPEN);
        long myInProgress = taskService.countByUserAndStatus(user, TaskStatus.IN_PROGRESS);
        long myInReview = taskService.countByUserAndStatus(user, TaskStatus.IN_REVIEW);
        long myCompleted = taskService.countByUserAndStatus(user, TaskStatus.COMPLETED);
        long myOverdue = taskService.countByUserOverdue(user);
        long myTotal = myOpen + myInProgress + myInReview + myCompleted;

        boolean isAdmin = AuthExpressions.isAdmin(user);

        // Per-project summaries
        List<Project> userProjects;
        if (isAdmin) {
            userProjects = projectService.getActiveProjects();
        } else {
            userProjects = projectService.getProjectsForUser(user.getId());
        }
        List<ProjectSummary> projectSummaries =
                userProjects.stream().map(this::buildProjectSummary).toList();

        // Editable projects for "New Task" button
        List<Project> editableProjects;
        if (isAdmin) {
            editableProjects = userProjects;
        } else {
            editableProjects = projectService.getEditableProjectsForUser(user.getId());
        }

        // System-wide stats (admin only)
        long totalTasks = 0;
        long totalOpen = 0;
        long totalCompleted = 0;
        long totalOverdue = 0;
        if (isAdmin) {
            totalTasks = taskService.countAll();
            totalOpen = taskService.countByStatus(TaskStatus.OPEN);
            totalCompleted = taskService.countByStatus(TaskStatus.COMPLETED);
            totalOverdue = taskService.countOverdue();
        }

        List<AuditLog> activity = auditLogService.getRecentByActions(TASK_ACTIONS);
        List<Long> taskIds =
                activity.stream()
                        .map(AuditLog::getEntityId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        Map<Long, String> activityTaskTitles = taskService.getTitlesByIds(taskIds);

        return new DashboardStats(
                myOpen,
                myInProgress,
                myInReview,
                myCompleted,
                myOverdue,
                myTotal,
                taskService.getRecentTasksByUser(user),
                taskService.getDueSoon(user),
                projectSummaries,
                totalTasks,
                totalOpen,
                totalCompleted,
                totalOverdue,
                presenceService.getOnlineCount(),
                activity,
                activityTaskTitles,
                editableProjects);
    }

    private ProjectSummary buildProjectSummary(Project project) {
        Long pid = project.getId();
        long open = taskService.countByStatusForProject(pid, TaskStatus.OPEN);
        long inProgress = taskService.countByStatusForProject(pid, TaskStatus.IN_PROGRESS);
        long inReview = taskService.countByStatusForProject(pid, TaskStatus.IN_REVIEW);
        long completed = taskService.countByStatusForProject(pid, TaskStatus.COMPLETED);
        long overdue = taskService.countOverdueForProject(pid);
        long total = taskService.countForProject(pid);
        return ProjectSummary.of(project, open, inProgress, inReview, completed, overdue, total);
    }
}
