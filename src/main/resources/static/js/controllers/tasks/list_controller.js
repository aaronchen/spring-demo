import { Controller } from "@hotwired/stimulus";
import { requireOk } from "lib/api";
import { showToast } from "lib/toast";
import { showConfirm } from "lib/confirm";
import { getCookie, setCookie } from "lib/cookies";
import { onConnect } from "lib/websocket";
import { resolveLabel, t } from "lib/i18n";

// Task list page — sort, filter, search, pagination, view switching, saved views,
// WebSocket stale-data banners, and modal state management.
//
// Mounted on the workspace wrapper in tasks.html and project.html.
// TASKS_BASE is configured via data-tasks--list-base-value.

function buildSortLabels() {
    return {
        "title,asc": t("task.sort.title.asc"),
        "title,desc": t("task.sort.title.desc"),
        "createdAt,asc": t("task.sort.createdAt.asc"),
        "createdAt,desc": t("task.sort.createdAt.desc"),
        "priorityOrder,asc": t("task.sort.priorityOrder.asc"),
        "priorityOrder,desc": t("task.sort.priorityOrder.desc"),
        "dueDate,asc": t("task.sort.dueDate.asc"),
        "dueDate,desc": t("task.sort.dueDate.desc"),
        "updatedAt,asc": t("task.sort.updatedAt.asc"),
        "updatedAt,desc": t("task.sort.updatedAt.desc"),
        "description,asc": t("task.sort.description.asc"),
        "description,desc": t("task.sort.description.desc"),
    };
}

const OVERDUE_CONFIG = { btnCss: "btn-danger", icon: "bi-exclamation-circle-fill" };

const priorityEnum = APP_CONFIG.enums.priority;

export default class extends Controller {
    static values = {
        base: String, // task list base URL (replaces TASKS_BASE_OVERRIDE)
        wsProjectIds: String, // comma-separated project IDs for WebSocket subscriptions
    };

    connect() {
        this.activeSorts = [{ field: "createdAt", direction: "desc" }];
        this.currentPage = 0;
        this.pageSize = parseInt(getCookie("pageSize") || "25");
        this.currentView = "cards";
        this.currentMonth = null;
        this.selectedUserId = null;
        this.selectedTagIds = [];
        this.activeViewName = null;
        this._keepActiveView = false;
        this.searchDebounce = null;

        // Store default label
        const mineBtn = document.getElementById("user-filter-mine");
        if (mineBtn) {
            const label = document.getElementById("user-filter-label");
            if (label) mineBtn.dataset.defaultLabel = label.textContent;
            if (!this.selectedUserId && mineBtn.classList.contains("active")) {
                this.selectedUserId = mineBtn.dataset.currentUserId;
            }
        }

        this.initFromUrl();
        this.bindEvents();
        this.loadSavedViews();
        this.initWebSocket();

        // Listen for events from sibling controllers
        this.refreshHandler = () => this.doSearch(false);
        this.switchViewHandler = (e) => this.switchView(e.detail.view);
        this.element.addEventListener("tasks:refresh", this.refreshHandler);
        this.element.addEventListener("tasks:switch-view", this.switchViewHandler);
    }

    disconnect() {
        window.removeEventListener("popstate", this.popstateHandler);
        document.removeEventListener("htmx:afterSwap", this.afterSwapHandler);
        document.body.removeEventListener("taskSaved", this.taskSavedHandler);
        document.body.removeEventListener("taskDeleted", this.taskDeletedHandler);
        this.element.removeEventListener("tasks:refresh", this.refreshHandler);
        this.element.removeEventListener("tasks:switch-view", this.switchViewHandler);
    }

    // ── Event binding ────────────────────────────────────────────────────

