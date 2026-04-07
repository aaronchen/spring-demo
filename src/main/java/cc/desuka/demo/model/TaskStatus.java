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
            case BACKLOG -> "bg-backlog";
            case OPEN -> "bg-secondary";
            case IN_PROGRESS -> "bg-warning text-dark";
            case IN_REVIEW -> "bg-info text-white";
            case COMPLETED -> "bg-success";
            case CANCELLED -> "bg-dark";
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
