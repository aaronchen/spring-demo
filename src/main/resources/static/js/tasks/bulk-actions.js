// Bulk actions for table view — cross-page selection with floating action bar

const bulkSelectedIds = new Set();
const bulkSelectedProjectIds = new Map(); // taskId → projectId
let bulkAssignUsers = [];
let bulkAssignProjectId = null; // project ID the user list was loaded for

// ── Selection management ──

function onBulkSelectChange(checkbox) {
    const taskId = checkbox.dataset.taskId;
    const projectId = checkbox.dataset.projectId;
    if (checkbox.checked) {
        bulkSelectedIds.add(taskId);
        bulkSelectedProjectIds.set(taskId, projectId);
    } else {
        bulkSelectedIds.delete(taskId);
        bulkSelectedProjectIds.delete(taskId);
    }
    renderBulkBar();
}

function toggleSelectAll(checked) {
    document.querySelectorAll('.bulk-select-checkbox').forEach(cb => {
        cb.checked = checked;
        const taskId = cb.dataset.taskId;
        const projectId = cb.dataset.projectId;
        if (checked) {
            bulkSelectedIds.add(taskId);
            bulkSelectedProjectIds.set(taskId, projectId);
        } else {
            bulkSelectedIds.delete(taskId);
            bulkSelectedProjectIds.delete(taskId);
        }
    });
    renderBulkBar();
}

function clearBulkSelection() {
    bulkSelectedIds.clear();
    bulkSelectedProjectIds.clear();
    bulkAssignUsers = [];
    bulkAssignProjectId = null;
    document.querySelectorAll('.bulk-select-checkbox').forEach(cb => {
        cb.checked = false;
    });
    const selectAll = document.getElementById('bulk-select-all');
    if (selectAll) selectAll.checked = false;
    renderBulkBar();
}

// Re-check visible checkboxes after HTMX swaps (page navigation)
function recheckVisibleBoxes() {
    document.querySelectorAll('.bulk-select-checkbox').forEach(cb => {
        const taskId = cb.dataset.taskId;
        cb.checked = bulkSelectedIds.has(taskId);
        // Keep project mapping up to date for visible rows
        if (cb.checked) {
            bulkSelectedProjectIds.set(taskId, cb.dataset.projectId);
        }
    });
    updateSelectAllState();
}

function updateSelectAllState() {
    const selectAll = document.getElementById('bulk-select-all');
    if (!selectAll) return;
    const boxes = document.querySelectorAll('.bulk-select-checkbox');
    if (boxes.length === 0) {
        selectAll.checked = false;
        selectAll.indeterminate = false;
        return;
    }
    const checkedCount = Array.from(boxes).filter(cb => cb.checked).length;
    selectAll.checked = checkedCount === boxes.length;
    selectAll.indeterminate = checkedCount > 0 && checkedCount < boxes.length;
}

// Returns the single project ID if all selected tasks are in the same project, else null
function getCommonProjectId() {
    const projectIds = new Set(bulkSelectedProjectIds.values());
    return projectIds.size === 1 ? projectIds.values().next().value : null;
}

function renderBulkBar() {
    const bar = document.getElementById('bulk-action-bar');
    if (!bar) return;
    const count = bulkSelectedIds.size;
    if (count > 0) {
        bar.classList.remove('d-none');
        const label = (APP_CONFIG.messages['task.bulk.selected'] || '{0} selected')
            .replace('{0}', count);
        document.getElementById('bulk-selected-count').textContent = label;
    } else {
        bar.classList.add('d-none');
    }

    // Show assign button only when all selected tasks are in the same project
    const assignBtn = document.getElementById('bulk-assign-container');
    if (assignBtn) {
        const commonProjectId = getCommonProjectId();
        assignBtn.classList.toggle('d-none', commonProjectId === null);
        // Reset cached users when project changes
        if (commonProjectId !== bulkAssignProjectId) {
            bulkAssignUsers = [];
            bulkAssignProjectId = null;
        }
    }

    updateSelectAllState();
}

// ── Bulk actions ──