    bindEvents() {
        const tasksView = document.getElementById("tasks-view");

        // Pagination custom events (safe: tasksView is inside this.element, destroyed with it)
        tasksView.addEventListener("pagination:navigate", (e) => this.navigateToPage(e.detail.page));
        tasksView.addEventListener("pagination:resize", (e) => this.onPageSizeChange(e.detail.size));

        // Debounced live search (safe: searchInput is inside this.element)
        const searchInput = document.getElementById("search-input");
        const searchClearBtn = document.getElementById("search-clear-btn");

        searchInput.addEventListener("input", () => {
            searchClearBtn.classList.toggle("d-none", searchInput.value === "");
            clearTimeout(this.searchDebounce);
            this.searchDebounce = setTimeout(() => this.doSearch(true), 300);
        });

        searchClearBtn.addEventListener("click", () => {
            searchInput.value = "";
            searchClearBtn.classList.add("d-none");
            searchInput.focus();
            this.doSearch(true);
        });

        // Document-level listeners — stored for cleanup in disconnect()
        this.afterSwapHandler = (evt) => {
            if (evt.detail.target?.id === "tasks-view") {
                this.highlightActiveTags();
            }
            if (evt.detail.target?.id === "task-modal-content") {
                bootstrap.Modal.getOrCreateInstance(document.getElementById("task-modal")).show();
            }
        };
        document.addEventListener("htmx:afterSwap", this.afterSwapHandler);

        this.taskSavedHandler = () => {
            const modal = document.getElementById("task-modal");
            if (modal) bootstrap.Modal.getInstance(modal)?.hide();
            showToast(t("toast.task.saved"), "success");
            this.doSearch(false);
        };
        document.body.addEventListener("taskSaved", this.taskSavedHandler);

        this.taskDeletedHandler = () => {
            showToast(t("toast.task.deleted"), "success");
            this.doSearch(false);
        };
        document.body.addEventListener("taskDeleted", this.taskDeletedHandler);

        // Delete modal (safe: inside this.element)
        const deleteModal = document.getElementById("task-delete-modal");
        if (deleteModal)
            deleteModal.addEventListener("show.bs.modal", (e) => {
                const btn = e.relatedTarget;
                if (!btn) return;
                document.getElementById("task-delete-modal-title").textContent = btn.dataset.taskTitle;
                const confirmBtn = document.getElementById("delete-confirm-btn");
                confirmBtn.setAttribute(
                    "hx-delete",
                    APP_CONFIG.routes.taskDetail.params({ taskId: btn.dataset.taskId }).build(),
                );
                htmx.process(confirmBtn);
            });

        // Browser back/forward
        this.popstateHandler = () => {
            this.initFromUrl();
            this.doSearch(false);
        };
        window.addEventListener("popstate", this.popstateHandler);

        this.initSavedViewsDelegation();
    }

    // ── WebSocket ────────────────────────────────────────────────────────

    initWebSocket() {
        const currentUserId = document.querySelector('meta[name="_userId"]')?.content;
        const staleBanner = document.getElementById("stale-banner");
        const staleRefresh = document.getElementById("stale-banner-refresh");
        const wsProjectIds = this.hasWsProjectIdsValue ? this.wsProjectIdsValue : null;

        if (!staleBanner || !wsProjectIds) return;

        onConnect((client) => {
            wsProjectIds.split(",").forEach((id) => {
                const topic = APP_CONFIG.routes.topicProjectTasks.params({ projectId: id.trim() }).build();
                client.subscribe(topic, (message) => {
                    const data = JSON.parse(message.body);
                    if (currentUserId && String(data.userId) === currentUserId) return;
                    staleBanner.classList.remove("d-none");
                });
            });
        });

        staleRefresh.addEventListener("click", (e) => {
            e.preventDefault();
            staleBanner.classList.add("d-none");
            this.doSearch(false);
        });
    }

    // ── URL / Search / Pagination ────────────────────────────────────────

