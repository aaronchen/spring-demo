package cc.desuka.demo.model;

public enum ProjectStatus implements Translatable {
    ACTIVE("project.status.active"),
    ARCHIVED("project.status.archived");

    private final String messageKey;

    ProjectStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }
}
