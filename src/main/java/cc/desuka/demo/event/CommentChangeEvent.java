package cc.desuka.demo.event;

import java.util.UUID;

public record CommentChangeEvent(String action, UUID taskId, long commentId, UUID userId) {}