    buildUrl(page) {
        const params = new URLSearchParams();
        const search = document.getElementById("search-input").value;
        const statusFilter = document.getElementById("current-status-filter").value;
        const overdue = document.getElementById("current-overdue-filter").value;
        if (search) params.set("search", search);
        if (statusFilter && statusFilter !== "ALL") params.set("statusFilter", statusFilter);
        if (overdue === "true") params.set("overdue", "true");
        const priority = document.getElementById("current-priority-filter").value;
        if (priority) params.set("priority", priority);
        this.activeSorts.forEach((s) => params.append("sort", `${s.field},${s.direction}`));
        if (this.currentView === "calendar") {
            if (this.currentMonth) params.set("month", this.currentMonth);
        } else if (this.currentView === "board") {
            // Board view: unpaged
        } else {
            params.set("size", this.pageSize);
            if (page > 0) params.set("page", page);
        }
        params.set("selectedUserId", this.selectedUserId || "");
        if (this.selectedTagIds.length > 0) params.set("tags", this.selectedTagIds.join(","));
        const sprintEl = document.getElementById("current-sprint-filter");
        if (sprintEl) params.set("sprintId", sprintEl.value);
        params.set("view", this.currentView);
        return `${this.baseValue}?${params.toString()}`;
    }

    doSearch(resetPage) {
        if (resetPage) {
            this.currentPage = 0;
            this.element.dispatchEvent(new CustomEvent("tasks:bulk-clear", { bubbles: true }));
        }
        if (this._keepActiveView) {
            this._keepActiveView = false;
        } else if (resetPage) {
            this.clearActiveView();
        }
        const url = this.buildUrl(this.currentPage);
        htmx.ajax("GET", url, { target: "#tasks-view", swap: "innerHTML" });
        window.history.replaceState({}, "", url);
    }

    navigateToPage(page) {
        if (page < 0) return;
        this.currentPage = page;
        const url = this.buildUrl(page);
        htmx.ajax("GET", url, { target: "#tasks-view", swap: "innerHTML" });
        window.history.pushState({}, "", url);
    }

    onPageSizeChange(newSize) {
        this.pageSize = parseInt(newSize);
        setCookie("pageSize", this.pageSize);
        this.currentPage = 0;
        this.doSearch(false);
    }

    navigateMonth(eventOrMonth) {
        const month = eventOrMonth?.currentTarget ? eventOrMonth.currentTarget.dataset.month : eventOrMonth;
        this.currentMonth = month;
        this.doSearch(false);
    }

    exportTasks() {
        const params = new URLSearchParams();
        const search = document.getElementById("search-input").value;
        const statusFilter = document.getElementById("current-status-filter").value;
        const overdueVal = document.getElementById("current-overdue-filter").value;
        if (search) params.set("search", search);
        if (statusFilter && statusFilter !== "ALL") params.set("statusFilter", statusFilter);
        if (overdueVal === "true") params.set("overdue", "true");
        const priority = document.getElementById("current-priority-filter").value;
        if (priority) params.set("priority", priority);
        if (this.selectedUserId) params.set("selectedUserId", this.selectedUserId);
        if (this.selectedTagIds.length > 0) params.set("tags", this.selectedTagIds.join(","));
        const sprintEl = document.getElementById("current-sprint-filter");
        if (sprintEl) params.set("sprintId", sprintEl.value);
        if (this.activeSorts.length > 0) {
            params.set("sort", `${this.activeSorts[0].field},${this.activeSorts[0].direction}`);
        }
        window.location.href = `${this.baseValue}/export?${params.toString()}`;
    }

    // ── View switching ───────────────────────────────────────────────────

    switchView(eventOrView) {
        const view = eventOrView?.currentTarget ? eventOrView.currentTarget.dataset.view : eventOrView;
        if (this.currentView === "table" && view !== "table") {
            this.element.dispatchEvent(new CustomEvent("tasks:edit-mode-off", { bubbles: true }));
        }
        this.element.dispatchEvent(new CustomEvent("tasks:bulk-clear", { bubbles: true }));
        this.currentView = view;
        this.clearActiveView();
        this.renderViewToggle();
        this.doSearch(false);
    }

    renderViewToggle() {
        document.getElementById("view-cards")?.classList.toggle("active", this.currentView === "cards");
        document.getElementById("view-table")?.classList.toggle("active", this.currentView === "table");
        document.getElementById("view-calendar")?.classList.toggle("active", this.currentView === "calendar");
        document.getElementById("view-board")?.classList.toggle("active", this.currentView === "board");
    }

    // ── Sort ─────────────────────────────────────────────────────────────

