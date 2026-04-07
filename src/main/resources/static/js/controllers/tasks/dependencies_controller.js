import { Controller } from "@hotwired/stimulus";
import { resolveLabel } from "lib/i18n";

const DEFAULT_BADGE = { css: "bg-secondary", icon: "bi-circle", terminal: false };

// Dependency picker — handles add/remove via DOM manipulation (hidden inputs).
// Dependencies are saved with the form, not via separate API calls.
// Stimulus lifecycle handles re-binding after HTMX swaps automatically.

export default class extends Controller {
    connect() {
        this.bindPickers();
        this.updateExcludeLists();

        this.afterSettleHandler = () => {
            this.bindPickers();
            this.updateExcludeLists();
        };
        document.addEventListener("htmx:afterSettle", this.afterSettleHandler);
    }

    disconnect() {
        document.removeEventListener("htmx:afterSettle", this.afterSettleHandler);
    }

    bindPickers() {
        document.querySelectorAll("searchable-select.dep-picker").forEach((select) => {
            if (select.dataset.depBound) return;
            select.dataset.depBound = "true";
            select.addEventListener("change", (e) => {
                const taskId = e.detail?.value;
                const taskTitle = e.detail?.text;
                const taskData = e.detail?.data;
                if (!taskId) return;

                const direction = select.dataset.direction;
                const inputName = direction === "blockedBy" ? "blockedByIds" : "blocksIds";
                const listId = `dep-list-${direction}`;
                const list = document.getElementById(listId);
                if (!list) return;

                const template = document.getElementById("dep-item-template");
                if (!template) return;
                const item = template.content.firstElementChild.cloneNode(true);
                item.querySelector('input[type="hidden"]').name = inputName;
                item.querySelector('input[type="hidden"]').value = taskId;

                const status = taskData?.status || "OPEN";
                const info = APP_CONFIG.enums.taskStatus[status] || DEFAULT_BADGE;
                const badge = item.querySelector(".badge");
                badge.className = `badge me-2 ${info.css}`;
                badge.querySelector("i").className = `bi ${info.icon}`;
                badge.querySelector("span").textContent = resolveLabel("task.status", status);

                const link = item.querySelector("a");
                link.href = APP_CONFIG.routes.taskDetail.params({ taskId }).build();
                link.textContent = taskTitle;
                if (info.terminal) {
                    link.classList.add("text-decoration-line-through");
                }
                list.appendChild(item);

                select.reset();
                this.updateExcludeLists();
            });
        });
    }

    updateExcludeLists() {
        const container = document.getElementById("task-dependencies");
        if (!container) return;

        const pickers = container.querySelectorAll("searchable-select.dep-picker");
        if (!pickers.length) return;

        const ownTaskId = pickers[0].dataset.taskId;
        const projectId = pickers[0].dataset.projectId;
        const excludeIds = new Set();
        excludeIds.add(ownTaskId);

        container.querySelectorAll('.dep-item input[type="hidden"]').forEach((input) => {
            excludeIds.add(input.value);
        });

        const idsParam = Array.from(excludeIds).join(",");
        const src = `${APP_CONFIG.routes.apiTaskSearchForDependency}?projectId=${projectId}&excludeTaskIds=${idsParam}`;

        pickers.forEach((picker) => {
            picker.setSrc(src);
        });
    }

    removeItem(eventOrBtn) {
        const btn = eventOrBtn?.currentTarget || eventOrBtn;
        const item = btn.closest(".dep-item");
        if (!item) return;
        item.remove();
        this.updateExcludeLists();
    }
}
