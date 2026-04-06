import { Controller } from "@hotwired/stimulus";
import { requireOk } from "lib/api";
import { onConnect } from "lib/websocket";
import { escapeHtml } from "lib/html";

export default class extends Controller {
    static targets = ["toggle", "panel", "list", "empty"];

    static values = {
        maxItems: { type: Number, default: 10 },
    };

    connect() {
        // Close on outside click
        this.outsideClickHandler = (e) => {
            if (!this.element.contains(e.target) && !this.panelTarget.classList.contains("d-none")) {
                this.panelTarget.classList.add("d-none");
                this.toggleTarget.classList.remove("active");
            }
        };
        document.addEventListener("click", this.outsideClickHandler);

        onConnect((client) => {
            client.subscribe("/user/queue/recent-views", (message) => {
                const data = JSON.parse(message.body);
                if (data.deleted) {
                    this.removeItem(data.entityType, data.entityId);
                } else if (data.titleOnly) {
                    this.updateTitleInPlace(data.entityType, data.entityId, data.entityTitle);
                } else {
                    this.updateList(data.entityType, data.entityId, data.entityTitle, data.href, data.viewedAt);
                }
            });

            this.loadRecentViews();
        });
    }

    disconnect() {
        document.removeEventListener("click", this.outsideClickHandler);
    }

    toggle(event) {
        event.preventDefault();
        this.panelTarget.classList.toggle("d-none");
        this.toggleTarget.classList.toggle("active", !this.panelTarget.classList.contains("d-none"));
    }

    loadRecentViews() {
        fetch(APP_CONFIG.routes.apiRecentViews)
            .then(requireOk)
            .then((res) => res.json())
            .then((items) => {
                this.listTarget.innerHTML = "";
                if (items.length === 0) {
                    this.emptyTarget.classList.remove("d-none");
                    return;
                }
                this.emptyTarget.classList.add("d-none");
                items.forEach((item) => {
                    this.listTarget.appendChild(
                        this.createItem(item.entityType, item.entityId, item.entityTitle, item.href, item.viewedAt)
                    );
                });
            });
    }

    formatTime(dateStr) {
        const date = new Date(dateStr);
        return date.toLocaleDateString("en-US", { month: "short", day: "numeric" })
            + " " + date.toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit", hour12: false });
    }

    createItem(entityType, entityId, title, href, viewedAt) {
        const iconClass = entityType === "TASK"
            ? "bi bi-check2-square text-primary"
            : "bi bi-folder text-info";
        const timeStr = viewedAt ? this.formatTime(viewedAt) : "";

        const item = document.createElement("a");
        item.href = href;
        item.className = "list-group-item list-group-item-action py-2 px-3";
        item.dataset.entityType = entityType;
        item.dataset.entityId = entityId;
        item.innerHTML = `<div class="d-flex align-items-center gap-2">
            <i class="${iconClass}" style="font-size: 0.8rem;"></i>
            <div class="text-truncate" style="min-width: 0;">
                <div class="text-truncate small rv-title">${escapeHtml(title)}</div>
                <small class="text-muted" style="font-size: 0.7rem;">${timeStr}</small>
            </div>
        </div>`;
        return item;
    }

    removeItem(entityType, entityId) {
        const existing = this.listTarget.querySelector(
            `[data-entity-type="${entityType}"][data-entity-id="${entityId}"]`);
        if (existing) existing.remove();
        if (this.listTarget.children.length === 0) {
            this.emptyTarget.classList.remove("d-none");
        }
    }

    updateTitleInPlace(entityType, entityId, title) {
        const existing = this.listTarget.querySelector(
            `[data-entity-type="${entityType}"][data-entity-id="${entityId}"]`);
        if (existing) {
            const titleEl = existing.querySelector(".rv-title");
            if (titleEl) titleEl.textContent = title;
        }
    }

    updateList(entityType, entityId, title, href, viewedAt) {
        this.emptyTarget.classList.add("d-none");

        const existing = this.listTarget.querySelector(
            `[data-entity-type="${entityType}"][data-entity-id="${entityId}"]`);
        if (existing) existing.remove();

        this.listTarget.prepend(this.createItem(entityType, entityId, title, href, viewedAt));

        while (this.listTarget.children.length > this.maxItemsValue) {
            this.listTarget.lastElementChild.remove();
        }
    }

}
