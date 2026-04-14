package cc.desuka.demo.dto;

import cc.desuka.demo.model.TaskStatus;
import java.time.LocalDate;
import java.util.UUID;

/** Typed projection records for analytics aggregate queries. Replaces raw {@code Object[]}. */
public final class AnalyticsProjection {

    private AnalyticsProjection() {}

    /** Common accessor for user-scoped projections. */
    public sealed interface UserScoped permits UserStatusCount, UserCount {
        UUID userId();
    }

    public record ProjectStatusCount(UUID projectId, TaskStatus status, long count) {}

    public record ProjectCount(UUID projectId, long count) {}

    public record UserStatusCount(UUID userId, TaskStatus status, long count)
            implements UserScoped {}

    public record UserCount(UUID userId, long count) implements UserScoped {}

    public record DailyCount(LocalDate date, long value) {}
}
