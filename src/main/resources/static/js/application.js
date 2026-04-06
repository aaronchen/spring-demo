import { Application } from "@hotwired/stimulus";

// Side-effect imports: global listeners and infrastructure
import "lib/websocket";         // activates STOMP client
import "lib/htmx-csrf";        // CSRF token injection for HTMX requests
import "lib/htmx-errors";      // toast on HTMX error responses
import "lib/date-range";       // date-range-start min constraint
import "lib/mention-encoding"; // @mention encoding on submit/configRequest

// Backward compat for inline template handlers (task-layout.html).
// Will be removed when task controllers migrate these to data-action (Phase 6/7).
import { isMentionMenuActive, clearMentions } from "lib/mentions";
window.isMentionMenuActive = isMentionMenuActive;
window.clearMentions = clearMentions;

// Controllers
import PresenceController from "./controllers/presence_controller.js";
import RecentViewsController from "./controllers/recent_views_controller.js";
import AnalyticsController from "./controllers/analytics_controller.js";
import AuditController from "./controllers/audit_controller.js";
import NotificationsBadgeController from "./controllers/notifications/badge_controller.js";
import NotificationsPageController from "./controllers/notifications/page_controller.js";
import MentionController from "./controllers/mention_controller.js";
import TasksListController from "./controllers/tasks/list_controller.js";

const app = Application.start();

app.register("presence", PresenceController);
app.register("recent-views", RecentViewsController);
app.register("analytics", AnalyticsController);
app.register("audit", AuditController);
app.register("notifications--badge", NotificationsBadgeController);
app.register("notifications--page", NotificationsPageController);
app.register("mention", MentionController);
app.register("tasks--list", TasksListController);

// Expose for debugging: window.Stimulus.controllers shows all active instances
window.Stimulus = app;
