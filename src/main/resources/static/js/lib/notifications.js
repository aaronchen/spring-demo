// Shared notification helpers — used by both badge and page controllers.
// escapeHtml and fire re-exported from lib/html for backward compat with existing consumers.

export { escapeHtml, fire } from "lib/html";

export function getNotificationIcon(type) {
    switch (type) {
        case "TASK_ASSIGNED": return "bi-person-plus text-primary";
        case "TASK_UPDATED": return "bi-pencil-square text-primary";
        case "COMMENT_ADDED": return "bi-chat-dots text-success";
        case "COMMENT_MENTIONED": return "bi-at text-info";
        case "TASK_DUE_REMINDER": return "bi-calendar-event text-warning";
        case "TASK_OVERDUE": return "bi-clock text-danger";
        case "SYSTEM": return "bi-megaphone text-warning";
        default: return "bi-bell text-secondary";
    }
}

export function formatRelativeTime(dateStr) {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now - date;
    const diffMin = Math.floor(diffMs / 60000);
    const diffHr = Math.floor(diffMs / 3600000);
    const diffDay = Math.floor(diffMs / 86400000);

    if (diffMin < 1) return APP_CONFIG.messages["notification.time.now"];
    if (diffMin < 60) return APP_CONFIG.messages["notification.time.minutes"].replace("{0}", diffMin);
    if (diffHr < 24) return APP_CONFIG.messages["notification.time.hours"].replace("{0}", diffHr);
    return APP_CONFIG.messages["notification.time.days"].replace("{0}", diffDay);
}