    resetSort() {
        this.activeSorts = [{ field: "createdAt", direction: "desc" }];
        this.renderSorts();
        this.doSearch(true);
    }

    toggleSort(eventOrField, direction) {
        let field;
        if (eventOrField?.currentTarget) {
            field = eventOrField.currentTarget.dataset.sortField;
            direction = eventOrField.currentTarget.dataset.sortDir;
        } else {
            field = eventOrField;
        }
        const idx = this.activeSorts.findIndex((s) => s.field === field && s.direction === direction);
        if (idx >= 0) {
            if (this.activeSorts.length === 1) return;
            this.activeSorts.splice(idx, 1);
        } else {
            const oppositeIdx = this.activeSorts.findIndex((s) => s.field === field);
            if (oppositeIdx >= 0) this.activeSorts.splice(oppositeIdx, 1);
            this.activeSorts.push({ field, direction });
        }
        this.renderSorts();
        this.doSearch(true);
    }

    renderSorts() {
        const label = this.activeSorts
            .map((s) => buildSortLabels()[`${s.field},${s.direction}`] || `${s.field} ${s.direction}`)
            .join(", ");
        document.getElementById("sort-label").textContent = label;

        document.querySelectorAll("[data-sort-field]").forEach((item) => {
            const check = item.querySelector(".sort-check");
            const isActive = this.activeSorts.some(
                (s) => s.field === item.dataset.sortField && s.direction === item.dataset.sortDir,
            );
            if (check) check.style.visibility = isActive ? "visible" : "hidden";
        });
    }

    // ── User filter ──────────────────────────────────────────────────────

    setUserFilter(eventOrId, userName) {
        let id;
        if (eventOrId?.currentTarget) {
            id = eventOrId.currentTarget.dataset.userId;
            userName = eventOrId.currentTarget.dataset.userName;
        } else {
            id = eventOrId;
        }
        this.selectedUserId = String(id);
        this.renderUserFilter(userName);
        this.doSearch(true);
    }

    setMyFilter() {
        const btn = document.getElementById("user-filter-mine");
        this.selectedUserId = btn.dataset.currentUserId;
        this.renderUserFilter(null);
        this.doSearch(true);
    }

    clearUserFilter() {
        this.selectedUserId = null;
        this.renderUserFilter(null);
        this.doSearch(true);
    }

    resetUserFilter(event) {
        if (event?.stopPropagation) event.stopPropagation();
        const mineBtn = document.getElementById("user-filter-mine");
        this.selectedUserId = mineBtn?.dataset.currentUserId;
        this.renderUserFilter(null);
        this.doSearch(true);
    }

    renderUserFilter(userName) {
        const allBtn = document.getElementById("user-filter-all");
        const mineBtn = document.getElementById("user-filter-mine");
        if (!allBtn || !mineBtn) return;
        const label = document.getElementById("user-filter-label");
        const clearIcon = document.getElementById("user-filter-clear");
        const defaultLabel = mineBtn.dataset.defaultLabel || label.textContent;

        allBtn.classList.toggle("active", this.selectedUserId === null);
        mineBtn.classList.toggle("active", this.selectedUserId !== null);

        if (this.selectedUserId === null) {
            label.textContent = defaultLabel;
            clearIcon.classList.add("d-none");
        } else if (this.selectedUserId === mineBtn.dataset.currentUserId) {
            label.textContent = defaultLabel;
            clearIcon.classList.add("d-none");
        } else {
            label.textContent = userName || t("task.field.user");
            clearIcon.classList.remove("d-none");
        }
    }

    // ── Sprint filter ────────────────────────────────────────────────────

    setSprintFilter(eventOrValue) {
        const value = eventOrValue?.currentTarget ? eventOrValue.currentTarget.dataset.value : eventOrValue;
        const hidden = document.getElementById("current-sprint-filter");
        const label = document.getElementById("sprint-filter-label");
        if (hidden) hidden.value = value;
        if (label) {
            if (!value) {
                label.textContent = t("task.field.sprint");
            } else if (value === "0") {
                label.textContent = t("sprint.filter.noSprint");
            } else {
                const item = document.querySelector(`.sprint-filter-item[data-value="${value}"]`);
                if (item) label.textContent = item.dataset.name || item.textContent.trim();
            }
        }
        this.renderSprintFilter();
        this.doSearch(true);
    }

