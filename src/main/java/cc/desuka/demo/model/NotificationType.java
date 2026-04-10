package cc.desuka.demo.model;

public enum NotificationType {
    TASK_ASSIGNED,
    TASK_UPDATED,
    COMMENT_ADDED,
    COMMENT_MENTIONED,
    TASK_DUE_REMINDER,
    TASK_OVERDUE,
    SYSTEM;

    public String getIcon() {
        return switch (this) {
            case TASK_ASSIGNED -> "bi-person-plus";
            case TASK_UPDATED -> "bi-pencil-square";
            case COMMENT_ADDED -> "bi-chat-dots";
            case COMMENT_MENTIONED -> "bi-at";
            case TASK_DUE_REMINDER -> "bi-calendar-event";
            case TASK_OVERDUE -> "bi-clock";
            case SYSTEM -> "bi-megaphone";
        };
    }

    public String getCssClass() {
        return switch (this) {
            case TASK_ASSIGNED, TASK_UPDATED -> "text-primary";
            case COMMENT_ADDED -> "text-success";
            case COMMENT_MENTIONED -> "text-info";
            case TASK_DUE_REMINDER, SYSTEM -> "text-warning";
            case TASK_OVERDUE -> "text-danger";
        };
    }
}
