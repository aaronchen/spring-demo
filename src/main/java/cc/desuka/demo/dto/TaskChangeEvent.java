package cc.desuka.demo.dto;

public record TaskChangeEvent(String action, long taskId, long userId) {
}
