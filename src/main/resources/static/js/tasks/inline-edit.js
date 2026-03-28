// Inline editing for table view — toggle edit mode to make cells editable

const INLINE_PRIORITY_OPTIONS = [
    { value: 'LOW',    label: 'Low' },
    { value: 'MEDIUM', label: 'Medium' },
    { value: 'HIGH',   label: 'High' },
];

const INLINE_STATUS_OPTIONS = [
    { value: 'BACKLOG',     label: 'Backlog' },
    { value: 'OPEN',        label: 'Open' },
    { value: 'IN_PROGRESS', label: 'In Progress' },
    { value: 'IN_REVIEW',   label: 'In Review' },
    { value: 'COMPLETED',   label: 'Completed' },
    { value: 'CANCELLED',   label: 'Cancelled' },
];

let editModeActive = false;

document.addEventListener('DOMContentLoaded', function() {
    // Populate labels from messages
    INLINE_PRIORITY_OPTIONS.forEach(opt => {
        const key = `task.priority.${opt.value.toLowerCase()}`;
        if (APP_CONFIG.messages[key]) opt.label = APP_CONFIG.messages[key];
    });
    INLINE_STATUS_OPTIONS.forEach(opt => {
        const key = `task.status.${opt.value === 'IN_PROGRESS' ? 'inProgress' : (opt.value === 'IN_REVIEW' ? 'inReview' : opt.value.toLowerCase())}`;
        if (APP_CONFIG.messages[key]) opt.label = APP_CONFIG.messages[key];
    });

    const tasksView = document.getElementById('tasks-view');
    if (!tasksView) return;

    // Click on editable cells in edit mode
    tasksView.addEventListener('click', function(e) {
        if (!editModeActive) return;
        const cell = e.target.closest('td[data-editable="true"]');
        if (!cell || cell.querySelector('.inline-edit-input')) return;
        e.preventDefault();
        e.stopPropagation();
        startInlineEdit(cell);
    }, true);

    // After HTMX swaps, re-apply edit mode styling
    document.addEventListener('htmx:afterSwap', function(evt) {
        if (evt.detail.target && evt.detail.target.id === 'tasks-view' && editModeActive) {
            applyEditModeStyles();
        }
    });
});

function toggleEditMode() {
    editModeActive = !editModeActive;
    const btn = document.getElementById('edit-mode-btn');
    const icon = btn?.querySelector('i.bi');
    if (btn) {
        btn.classList.toggle('active', editModeActive);
    }
    if (icon) {
        icon.classList.toggle('bi-pencil-square', !editModeActive);
        icon.classList.toggle('bi-pencil-fill', editModeActive);
    }
    if (editModeActive) {
        applyEditModeStyles();
    } else {
        removeEditModeStyles();
    }
}

function applyEditModeStyles() {
    document.querySelectorAll('td[data-editable="true"]').forEach(cell => {
        cell.classList.add('inline-edit-active');
        // Suppress links within editable cells
        cell.querySelectorAll('a').forEach(link => {
            link.dataset.originalHref = link.getAttribute('href');
            link.removeAttribute('href');
            link.style.pointerEvents = 'none';
        });
    });
}

function removeEditModeStyles() {
    document.querySelectorAll('td.inline-edit-active').forEach(cell => {
        cell.classList.remove('inline-edit-active');
        // Restore links
        cell.querySelectorAll('a').forEach(link => {
            if (link.dataset.originalHref) {
                link.setAttribute('href', link.dataset.originalHref);
                delete link.dataset.originalHref;
            }
            link.style.pointerEvents = '';
        });
    });
}

function startInlineEdit(cell) {
    const field = cell.dataset.field;
    const taskId = cell.dataset.taskId;
    const currentValue = cell.dataset.value || '';
    const originalContent = cell.innerHTML;

    switch (field) {
        case 'title':
        case 'description':
            showTextInput(cell, taskId, field, currentValue, originalContent);
            break;
        case 'priority':
            showSelectInput(cell, taskId, field, currentValue, originalContent, INLINE_PRIORITY_OPTIONS);
            break;
        case 'status':
            showSelectInput(cell, taskId, field, currentValue, originalContent, INLINE_STATUS_OPTIONS);
            break;
        case 'dueDate':
            showDateInput(cell, taskId, field, currentValue, originalContent);
            break;
    }
}

