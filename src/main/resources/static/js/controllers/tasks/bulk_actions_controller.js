import { Controller } from "@hotwired/stimulus";
import { requireOk, csrfHeaders } from "lib/api";
import { showToast } from "lib/toast";
import { showConfirm } from "lib/confirm";
import { t } from "lib/i18n";

// Bulk actions for table view — cross-page selection with floating action bar.

export default class extends Controller {
    connect() {
        this.selectedIds = new Set();
        this.selectedProjectIds = new Map();
        this.assignUsers = [];
        this.assignProjectId = null;

        // Re-check boxes after HTMX swaps (page navigation)
        this.afterSwapHandler = (evt) => {
            if (evt.detail.target?.id === "tasks-view") {
                this.recheckVisibleBoxes();
                this.renderBar();
            }
        };
        document.addEventListener("htmx:afterSwap", this.afterSwapHandler);

        // Load user list for assign dropdown when it opens
        const assignDropdown = document.getElementById("bulkAssignDropdown");
        if (assignDropdown) {
            assignDropdown.addEventListener("shown.bs.dropdown", () => {
                const currentProjectId = this.getCommonProjectId();
                if (this.assignUsers.length === 0 || this.assignProjectId !== currentProjectId) {
                    this.loadAssignUsers();
                } else {
                    this.renderAssignList(this.assignUsers);
                }
                const searchInput = document.getElementById("bulk-assign-search");
                if (searchInput) {
                    searchInput.value = "";
                    searchInput.focus();
                }
            });
        }

        // Listen for clear events from list controller
        this.bulkClearHandler = () => this.clearSelection();
        this.element.addEventListener("tasks:bulk-clear", this.bulkClearHandler);
    }

    disconnect() {
        if (this.afterSwapHandler) {
            document.removeEventListener("htmx:afterSwap", this.afterSwapHandler);
        }
        this.element.removeEventListener("tasks:bulk-clear", this.bulkClearHandler);
    }

    // ── Selection ────────────────────────────────────────────────────────

    onSelectChange(eventOrCheckbox) {
        const checkbox = eventOrCheckbox?.currentTarget || eventOrCheckbox;
        const taskId = checkbox.dataset.taskId;
        const projectId = checkbox.dataset.projectId;
        if (checkbox.checked) {
            this.selectedIds.add(taskId);
            this.selectedProjectIds.set(taskId, projectId);
        } else {
            this.selectedIds.delete(taskId);
            this.selectedProjectIds.delete(taskId);
        }
        this.renderBar();
    }

    toggleSelectAll(eventOrChecked) {
        const checked = eventOrChecked?.currentTarget ? eventOrChecked.currentTarget.checked : eventOrChecked;
        document.querySelectorAll(".bulk-select-checkbox").forEach((cb) => {
            cb.checked = checked;
            const taskId = cb.dataset.taskId;
            const projectId = cb.dataset.projectId;
            if (checked) {
                this.selectedIds.add(taskId);
                this.selectedProjectIds.set(taskId, projectId);
            } else {
                this.selectedIds.delete(taskId);
                this.selectedProjectIds.delete(taskId);
            }
        });
        this.renderBar();
    }

    clearSelection() {
        this.selectedIds.clear();
        this.selectedProjectIds.clear();
        this.assignUsers = [];
        this.assignProjectId = null;
        document.querySelectorAll(".bulk-select-checkbox").forEach((cb) => {
            cb.checked = false;
        });
        const selectAll = document.getElementById("bulk-select-all");
        if (selectAll) selectAll.checked = false;
        this.renderBar();
    }

    recheckVisibleBoxes() {
        document.querySelectorAll(".bulk-select-checkbox").forEach((cb) => {
            const taskId = cb.dataset.taskId;
            cb.checked = this.selectedIds.has(taskId);
            if (cb.checked) this.selectedProjectIds.set(taskId, cb.dataset.projectId);
        });
        this.updateSelectAllState();
    }

    updateSelectAllState() {
        const selectAll = document.getElementById("bulk-select-all");
        if (!selectAll) return;
        const boxes = document.querySelectorAll(".bulk-select-checkbox");
        if (boxes.length === 0) {
            selectAll.checked = false;
            selectAll.indeterminate = false;
            return;
        }
        const checkedCount = Array.from(boxes).filter((cb) => cb.checked).length;
        selectAll.checked = checkedCount === boxes.length;
        selectAll.indeterminate = checkedCount > 0 && checkedCount < boxes.length;
    }

    getCommonProjectId() {
        const projectIds = new Set(this.selectedProjectIds.values());
        return projectIds.size === 1 ? projectIds.values().next().value : null;
    }

