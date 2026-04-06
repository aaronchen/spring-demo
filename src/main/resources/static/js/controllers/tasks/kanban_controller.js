import { Controller } from "@hotwired/stimulus";
import { showToast } from "lib/toast";

// Kanban board — drag-and-drop status changes using native HTML5 DnD API.
// NOTE: Drag handler functions exposed on window temporarily for ondrag* attributes.

export default class extends Controller {
    connect() {
        this.exposeGlobals();
    }

    disconnect() {
        this.removeGlobals();
    }

    dragStart(e) {
        const card = e.target.closest(".kanban-card");
        e.dataTransfer.setData("text/plain", card.dataset.taskId);
        e.dataTransfer.effectAllowed = "move";
        card.classList.add("kanban-dragging");
    }

    dragEnd(e) {
        const card = e.target.closest(".kanban-card");
        if (card) card.classList.remove("kanban-dragging");
        document.querySelectorAll(".kanban-column").forEach((col) => {
            col.classList.remove("kanban-drop-target");
        });
    }

    dragOver(e) {
        e.preventDefault();
        e.dataTransfer.dropEffect = "move";
    }

    dragEnter(e) {
        e.preventDefault();
        const column = e.target.closest(".kanban-column");
        if (column) column.classList.add("kanban-drop-target");
    }

    dragLeave(e) {
        const column = e.target.closest(".kanban-column");
        if (column && !column.contains(e.relatedTarget)) {
            column.classList.remove("kanban-drop-target");
        }
    }

    drop(e) {
        e.preventDefault();
        const column = e.target.closest(".kanban-column");
        if (!column) return;
        column.classList.remove("kanban-drop-target");

        const taskId = e.dataTransfer.getData("text/plain");
        const newStatus = column.dataset.status;

        const card = document.querySelector(`.kanban-card[data-task-id="${taskId}"]`);
        if (!card) return;

        const oldStatus = card.dataset.status;
        if (oldStatus === newStatus) return;

        if (card.dataset.blocked === "true" && newStatus === "COMPLETED") {
            showToast(APP_CONFIG.messages["task.dependency.blocked.drag"] || "This task is blocked", "warning");
            return;
        }

        // Optimistic UI update
        const columnBody = column.querySelector(".kanban-column-body");
        const emptyMsg = columnBody.querySelector(".kanban-empty");
        if (emptyMsg) emptyMsg.remove();
        columnBody.appendChild(card);
        card.dataset.status = newStatus;
        card.classList.remove("kanban-dragging");
        this.updateColumnCounts();

        // POST status change
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "X-CSRF-TOKEN";

        const params = new URLSearchParams();
        params.set("status", newStatus);

        const headers = { "Content-Type": "application/x-www-form-urlencoded", "HX-Request": "true" };
        if (csrfToken) headers[csrfHeader] = csrfToken;

        fetch(`${APP_CONFIG.routes.tasks}/${taskId}/status`, {
            method: "POST",
            headers,
            body: params.toString(),
        }).then((response) => {
            if (!response.ok) {
                this.revertCard(card, oldStatus);
                if (response.status === 409) {
                    response.json().then((data) => {
                        showToast(data.detail || APP_CONFIG.messages["task.dependency.blocked.drag"] || "This task is blocked", "warning");
                    }).catch(() => {
                        showToast(APP_CONFIG.messages["task.dependency.blocked.drag"] || "This task is blocked", "warning");
                    });
                } else {
                    showToast(APP_CONFIG.messages["toast.error.generic"] || "Failed to update status", "danger");
                }
            }
        }).catch(() => {
            this.revertCard(card, oldStatus);
            showToast(APP_CONFIG.messages["toast.error.generic"] || "Failed to update status", "danger");
        });
    }

    revertCard(card, oldStatus) {
        const oldColumn = document.querySelector(`.kanban-column[data-status="${oldStatus}"]`);
        if (oldColumn) {
            oldColumn.querySelector(".kanban-column-body").appendChild(card);
            card.dataset.status = oldStatus;
        }
        this.updateColumnCounts();
    }

    updateColumnCounts() {
        document.querySelectorAll(".kanban-column").forEach((col) => {
            const count = col.querySelectorAll(".kanban-card").length;
            const badge = col.querySelector(".kanban-count");
            if (badge) badge.textContent = count;

            const body = col.querySelector(".kanban-column-body");
            const existing = body.querySelector(".kanban-empty");
            if (count === 0 && !existing) {
                const emptyDiv = document.createElement("div");
                emptyDiv.className = "kanban-empty text-muted small text-center py-3";
                emptyDiv.textContent = APP_CONFIG.messages["task.board.empty"] || "No tasks";
                body.appendChild(emptyDiv);
            } else if (count > 0 && existing) {
                existing.remove();
            }
        });
    }

    exposeGlobals() {
        window.kanbanDragStart = (e) => this.dragStart(e);
        window.kanbanDragEnd = (e) => this.dragEnd(e);
        window.kanbanDragOver = (e) => this.dragOver(e);
        window.kanbanDragEnter = (e) => this.dragEnter(e);
        window.kanbanDragLeave = (e) => this.dragLeave(e);
        window.kanbanDrop = (e) => this.drop(e);
    }

    removeGlobals() {
        ["kanbanDragStart", "kanbanDragEnd", "kanbanDragOver",
         "kanbanDragEnter", "kanbanDragLeave", "kanbanDrop"].forEach((n) => delete window[n]);
    }
}
