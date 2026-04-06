import { Controller } from "@hotwired/stimulus";
import { requireOk } from "lib/api";
import { showToast } from "lib/toast";
import { onConnect } from "lib/websocket";
import { getNotificationIcon, formatRelativeTime, escapeHtml, fire } from "lib/notifications";

// Navbar notification bell — badge count, dropdown list, WebSocket subscription.
// Communicates with the notifications page via custom DOM events.
export default class extends Controller {
    static targets = ["badge", "list", "empty", "markAllRead"];

    connect() {
        // Event listeners for cross-component communication
        this.onReceived = (e) => this.handleReceived(e.detail);
        this.onRead = (e) => {
            this.refreshBadge();
            const item = this.listTarget.querySelector(`[data-notification-id="${e.detail.id}"]`);
            if (item) item.classList.remove("fw-semibold");
        };
        this.onAllRead = () => {
            this.updateBadge(0);
            this.listTarget.querySelectorAll(".fw-semibold").forEach((el) => {
                el.classList.remove("fw-semibold");
            });
        };
        this.onCleared = () => {
            this.updateBadge(0);
            this.listTarget.innerHTML = "";
            this.emptyTarget.classList.remove("d-none");
        };

        document.addEventListener("notification:received", this.onReceived);
        document.addEventListener("notification:read", this.onRead);
        document.addEventListener("notification:allRead", this.onAllRead);
        document.addEventListener("notification:cleared", this.onCleared);

        onConnect((client) => {
            client.subscribe("/user/queue/notifications", (message) => {
                const data = JSON.parse(message.body);
                fire("notification:received", data);
                showToast(data.message, "info", { href: data.link });
            });

            this.refreshBadge();
            this.loadRecentNotifications();
        });
    }

    disconnect() {
        document.removeEventListener("notification:received", this.onReceived);
        document.removeEventListener("notification:read", this.onRead);
        document.removeEventListener("notification:allRead", this.onAllRead);
        document.removeEventListener("notification:cleared", this.onCleared);
    }

    // ── Actions ──────────────────────────────────────────────────────────

    markAllRead(event) {
        event.preventDefault();
        event.stopPropagation();
        fetch(APP_CONFIG.routes.apiNotificationsReadAll, { method: "PATCH" })
            .then(requireOk)
            .then(() => fire("notification:allRead"))
            .catch((err) => console.error("Failed to mark all as read:", err));
    }

    // ── Badge ────────────────────────────────────────────────────────────

    refreshBadge() {
        fetch(APP_CONFIG.routes.apiNotificationsUnreadCount)
            .then(requireOk)
            .then((res) => res.json())
            .then((data) => this.updateBadge(data.count));
    }

    updateBadge(count) {
        if (count > 0) {
            this.badgeTarget.textContent = count > 99 ? "99+" : count;
            this.badgeTarget.classList.remove("d-none");
        } else {
            this.badgeTarget.classList.add("d-none");
        }
    }

    // ── Dropdown list ────────────────────────────────────────────────────

    loadRecentNotifications() {
        fetch(`${APP_CONFIG.routes.apiNotifications}?size=10`)
            .then(requireOk)
            .then((res) => res.json())
            .then((data) => this.renderList(data.content));
    }

    renderList(notifications) {
        this.listTarget.innerHTML = "";
        if (notifications.length === 0) {
            this.emptyTarget.classList.remove("d-none");
            return;
        }
        this.emptyTarget.classList.add("d-none");
        notifications.forEach((n) => {
            this.listTarget.appendChild(this.createItem(n));
        });
    }

    handleReceived(n) {
        // Update badge
        const current = this.badgeTarget.classList.contains("d-none") ? 0 : parseInt(this.badgeTarget.textContent) || 0;
        this.updateBadge(current + 1);

        // Prepend to dropdown list
        this.emptyTarget.classList.add("d-none");
        this.listTarget.prepend(this.createItem(n));
        while (this.listTarget.children.length > 10) {
            this.listTarget.removeChild(this.listTarget.lastChild);
        }
    }

    createItem(n) {
        const item = document.createElement("div");
        item.className = `dropdown-item py-2 ${n.read ? "" : "fw-semibold"}`;
        item.style.cursor = "pointer";
        item.dataset.notificationId = n.id;

        const icon = getNotificationIcon(n.type);
        const time = formatRelativeTime(n.createdAt);
        const actionLink = n.link
            ? `<a href="${n.link}" class="text-muted ms-2 flex-shrink-0" title="${n.link}"><i class="bi bi-box-arrow-up-right"></i></a>`
            : "";

        item.innerHTML = `<div class="d-flex align-items-start">
            <i class="bi ${icon} me-2 mt-1"></i>
            <div class="flex-grow-1" style="min-width: 0;">
                <div class="text-truncate" style="max-width: 280px;">${escapeHtml(n.message)}</div>
                <small class="text-muted">${time}</small>
            </div>
            ${actionLink}
        </div>`;

        item.addEventListener("click", () => {
            if (!n.read) {
                fetch(APP_CONFIG.routes.apiNotificationRead.resolve({ id: n.id }), { method: "PATCH" })
                    .then(requireOk)
                    .then(() => fire("notification:read", { id: n.id }))
                    .catch((err) => console.error("Failed to mark notification as read:", err));
                item.classList.remove("fw-semibold");
                n.read = true;
            }
        });

        return item;
    }
}