    renderBar() {
        const bar = document.getElementById("bulk-action-bar");
        if (!bar) return;
        const count = this.selectedIds.size;
        if (count > 0) {
            bar.classList.remove("d-none");
            const label = t("task.bulk.selected", count) || `${count} selected`;
            document.getElementById("bulk-selected-count").textContent = label;
        } else {
            bar.classList.add("d-none");
        }

        const assignBtn = document.getElementById("bulk-assign-container");
        if (assignBtn) {
            const commonProjectId = this.getCommonProjectId();
            assignBtn.classList.toggle("d-none", commonProjectId === null);
            if (commonProjectId !== this.assignProjectId) {
                this.assignUsers = [];
                this.assignProjectId = null;
            }
        }

        this.updateSelectAllState();
    }

    // ── Actions ──────────────────────────────────────────────────────────

    executeAction(eventOrAction, value) {
        let action;
        if (eventOrAction?.currentTarget) {
            action = eventOrAction.currentTarget.dataset.bulkAction;
            value = eventOrAction.currentTarget.dataset.value || "";
        } else {
            action = eventOrAction;
        }
        if (this.selectedIds.size === 0) return;

        const headers = { "Content-Type": "application/json", ...csrfHeaders() };

        fetch(`${APP_CONFIG.routes.tasks}/bulk`, {
            method: "POST",
            headers,
            body: JSON.stringify({ taskIds: Array.from(this.selectedIds), action, value: value || "" }),
        })
            .then(requireOk)
            .then((response) => response.json())
            .then((data) => {
                const count = data.count || 0;
                const msgKey = action === "DELETE" ? "toast.task.bulk.deleted" : "toast.task.bulk.success";
                const msg = t(msgKey, count) || `${count} tasks updated.`;
                showToast(msg, "success");
                if (data.skipped > 0) showToast(data.skippedMessage, "warning");
                this.clearSelection();
                this.element.dispatchEvent(new CustomEvent("tasks:refresh", { bubbles: true }));
            })
            .catch(() => {
                showToast(t("toast.error.generic") || "An error occurred", "danger");
            });
    }

    executeEffort() {
        const input = document.getElementById("bulk-effort-input");
        if (input) this.executeAction("EFFORT", input.value);
    }

    executeDelete() {
        if (this.selectedIds.size === 0) return;
        const count = this.selectedIds.size;
        const msg = t("task.bulk.confirm.delete", count) || `Are you sure you want to delete ${count} tasks?`;

        showConfirm(
            {
                title: t("task.bulk.confirm.delete.title") || "Delete Tasks",
                message: msg,
                confirmText: t("action.delete") || "Delete",
                confirmClass: "btn btn-danger",
                headerClass: "bg-danger text-white",
            },
            () => {
                this.executeAction("DELETE", "");
            },
        );
    }

    // ── Assign user list ─────────────────────────────────────────────────

    loadAssignUsers() {
        const projectId = this.getCommonProjectId();
        if (!projectId) return;

        const url = APP_CONFIG.routes.apiProjectMembersAssignable.params({ projectId }).build();
        fetch(url)
            .then(requireOk)
            .then((r) => r.json())
            .then((users) => {
                this.assignUsers = users;
                this.assignProjectId = projectId;
                this.renderAssignList(users);
            })
            .catch((err) => console.error("Failed to load assignable users:", err));
    }

    filterAssignUsers(eventOrQuery) {
        const query = eventOrQuery?.currentTarget ? eventOrQuery.currentTarget.value : eventOrQuery;
        const q = query.toLowerCase();
        const filtered = q ? this.assignUsers.filter((u) => u.name.toLowerCase().includes(q)) : this.assignUsers;
        this.renderAssignList(filtered);
    }

    renderAssignList(users) {
        const container = document.getElementById("bulk-assign-list");
        if (!container) return;
        container.innerHTML = "";

        const unassignLink = document.createElement("a");
        unassignLink.className = "dropdown-item text-muted";
        unassignLink.href = "#";
        unassignLink.innerHTML = `<i class="bi bi-person-slash"></i> ${t("task.field.user.unassigned") || "Unassigned"}`;
        unassignLink.addEventListener("click", (e) => {
            e.preventDefault();
            this.executeAction("ASSIGN", "");
            this.closeAssignDropdown();
        });
        container.appendChild(unassignLink);

        users.forEach((user) => {
            const link = document.createElement("a");
            link.className = "dropdown-item";
            link.href = "#";
            link.textContent = user.name;
            link.addEventListener("click", (e) => {
                e.preventDefault();
                this.executeAction("ASSIGN", String(user.id));
                this.closeAssignDropdown();
            });
            container.appendChild(link);
        });
    }

    closeAssignDropdown() {
        const dd = document.getElementById("bulkAssignDropdown");
        if (dd) bootstrap.Dropdown.getOrCreateInstance(dd).hide();
    }
}