function executeBulkAction(action, value) {
    if (bulkSelectedIds.size === 0) return;

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

    const headers = { 'Content-Type': 'application/json' };
    if (csrfToken) headers[csrfHeader] = csrfToken;

    const body = {
        taskIds: Array.from(bulkSelectedIds).map(Number),
        action: action,
        value: value || '',
    };

    fetch(`${APP_CONFIG.routes.tasks}/bulk`, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(body),
    })
        .then(response => {
            if (!response.ok) throw new Error('Bulk action failed');
            return response.json();
        })
        .then(data => {
            const count = data.count || 0;
            const msgKey = action === 'DELETE' ? 'toast.task.bulk.deleted' : 'toast.task.bulk.success';
            const msg = (APP_CONFIG.messages[msgKey] || `${count} tasks updated.`)
                .replace('{0}', count);
            showToast(msg, 'success');
            if (data.skipped > 0) {
                showToast(data.skippedMessage, 'warning');
            }
            clearBulkSelection();
            doSearch(false);
        })
        .catch(() => {
            showToast(APP_CONFIG.messages['toast.error.generic'] || 'An error occurred', 'danger');
        });
}

function executeBulkDelete() {
    if (bulkSelectedIds.size === 0) return;
    const count = bulkSelectedIds.size;
    const msg = (APP_CONFIG.messages['task.bulk.confirm.delete'] || `Are you sure you want to delete ${count} tasks?`)
        .replace('{0}', count);

    showConfirm({
        title: APP_CONFIG.messages['task.bulk.confirm.delete.title'] || 'Delete Tasks',
        message: msg,
        confirmText: APP_CONFIG.messages['action.delete'] || 'Delete',
        confirmClass: 'btn btn-danger',
        headerClass: 'bg-danger text-white',
    }, function () {
        executeBulkAction('DELETE', '');
    });
}

// ── Assign user list ──

function loadBulkAssignUsers() {
    const projectId = getCommonProjectId();
    if (!projectId) return;

    const url = resolveRoute(APP_CONFIG.routes.apiProjectMembersAssignable, { projectId });
    fetch(url)
        .then(r => r.json())
        .then(users => {
            bulkAssignUsers = users;
            bulkAssignProjectId = projectId;
            renderBulkAssignList(users);
        })
        .catch(() => {});
}

function filterBulkAssignUsers(query) {
    const q = query.toLowerCase();
    const filtered = q
        ? bulkAssignUsers.filter(u => u.name.toLowerCase().includes(q))
        : bulkAssignUsers;
    renderBulkAssignList(filtered);
}

function renderBulkAssignList(users) {
    const container = document.getElementById('bulk-assign-list');
    if (!container) return;
    container.innerHTML = '';

    // Unassign option
    const unassignLink = document.createElement('a');
    unassignLink.className = 'dropdown-item text-muted';
    unassignLink.href = '#';
    const unassignLabel = APP_CONFIG.messages['task.field.user.unassigned'] || 'Unassigned';
    unassignLink.innerHTML = `<i class="bi bi-person-slash"></i> ${unassignLabel}`;
    unassignLink.addEventListener('click', function (e) {
        e.preventDefault();
        executeBulkAction('ASSIGN', '');
        closeBulkAssignDropdown();
    });
    container.appendChild(unassignLink);

    users.forEach(user => {
        const link = document.createElement('a');
        link.className = 'dropdown-item';
        link.href = '#';
        link.textContent = user.name;
        link.addEventListener('click', function (e) {
            e.preventDefault();
            executeBulkAction('ASSIGN', String(user.id));
            closeBulkAssignDropdown();
        });
        container.appendChild(link);
    });
}

function closeBulkAssignDropdown() {
    const dd = document.getElementById('bulkAssignDropdown');
    if (dd) bootstrap.Dropdown.getOrCreateInstance(dd).hide();
}

// ── Initialization ──

document.addEventListener('DOMContentLoaded', function () {
    // After HTMX swaps, re-check boxes for cross-page selection
    document.addEventListener('htmx:afterSwap', function (evt) {
        if (evt.detail.target && evt.detail.target.id === 'tasks-view') {
            recheckVisibleBoxes();
            renderBulkBar();
        }
    });

    // Load user list for assign dropdown when it opens
    const assignDropdown = document.getElementById('bulkAssignDropdown');
    if (assignDropdown) {
        assignDropdown.addEventListener('shown.bs.dropdown', function () {
            const currentProjectId = getCommonProjectId();
            if (bulkAssignUsers.length === 0 || bulkAssignProjectId !== currentProjectId) {
                loadBulkAssignUsers();
            } else {
                renderBulkAssignList(bulkAssignUsers);
            }
            const searchInput = document.getElementById('bulk-assign-search');
            if (searchInput) {
                searchInput.value = '';
                searchInput.focus();
            }
        });
    }
});
