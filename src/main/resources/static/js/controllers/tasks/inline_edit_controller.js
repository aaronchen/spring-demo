import { Controller } from "@hotwired/stimulus";
import { csrfHeaders } from "lib/api";
import { showToast } from "lib/toast";
import { resolveLabel } from "lib/i18n";

const INLINE_PRIORITY_OPTIONS = ["LOW", "MEDIUM", "HIGH"]
    .map((v) => ({ value: v, label: resolveLabel("task.priority", v) }));

const INLINE_STATUS_OPTIONS = ["BACKLOG", "OPEN", "IN_PROGRESS", "IN_REVIEW", "COMPLETED", "CANCELLED"]
    .map((v) => ({ value: v, label: resolveLabel("task.status", v) }));

// Inline editing for table view — toggle edit mode to make cells editable.

export default class extends Controller {
    connect() {
        this.editModeActive = false;

        const tasksView = document.getElementById("tasks-view");
        if (!tasksView) return;

        this.cellClickHandler = (e) => {
            if (!this.editModeActive) return;
            const cell = e.target.closest('td[data-editable="true"]');
            if (!cell || cell.querySelector(".inline-edit-input")) return;
            e.preventDefault();
            e.stopPropagation();
            this.startInlineEdit(cell);
        };
        tasksView.addEventListener("click", this.cellClickHandler, true);

        this.afterSwapHandler = (evt) => {
            if (evt.detail.target?.id === "tasks-view" && this.editModeActive) {
                this.applyEditModeStyles();
            }
        };
        document.addEventListener("htmx:afterSwap", this.afterSwapHandler);

        // Listen for events from sibling controllers
        this.editModeOffHandler = () => {
            if (this.editModeActive) this.toggleEditMode();
        };
        this.toggleEditModeHandler = () => this.toggleEditMode();
        this.element.addEventListener("tasks:edit-mode-off", this.editModeOffHandler);
        this.element.addEventListener("tasks:toggle-edit-mode", this.toggleEditModeHandler);
    }

    disconnect() {
        const tasksView = document.getElementById("tasks-view");
        if (tasksView && this.cellClickHandler) {
            tasksView.removeEventListener("click", this.cellClickHandler, true);
        }
        if (this.afterSwapHandler) {
            document.removeEventListener("htmx:afterSwap", this.afterSwapHandler);
        }
        this.element.removeEventListener("tasks:edit-mode-off", this.editModeOffHandler);
        this.element.removeEventListener("tasks:toggle-edit-mode", this.toggleEditModeHandler);
    }

    toggleEditMode() {
        this.editModeActive = !this.editModeActive;
        const btn = document.getElementById("edit-mode-btn");
        const icon = btn?.querySelector("i.bi");
        if (btn) btn.classList.toggle("active", this.editModeActive);
        if (icon) {
            icon.classList.toggle("bi-pencil-square", !this.editModeActive);
            icon.classList.toggle("bi-pencil-fill", this.editModeActive);
        }
        if (this.editModeActive) {
            this.applyEditModeStyles();
        } else {
            this.removeEditModeStyles();
        }
    }

    applyEditModeStyles() {
        document.querySelectorAll('td[data-editable="true"]').forEach((cell) => {
            cell.classList.add("inline-edit-active");
            cell.querySelectorAll("a").forEach((link) => {
                link.dataset.originalHref = link.getAttribute("href");
                link.removeAttribute("href");
                link.style.pointerEvents = "none";
            });
        });
    }

    removeEditModeStyles() {
        document.querySelectorAll("td.inline-edit-active").forEach((cell) => {
            cell.classList.remove("inline-edit-active");
            cell.querySelectorAll("a").forEach((link) => {
                if (link.dataset.originalHref) {
                    link.setAttribute("href", link.dataset.originalHref);
                    delete link.dataset.originalHref;
                }
                link.style.pointerEvents = "";
            });
        });
    }

    startInlineEdit(cell) {
        const field = cell.dataset.field;
        const taskId = cell.dataset.taskId;
        const currentValue = cell.dataset.value || "";
        const originalContent = cell.innerHTML;

        switch (field) {
            case "title":
            case "description":
                this.showTextInput(cell, taskId, field, currentValue, originalContent);
                break;
            case "priority":
                this.showSelectInput(cell, taskId, field, currentValue, originalContent, INLINE_PRIORITY_OPTIONS);
                break;
            case "status":
                this.showSelectInput(cell, taskId, field, currentValue, originalContent, INLINE_STATUS_OPTIONS);
                break;
            case "effort":
                this.showNumberInput(cell, taskId, field, currentValue, originalContent);
                break;
            case "dueDate":
                this.showDateInput(cell, taskId, field, currentValue, originalContent);
                break;
        }
    }

