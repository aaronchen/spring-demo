package cc.desuka.demo.event;

import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.User;

public record TaskUpdatedEvent(Task task, User actor) {}
