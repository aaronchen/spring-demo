package cc.desuka.demo.model;

/**
 * Implemented by enums whose display names are externalized in messages.properties. Each constant
 * maps to a message key (e.g. {@code task.status.inProgress}).
 */
public interface Translatable {

    String getMessageKey();
}