    renderSprintFilter() {
        const hidden = document.getElementById("current-sprint-filter");
        if (!hidden) return;
        const value = hidden.value;
        document.querySelectorAll(".sprint-filter-item").forEach((item) => {
            const check = item.querySelector(".filter-check");
            if (check) check.style.visibility = item.dataset.value === value ? "visible" : "hidden";
        });
    }

    // ── Status filter ────────────────────────────────────────────────────

    setStatusFilter(eventOrStatus) {
        const status = eventOrStatus?.currentTarget ? eventOrStatus.currentTarget.dataset.status || "" : eventOrStatus;
        const current = document.getElementById("current-status-filter").value;
        const currentOverdue = document.getElementById("current-overdue-filter").value;

        if (status === "OVERDUE") {
            if (currentOverdue === "true") {
                document.getElementById("current-overdue-filter").value = "false";
                document.getElementById("current-status-filter").value = "ALL";
            } else {
                document.getElementById("current-overdue-filter").value = "true";
                document.getElementById("current-status-filter").value = "ALL";
            }
        } else if (status) {
            if (current === status && currentOverdue !== "true") {
                document.getElementById("current-status-filter").value = "ALL";
            } else {
                document.getElementById("current-status-filter").value = status;
            }
            document.getElementById("current-overdue-filter").value = "false";
        } else {
            document.getElementById("current-status-filter").value = "ALL";
            document.getElementById("current-overdue-filter").value = "false";
        }

        const dd = document.getElementById("statusDropdown");
        if (dd) bootstrap.Dropdown.getOrCreateInstance(dd).hide();
        this.renderStatusButton();
        this.doSearch(true);
    }

    renderStatusButton() {
        const statusFilter = document.getElementById("current-status-filter").value;
        const overdue = document.getElementById("current-overdue-filter").value === "true";
        const label = document.getElementById("status-filter-label");
        const btn = document.getElementById("statusDropdown");
        const icon = btn.querySelector("i");
        const baseLabel = t("task.field.status");

        const effectiveStatus = overdue ? "OVERDUE" : statusFilter !== "ALL" ? statusFilter : "";

        document.querySelectorAll(".status-filter-item").forEach((item) => {
            const check = item.querySelector(".filter-check");
            const itemStatus = item.dataset.status;
            let isActive;
            if (itemStatus === "OVERDUE") {
                isActive = overdue;
            } else if (itemStatus === "ALL") {
                isActive = !overdue && statusFilter === "ALL";
            } else {
                isActive = !overdue && itemStatus === statusFilter;
            }
            if (check) check.style.visibility = isActive ? "visible" : "hidden";
        });

        btn.className = "btn btn-sm btn-outline-secondary dropdown-toggle";
        icon.className = "bi bi-list-ul";

        const statusCfg = effectiveStatus === "OVERDUE" ? OVERDUE_CONFIG : APP_CONFIG.enums.taskStatus[effectiveStatus];
        if (statusCfg) {
            label.textContent =
                effectiveStatus === "OVERDUE"
                    ? t("task.dueDate.overdue")
                    : resolveLabel("task.status", effectiveStatus);
            icon.className = `bi ${statusCfg.icon}`;
            btn.className = `btn btn-sm ${statusCfg.btnCss} dropdown-toggle`;
        } else {
            label.textContent = baseLabel;
        }
    }

    // ── Priority filter ──────────────────────────────────────────────────

    setPriorityFilter(eventOrPriority) {
        const priority = eventOrPriority?.currentTarget
            ? eventOrPriority.currentTarget.dataset.priority || ""
            : eventOrPriority;
        const current = document.getElementById("current-priority-filter").value;
        if (priority && current === priority) {
            document.getElementById("current-priority-filter").value = "";
        } else {
            document.getElementById("current-priority-filter").value = priority;
        }
        const dd = document.getElementById("priorityDropdown");
        if (dd) bootstrap.Dropdown.getOrCreateInstance(dd).hide();
        this.renderPriorityButton();
        this.doSearch(true);
    }

