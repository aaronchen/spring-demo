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

    public String getCssClass() {
        return switch (this) {
            case LOW -> "bg-success text-white";
            case MEDIUM -> "bg-warning text-dark";
            case HIGH -> "bg-danger text-white";
        };
    }

    public String getBtnClass() {
        return switch (this) {
            case LOW -> "btn-success";
            case MEDIUM -> "btn-warning";
            case HIGH -> "btn-danger";
        };
    }

    public String getIcon() {
        return switch (this) {
            case LOW -> "bi-reception-1";
            case MEDIUM -> "bi-reception-2";
            case HIGH -> "bi-reception-4";
        };
    }

    public String getChartColor() {
        return switch (this) {
            case LOW -> "#198754";
            case MEDIUM -> "#ffc107";
            case HIGH -> "#dc3545";
        };
    }
}
