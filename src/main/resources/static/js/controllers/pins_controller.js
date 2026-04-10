import { Controller } from "@hotwired/stimulus";
import { requireOk, csrfHeaders } from "lib/api";
import { t } from "lib/i18n";
import { showToast } from "lib/toast";
import { onConnect } from "lib/websocket";
import { escapeHtml } from "lib/html";
import { setupDrawer } from "lib/drawer";

// Pinned items — manages the pinned drawer and pin toggle icons across the page.
// Pin icons dispatch "pin:toggle" custom events; this controller listens on document.

const COLUMN_THRESHOLD = 10;

export default class extends Controller {
    static targets = ["toggle", "panel", "list", "empty", "count"];
    static values = { sortOrder: { type: String, default: "pinnedDate" } };

    // In-memory map: "TASK:uuid" → pinId (Long)
    pinnedIds = new Map();
    dragItem = null;
    dropHandled = false;

    connect() {
        this.drawer = setupDrawer({
            element: this.element,
            name: "pins",
            getPanel: () => (this.hasPanelTarget ? this.panelTarget : null),
            getToggle: () => (this.hasToggleTarget ? this.toggleTarget : null),
        });

        // Listen for pin:toggle from pin icons anywhere on the page
        this.pinToggleHandler = (e) => this.handleToggle(e);
        document.addEventListener("pin:toggle", this.pinToggleHandler);

        // Re-sync icons after HTMX content swaps (scoped to swapped element)
        this.afterSettleHandler = (e) => this.syncPinIcons(e.detail?.elt || document);
        document.addEventListener("htmx:afterSettle", this.afterSettleHandler);

        // WebSocket subscription + initial load
        this.deregisterWs = onConnect((client) => {
            client.subscribe("/user/queue/pins", (message) => {
                const data = JSON.parse(message.body);
                if (data.deleted) {
                    this.handleRemoved(data);
                } else if (data.titleOnly) {
                    this.handleTitleSync(data);
                } else if (data.pinned) {
                    this.handlePinned(data);
                }
            });
            this.loadPins();
        });
    }

    disconnect() {
        this.drawer.destroy();
        document.removeEventListener("pin:toggle", this.pinToggleHandler);
        document.removeEventListener("htmx:afterSettle", this.afterSettleHandler);
        if (this.deregisterWs) this.deregisterWs();
    }

    toggle(event) {
        this.drawer.toggle(event);
    }

    // ── API ────────────────────────────────────────────────────────────

    loadPins() {
        fetch(APP_CONFIG.routes.apiPins)
            .then(requireOk)
            .then((res) => res.json())
            .then((items) => {
                this.pinnedIds.clear();
                if (this.hasListTarget) this.listTarget.innerHTML = "";
                items.forEach((item) => {
                    this.pinnedIds.set(`${item.entityType}:${item.entityId}`, item.id);
                    if (this.hasListTarget) {
                        this.listTarget.appendChild(this.createItem(item));
                    }
                });
                this.refreshUI();
            })
            .catch((err) => console.error("Failed to load pins:", err));
    }

    handleToggle(event) {
        const { entityType, entityId, entityTitle } = event.detail;
        if (!entityType || !entityId) return;

        const key = `${entityType}:${entityId}`;
        if (this.pinnedIds.has(key)) {
            const pinId = this.pinnedIds.get(key);
            fetch(`${APP_CONFIG.routes.apiPins}/${pinId}`, {
                method: "DELETE",
                headers: csrfHeaders(),
            })
                .then(requireOk)
                .catch((err) => console.error("Failed to unpin:", err));
        } else {
            fetch(APP_CONFIG.routes.apiPins, {
                method: "POST",
                headers: { "Content-Type": "application/json", ...csrfHeaders() },
                body: JSON.stringify({ entityType, entityId, entityTitle }),
            })
                .then((res) => {
                    if (res.status === 409) {
                        res.json().then((body) => {
                            showToast(body.detail, "warning");
                        });
                        return null;
                    }
                    return requireOk(res);
                })
                .catch((err) => {
                    if (err) console.error("Failed to pin:", err);
                });
        }
    }

    // ── WebSocket handlers ─────────────────────────────────────────────

    handlePinned(data) {
        const key = `${data.entityType}:${data.entityId}`;
        this.pinnedIds.set(key, data.id);
        if (this.hasListTarget) {
            this.removeItemFromList(data.entityType, data.entityId);
            this.listTarget.prepend(this.createItem(data));
        }
        this.refreshUI();
    }

    handleRemoved(data) {
        const key = `${data.entityType}:${data.entityId}`;
        this.pinnedIds.delete(key);
        this.removeItemFromList(data.entityType, data.entityId);
        this.refreshUI();
    }

    handleTitleSync(data) {
        if (this.hasListTarget) {
            const item = this.listTarget.querySelector(
                `[data-entity-type="${data.entityType}"][data-entity-id="${data.entityId}"]`,
            );
            if (item) {
                const titleEl = item.querySelector(".pin-title");
                if (titleEl) titleEl.textContent = data.entityTitle;
                if (data.href) item.href = data.href;
            }
        }
    }

