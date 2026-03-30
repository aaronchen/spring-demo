package cc.desuka.demo.model;

public enum Recurrence implements Translatable {
    DAILY("recurring.recurrence.daily"),
    WEEKLY("recurring.recurrence.weekly"),
    BIWEEKLY("recurring.recurrence.biweekly"),
    MONTHLY("recurring.recurrence.monthly");

    private final String messageKey;

    Recurrence(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
