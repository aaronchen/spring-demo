import { Controller } from "@hotwired/stimulus";
import { getNotificationIcon, formatRelativeTime, escapeHtml, fire } from "lib/notifications";

// Full notifications page — handles HTMX mark-read/clear events and
// live WebSocket updates (new notifications prepended to the list).
export default class extends Controller {
    static targets = ["list", "empty", "clearBtn"];

    connect() {
        this.onReceived = (e) => this.handleReceived(e.detail);
        this.onCleared = () => this.handleCleared();
        this.onRead = (e) => this.handleRead(e.detail);
        this.onAllRead = () => this.handleAllRead();

        document.addEventListener("notification:received", this.onReceived);
        document.addEventListener("notification:cleared", this.onCleared);
        document.addEventListener("notification:read", this.onRead);
        document.addEventListener("notification:allRead", this.onAllRead);

        // HTMX afterRequest — fire events when mark-read or clear-all completes
        this.afterRequestHandler = (e) => this.handleAfterRequest(e);
        document.body.addEventListener("htmx:afterRequest", this.afterRequestHandler);
    }

    disconnect() {
        document.removeEventListener("notification:received", this.onReceived);
        document.removeEventListener("notification:cleared", this.onCleared);
        document.removeEventListener("notification:read", this.onRead);
        document.removeEventListener("notification:allRead", this.onAllRead);
        document.body.removeEventListener("htmx:afterRequest", this.afterRequestHandler);
    }

    // ── HTMX event producer ──────────────────────────────────────────────

    handleAfterRequest(e) {
        const el = e.detail.elt;
        if (el.getAttribute("hx-delete") == APP_CONFIG.routes.apiNotifications) {
            fire("notification:cleared");
            return;
        }
        if (el.getAttribute("hx-patch")?.includes("/read")) {
            el.classList.remove("list-group-item-light", "fw-semibold");
            const badge = el.querySelector(".badge");
            if (badge) badge.remove();
            fire("notification:read", { id: el.dataset.id });
        }
    }

    // ── Event consumers ──────────────────────────────────────────────────

    handleReceived(n) {
        this.emptyTarget.classList.add("d-none");
        this.listTarget.classList.remove("d-none");

        const icon = getNotificationIcon(n.type);
        const actionLink = n.link
            ? `<a href="${n.link}" class="text-muted ms-2 align-self-center flex-shrink-0"><i class="bi bi-box-arrow-up-right"></i></a>`
            : "";

        const row = document.createElement("div");
        row.className = "list-group-item list-group-item-action list-group-item-light fw-semibold";
        row.style.cursor = "pointer";
        row.dataset.id = n.id;
        row.setAttribute("hx-patch", APP_CONFIG.routes.apiNotificationRead.params({ id: n.id }).build());
        row.setAttribute("hx-swap", "none");
        htmx.process(row);

        row.innerHTML = `<div class="d-flex align-items-start py-1">
            <i class="bi ${icon} me-3 mt-1"></i>
            <div class="flex-grow-1">
                <div>${escapeHtml(n.message)}</div>
                <small class="text-muted">${formatRelativeTime(n.createdAt)}</small>
            </div>
            ${actionLink}
            <span class="badge bg-primary ms-2 align-self-center">${APP_CONFIG.messages["notification.new"]}</span>
        </div>`;

        this.listTarget.prepend(row);

        if (this.hasClearBtnTarget) {
            this.clearBtnTarget.classList.remove("d-none");
        }
    }

    handleCleared() {
        this.listTarget.innerHTML = "";
        this.listTarget.classList.add("d-none");
        this.emptyTarget.classList.remove("d-none");
        if (this.hasClearBtnTarget) {
            this.clearBtnTarget.classList.add("d-none");
        }
    }

    handleRead(detail) {
        const row = this.listTarget.querySelector(`[data-id="${detail.id}"]`);
        if (row) {
            row.classList.remove("list-group-item-light", "fw-semibold");
            const badge = row.querySelector(".badge");
            if (badge) badge.remove();
        }
    }

    handleAllRead() {
        this.listTarget.querySelectorAll(".list-group-item").forEach((el) => {
            el.classList.remove("list-group-item-light", "fw-semibold");
            const badge = el.querySelector(".badge");
            if (badge) badge.remove();
        });
    }
}
