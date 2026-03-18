package cc.desuka.demo.event;

public record TaskChangeEvent(String action, long taskId, long userId) {}
