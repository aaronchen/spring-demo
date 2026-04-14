package cc.desuka.demo.service;

import cc.desuka.demo.dto.AnalyticsProjection;
import cc.desuka.demo.dto.AnalyticsProjection.DailyCount;
import cc.desuka.demo.dto.AnalyticsProjection.ProjectCount;
import cc.desuka.demo.dto.AnalyticsProjection.ProjectStatusCount;
import cc.desuka.demo.dto.AnalyticsProjection.UserCount;
import cc.desuka.demo.dto.AnalyticsProjection.UserStatusCount;
import cc.desuka.demo.dto.AnalyticsResponse;
import cc.desuka.demo.dto.AnalyticsResponse.BurndownPoint;
import cc.desuka.demo.dto.AnalyticsResponse.EffortDistribution;
import cc.desuka.demo.dto.AnalyticsResponse.OverdueAnalysis;
import cc.desuka.demo.dto.AnalyticsResponse.PriorityBreakdown;
import cc.desuka.demo.dto.AnalyticsResponse.StatusBreakdown;
import cc.desuka.demo.dto.AnalyticsResponse.VelocityPoint;
import cc.desuka.demo.dto.AnalyticsResponse.WorkloadDistribution;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Sprint;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.repository.AnalyticsRepository;
import cc.desuka.demo.repository.TaskRepository;
import cc.desuka.demo.repository.TaskSpecifications;
import cc.desuka.demo.util.Messages;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only analytics aggregations for charts and dashboards. */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final int BURNDOWN_DAYS = 30;
    private static final int VELOCITY_WEEKS = 12;

    private final TaskRepository taskRepository;
    private final AnalyticsRepository analyticsRepository;
    private final SprintQueryService sprintQueryService;
    private final UserQueryService userQueryService;
    private final Messages messages;

    public AnalyticsService(
            TaskRepository taskRepository,
            AnalyticsRepository analyticsRepository,
            SprintQueryService sprintQueryService,
            UserQueryService userQueryService,
            Messages messages) {
        this.taskRepository = taskRepository;
        this.analyticsRepository = analyticsRepository;
        this.sprintQueryService = sprintQueryService;
        this.userQueryService = userQueryService;
        this.messages = messages;
    }

    public AnalyticsResponse getProjectAnalytics(UUID projectId) {
        return buildAnalytics(projectId, null, null);
    }

    public AnalyticsResponse getProjectAnalytics(UUID projectId, Long sprintId) {
        return buildAnalytics(projectId, null, sprintId);
    }

    /**
     * @param accessibleProjectIds null = admin (all projects); non-null = scoped
     */
    public AnalyticsResponse getCrossProjectAnalytics(List<UUID> accessibleProjectIds) {
        return buildAnalytics(null, accessibleProjectIds, null);
    }

    private AnalyticsResponse buildAnalytics(UUID projectId, List<UUID> projectIds, Long sprintId) {
        return new AnalyticsResponse(
                buildStatusBreakdown(projectId, projectIds, sprintId),
                buildPriorityBreakdown(projectId, projectIds, sprintId),
                buildWorkloadDistribution(projectId, projectIds, sprintId),
                buildBurndown(projectId, projectIds, sprintId),
                buildVelocity(projectId, projectIds, sprintId),
                buildOverdueAnalysis(projectId, projectIds, sprintId),
                buildEffortDistribution(projectId, projectIds, sprintId));
    }

    // ── Status Breakdown ─────────────────────────────────────────────────

    private StatusBreakdown buildStatusBreakdown(
            UUID projectId, List<UUID> projectIds, Long sprintId) {
        Specification<cc.desuka.demo.model.Task> scope =
                projectScope(projectId, projectIds).and(TaskSpecifications.withSprintId(sprintId));
        Map<String, Long> counts = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            long count =
                    taskRepository.count(
                            scope.and((root, query, cb) -> cb.equal(root.get("status"), status)));
            counts.put(status.name(), count);
        }
        return new StatusBreakdown(counts);
    }

    // ── Priority Breakdown ───────────────────────────────────────────────

    private PriorityBreakdown buildPriorityBreakdown(
            UUID projectId, List<UUID> projectIds, Long sprintId) {
        Specification<cc.desuka.demo.model.Task> scope =
                projectScope(projectId, projectIds).and(TaskSpecifications.withSprintId(sprintId));
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Priority priority : Priority.values()) {
            long count =
                    taskRepository.count(
                            scope.and(
                                    (root, query, cb) -> cb.equal(root.get("priority"), priority)));
            counts.put(priority.name(), count);
        }
        return new PriorityBreakdown(counts);
    }

    // ── Workload Distribution ────────────────────────────────────────────

    private WorkloadDistribution buildWorkloadDistribution(
            UUID projectId, List<UUID> projectIds, Long sprintId) {
        List<UserStatusCount> rows =
                analyticsRepository.countByUserAndStatus(projectId, projectIds, sprintId);

        // Collect unique user IDs preserving order, null = unassigned
        Set<UUID> seenIds = new LinkedHashSet<>();
        for (UserStatusCount row : rows) {
            seenIds.add(row.userId());
        }
        List<UUID> userIds = new ArrayList<>(seenIds);

        // Resolve names
        Map<UUID, String> nameMap = buildUserNameMap(rows);
        List<String> assigneeNames = new ArrayList<>();
        Map<UUID, Integer> userIndex = new LinkedHashMap<>();
        for (int i = 0; i < userIds.size(); i++) {
            UUID uid = userIds.get(i);
            userIndex.put(uid, i);
            assigneeNames.add(resolveUserName(uid, nameMap));
        }

        // Build status counts: status -> [count per assignee]
        Map<String, List<Long>> statusCounts = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            List<Long> counts = new ArrayList<>();
            for (int i = 0; i < assigneeNames.size(); i++) {
                counts.add(0L);
            }
            statusCounts.put(status.name(), counts);
        }

        for (UserStatusCount row : rows) {
            int idx = userIndex.get(row.userId());
            statusCounts.get(row.status().name()).set(idx, row.count());
        }

        return new WorkloadDistribution(assigneeNames, statusCounts);
    }

    // ── Burndown Chart ───────────────────────────────────────────────────

    private List<BurndownPoint> buildBurndown(
            UUID projectId, List<UUID> projectIds, Long sprintId) {
        // When scoped to a sprint, use the sprint's date range instead of rolling 30 days
        LocalDate startDate;
        LocalDate endDate;
        if (sprintId != null) {
            Sprint sprint = sprintQueryService.getSprintById(sprintId);
            startDate = sprint.getStartDate();
            endDate = sprint.isActive() ? LocalDate.now() : sprint.getEndDate();
        } else {
            startDate = LocalDate.now().minusDays(BURNDOWN_DAYS);
            endDate = LocalDate.now();
        }
        LocalDateTime from = startDate.atStartOfDay();
        List<TaskStatus> terminal = TaskStatus.terminalStatuses();

        long initial =
                analyticsRepository.countOpenAtDate(
                        projectId, projectIds, sprintId, from, terminal);

        Map<LocalDate, Long> createdByDay =
                toDateMap(
                        analyticsRepository.countCreatedPerDay(
                                projectId, projectIds, sprintId, from));
        Map<LocalDate, Long> completedByDay =
                toDateMap(
                        analyticsRepository.countCompletedPerDay(
                                projectId, projectIds, sprintId, from));

        List<BurndownPoint> points = new ArrayList<>();
        long remaining = initial;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            remaining += createdByDay.getOrDefault(date, 0L);
            remaining -= completedByDay.getOrDefault(date, 0L);
            points.add(new BurndownPoint(date, remaining));
        }

        return points;
    }

    // ── Velocity ─────────────────────────────────────────────────────────

    private List<VelocityPoint> buildVelocity(
            UUID projectId, List<UUID> projectIds, Long sprintId) {
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.minusWeeks(VELOCITY_WEEKS).with(DayOfWeek.MONDAY);
        LocalDateTime from = weekStart.atStartOfDay();

        List<DailyCount> dailyCompleted =
                analyticsRepository.countCompletedPerDay(projectId, projectIds, sprintId, from);
        List<DailyCount> dailyEffort =
                analyticsRepository.sumEffortCompletedPerDay(projectId, projectIds, sprintId, from);

        // Bucket by ISO week (Monday)
        Map<LocalDate, Long> weeklyTotals = new TreeMap<>();
        Map<LocalDate, Long> weeklyEffort = new TreeMap<>();
        for (LocalDate w = weekStart;
                !w.isAfter(now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
                w = w.plusWeeks(1)) {
            weeklyTotals.put(w, 0L);
            weeklyEffort.put(w, 0L);
        }

        for (DailyCount row : dailyCompleted) {
            LocalDate monday = row.date().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            weeklyTotals.merge(monday, row.value(), (a, b) -> a + b);
        }

        for (DailyCount row : dailyEffort) {
            LocalDate monday = row.date().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            weeklyEffort.merge(monday, row.value(), (a, b) -> a + b);
        }

        return weeklyTotals.entrySet().stream()
                .map(
                        e -> {
                            Long effort = weeklyEffort.getOrDefault(e.getKey(), 0L);
                            return new VelocityPoint(
                                    e.getKey(), e.getValue(), effort > 0 ? effort : null);
                        })
                .toList();
    }

    // ── Overdue Analysis ─────────────────────────────────────────────────

    private OverdueAnalysis buildOverdueAnalysis(
            UUID projectId, List<UUID> projectIds, Long sprintId) {
        List<TaskStatus> terminal = TaskStatus.terminalStatuses();
        List<UserCount> rows =
                analyticsRepository.countOverdueByUser(projectId, projectIds, sprintId, terminal);

        Map<UUID, String> nameMap = buildUserNameMap(rows);
        List<String> assignees = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (UserCount row : rows) {
            assignees.add(resolveUserName(row.userId(), nameMap));
            counts.add(row.count());
        }

        return new OverdueAnalysis(assignees, counts);
    }

    // ── Effort Distribution ─────────────────────────────────────────────

    private EffortDistribution buildEffortDistribution(
            UUID projectId, List<UUID> projectIds, Long sprintId) {
        List<UserCount> rows = analyticsRepository.sumEffortByUser(projectId, projectIds, sprintId);

        Map<UUID, String> nameMap = buildUserNameMap(rows);
        List<String> assignees = new ArrayList<>();
        List<Long> efforts = new ArrayList<>();
        for (UserCount row : rows) {
            assignees.add(resolveUserName(row.userId(), nameMap));
            efforts.add(row.count());
        }

        return new EffortDistribution(assignees, efforts);
    }

    // ── Dashboard aggregates ─────────────────────────────────────────────

    public Map<UUID, Map<TaskStatus, Long>> countByProjectAndStatus(List<UUID> projectIds) {
        Map<UUID, Map<TaskStatus, Long>> result = new LinkedHashMap<>();
        for (ProjectStatusCount row : analyticsRepository.countByProjectAndStatus(projectIds)) {
            result.computeIfAbsent(row.projectId(), k -> new LinkedHashMap<>())
                    .put(row.status(), row.count());
        }
        return result;
    }

    public Map<UUID, Long> countOverdueByProject(List<UUID> projectIds) {
        Map<UUID, Long> result = new LinkedHashMap<>();
        for (ProjectCount row :
                analyticsRepository.countOverdueByProject(
                        projectIds, TaskStatus.terminalStatuses())) {
            result.put(row.projectId(), row.count());
        }
        return result;
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Map<UUID, String> buildUserNameMap(
            List<? extends AnalyticsProjection.UserScoped> rows) {
        List<UUID> userIds =
                rows.stream().map(AnalyticsProjection.UserScoped::userId).distinct().toList();
        return userQueryService.getNamesByIds(userIds);
    }

    private String resolveUserName(UUID userId, Map<UUID, String> nameMap) {
        if (userId == null) {
            return messages.get("analytics.label.unassigned");
        }
        return nameMap.getOrDefault(userId, messages.get("analytics.label.unassigned"));
    }

    private Specification<cc.desuka.demo.model.Task> projectScope(
            UUID projectId, List<UUID> projectIds) {
        if (projectId != null) {
            return TaskSpecifications.withProjectId(projectId);
        } else if (projectIds != null) {
            return TaskSpecifications.withProjectIds(projectIds);
        }
        return (root, query, cb) -> cb.conjunction();
    }

    private Map<LocalDate, Long> toDateMap(List<DailyCount> rows) {
        Map<LocalDate, Long> map = new LinkedHashMap<>();
        for (DailyCount row : rows) {
            map.put(row.date(), row.value());
        }
        return map;
    }
}
