package cc.desuka.demo.event;

public record CommentChangeEvent(String action, long taskId, long commentId, long userId) {}
