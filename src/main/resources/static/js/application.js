import { Application } from "@hotwired/stimulus";

// Expose showToast/showConfirm on window for inline <script> blocks
// in admin pages and OOB HTMX panel responses.
import { showToast } from "lib/toast";
import { showConfirm } from "lib/confirm";
window.showToast = showToast;
window.showConfirm = showConfirm;

// Side-effect imports: global listeners and infrastructure
import "lib/websocket";         // activates STOMP client
import "lib/htmx-csrf";        // CSRF token injection for HTMX requests
import "lib/htmx-errors";      // toast on HTMX error responses
import "lib/date-range";       // date-range-start min constraint
import "lib/mention-encoding"; // @mention encoding on submit/configRequest

// Controllers
import PresenceController from "./controllers/presence_controller.js";
import RecentViewsController from "./controllers/recent_views_controller.js";
import AnalyticsController from "./controllers/analytics_controller.js";
import AuditController from "./controllers/audit_controller.js";
import NotificationsBadgeController from "./controllers/notifications/badge_controller.js";
import NotificationsPageController from "./controllers/notifications/page_controller.js";
import MentionController from "./controllers/mention_controller.js";
import TasksListController from "./controllers/tasks/list_controller.js";
import TasksFormController from "./controllers/tasks/form_controller.js";
import TasksKanbanController from "./controllers/tasks/kanban_controller.js";
import TasksInlineEditController from "./controllers/tasks/inline_edit_controller.js";
import TasksBulkActionsController from "./controllers/tasks/bulk_actions_controller.js";
import TasksKeyboardShortcutsController from "./controllers/tasks/keyboard_shortcuts_controller.js";
import TasksDependenciesController from "./controllers/tasks/dependencies_controller.js";
import TasksLiveUpdateController from "./controllers/tasks/live_update_controller.js";
import DashboardController from "./controllers/dashboard_controller.js";

const app = Application.start();

app.register("presence", PresenceController);
app.register("recent-views", RecentViewsController);
app.register("analytics", AnalyticsController);
app.register("audit", AuditController);
app.register("notifications--badge", NotificationsBadgeController);
app.register("notifications--page", NotificationsPageController);
app.register("mention", MentionController);
app.register("tasks--list", TasksListController);
app.register("tasks--form", TasksFormController);
app.register("tasks--kanban", TasksKanbanController);
app.register("tasks--inline-edit", TasksInlineEditController);
app.register("tasks--bulk-actions", TasksBulkActionsController);
app.register("tasks--keyboard-shortcuts", TasksKeyboardShortcutsController);
app.register("tasks--dependencies", TasksDependenciesController);
app.register("tasks--live-update", TasksLiveUpdateController);
app.register("dashboard", DashboardController);

// Expose for debugging: window.Stimulus.controllers shows all active instances
window.Stimulus = app;
