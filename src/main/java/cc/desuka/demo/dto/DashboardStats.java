package cc.desuka.demo.dto;

import cc.desuka.demo.model.AuditLog;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.Task;
import java.util.List;
import java.util.Map;

public record DashboardStats(
        // Personal stats
        long myOpen,
        long myInProgress,
        long myInReview,
        long myCompleted,
        long myOverdue,
        long myTotal,
        List<Task> myRecentTasks,
        List<Task> dueThisWeek,

        // Per-project summaries
        List<ProjectSummary> projectSummaries,

        // System-wide stats (admin only)
        long totalTasks,
        long totalOpen,
        long totalCompleted,
        long totalOverdue,
        int onlineCount,
        List<AuditLog> recentActivity,
        Map<Long, String> activityTaskTitles,

        // Editable projects for "New Task" button
        List<Project> editableProjects) {}
