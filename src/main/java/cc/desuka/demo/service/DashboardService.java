package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.audit.AuditLogService;
import cc.desuka.demo.dto.DashboardStats;
import cc.desuka.demo.model.AuditLog;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.User;
import cc.desuka.demo.presence.PresenceService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private static final List<String> TASK_ACTIONS =
            List.of(AuditEvent.TASK_CREATED, AuditEvent.TASK_UPDATED, AuditEvent.TASK_DELETED);

    private final TaskService taskService;
    private final AuditLogService auditLogService;
    private final PresenceService presenceService;

    public DashboardService(
            TaskService taskService,
            AuditLogService auditLogService,
            PresenceService presenceService) {
        this.taskService = taskService;
        this.auditLogService = auditLogService;
        this.presenceService = presenceService;
    }

    /**
     * @param accessibleProjectIds null = admin (show all); non-null = scoped to these projects
     */
    public DashboardStats buildStats(User user, List<Long> accessibleProjectIds) {
        long myOpen = taskService.countByUserAndStatus(user, TaskStatus.OPEN);
        long myInProgress = taskService.countByUserAndStatus(user, TaskStatus.IN_PROGRESS);
        long myCompleted = taskService.countByUserAndStatus(user, TaskStatus.COMPLETED);
        long myOverdue = taskService.countByUserOverdue(user);
        long myTotal = myOpen + myInProgress + myCompleted;

        long totalTasks;
        long totalOpen;
        long totalCompleted;
        long totalOverdue;
        if (accessibleProjectIds == null) {
            totalTasks = taskService.countAll();
            totalOpen = taskService.countByStatus(TaskStatus.OPEN);
            totalCompleted = taskService.countByStatus(TaskStatus.COMPLETED);
            totalOverdue = taskService.countOverdue();
        } else {
            totalTasks = taskService.countForProjects(accessibleProjectIds);
            totalOpen = taskService.countByStatusForProjects(accessibleProjectIds, TaskStatus.OPEN);
            totalCompleted =
                    taskService.countByStatusForProjects(
                            accessibleProjectIds, TaskStatus.COMPLETED);
            totalOverdue = taskService.countOverdueForProjects(accessibleProjectIds);
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
                myCompleted,
                myOverdue,
                myTotal,
                taskService.getRecentTasksByUser(user),
                taskService.getDueThisWeek(user),
                totalTasks,
                totalOpen,
                totalCompleted,
                totalOverdue,
                presenceService.getOnlineCount(),
                activity,
                activityTaskTitles);
    }
}