    renderPriorityButton() {
        const priority = document.getElementById("current-priority-filter").value;
        const label = document.getElementById("priority-filter-label");
        const btn = document.getElementById("priorityDropdown");
        const icon = btn.querySelector("i");
        const baseLabel = t("task.field.priority");

        document.querySelectorAll(".priority-filter-item").forEach((item) => {
            const check = item.querySelector(".filter-check");
            if (check) check.style.visibility = item.dataset.priority === priority ? "visible" : "hidden";
        });

        btn.className = "btn btn-sm btn-outline-secondary dropdown-toggle";
        icon.className = "bi bi-reception-2";

        const cfg = priority ? priorityEnum[priority] : null;
        if (cfg) {
            label.textContent = resolveLabel("task.priority", priority);
            icon.className = `bi ${cfg.icon}`;
            btn.className = `btn btn-sm ${cfg.btnCss} dropdown-toggle`;
        } else {
            label.textContent = baseLabel;
        }
    }

    // ── Tag filter ───────────────────────────────────────────────────────

    toggleTagFilter(eventOrElOrId) {
        let tagId;
        if (eventOrElOrId?.currentTarget) {
            tagId = String(eventOrElOrId.currentTarget.dataset.tagId);
        } else {
            tagId = String(typeof eventOrElOrId === "object" ? eventOrElOrId.dataset.tagId : eventOrElOrId);
        }
        const idx = this.selectedTagIds.indexOf(tagId);
        if (idx >= 0) {
            this.selectedTagIds.splice(idx, 1);
        } else {
            this.selectedTagIds.push(tagId);
        }
        this.renderTagFilter();
        this.doSearch(true);
    }

    clearAllTags() {
        this.selectedTagIds = [];
        this.renderTagFilter();
        this.doSearch(true);
    }

    renderTagFilter() {
        document.querySelectorAll(".tag-filter-item").forEach((item) => {
            const check = item.querySelector(".filter-check");
            if (check) {
                check.style.visibility = this.selectedTagIds.includes(String(item.dataset.tagId))
                    ? "visible"
                    : "hidden";
            }
        });

        const label = document.getElementById("tag-filter-label");
        if (label) {
            const baseLabel = label.dataset.defaultLabel || label.textContent.replace(/ \(\d+\)$/, "");
            if (!label.dataset.defaultLabel) label.dataset.defaultLabel = baseLabel;
            label.textContent =
                this.selectedTagIds.length > 0 ? `${baseLabel} (${this.selectedTagIds.length})` : baseLabel;
        }

        this.highlightActiveTags();
    }

    highlightActiveTags() {
        document.querySelectorAll("#tasks-view a.badge[data-tag-id]").forEach((badge) => {
            badge.classList.toggle("tag-active", this.selectedTagIds.includes(String(badge.dataset.tagId)));
        });
    }

    // ── Saved Views ──────────────────────────────────────────────────────

    loadSavedViews() {
        fetch(APP_CONFIG.routes.apiViews.build())
            .then(requireOk)
            .then((r) => r.json())
            .then((views) => this.renderSavedViewsList(views))
            .catch((err) => console.error("Failed to load saved views:", err));
    }

    initSavedViewsDelegation() {
        const container = document.getElementById("saved-views-list");
        if (!container) return;
        container.addEventListener("click", (e) => {
            const deleteBtn = e.target.closest("[data-view-delete]");
            if (deleteBtn) {
                e.preventDefault();
                e.stopPropagation();
                this.deleteSavedView(deleteBtn.dataset.viewDelete);
                return;
            }
            const viewItem = e.target.closest("[data-view-id]");
            if (viewItem) {
                e.preventDefault();
                this.applySavedView(this.savedViews.find((v) => String(v.id) === viewItem.dataset.viewId));
            }
        });
    }

