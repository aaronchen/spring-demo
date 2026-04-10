package cc.desuka.demo.config;

import cc.desuka.demo.util.RouteTemplate;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized route configuration for Thymeleaf templates and the /config.js endpoint.
 *
 * <p>Every route is a {@link RouteTemplate}. Routes with {@code {placeholder}} tokens use the
 * builder API: {@code route.params("key", value).build()}. Routes without placeholders work
 * transparently as strings via {@link RouteTemplate#toString()}.
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
    private RouteTemplate taskEdit = new RouteTemplate("/tasks/{taskId}/edit");
    private RouteTemplate taskToggle = new RouteTemplate("/tasks/{taskId}/toggle");
    private RouteTemplate taskNew = new RouteTemplate("/tasks/new");
    private RouteTemplate taskComments = new RouteTemplate("/tasks/{taskId}/comments");
    private RouteTemplate taskCommentDelete =
            new RouteTemplate("/tasks/{taskId}/comments/{commentId}");

    // ── Parameterized project settings routes ──────────────────────────
    private RouteTemplate projectArchive = new RouteTemplate("/projects/{projectId}/archive");
    private RouteTemplate projectMembers = new RouteTemplate("/projects/{projectId}/members");
    private RouteTemplate projectMemberRole =
            new RouteTemplate("/projects/{projectId}/members/{userId}/role");
    private RouteTemplate projectMemberDelete =
            new RouteTemplate("/projects/{projectId}/members/{userId}");
    private RouteTemplate projectSprints = new RouteTemplate("/projects/{projectId}/sprints");
    private RouteTemplate projectSprintsPanel =
            new RouteTemplate("/projects/{projectId}/sprints/panel");
    private RouteTemplate projectSprintDetail =
            new RouteTemplate("/projects/{projectId}/sprints/{sprintId}");
    private RouteTemplate projectRecurringTemplates =
            new RouteTemplate("/projects/{projectId}/recurring-templates");
    private RouteTemplate projectRecurringDetail =
            new RouteTemplate("/projects/{projectId}/recurring-templates/{templateId}");
    private RouteTemplate projectSprintEdit =
            new RouteTemplate("/projects/{projectId}/sprints/{sprintId}/edit");
    private RouteTemplate projectRecurringPanel =
            new RouteTemplate("/projects/{projectId}/recurring-templates/panel");
    private RouteTemplate projectRecurringEdit =
            new RouteTemplate("/projects/{projectId}/recurring-templates/{templateId}/edit");
    private RouteTemplate projectRecurringGenerate =
            new RouteTemplate("/projects/{projectId}/recurring-templates/{templateId}/generate");
    private RouteTemplate projectRecurringToggle =
            new RouteTemplate("/projects/{projectId}/recurring-templates/{templateId}/toggle");

    // ── Admin routes ──────────────────────────────────────────────────
    private RouteTemplate adminTags = new RouteTemplate("/admin/tags");
    private RouteTemplate adminUserEdit = new RouteTemplate("/admin/users/{userId}/edit");
    private RouteTemplate adminUserEnable = new RouteTemplate("/admin/users/{userId}/enable");
    private RouteTemplate adminTagDelete = new RouteTemplate("/admin/tags/{tagId}");

    // ── API resource routes ───────────────────────────────────────────
    private RouteTemplate apiTasks = new RouteTemplate("/api/tasks");
    private RouteTemplate apiProjects = new RouteTemplate("/api/projects");
    private RouteTemplate apiUsers = new RouteTemplate("/api/users");
    private RouteTemplate apiTags = new RouteTemplate("/api/tags");
    private RouteTemplate apiNotifications = new RouteTemplate("/api/notifications");
    private RouteTemplate apiPins = new RouteTemplate("/api/pins");
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

    // WebSocket STOMP topics
    private RouteTemplate topicProjectTasks =
            new RouteTemplate("/topic/projects/{projectId}/tasks");
    private RouteTemplate topicTaskComments = new RouteTemplate("/topic/tasks/{taskId}/comments");
    private RouteTemplate topicPresence = new RouteTemplate("/topic/presence");
}
