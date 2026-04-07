package cc.desuka.demo.model;

import java.util.List;

public enum TaskStatus implements Translatable {
    BACKLOG("task.status.backlog"),
    OPEN("task.status.open"),
    IN_PROGRESS("task.status.inProgress"),
    IN_REVIEW("task.status.inReview"),
    COMPLETED("task.status.completed"),
    CANCELLED("task.status.cancelled");

    private final String messageKey;

    TaskStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getCssClass() {
        return switch (this) {
            case BACKLOG -> "bg-backlog text-dark";
            case OPEN -> "bg-secondary text-white";
            case IN_PROGRESS -> "bg-warning text-dark";
            case IN_REVIEW -> "bg-info text-white";
            case COMPLETED -> "bg-success text-white";
            case CANCELLED -> "bg-dark text-white";
        };
    }

    public String getBtnClass() {
        return switch (this) {
            case BACKLOG -> "btn-backlog";
            case OPEN -> "btn-secondary";
            case IN_PROGRESS -> "btn-warning";
            case IN_REVIEW -> "btn-info";
            case COMPLETED -> "btn-success";
            case CANCELLED -> "btn-dark";
        };
    }

    public String getTextClass() {
        return switch (this) {
            case BACKLOG, IN_PROGRESS -> "text-dark";
            default -> "text-white";
        };
    }

    public String getBorderClass() {
        return switch (this) {
            case BACKLOG, OPEN -> "border-secondary";
            case IN_PROGRESS -> "border-warning";
            case IN_REVIEW -> "border-info";
            case COMPLETED -> "border-success";
            case CANCELLED -> "border-dark";
        };
    }

    public String getIcon() {
        return switch (this) {
            case BACKLOG -> "bi-inbox";
            case OPEN -> "bi-circle";
            case IN_PROGRESS -> "bi-play-circle-fill";
            case IN_REVIEW -> "bi-eye-fill";
            case COMPLETED -> "bi-check-circle-fill";
            case CANCELLED -> "bi-x-circle-fill";
        };
    }

    public String getChartColor() {
        return switch (this) {
            case BACKLOG -> "#adb5bd";
            case OPEN -> "#0d6efd";
            case IN_PROGRESS -> "#ffc107";
            case IN_REVIEW -> "#0dcaf0";
            case COMPLETED -> "#198754";
            case CANCELLED -> "#dc3545";
        };
    }

    // Terminal statuses — tasks that are "done" (successfully or not).
    // Used by overdue checks, incomplete counts, and due reminders.
    private static final List<TaskStatus> TERMINAL = List.of(COMPLETED, CANCELLED);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public static List<TaskStatus> terminalStatuses() {
        return TERMINAL;
    }
}
