package cc.desuka.demo.model;

public enum Role implements Translatable {
    USER("role.user"),
    ADMIN("role.admin");

    private final String messageKey;

    Role(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }
}
