package cc.desuka.demo.model;

import java.util.List;

public enum TaskStatus {
    BACKLOG,
    OPEN,
    IN_PROGRESS,
    IN_REVIEW,
    COMPLETED,
    CANCELLED;

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
