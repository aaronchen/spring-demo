package cc.desuka.demo.service;

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
import cc.desuka.demo.model.User;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final int BURNDOWN_DAYS = 30;
    private static final int VELOCITY_WEEKS = 12;

    private final TaskRepository taskRepository;
    private final AnalyticsRepository analyticsRepository;
    private final SprintQueryService sprintQueryService;
    private final UserService userService;
    private final Messages messages;

    public AnalyticsService(
            TaskRepository taskRepository,
            AnalyticsRepository analyticsRepository,
            SprintQueryService sprintQueryService,
            UserService userService,
            Messages messages) {
        this.taskRepository = taskRepository;
        this.analyticsRepository = analyticsRepository;
        this.sprintQueryService = sprintQueryService;
        this.userService = userService;
        this.messages = messages;
    }

    public AnalyticsResponse getProjectAnalytics(Long projectId) {
        return buildAnalytics(projectId, null, null);
    }

    public AnalyticsResponse getProjectAnalytics(Long projectId, Long sprintId) {
        return buildAnalytics(projectId, null, sprintId);
    }

    /**
     * @param accessibleProjectIds null = admin (all projects); non-null = scoped
     */
    public AnalyticsResponse getCrossProjectAnalytics(List<Long> accessibleProjectIds) {
        return buildAnalytics(null, accessibleProjectIds, null);
    }

    private AnalyticsResponse buildAnalytics(Long projectId, List<Long> projectIds, Long sprintId) {
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
            Long projectId, List<Long> projectIds, Long sprintId) {
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
            Long projectId, List<Long> projectIds, Long sprintId) {
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
            Long projectId, List<Long> projectIds, Long sprintId) {
        List<Object[]> rows =
                analyticsRepository.countByUserAndStatus(projectId, projectIds, sprintId);

        // Collect unique user IDs preserving order, null = unassigned
        List<Long> userIds = new ArrayList<>();
        for (Object[] row : rows) {
            Long userId = (Long) row[0];
            if (!userIds.contains(userId)) {
                userIds.add(userId);
            }
        }

        // Resolve names
        Map<Long, String> nameMap = buildUserNameMap(rows, 0);
        List<String> assigneeNames = new ArrayList<>();
        Map<Long, Integer> userIndex = new LinkedHashMap<>();
        for (int i = 0; i < userIds.size(); i++) {
            Long uid = userIds.get(i);
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

        for (Object[] row : rows) {
            Long userId = (Long) row[0];
            TaskStatus status = (TaskStatus) row[1];
            Long count = (Long) row[2];
            int idx = userIndex.get(userId);
            statusCounts.get(status.name()).set(idx, count);
        }

        return new WorkloadDistribution(assigneeNames, statusCounts);
    }

    // ── Burndown Chart ───────────────────────────────────────────────────

    private List<BurndownPoint> buildBurndown(
            Long projectId, List<Long> projectIds, Long sprintId) {
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
            Long projectId, List<Long> projectIds, Long sprintId) {
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.minusWeeks(VELOCITY_WEEKS).with(DayOfWeek.MONDAY);
        LocalDateTime from = weekStart.atStartOfDay();

        List<Object[]> dailyCompleted =
                analyticsRepository.countCompletedPerDay(projectId, projectIds, sprintId, from);
        List<Object[]> dailyEffort =
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

        for (Object[] row : dailyCompleted) {
            LocalDate date = (LocalDate) row[0];
            Long count = (Long) row[1];
            LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            weeklyTotals.merge(monday, count, (a, b) -> a + b);
        }

        for (Object[] row : dailyEffort) {
            LocalDate date = (LocalDate) row[0];
            Long effort = (Long) row[1];
            LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            weeklyEffort.merge(monday, effort, (a, b) -> a + b);
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
            Long projectId, List<Long> projectIds, Long sprintId) {
        List<TaskStatus> terminal = TaskStatus.terminalStatuses();
        List<Object[]> rows =
                analyticsRepository.countOverdueByUser(projectId, projectIds, sprintId, terminal);

        Map<Long, String> nameMap = buildUserNameMap(rows, 0);
        List<String> assignees = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (Object[] row : rows) {
            Long userId = (Long) row[0];
            Long count = (Long) row[1];
            assignees.add(resolveUserName(userId, nameMap));
            counts.add(count);
        }

        return new OverdueAnalysis(assignees, counts);
    }

    // ── Effort Distribution ─────────────────────────────────────────────

    private EffortDistribution buildEffortDistribution(
            Long projectId, List<Long> projectIds, Long sprintId) {
        List<Object[]> rows = analyticsRepository.sumEffortByUser(projectId, projectIds, sprintId);

        Map<Long, String> nameMap = buildUserNameMap(rows, 0);
        List<String> assignees = new ArrayList<>();
        List<Long> efforts = new ArrayList<>();
        for (Object[] row : rows) {
            Long userId = (Long) row[0];
            Long effort = (Long) row[1];
            assignees.add(resolveUserName(userId, nameMap));
            efforts.add(effort);
        }

        return new EffortDistribution(assignees, efforts);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Map<Long, String> buildUserNameMap(List<Object[]> rows, int userIdIndex) {
        List<Long> userIds =
                rows.stream()
                        .map(row -> (Long) row[userIdIndex])
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        return userService.getAllUsers().stream()
                .filter(u -> userIds.contains(u.getId()))
                .collect(Collectors.toMap(User::getId, User::getName));
    }

    private String resolveUserName(Long userId, Map<Long, String> nameMap) {
        if (userId == null) {
            return messages.get("analytics.label.unassigned");
        }
        return nameMap.getOrDefault(userId, messages.get("analytics.label.unassigned"));
    }

    private Specification<cc.desuka.demo.model.Task> projectScope(
            Long projectId, List<Long> projectIds) {
        if (projectId != null) {
            return TaskSpecifications.withProjectId(projectId);
        } else if (projectIds != null) {
            return TaskSpecifications.withProjectIds(projectIds);
        }
        return (root, query, cb) -> cb.conjunction();
    }

    private Map<LocalDate, Long> toDateMap(List<Object[]> rows) {
        Map<LocalDate, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.put((LocalDate) row[0], (Long) row[1]);
        }
        return map;
    }
}
