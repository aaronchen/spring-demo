package cc.desuka.demo.util;

public enum FormMode {
    VIEW("view"),
    CREATE("create"),
    EDIT("edit");

    private final String value;

    FormMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
