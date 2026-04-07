import { Application } from "@hotwired/stimulus";

// Side-effect imports — each module registers document-level event listeners
// or initializes shared services just by being imported (no exported values used).
import "lib/websocket";         // activates STOMP client
import "lib/htmx-csrf";        // CSRF token injection for HTMX requests
import "lib/htmx-errors";      // toast on HTMX error responses
import "lib/flash-toast";      // HX-Trigger showToast listener + flash attribute scan
import "lib/date-range";       // date-range-start min constraint
import "lib/mention-encoding"; // @mention encoding on submit/configRequest

// Controllers
import PresenceController from "controllers/presence";
import RecentViewsController from "controllers/recent-views";
import AnalyticsController from "controllers/analytics";
import AuditController from "controllers/audit";
import NotificationsBadgeController from "controllers/notifications/badge";
import NotificationsPageController from "controllers/notifications/page";
import MentionController from "controllers/mention";
import TasksListController from "controllers/tasks/list";
import TasksFormController from "controllers/tasks/form";
import TasksKanbanController from "controllers/tasks/kanban";
import TasksInlineEditController from "controllers/tasks/inline-edit";
import TasksBulkActionsController from "controllers/tasks/bulk-actions";
import TasksKeyboardShortcutsController from "controllers/tasks/keyboard-shortcuts";
import TasksDependenciesController from "controllers/tasks/dependencies";
import TasksLiveUpdateController from "controllers/tasks/live-update";
import DashboardController from "controllers/dashboard";

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
