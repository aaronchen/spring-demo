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

    private final TaskQueryService taskQueryService;
    private final ProjectQueryService projectQueryService;
    private final AuditLogService auditLogService;
    private final PresenceService presenceService;

    public DashboardService(
            TaskQueryService taskQueryService,
            ProjectQueryService projectQueryService,
            AuditLogService auditLogService,
            PresenceService presenceService) {
        this.taskQueryService = taskQueryService;
        this.projectQueryService = projectQueryService;
        this.auditLogService = auditLogService;
        this.presenceService = presenceService;
    }

    /**
     * @param accessibleProjectIds null = admin (show all); non-null = scoped to these projects
     */
    public DashboardStats buildStats(User user, List<Long> accessibleProjectIds) {
        long myOpen = taskQueryService.countByUserAndStatus(user, TaskStatus.OPEN);
        long myInProgress = taskQueryService.countByUserAndStatus(user, TaskStatus.IN_PROGRESS);
        long myInReview = taskQueryService.countByUserAndStatus(user, TaskStatus.IN_REVIEW);
        long myCompleted = taskQueryService.countByUserAndStatus(user, TaskStatus.COMPLETED);
        long myOverdue = taskQueryService.countByUserOverdue(user);
        long myTotal = myOpen + myInProgress + myInReview + myCompleted;

        boolean isAdmin = AuthExpressions.isAdmin(user);

        // Per-project summaries
        List<Project> userProjects;
        if (isAdmin) {
            userProjects = projectQueryService.getActiveProjects();
        } else {
            userProjects = projectQueryService.getProjectsForUser(user.getId());
        }
        List<ProjectSummary> projectSummaries =
                userProjects.stream().map(this::buildProjectSummary).toList();

        // Editable projects for "New Task" button
        List<Project> editableProjects;
        if (isAdmin) {
            editableProjects = userProjects;
        } else {
            editableProjects = projectQueryService.getEditableProjectsForUser(user.getId());
        }

        // System-wide stats (admin only)
        long totalTasks = 0;
        long totalOpen = 0;
        long totalCompleted = 0;
        long totalOverdue = 0;
        if (isAdmin) {
            totalTasks = taskQueryService.countAll();
            totalOpen = taskQueryService.countByStatus(TaskStatus.OPEN);
            totalCompleted = taskQueryService.countByStatus(TaskStatus.COMPLETED);
            totalOverdue = taskQueryService.countOverdue();
        }

        List<AuditLog> activity = auditLogService.getRecentByActions(TASK_ACTIONS);
        List<Long> taskIds =
                activity.stream()
                        .map(AuditLog::getEntityId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        Map<Long, String> activityTaskTitles = taskQueryService.getTitlesByIds(taskIds);

        return new DashboardStats(
                myOpen,
                myInProgress,
                myInReview,
                myCompleted,
                myOverdue,
                myTotal,
                taskQueryService.getRecentTasksByUser(user),
                taskQueryService.getDueSoon(user),
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
        long open = taskQueryService.countByStatusForProject(pid, TaskStatus.OPEN);
        long inProgress = taskQueryService.countByStatusForProject(pid, TaskStatus.IN_PROGRESS);
        long inReview = taskQueryService.countByStatusForProject(pid, TaskStatus.IN_REVIEW);
        long completed = taskQueryService.countByStatusForProject(pid, TaskStatus.COMPLETED);
        long overdue = taskQueryService.countOverdueForProject(pid);
        long total = taskQueryService.countForProject(pid);
        return ProjectSummary.of(project, open, inProgress, inReview, completed, overdue, total);
    }
}
