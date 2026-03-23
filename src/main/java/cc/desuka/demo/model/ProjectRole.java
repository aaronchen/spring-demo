package cc.desuka.demo.model;

public enum ProjectRole implements Translatable {
    VIEWER("project.role.viewer"),
    EDITOR("project.role.editor"),
    OWNER("project.role.owner");

    private final String messageKey;

    ProjectRole(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }
}
