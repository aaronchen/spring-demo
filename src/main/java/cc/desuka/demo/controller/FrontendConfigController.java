package cc.desuka.demo.controller;

import cc.desuka.demo.config.AppRoutesProperties;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontendConfigController {

    private final AppRoutesProperties appRoutes;

    public FrontendConfigController(AppRoutesProperties appRoutes) {
        this.appRoutes = appRoutes;
    }

    @GetMapping(value = "/config.js", produces = "application/javascript")
    public String configJs() {
        return """
            window.APP_CONFIG = {
                routes: {
                    projects: "%s",
                    tasks: "%s",
                    audit: "%s",
                    dashboard: "%s",
                    analytics: "%s",
                    apiTasks: "%s",
                    apiProjects: "%s",
                    apiUsers: "%s",
                    apiTags: "%s",
                    apiNotifications: "%s",
                    apiPresence: "%s",
                    apiAnalytics: "%s",
                    apiViews: "%s",
                    apiAudit: "%s",
                    apiProjectAnalytics: "%s",
                    apiProjectMembers: "%s",
                    apiProjectMembersAssignable: "%s",
                    apiNotificationRead: "%s",
                    apiNotificationsUnreadCount: "%s",
                    apiNotificationsReadAll: "%s",
                    apiTaskSearchForDependency: "%s",
                    apiViewById: "%s"
                },
                messages: %s
            };
            """
                .formatted(
                        escapeJs(appRoutes.getProjects()),
                        escapeJs(appRoutes.getTasks()),
                        escapeJs(appRoutes.getAudit()),
                        escapeJs(appRoutes.getDashboard()),
                        escapeJs(appRoutes.getAnalytics()),
                        escapeJs(appRoutes.getApiTasks()),
                        escapeJs(appRoutes.getApiProjects()),
                        escapeJs(appRoutes.getApiUsers()),
                        escapeJs(appRoutes.getApiTags()),
                        escapeJs(appRoutes.getApiNotifications()),
                        escapeJs(appRoutes.getApiPresence()),
                        escapeJs(appRoutes.getApiAnalytics()),
                        escapeJs(appRoutes.getApiViews()),
                        escapeJs(appRoutes.getApiAudit()),
                        escapeJs(appRoutes.getApiProjectAnalytics()),
                        escapeJs(appRoutes.getApiProjectMembers()),
                        escapeJs(appRoutes.getApiProjectMembersAssignable()),
                        escapeJs(appRoutes.getApiNotificationRead()),
                        escapeJs(appRoutes.getApiNotificationsUnreadCount()),
                        escapeJs(appRoutes.getApiNotificationsReadAll()),
                        escapeJs(appRoutes.getApiTaskSearchForDependency()),
                        escapeJs(appRoutes.getApiViewById()),
                        buildMessagesJson());
    }

    // NOTE: Uses JVM default locale, not the browser's Accept-Language. Fine while
    // the app is single-locale. For i18n, switch to MessageSource (which is locale-aware)
    // and pass the request Locale — but that conflicts with content-hash caching since
    // config.js would need to vary per locale (separate cache key or a non-cached endpoint).
    private String buildMessagesJson() {
        ResourceBundle bundle = ResourceBundle.getBundle("messages");
        String entries =
                bundle.keySet().stream()
                        .sorted()
                        .map(
                                key ->
                                        "\""
                                                + escapeJs(key)
                                                + "\":\""
                                                + escapeJs(bundle.getString(key))
                                                + "\"")
                        .collect(Collectors.joining(","));
        return "{" + entries + "}";
    }

    private static String escapeJs(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