    renderSavedViewsList(views) {
        const container = document.getElementById("saved-views-list");
        if (!container) return;
        this.savedViews = views;
        container.innerHTML = "";

        if (views.length === 0) {
            const empty = document.createElement("div");
            empty.className = "dropdown-item-text text-muted small";
            empty.textContent = t("task.views.empty");
            container.appendChild(empty);
            return;
        }

        views.forEach((view) => {
            const item = document.createElement("a");
            item.className = "dropdown-item d-flex align-items-center gap-2";
            item.href = "#";
            item.dataset.viewId = view.id;

            const label = document.createElement("span");
            label.className = "flex-grow-1 text-truncate";
            label.textContent = view.name;

            const deleteBtn = document.createElement("button");
            deleteBtn.className = "btn btn-sm text-danger border-0 p-0 lh-1";
            deleteBtn.innerHTML = '<i class="bi bi-x"></i>';
            deleteBtn.title = t("task.views.delete");
            deleteBtn.dataset.viewDelete = view.id;

            item.appendChild(label);
            item.appendChild(deleteBtn);
            container.appendChild(item);
        });
    }

    renderActiveViewLabel() {
        const label = document.getElementById("saved-views-label");
        const btn = document.getElementById("savedViewsDropdown");
        const icon = btn?.querySelector("i.bi");
        if (!label) return;

        const defaultText = t("task.views.saved");
        if (this.activeViewName) {
            label.textContent = this.activeViewName;
            btn?.classList.replace("btn-outline-secondary", "btn-primary");
            icon?.classList.replace("bi-bookmark", "bi-bookmark-fill");
        } else {
            label.textContent = defaultText;
            btn?.classList.replace("btn-primary", "btn-outline-secondary");
            icon?.classList.replace("bi-bookmark-fill", "bi-bookmark");
        }
    }

    clearActiveView() {
        if (this.activeViewName) {
            this.activeViewName = null;
            this.renderActiveViewLabel();
        }
    }

    applySavedView(view) {
        const data = view.data;
        const query = data.query || {};

        document.getElementById("search-input").value = query.search || "";
        document.getElementById("current-status-filter").value = query.statusFilter || "ALL";
        document.getElementById("current-overdue-filter").value = query.overdue ? "true" : "false";
        document.getElementById("current-priority-filter").value = query.priority || "";
        this.selectedUserId = query.selectedUserId || null;
        this.selectedTagIds = query.tags || [];
        const sprintHidden = document.getElementById("current-sprint-filter");
        if (sprintHidden) {
            sprintHidden.value = query.sprintId || "";
            const label = document.getElementById("sprint-filter-label");
            if (label) {
                if (!query.sprintId) {
                    label.textContent = t("task.field.sprint");
                } else if (query.sprintId === "0" || query.sprintId === 0) {
                    label.textContent = t("sprint.filter.noSprint");
                } else {
                    const item = document.querySelector(`[data-value="${query.sprintId}"]`);
                    label.textContent = item ? item.dataset.name || item.textContent.trim() : t("task.field.sprint");
                }
            }
        }
        if (data.view) this.currentView = data.view;
        if (data.sort) this.activeSorts = data.sort;

        this.activeViewName = view.name;

        this.renderStatusButton();
        this.renderPriorityButton();
        this.renderSprintFilter();
        this.renderViewToggle();
        this.renderTagFilter();
        this.renderUserFilter();
        this.renderSorts();
        this.renderActiveViewLabel();

        const dd = document.getElementById("savedViewsDropdown");
        if (dd) bootstrap.Dropdown.getOrCreateInstance(dd).hide();

        this._keepActiveView = true;
        this.doSearch(true);
    }