function showTextInput(cell, taskId, field, currentValue, originalContent) {
    const input = document.createElement('input');
    input.type = 'text';
    input.value = currentValue;
    input.className = 'form-control form-control-sm inline-edit-input';

    cell.innerHTML = '';
    cell.appendChild(input);
    input.focus();
    setTimeout(() => { input.setSelectionRange(0, 0); input.scrollLeft = 0; }, 0);

    input.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            saveInlineEdit(cell, taskId, field, input.value, originalContent);
        } else if (e.key === 'Escape') {
            cancelInlineEdit(cell, originalContent);
        }
    });

    input.addEventListener('blur', function() {
        setTimeout(() => {
            if (cell.contains(input)) {
                saveInlineEdit(cell, taskId, field, input.value, originalContent);
            }
        }, 150);
    });
}

function showSelectInput(cell, taskId, field, currentValue, originalContent, options) {
    const select = document.createElement('select');
    select.className = 'form-select form-select-sm inline-edit-input';

    // Check if the row is blocked (for disabling status options)
    const row = cell.closest('tr');
    const isBlocked = row?.dataset.blocked === 'true';
    const blockedStatuses = ['COMPLETED'];

    options.forEach(opt => {
        const option = document.createElement('option');
        option.value = opt.value;
        option.textContent = opt.label;
        if (opt.value === currentValue) option.selected = true;
        // Disable blocked status transitions
        if (field === 'status' && isBlocked && blockedStatuses.includes(opt.value)) {
            option.disabled = true;
        }
        select.appendChild(option);
    });

    cell.innerHTML = '';
    cell.appendChild(select);
    select.focus();

    select.addEventListener('change', function() {
        saveInlineEdit(cell, taskId, field, select.value, originalContent);
    });

    select.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            cancelInlineEdit(cell, originalContent);
        }
    });

    select.addEventListener('blur', function() {
        setTimeout(() => {
            if (cell.contains(select)) {
                cancelInlineEdit(cell, originalContent);
            }
        }, 150);
    });
}

function showDateInput(cell, taskId, field, currentValue, originalContent) {
    const input = document.createElement('input');
    input.type = 'date';
    input.value = currentValue;
    input.className = 'form-control form-control-sm inline-edit-input';

    cell.innerHTML = '';
    cell.appendChild(input);
    input.focus();

    input.addEventListener('change', function() {
        saveInlineEdit(cell, taskId, field, input.value, originalContent);
    });

    input.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            cancelInlineEdit(cell, originalContent);
        }
    });

    input.addEventListener('blur', function() {
        setTimeout(() => {
            if (cell.contains(input)) {
                saveInlineEdit(cell, taskId, field, input.value, originalContent);
            }
        }, 150);
    });
}

function cancelInlineEdit(cell, originalContent) {
    cell.innerHTML = originalContent;
    if (editModeActive) {
        cell.classList.add('inline-edit-active');
        cell.querySelectorAll('a').forEach(link => {
            link.dataset.originalHref = link.getAttribute('href');
            link.removeAttribute('href');
            link.style.pointerEvents = 'none';
        });
    }
}

function saveInlineEdit(cell, taskId, field, value, originalContent) {
    const row = cell.closest('tr');
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

    const params = new URLSearchParams();
    params.set('field', field);
    params.set('value', value);

    const headers = {
        'Content-Type': 'application/x-www-form-urlencoded',
        'HX-Request': 'true',
    };
    if (csrfToken) headers[csrfHeader] = csrfToken;

    fetch(`${APP_CONFIG.routes.tasks}/${taskId}/field`, {
        method: 'PATCH',
        headers: headers,
        body: params.toString(),
    }).then(response => {
        if (response.ok) {
            return response.text();
        }
        // Parse error detail for blocked task message
        if (response.headers.get('Content-Type')?.includes('json')) {
            return response.json().then(data => {
                throw new Error(data.detail || 'Failed to save');
            });
        }
        throw new Error('Failed to save');
    }).then(html => {
        const template = document.createElement('template');
        template.innerHTML = html.trim();
        const newRow = template.content.querySelector('tr');
        if (newRow && row) {
            row.replaceWith(newRow);
            htmx.process(newRow);
            if (editModeActive) {
                newRow.querySelectorAll('td[data-editable="true"]').forEach(cell => {
                    cell.classList.add('inline-edit-active');
                    cell.querySelectorAll('a').forEach(link => {
                        link.dataset.originalHref = link.getAttribute('href');
                        link.removeAttribute('href');
                        link.style.pointerEvents = 'none';
                    });
                });
            }
        }
    }).catch(err => {
        cancelInlineEdit(cell, originalContent);
        showToast(err.message || APP_CONFIG.messages['toast.error.generic'] || 'Failed to save', 'danger');
    });
}
