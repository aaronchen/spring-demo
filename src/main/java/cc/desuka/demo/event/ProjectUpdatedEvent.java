package cc.desuka.demo.event;

import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.User;

public record ProjectUpdatedEvent(Project project, User actor) {}
