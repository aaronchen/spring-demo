package cc.desuka.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized route configuration for Thymeleaf templates and the /config.js endpoint.
 *
 * <p>Defaults are defined here. Override in any Spring properties source:
 *
 * <pre>
 * # application-prod.properties
 * app.routes.api=https://api.example.com
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "app.routes")
public class AppRoutesProperties {

    // ── Web routes ────────────────────────────────────────────���───────
    private String projects = "/projects";
    private String tasks = "/tasks";
    private String audit = "/admin/audit";
    private String dashboard = "/dashboard";
    private String analytics = "/analytics";

    // ── API resource routes ─────────────────────────────────────────���──
    private String apiTasks = "/api/tasks";
    private String apiProjects = "/api/projects";
    private String apiUsers = "/api/users";
    private String apiTags = "/api/tags";
    private String apiNotifications = "/api/notifications";
    private String apiPresence = "/api/presence";
    private String apiAnalytics = "/api/analytics";
    private String apiViews = "/api/views";
    private String apiAudit = "/api/audit";

    // ── Parameterized API routes ──────────────────────────────────────
    private String apiProjectAnalytics = "/api/projects/{projectId}/analytics";
    private String apiProjectMembers = "/api/projects/{projectId}/members";
    private String apiProjectMembersAssignable = "/api/projects/{projectId}/members/assignable";
    private String apiNotificationRead = "/api/notifications/{id}/read";
    private String apiNotificationsUnreadCount = "/api/notifications/unread-count";
    private String apiNotificationsReadAll = "/api/notifications/read-all";
    private String apiTaskSearchForDependency = "/api/tasks/search-for-dependency";
    private String apiViewById = "/api/views/{id}";

    // ── Web routes ────────────────────────────────────────────────────

    public String getProjects() {
        return projects;
    }

    public void setProjects(String projects) {
        this.projects = projects;
    }

    public String getTasks() {
        return tasks;
    }

    public void setTasks(String tasks) {
        this.tasks = tasks;
    }

    public String getAudit() {
        return audit;
    }

    public void setAudit(String audit) {
        this.audit = audit;
    }

    public String getDashboard() {
        return dashboard;
    }

    public void setDashboard(String dashboard) {
        this.dashboard = dashboard;
    }

    public String getAnalytics() {
        return analytics;
    }

    public void setAnalytics(String analytics) {
        this.analytics = analytics;
    }

    // ── API resource routes ────────────────────────────────────────────

    public String getApiTasks() {
        return apiTasks;
    }

    public void setApiTasks(String apiTasks) {
        this.apiTasks = apiTasks;
    }

    public String getApiProjects() {
        return apiProjects;
    }

    public void setApiProjects(String apiProjects) {
        this.apiProjects = apiProjects;
    }

    public String getApiUsers() {
        return apiUsers;
    }

    public void setApiUsers(String apiUsers) {
        this.apiUsers = apiUsers;
    }

    public String getApiTags() {
        return apiTags;
    }

    public void setApiTags(String apiTags) {
        this.apiTags = apiTags;
    }

    public String getApiNotifications() {
        return apiNotifications;
    }

    public void setApiNotifications(String apiNotifications) {
        this.apiNotifications = apiNotifications;
    }

    public String getApiPresence() {
        return apiPresence;
    }

    public void setApiPresence(String apiPresence) {
        this.apiPresence = apiPresence;
    }

    public String getApiAnalytics() {
        return apiAnalytics;
    }

    public void setApiAnalytics(String apiAnalytics) {
        this.apiAnalytics = apiAnalytics;
    }

    public String getApiViews() {
        return apiViews;
    }

    public void setApiViews(String apiViews) {
        this.apiViews = apiViews;
    }

    public String getApiAudit() {
        return apiAudit;
    }

    public void setApiAudit(String apiAudit) {
        this.apiAudit = apiAudit;
    }

    // ── Parameterized API routes ─────────────────────────────────────────

    public String getApiProjectAnalytics() {
        return apiProjectAnalytics;
    }

    public String getApiProjectAnalytics(Long projectId) {
        return apiProjectAnalytics.replace("{projectId}", String.valueOf(projectId));
    }

    public void setApiProjectAnalytics(String apiProjectAnalytics) {
        this.apiProjectAnalytics = apiProjectAnalytics;
    }

    public String getApiProjectMembers() {
        return apiProjectMembers;
    }

    public String getApiProjectMembers(Long projectId) {
        return apiProjectMembers.replace("{projectId}", String.valueOf(projectId));
    }

    public void setApiProjectMembers(String apiProjectMembers) {
        this.apiProjectMembers = apiProjectMembers;
    }

    public String getApiProjectMembersAssignable() {
        return apiProjectMembersAssignable;
    }

    public String getApiProjectMembersAssignable(Long projectId) {
        return apiProjectMembersAssignable.replace("{projectId}", String.valueOf(projectId));
    }

    public void setApiProjectMembersAssignable(String apiProjectMembersAssignable) {
        this.apiProjectMembersAssignable = apiProjectMembersAssignable;
    }

    public String getApiNotificationRead() {
        return apiNotificationRead;
    }

    public String getApiNotificationRead(Long id) {
        return apiNotificationRead.replace("{id}", String.valueOf(id));
    }

    public void setApiNotificationRead(String apiNotificationRead) {
        this.apiNotificationRead = apiNotificationRead;
    }

    public String getApiNotificationsUnreadCount() {
        return apiNotificationsUnreadCount;
    }

    public void setApiNotificationsUnreadCount(String apiNotificationsUnreadCount) {
        this.apiNotificationsUnreadCount = apiNotificationsUnreadCount;
    }

    public String getApiNotificationsReadAll() {
        return apiNotificationsReadAll;
    }

    public void setApiNotificationsReadAll(String apiNotificationsReadAll) {
        this.apiNotificationsReadAll = apiNotificationsReadAll;
    }

    public String getApiTaskSearchForDependency() {
        return apiTaskSearchForDependency;
    }

    public void setApiTaskSearchForDependency(String apiTaskSearchForDependency) {
        this.apiTaskSearchForDependency = apiTaskSearchForDependency;
    }

    public String getApiViewById() {
        return apiViewById;
    }

    public String getApiViewById(Long id) {
        return apiViewById.replace("{id}", String.valueOf(id));
    }

    public void setApiViewById(String apiViewById) {
        this.apiViewById = apiViewById;
    }
}
