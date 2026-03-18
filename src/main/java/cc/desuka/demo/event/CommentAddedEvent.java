package cc.desuka.demo.event;

import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.User;

public record CommentAddedEvent(Comment comment, Task task, User actor) {}
