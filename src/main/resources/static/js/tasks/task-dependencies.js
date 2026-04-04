// Dependency picker — handles add/remove via DOM manipulation (hidden inputs).
// Dependencies are saved with the form, not via separate API calls.
// Binds to each .dep-picker; re-binds after HTMX swaps (e.g., modal content loaded).

(function () {
    function bindPickers() {
        document.querySelectorAll('searchable-select.dep-picker').forEach(function (select) {
            if (select.dataset.depBound) return;
            select.dataset.depBound = 'true';
            select.addEventListener('change', function (e) {
                const taskId = e.detail?.value;
                const taskTitle = e.detail?.text;
                if (!taskId) return;

                const direction = select.dataset.direction;
                const inputName = direction === 'blockedBy' ? 'blockedByIds' : 'blocksIds';
                const listId = `dep-list-${direction}`;
                const list = document.getElementById(listId);
                if (!list) return;

                // Build new list item with hidden input
                const item = document.createElement('div');
                item.className = 'list-group-item d-flex align-items-center px-0 py-1 dep-item';
                item.innerHTML =
                    `<input type="hidden" name="${inputName}" value="${taskId}">` +
                    `<span class="badge me-2 bg-secondary">${APP_CONFIG.messages['task.status.open'] || 'Open'}</span>` +
                    `<a class="flex-grow-1 small text-decoration-none" href="${APP_CONFIG.routes.tasks}/${taskId}" target="_blank">${taskTitle}</a>` +
                    `<button type="button" class="btn btn-sm btn-outline-danger ms-2 border-0" ` +
                    `onclick="removeDependencyItem(this)" title="${APP_CONFIG.messages['task.dependency.remove.title'] || 'Remove dependency'}">` +
                    `<i class="bi bi-x-lg"></i></button>`;
                list.appendChild(item);

                // Reset the picker and update exclude lists on all pickers
                select.reset();
                updateDepExcludeLists();
            });
        });
    }

    // Set initial src URLs from data attributes
    function initPickerUrls() {
        updateDepExcludeLists();
    }

    // Bind on initial page load
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { bindPickers(); initPickerUrls(); });
    } else {
        bindPickers();
        initPickerUrls();
    }

    // Re-bind after HTMX swaps (e.g., modal content loaded)
    document.addEventListener('htmx:afterSettle', function () { bindPickers(); initPickerUrls(); });
})();

// Collect all dependency task IDs currently in both lists + the task's own ID.
// Build the search URL from APP_CONFIG and update each picker's src.
function updateDepExcludeLists() {
    const container = document.getElementById('task-dependencies');
    if (!container) return;

    const pickers = container.querySelectorAll('searchable-select.dep-picker');
    if (!pickers.length) return;

    const ownTaskId = pickers[0].dataset.taskId;
    const projectId = pickers[0].dataset.projectId;
    const excludeIds = new Set();
    excludeIds.add(ownTaskId);

    container.querySelectorAll('.dep-item input[type="hidden"]').forEach(function (input) {
        excludeIds.add(input.value);
    });

    const idsParam = Array.from(excludeIds).join(',');
    const src = `${APP_CONFIG.routes.apiTaskSearchForDependency}?projectId=${projectId}&excludeTaskIds=${idsParam}`;

    pickers.forEach(function (picker) {
        picker.setSrc(src);
    });
}

// Remove a dependency item from the list (global — called from onclick)
function removeDependencyItem(btn) {
    const item = btn.closest('.dep-item');
    if (!item) return;
    const list = item.parentElement;
    item.remove();

    // Update exclude lists so removed task reappears in dropdowns
    updateDepExcludeLists();
}
