package cc.desuka.demo.event;

import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.User;

public record TaskAssignedEvent(Task task, User actor) {}
