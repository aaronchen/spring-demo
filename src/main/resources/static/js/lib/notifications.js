// Shared notification helpers — used by both badge and page controllers.
// escapeHtml and fire re-exported from lib/html for backward compat with existing consumers.

import { t } from "lib/i18n";
export { escapeHtml, fire } from "lib/html";

export function getNotificationIcon(type) {
    const cfg = APP_CONFIG.enums.notificationType[type];
    return cfg ? `${cfg.icon} ${cfg.css}` : "bi-bell text-secondary";
}

export function formatRelativeTime(dateStr) {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now - date;
    const diffMin = Math.floor(diffMs / 60000);
    const diffHr = Math.floor(diffMs / 3600000);
    const diffDay = Math.floor(diffMs / 86400000);

    if (diffMin < 1) return t("notification.time.now");
    if (diffMin < 60) return t("notification.time.minutes", diffMin);
    if (diffHr < 24) return t("notification.time.hours", diffHr);
    return t("notification.time.days", diffDay);
}
