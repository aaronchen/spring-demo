package cc.desuka.demo.config;

import cc.desuka.demo.util.RouteTemplate;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized route configuration for Thymeleaf templates and the /config.js endpoint.
 *
 * <p>Every route is a {@link RouteTemplate}. Routes with {@code {placeholder}} tokens are resolved
 * via {@link RouteTemplate#resolve(java.util.Map)}. Routes without placeholders work transparently
 * as strings via {@link RouteTemplate#toString()}.
 *
 * <p>Defaults are defined here. Override in any Spring properties source:
 *
 * <pre>
 * # application-prod.properties
 * app.routes.api-tasks=https://api.example.com/api/tasks
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.routes")
public class AppRoutesProperties {

    // ── Web routes ────────────────────────────────────────────────────
    private RouteTemplate projects = new RouteTemplate("/projects");
    private RouteTemplate tasks = new RouteTemplate("/tasks");
    private RouteTemplate audit = new RouteTemplate("/admin/audit");
    private RouteTemplate dashboard = new RouteTemplate("/dashboard");
    private RouteTemplate analytics = new RouteTemplate("/analytics");

    private RouteTemplate login = new RouteTemplate("/login");
    private RouteTemplate profile = new RouteTemplate("/profile");

    // ── Parameterized web routes ─────────────────────────────────────
    private RouteTemplate projectDetail = new RouteTemplate("/projects/{projectId}");
    private RouteTemplate projectSettings = new RouteTemplate("/projects/{projectId}/settings");
    private RouteTemplate taskDetail = new RouteTemplate("/tasks/{taskId}");

    // ── API resource routes ───────────────────────────────────────────
    private RouteTemplate apiTasks = new RouteTemplate("/api/tasks");
    private RouteTemplate apiProjects = new RouteTemplate("/api/projects");
    private RouteTemplate apiUsers = new RouteTemplate("/api/users");
    private RouteTemplate apiTags = new RouteTemplate("/api/tags");
    private RouteTemplate apiNotifications = new RouteTemplate("/api/notifications");
    private RouteTemplate apiPresence = new RouteTemplate("/api/presence");
    private RouteTemplate apiAnalytics = new RouteTemplate("/api/analytics");
    private RouteTemplate apiViews = new RouteTemplate("/api/views");
    private RouteTemplate apiRecentViews = new RouteTemplate("/api/recent-views");
    private RouteTemplate apiAudit = new RouteTemplate("/api/audit");

    // ── Parameterized API routes ──────────────────────────────────────
    private RouteTemplate apiProjectAnalytics =
            new RouteTemplate("/api/projects/{projectId}/analytics");
    private RouteTemplate apiProjectSprints =
            new RouteTemplate("/api/projects/{projectId}/sprints");
    private RouteTemplate apiProjectMembers =
            new RouteTemplate("/api/projects/{projectId}/members");
    private RouteTemplate apiProjectMembersAssignable =
            new RouteTemplate("/api/projects/{projectId}/members/assignable");
    private RouteTemplate apiNotificationRead = new RouteTemplate("/api/notifications/{id}/read");
    private RouteTemplate apiNotificationsUnreadCount =
            new RouteTemplate("/api/notifications/unread-count");
    private RouteTemplate apiNotificationsReadAll =
            new RouteTemplate("/api/notifications/read-all");
    private RouteTemplate apiTaskSearchForDependency =
            new RouteTemplate("/api/tasks/search-for-dependency");
    private RouteTemplate apiProjectRecurringTemplates =
            new RouteTemplate("/api/projects/{projectId}/recurring-templates");
    private RouteTemplate apiViewById = new RouteTemplate("/api/views/{id}");
}