    // ── DOM helpers ────────────────────────────────────────────────────

    get isManualSort() {
        return this.sortOrderValue === "manual";
    }

    refreshUI() {
        this.updateEmptyState();
        this.updateCount();
        this.updateColumns();
        this.syncPinIcons(document);
    }

    createItem(item) {
        const a = document.createElement("a");
        a.href = item.href;
        a.className = "pins-grid-item";
        a.dataset.entityType = item.entityType;
        a.dataset.entityId = item.entityId;
        a.dataset.pinId = item.id;

        if (this.isManualSort) {
            a.draggable = true;
            a.addEventListener("dragstart", (e) => this.onDragStart(e));
            a.addEventListener("dragover", (e) => this.onDragOver(e));
            a.addEventListener("dragend", () => this.onDragEnd());
            a.addEventListener("drop", (e) => this.onDrop(e));
        }

        const iconClass = item.entityType === "TASK" ? "bi bi-check2-square text-primary" : "bi bi-folder text-info";
        const dragHandle = this.isManualSort
            ? `<i class="bi bi-grip-vertical text-muted" style="cursor: grab; font-size: 0.75rem;"></i>`
            : "";

        a.innerHTML = `${dragHandle}<i class="${iconClass}" style="font-size: 0.85rem;"></i>
            <div class="text-truncate flex-grow-1" style="min-width: 0;">
                <div class="text-truncate small pin-title">${escapeHtml(item.entityTitle)}</div>
            </div>
            <button type="button" class="btn btn-sm btn-link p-0 pin-unpin-btn pin-color"
                    title="${t("pins.toggle") || "Unpin"}">
                <i class="bi bi-pin-angle-fill"></i>
            </button>`;

        // Unpin button — stop propagation so the link doesn't navigate
        const unpinBtn = a.querySelector(".pin-unpin-btn");
        unpinBtn.addEventListener("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            document.dispatchEvent(
                new CustomEvent("pin:toggle", {
                    detail: {
                        entityType: item.entityType,
                        entityId: item.entityId,
                        entityTitle: item.entityTitle,
                    },
                }),
            );
        });

        return a;
    }

    removeItemFromList(entityType, entityId) {
        if (!this.hasListTarget) return;
        const existing = this.listTarget.querySelector(
            `[data-entity-type="${entityType}"][data-entity-id="${entityId}"]`,
        );
        if (existing) existing.remove();
    }

    updateEmptyState() {
        if (this.hasEmptyTarget) {
            this.emptyTarget.classList.toggle("d-none", this.pinnedIds.size > 0);
        }
    }

    updateCount() {
        if (this.hasCountTarget) {
            this.countTarget.textContent = this.pinnedIds.size;
        }
    }

    updateColumns() {
        if (!this.hasPanelTarget) return;
        const cols = this.pinnedIds.size > COLUMN_THRESHOLD ? "2" : "1";
        this.panelTarget.dataset.columns = cols;
    }

    syncPinIcons(root) {
        root.querySelectorAll("[data-pin-target]").forEach((el) => {
            const key = `${el.dataset.entityType}:${el.dataset.entityId}`;
            const isPinned = this.pinnedIds.has(key);
            const pinIcon = el.querySelector(".bi-pin-angle");
            const pinFillIcon = el.querySelector(".bi-pin-angle-fill");
            if (pinIcon) pinIcon.classList.toggle("d-none", isPinned);
            if (pinFillIcon) pinFillIcon.classList.toggle("d-none", !isPinned);
        });
    }

    // ── Drag-and-drop reorder (manual sort only) ──────────────────────

    onDragStart(e) {
        this.dragItem = e.currentTarget;
        this.dropHandled = false;
        this.dragItem.classList.add("pins-dragging");
        e.dataTransfer.effectAllowed = "move";
    }

    onDragOver(e) {
        e.preventDefault();
        e.dataTransfer.dropEffect = "move";
        const target = e.currentTarget;
        if (target === this.dragItem || !this.dragItem) return;

        const rect = target.getBoundingClientRect();
        const midY = rect.top + rect.height / 2;
        if (e.clientY < midY) {
            target.parentNode.insertBefore(this.dragItem, target);
        } else {
            target.parentNode.insertBefore(this.dragItem, target.nextSibling);
        }
    }

    onDrop(e) {
        e.preventDefault();
        this.dropHandled = true;
        this.saveOrder();
    }

    onDragEnd() {
        if (this.dragItem) {
            this.dragItem.classList.remove("pins-dragging");
            this.dragItem = null;
        }
        if (!this.dropHandled) {
            this.saveOrder();
        }
    }

    saveOrder() {
        if (!this.hasListTarget) return;
        const orderedIds = [...this.listTarget.querySelectorAll("[data-pin-id]")].map((el) =>
            parseInt(el.dataset.pinId),
        );
        fetch(`${APP_CONFIG.routes.apiPins}/reorder`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json", ...csrfHeaders() },
            body: JSON.stringify(orderedIds),
        })
            .then(requireOk)
            .catch((err) => console.error("Failed to save pin order:", err));
    }
}