    showTextInput(cell, taskId, field, currentValue, originalContent) {
        const input = document.createElement("input");
        input.type = "text";
        input.value = currentValue;
        input.className = "form-control form-control-sm inline-edit-input";

        cell.innerHTML = "";
        cell.appendChild(input);
        input.focus();
        setTimeout(() => { input.setSelectionRange(0, 0); input.scrollLeft = 0; }, 0);

        input.addEventListener("keydown", (e) => {
            if (e.key === "Enter") { e.preventDefault(); this.saveInlineEdit(cell, taskId, field, input.value, originalContent); }
            else if (e.key === "Escape") { this.cancelInlineEdit(cell, originalContent); }
        });
        input.addEventListener("blur", () => {
            setTimeout(() => { if (cell.contains(input)) this.saveInlineEdit(cell, taskId, field, input.value, originalContent); }, 150);
        });
    }

    showSelectInput(cell, taskId, field, currentValue, originalContent, options) {
        const select = document.createElement("select");
        select.className = "form-select form-select-sm inline-edit-input";

        const row = cell.closest("tr");
        const isBlocked = row?.dataset.blocked === "true";

        options.forEach((opt) => {
            const option = document.createElement("option");
            option.value = opt.value;
            option.textContent = opt.label;
            if (opt.value === currentValue) option.selected = true;
            if (field === "status" && isBlocked && opt.value === "COMPLETED") option.disabled = true;
            select.appendChild(option);
        });

        cell.innerHTML = "";
        cell.appendChild(select);
        select.focus();

        select.addEventListener("change", () => this.saveInlineEdit(cell, taskId, field, select.value, originalContent));
        select.addEventListener("keydown", (e) => { if (e.key === "Escape") this.cancelInlineEdit(cell, originalContent); });
        select.addEventListener("blur", () => {
            setTimeout(() => { if (cell.contains(select)) this.cancelInlineEdit(cell, originalContent); }, 150);
        });
    }

    showDateInput(cell, taskId, field, currentValue, originalContent) {
        const input = document.createElement("input");
        input.type = "date";
        input.value = currentValue;
        input.className = "form-control form-control-sm inline-edit-input";

        cell.innerHTML = "";
        cell.appendChild(input);
        input.focus();

        input.addEventListener("change", () => this.saveInlineEdit(cell, taskId, field, input.value, originalContent));
        input.addEventListener("keydown", (e) => { if (e.key === "Escape") this.cancelInlineEdit(cell, originalContent); });
        input.addEventListener("blur", () => {
            setTimeout(() => { if (cell.contains(input)) this.saveInlineEdit(cell, taskId, field, input.value, originalContent); }, 150);
        });
    }

    showNumberInput(cell, taskId, field, currentValue, originalContent) {
        const input = document.createElement("input");
        input.type = "number";
        input.value = currentValue;
        input.min = "0";
        input.max = "32767";
        input.className = "form-control form-control-sm inline-edit-input";
        input.style.width = "80px";

        cell.innerHTML = "";
        cell.appendChild(input);
        input.focus();
        input.select();

        input.addEventListener("keydown", (e) => {
            if (e.key === "Enter") { e.preventDefault(); this.saveInlineEdit(cell, taskId, field, input.value, originalContent); }
            else if (e.key === "Escape") { this.cancelInlineEdit(cell, originalContent); }
        });
        input.addEventListener("blur", () => {
            setTimeout(() => { if (cell.contains(input)) this.saveInlineEdit(cell, taskId, field, input.value, originalContent); }, 150);
        });
    }

    cancelInlineEdit(cell, originalContent) {
        cell.innerHTML = originalContent;
        if (this.editModeActive) {
            cell.classList.add("inline-edit-active");
            cell.querySelectorAll("a").forEach((link) => {
                link.dataset.originalHref = link.getAttribute("href");
                link.removeAttribute("href");
                link.style.pointerEvents = "none";
            });
        }
    }

    saveInlineEdit(cell, taskId, field, value, originalContent) {
        const row = cell.closest("tr");
        const params = new URLSearchParams();
        params.set("field", field);
        params.set("value", value);

        const headers = { "Content-Type": "application/x-www-form-urlencoded", "HX-Request": "true", ...csrfHeaders() };

        fetch(`${APP_CONFIG.routes.tasks}/${taskId}/field`, {
            method: "PATCH",
            headers,
            body: params.toString(),
        }).then((response) => {
            if (response.ok) return response.text();
            if (response.headers.get("Content-Type")?.includes("json")) {
                return response.json().then((data) => { throw new Error(data.detail || "Failed to save"); });
            }
            throw new Error("Failed to save");
        }).then((html) => {
            const template = document.createElement("template");
            template.innerHTML = html.trim();
            const newRow = template.content.querySelector("tr");
            if (newRow && row) {
                row.replaceWith(newRow);
                htmx.process(newRow);
                if (this.editModeActive) {
                    newRow.querySelectorAll('td[data-editable="true"]').forEach((c) => {
                        c.classList.add("inline-edit-active");
                        c.querySelectorAll("a").forEach((link) => {
                            link.dataset.originalHref = link.getAttribute("href");
                            link.removeAttribute("href");
                            link.style.pointerEvents = "none";
                        });
                    });
                }
            }
        }).catch((err) => {
            this.cancelInlineEdit(cell, originalContent);
            showToast(err.message || APP_CONFIG.messages["toast.error.generic"] || "Failed to save", "danger");
        });
    }

}
