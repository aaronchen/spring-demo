// Kanban board — drag-and-drop status changes using native HTML5 Drag and Drop API

function kanbanDragStart(e) {
    const card = e.target.closest('.kanban-card');
    e.dataTransfer.setData('text/plain', card.dataset.taskId);
    e.dataTransfer.effectAllowed = 'move';
    card.classList.add('kanban-dragging');
}

function kanbanDragEnd(e) {
    const card = e.target.closest('.kanban-card');
    if (card) card.classList.remove('kanban-dragging');
    // Clean up all drop zone highlights
    document.querySelectorAll('.kanban-column').forEach(col => {
        col.classList.remove('kanban-drop-target');
    });
}

function kanbanDragOver(e) {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
}

function kanbanDragEnter(e) {
    e.preventDefault();
    const column = e.target.closest('.kanban-column');
    if (column) column.classList.add('kanban-drop-target');
}

function kanbanDragLeave(e) {
    const column = e.target.closest('.kanban-column');
    // Only remove highlight if we're actually leaving the column (not entering a child)
    if (column && !column.contains(e.relatedTarget)) {
        column.classList.remove('kanban-drop-target');
    }
}

function kanbanDrop(e) {
    e.preventDefault();
    const column = e.target.closest('.kanban-column');
    if (!column) return;
    column.classList.remove('kanban-drop-target');

    const taskId = e.dataTransfer.getData('text/plain');
    const newStatus = column.dataset.status;

    // Find the dragged card
    const card = document.querySelector(`.kanban-card[data-task-id="${taskId}"]`);
    if (!card) return;

    const oldStatus = card.dataset.status;
    if (oldStatus === newStatus) return;

    // Prevent blocked tasks from being completed
    const blockedStatuses = ['COMPLETED'];
    if (card.dataset.blocked === 'true' && blockedStatuses.includes(newStatus)) {
        showToast(APP_CONFIG.messages['task.dependency.blocked.drag'] || 'This task is blocked', 'warning');
        return;
    }

    // Optimistic UI update: move card to new column
    const columnBody = column.querySelector('.kanban-column-body');
    const emptyMsg = columnBody.querySelector('.kanban-empty');
    if (emptyMsg) emptyMsg.remove();
    columnBody.appendChild(card);
    card.dataset.status = newStatus;
    card.classList.remove('kanban-dragging');

    // Update column counts
    updateColumnCounts();

    // POST status change to server
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

    const params = new URLSearchParams();
    params.set('status', newStatus);

    const headers = {
        'Content-Type': 'application/x-www-form-urlencoded',
        'HX-Request': 'true',
    };
    if (csrfToken) headers[csrfHeader] = csrfToken;

    fetch(`${APP_CONFIG.routes.tasks}/${taskId}/status`, {
        method: 'POST',
        headers: headers,
        body: params.toString(),
    }).then(response => {
        if (!response.ok) {
            // Revert on failure: move card back
            revertCard(card, oldStatus);
            if (response.status === 409) {
                response.json().then(data => {
                    showToast(data.detail || APP_CONFIG.messages['task.dependency.blocked.drag'] || 'This task is blocked', 'warning');
                }).catch(() => {
                    showToast(APP_CONFIG.messages['task.dependency.blocked.drag'] || 'This task is blocked', 'warning');
                });
            } else {
                showToast(APP_CONFIG.messages['toast.error.generic'] || 'Failed to update status', 'danger');
            }
        }
    }).catch(() => {
        revertCard(card, oldStatus);
        showToast(APP_CONFIG.messages['toast.error.generic'] || 'Failed to update status', 'danger');
    });
}

function revertCard(card, oldStatus) {
    const oldColumn = document.querySelector(`.kanban-column[data-status="${oldStatus}"]`);
    if (oldColumn) {
        oldColumn.querySelector('.kanban-column-body').appendChild(card);
        card.dataset.status = oldStatus;
    }
    updateColumnCounts();
}

function updateColumnCounts() {
    document.querySelectorAll('.kanban-column').forEach(col => {
        const count = col.querySelectorAll('.kanban-card').length;
        const badge = col.querySelector('.kanban-count');
        if (badge) badge.textContent = count;

        // Show/hide empty message
        const body = col.querySelector('.kanban-column-body');
        const existing = body.querySelector('.kanban-empty');
        if (count === 0 && !existing) {
            const emptyDiv = document.createElement('div');
            emptyDiv.className = 'kanban-empty text-muted small text-center py-3';
            emptyDiv.textContent = APP_CONFIG.messages['task.board.empty'] || 'No tasks';
            body.appendChild(emptyDiv);
        } else if (count > 0 && existing) {
            existing.remove();
        }
    });
}
