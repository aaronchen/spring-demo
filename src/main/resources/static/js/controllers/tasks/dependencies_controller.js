import { Controller } from "@hotwired/stimulus";

// Dependency picker — handles add/remove via DOM manipulation (hidden inputs).
// Dependencies are saved with the form, not via separate API calls.
// Stimulus lifecycle handles re-binding after HTMX swaps automatically.

export default class extends Controller {
    connect() {
        this.bindPickers();
        this.updateExcludeLists();

        this.afterSettleHandler = () => { this.bindPickers(); this.updateExcludeLists(); };
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
                if (!taskId) return;

                const direction = select.dataset.direction;
                const inputName = direction === "blockedBy" ? "blockedByIds" : "blocksIds";
                const listId = `dep-list-${direction}`;
                const list = document.getElementById(listId);
                if (!list) return;

                const item = document.createElement("div");
                item.className = "list-group-item d-flex align-items-center px-0 py-1 dep-item";
                item.innerHTML =
                    `<input type="hidden" name="${inputName}" value="${taskId}">` +
                    `<span class="badge me-2 bg-secondary">${APP_CONFIG.messages["task.status.open"] || "Open"}</span>` +
                    `<a class="flex-grow-1 small text-decoration-none" href="${APP_CONFIG.routes.tasks}/${taskId}" target="_blank">${taskTitle}</a>` +
                    `<button type="button" class="btn btn-sm btn-outline-danger ms-2 border-0" ` +
                    `data-action="click->tasks--dependencies#removeItem" title="${APP_CONFIG.messages["task.dependency.remove.title"] || "Remove dependency"}">` +
                    `<i class="bi bi-x-lg"></i></button>`;
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
