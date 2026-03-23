package cc.desuka.demo.model;

public enum Priority implements Translatable {
    LOW("task.priority.low"),
    MEDIUM("task.priority.medium"),
    HIGH("task.priority.high");

    private final String messageKey;

    Priority(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
