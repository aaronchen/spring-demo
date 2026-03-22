package cc.desuka.demo.model;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

public enum TaskStatusFilter {
    ALL,
    BACKLOG,
    OPEN,
    IN_PROGRESS,
    IN_REVIEW,
    COMPLETED,
    CANCELLED;

    public static final String DEFAULT = "ALL";

    public static TaskStatusFilter from(String value) {
        if (value == null) return ALL;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ALL;
        }
    }

    /**
     * Auto-registered by Spring Boot — binds ?statusFilter=completed → TaskStatusFilter.COMPLETED
     */
    @Component
    public static class StringConverter implements Converter<String, TaskStatusFilter> {
        @Override
        public TaskStatusFilter convert(String source) {
            return TaskStatusFilter.from(source);
        }
    }
}
