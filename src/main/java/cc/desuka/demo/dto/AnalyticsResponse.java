package cc.desuka.demo.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AnalyticsResponse(
        StatusBreakdown statusBreakdown,
        PriorityBreakdown priorityBreakdown,
        WorkloadDistribution workloadDistribution,
        List<BurndownPoint> burndown,
        List<VelocityPoint> velocity,
        OverdueAnalysis overdueAnalysis) {

    public record StatusBreakdown(Map<String, Long> counts) {}

    public record PriorityBreakdown(Map<String, Long> counts) {}

    public record WorkloadDistribution(
            List<String> assignees, Map<String, List<Long>> statusCounts) {}

    public record BurndownPoint(LocalDate date, long remaining) {}

    public record VelocityPoint(LocalDate weekStart, long completed) {}

    public record OverdueAnalysis(List<String> assignees, List<Long> counts) {}
}