    saveCurrentView() {
        const dd = document.getElementById("savedViewsDropdown");
        if (dd) bootstrap.Dropdown.getOrCreateInstance(dd).hide();

        const promptLabel = t("task.views.name.prompt");
        const inputId = "save-view-name-input";

        showConfirm(
            {
                title: t("task.views.save"),
                message: `<label for="${inputId}" class="form-label">${promptLabel}</label>
                      <input type="text" id="${inputId}" class="form-control" maxlength="80" autocomplete="off" autofocus>`,
                confirmText: t("admin.settings.save"),
                confirmClass: "btn btn-primary",
                headerClass: "bg-primary text-white",
            },
            () => {
                const input = document.getElementById(inputId);
                const name = input ? input.value.trim() : "";
                if (!name) {
                    if (input) input.classList.add("is-invalid");
                    return false;
                }

                const statusValue = document.getElementById("current-status-filter").value || "ALL";
                const priorityValue = document.getElementById("current-priority-filter").value || "";
                const data = {
                    type: "task",
                    query: {
                        search: document.getElementById("search-input").value || null,
                        statusFilter: statusValue !== "ALL" ? statusValue : null,
                        overdue: document.getElementById("current-overdue-filter").value === "true",
                        priority: priorityValue || null,
                        selectedUserId: this.selectedUserId || null,
                        tags: this.selectedTagIds.length > 0 ? this.selectedTagIds.slice() : null,
                        sprintId: (document.getElementById("current-sprint-filter") || {}).value || null,
                    },
                    view: this.currentView,
                    sort: this.activeSorts.slice(),
                };

                fetch(APP_CONFIG.routes.apiViews.build(), {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ name, data }),
                })
                    .then(requireOk)
                    .then(() => {
                        this.activeViewName = name;
                        this.renderActiveViewLabel();
                        this.loadSavedViews();
                        showToast(t("toast.view.saved"), "success");
                    })
                    .catch((err) => console.error("Failed to save view:", err));
            },
        );
    }

    deleteSavedView(id) {
        fetch(APP_CONFIG.routes.apiViewById.params({ id }).build(), { method: "DELETE" })
            .then(requireOk)
            .then(() => {
                this.clearActiveView();
                this.loadSavedViews();
            })
            .catch((err) => console.error("Failed to delete view:", err));
    }

    // ── Init from URL ────────────────────────────────────────────────────

    initFromUrl() {
        const params = new URLSearchParams(window.location.search);

        const sortParams = params.getAll("sort");
        if (sortParams.length > 0) {
            this.activeSorts = sortParams.map((s) => {
                const parts = s.split(",");
                return { field: parts[0], direction: parts[1] || "desc" };
            });
        }

        this.currentPage = parseInt(params.get("page") || "0");

        if (params.has("size")) {
            this.pageSize = parseInt(params.get("size"));
            setCookie("pageSize", this.pageSize);
        }

        const statusFilter = params.get("statusFilter") || "ALL";
        document.getElementById("current-status-filter").value = statusFilter;
        const overdue = params.get("overdue") === "true";
        document.getElementById("current-overdue-filter").value = overdue;
        this.renderStatusButton();

        document.getElementById("current-priority-filter").value = params.get("priority") || "";
        this.renderPriorityButton();

        this.renderSprintFilter();

        document.getElementById("search-input").value = params.get("search") || "";
        const clearBtn = document.getElementById("search-clear-btn");
        if (clearBtn) clearBtn.classList.toggle("d-none", !params.get("search"));

        if (params.has("view")) {
            this.currentView = params.get("view");
        } else if (document.getElementById("view-board")?.classList.contains("active")) {
            this.currentView = "board";
        } else if (document.getElementById("view-calendar")?.classList.contains("active")) {
            this.currentView = "calendar";
        } else if (document.getElementById("view-table")?.classList.contains("active")) {
            this.currentView = "table";
        } else {
            this.currentView = "cards";
        }

        this.currentMonth = params.get("month") || null;
        this.renderViewToggle();

        if (params.has("selectedUserId")) {
            this.selectedUserId = params.get("selectedUserId") || null;
        }
        const currentUsrId = document.getElementById("user-filter-mine")?.dataset.currentUserId;
        if (this.selectedUserId && this.selectedUserId !== currentUsrId) {
            const filterUserName = document.getElementById("user-filter-mine")?.dataset.filterUserName;
            this.renderUserFilter(filterUserName || null);
        } else {
            this.renderUserFilter();
        }

        const tagsParam = params.get("tags");
        this.selectedTagIds = tagsParam ? tagsParam.split(",") : [];
        this.renderTagFilter();

        this.renderSorts();
    }
}
