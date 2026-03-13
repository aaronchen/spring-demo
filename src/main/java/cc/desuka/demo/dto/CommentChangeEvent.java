package cc.desuka.demo.dto;

public record CommentChangeEvent(String action, long taskId, long commentId, long userId) {
}
