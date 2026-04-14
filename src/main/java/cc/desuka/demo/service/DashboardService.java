package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.audit.AuditLogService;
import cc.desuka.demo.dto.DashboardStats;
import cc.desuka.demo.dto.ProjectSummary;
import cc.desuka.demo.dto.UserTaskCounts;
import cc.desuka.demo.model.AuditLog;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.User;
import cc.desuka.demo.presence.PresenceService;
import cc.desuka.demo.security.AuthExpressions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only dashboard statistics composed from multiple query services. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final List<String> TASK_ACTIONS =
            List.of(AuditEvent.TASK_CREATED, AuditEvent.TASK_UPDATED, AuditEvent.TASK_DELETED);

    private final TaskQueryService taskQueryService;
    private final ProjectQueryService projectQueryService;
    private final AnalyticsService analyticsService;
    private final AuditLogService auditLogService;
    private final PresenceService presenceService;

    public DashboardService(
            TaskQueryService taskQueryService,
            ProjectQueryService projectQueryService,
            AnalyticsService analyticsService,
            AuditLogService auditLogService,
            PresenceService presenceService) {
        this.taskQueryService = taskQueryService;
        this.projectQueryService = projectQueryService;
        this.analyticsService = analyticsService;
        this.auditLogService = auditLogService;
        this.presenceService = presenceService;
    }

    /**
     * @param accessibleProjectIds null = admin (show all); non-null = scoped to these projects
     */
    public DashboardStats buildStats(User user, List<UUID> accessibleProjectIds) {
        UserTaskCounts myCounts = taskQueryService.countsByUser(user);

        boolean isAdmin = AuthExpressions.isAdmin(user);

        // Per-project summaries (2 aggregate queries instead of 6N)
        List<Project> userProjects;
        if (isAdmin) {
            userProjects = projectQueryService.getActiveProjects();
        } else {
            userProjects = projectQueryService.getProjectsForUser(user.getId());
        }
        List<ProjectSummary> projectSummaries = buildProjectSummaries(userProjects);

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
        List<UUID> taskIds =
                activity.stream()
                        .map(AuditLog::getEntityId)
                        .filter(Objects::nonNull)
                        .map(UUID::fromString)
                        .distinct()
                        .toList();
        Map<String, String> activityTaskTitles = new LinkedHashMap<>();
        taskQueryService
                .getTitlesByIds(taskIds)
                .forEach((id, title) -> activityTaskTitles.put(id.toString(), title));

        return new DashboardStats(
                myCounts.open(),
                myCounts.inProgress(),
                myCounts.inReview(),
                myCounts.completed(),
                myCounts.overdue(),
                myCounts.total(),
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

    private List<ProjectSummary> buildProjectSummaries(List<Project> projects) {
        if (projects.isEmpty()) return List.of();

        List<UUID> projectIds = projects.stream().map(Project::getId).toList();
        Map<UUID, Map<TaskStatus, Long>> statusByProject =
                analyticsService.countByProjectAndStatus(projectIds);
        Map<UUID, Long> overdueByProject = analyticsService.countOverdueByProject(projectIds);

        return projects.stream()
                .map(
                        project -> {
                            Map<TaskStatus, Long> counts =
                                    statusByProject.getOrDefault(project.getId(), Map.of());
                            long open = counts.getOrDefault(TaskStatus.OPEN, 0L);
                            long inProgress = counts.getOrDefault(TaskStatus.IN_PROGRESS, 0L);
                            long inReview = counts.getOrDefault(TaskStatus.IN_REVIEW, 0L);
                            long completed = counts.getOrDefault(TaskStatus.COMPLETED, 0L);
                            long overdue = overdueByProject.getOrDefault(project.getId(), 0L);
                            long total = counts.values().stream().mapToLong(Long::longValue).sum();
                            return ProjectSummary.of(
                                    project, open, inProgress, inReview, completed, overdue, total);
                        })
                .toList();
    }
}
