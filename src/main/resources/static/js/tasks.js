// Task list page — sort, filter, search, pagination, and modal state management

const SORT_LABELS = {
    'title,asc':        'Title ↑',
    'title,desc':       'Title ↓',
    'createdAt,asc':    'Oldest First',
    'createdAt,desc':   'Newest First',
    'priorityOrder,asc':  'Priority ↑',
    'priorityOrder,desc': 'Priority ↓',
    'dueDate,asc':      'Due Date ↑',
    'dueDate,desc':     'Due Date ↓',
    'description,asc':  'Desc ↑',
    'description,desc': 'Desc ↓',
};

let activeSorts = [{field: 'createdAt', direction: 'desc'}];
let currentPage = 0;
let pageSize = parseInt(getCookie('pageSize') || '25');
let currentView = 'cards';
let selectedUserId = null;
let selectedTagIds = [];
const TASKS_BASE = APP_CONFIG.routes.tasks;

// Build the URL for the current state
function buildUrl(page) {
    const params = new URLSearchParams();
    const search = document.getElementById('search-input').value;
    const statusFilter = document.getElementById('current-status-filter').value;
    const overdue = document.getElementById('current-overdue-filter').value;
    if (search) params.set('search', search);
    if (statusFilter && statusFilter !== 'ALL') params.set('statusFilter', statusFilter);
    if (overdue === 'true') params.set('overdue', 'true');
    const priority = document.getElementById('current-priority-filter').value;
    if (priority) params.set('priority', priority);
    activeSorts.forEach(s => params.append('sort', `${s.field},${s.direction}`));
    params.set('size', pageSize);
    if (page > 0) params.set('page', page);
    // Always include selectedUserId: actual ID for "Mine"/specific user, empty for "All Users".
    // This ensures bookmarked URLs preserve the user filter choice.
    params.set('selectedUserId', selectedUserId || '');
    if (selectedTagIds.length > 0) params.set('tags', selectedTagIds.join(','));
    if (currentView !== 'cards') params.set('view', currentView);
    return `${TASKS_BASE}?${params.toString()}`;
}

// Export current filtered tasks as CSV
function exportTasks() {
    const params = new URLSearchParams();
    const search = document.getElementById('search-input').value;
    const statusFilter = document.getElementById('current-status-filter').value;
    const overdueVal = document.getElementById('current-overdue-filter').value;
    if (search) params.set('search', search);
    if (statusFilter && statusFilter !== 'ALL') params.set('statusFilter', statusFilter);
    if (overdueVal === 'true') params.set('overdue', 'true');
    const priority = document.getElementById('current-priority-filter').value;
    if (priority) params.set('priority', priority);
    if (selectedUserId) params.set('selectedUserId', selectedUserId);
    if (selectedTagIds.length > 0) params.set('tags', selectedTagIds.join(','));
    if (activeSorts.length > 0) {
        params.set('sort', `${activeSorts[0].field},${activeSorts[0].direction}`);
    }
    window.location.href = `${TASKS_BASE}/export?${params.toString()}`;
}

// Fetch grid fragment via HTMX and update the URL (replaces history — no back entry)
function doSearch(resetPage) {
    if (resetPage) currentPage = 0;
    const url = buildUrl(currentPage);
    htmx.ajax('GET', url, {target: '#tasks-view', swap: 'innerHTML'});
    window.history.replaceState({}, '', url);
}

// Navigate to a specific page (pushes history — back/forward works)
function navigateToPage(page) {
    if (page < 0) return;
    currentPage = page;
    const url = buildUrl(page);
    htmx.ajax('GET', url, {target: '#tasks-view', swap: 'innerHTML'});
    window.history.pushState({}, '', url);
}

// Per-page size change
function onPageSizeChange(newSize) {
    pageSize = parseInt(newSize);
    setCookie('pageSize', pageSize);
    currentPage = 0;
    doSearch(false);
}

// Switch between card and table views
function switchView(view) {
    currentView = view;
    renderViewToggle();
    doSearch(false);
}

// Update active state on view toggle buttons
function renderViewToggle() {
    document.getElementById('view-cards').classList.toggle('active', currentView === 'cards');
    document.getElementById('view-table').classList.toggle('active', currentView === 'table');
}


// Reset sort to default (newest first)
function resetSort() {
    activeSorts = [{field: 'createdAt', direction: 'desc'}];
    renderSorts();
    doSearch(true);
}

// Toggle a sort option (mutual exclusion within same field)
function toggleSort(field, direction) {
    const idx = activeSorts.findIndex(s => s.field === field && s.direction === direction);
    if (idx >= 0) {
        if (activeSorts.length === 1) return; // Keep at least one sort active
        activeSorts.splice(idx, 1);
    } else {
        // Remove the opposite direction for this field
        const oppositeIdx = activeSorts.findIndex(s => s.field === field);
        if (oppositeIdx >= 0) activeSorts.splice(oppositeIdx, 1);
        activeSorts.push({field, direction});
    }
    renderSorts();
    doSearch(true);
}

// Update sort button label and checkmarks
function renderSorts() {
    const label = activeSorts
        .map(s => SORT_LABELS[`${s.field},${s.direction}`] || `${s.field} ${s.direction}`)
        .join(', ');
    document.getElementById('sort-label').textContent = label;

    document.querySelectorAll('[data-sort-field]').forEach(item => {
        const check = item.querySelector('.sort-check');
        const isActive = activeSorts.some(
            s => s.field === item.dataset.sortField && s.direction === item.dataset.sortDir
        );
        if (check) check.style.visibility = isActive ? 'visible' : 'hidden';
    });
}

// Read URL params and sync all UI state
function initFromUrl() {
    const params = new URLSearchParams(window.location.search);

    // Sort
    const sortParams = params.getAll('sort');
    if (sortParams.length > 0) {
        activeSorts = sortParams.map(s => {
            const parts = s.split(',');
            return {field: parts[0], direction: parts[1] || 'desc'};
        });
    }

    // Page
    currentPage = parseInt(params.get('page') || '0');

    // Size (URL takes precedence over cookie)
    if (params.has('size')) {
        pageSize = parseInt(params.get('size'));
        setCookie('pageSize', pageSize);
    }

    // Status filter
    const statusFilter = params.get('statusFilter') || 'ALL';
    document.getElementById('current-status-filter').value = statusFilter;
    document.querySelectorAll('[id^="status-filter-"]').forEach(btn => {
        btn.classList.toggle('active', btn.id === 'status-filter-' + statusFilter);
    });

    // Overdue filter
    const overdue = params.get('overdue') === 'true';
    document.getElementById('current-overdue-filter').value = overdue;
    document.getElementById('overdue-filter').classList.toggle('active', overdue);

    // Priority filter
    document.getElementById('current-priority-filter').value = params.get('priority') || '';
    renderPriorityButton();

    // Search
    document.getElementById('search-input').value = params.get('search') || '';
    const clearBtn = document.getElementById('search-clear-btn');
    if (clearBtn) clearBtn.classList.toggle('d-none', !params.get('search'));

    // View: URL param takes precedence; otherwise read from server-rendered active state
    currentView = params.get('view')
        || (document.getElementById('view-table')?.classList.contains('active') ? 'table' : 'cards');
    renderViewToggle();

    // User filter: explicit URL param wins; otherwise keep default from server
    if (params.has('selectedUserId')) {
        selectedUserId = params.get('selectedUserId') || null; // empty string → null (All Users)
    }
    // If filtering by another user, read their name from the data attribute
    const currentUsrId = document.getElementById('user-filter-mine')?.dataset.currentUserId;
    if (selectedUserId && selectedUserId !== currentUsrId) {
        const filterUserName = document.getElementById('user-filter-mine')?.dataset.filterUserName;
        renderUserFilter(filterUserName || null);
    } else {
        renderUserFilter();
    }

    // Tag filter
    const tagsParam = params.get('tags');
    selectedTagIds = tagsParam ? tagsParam.split(',') : [];
    renderTagFilter();

    // Render sort checkmarks and label
    renderSorts();
}

// ── User filter ──

function setUserFilter(id, userName) {
    selectedUserId = String(id);
    renderUserFilter(userName);
    doSearch(true);
}

function setMyFilter() {
    const btn = document.getElementById('user-filter-mine');
    selectedUserId = btn.dataset.currentUserId;
    renderUserFilter(null);
    doSearch(true);
}

function clearUserFilter() {
    // "All Users" button: always show all users
    selectedUserId = null;
    renderUserFilter(null);
    doSearch(true);
}

function resetUserFilter() {
    // "×" on user label: go back to Mine
    const mineBtn = document.getElementById('user-filter-mine');
    selectedUserId = mineBtn?.dataset.currentUserId;
    renderUserFilter(null);
    doSearch(true);
}

function renderUserFilter(userName) {
    const allBtn = document.getElementById('user-filter-all');
    const mineBtn = document.getElementById('user-filter-mine');
    if (!allBtn || !mineBtn) return;
    const label = document.getElementById('user-filter-label');
    const clearIcon = document.getElementById('user-filter-clear');
    const defaultLabel = mineBtn.dataset.defaultLabel || label.textContent;

    allBtn.classList.toggle('active', selectedUserId === null);
    mineBtn.classList.toggle('active', selectedUserId !== null);

    if (selectedUserId === null) {
        label.textContent = defaultLabel;
        clearIcon.classList.add('d-none');
    } else if (selectedUserId === mineBtn.dataset.currentUserId) {
        label.textContent = defaultLabel;
        clearIcon.classList.add('d-none');
    } else {
        label.textContent = userName || 'User';
        clearIcon.classList.remove('d-none');
    }
}

// ── Status filter (clickable badges on cards/rows) ──

function setStatusFilter(status) {
    // status: 'OPEN', 'IN_PROGRESS', 'COMPLETED', or 'overdue'
    if (status === 'overdue') {
        document.querySelectorAll('[id^="status-filter-"]').forEach(btn =>
            btn.classList.remove('active'));
        document.getElementById('status-filter-ALL').classList.add('active');
        document.getElementById('current-status-filter').value = 'ALL';
        document.getElementById('current-overdue-filter').value = 'true';
        document.getElementById('overdue-filter').classList.add('active');
    } else {
        document.querySelectorAll('[id^="status-filter-"]').forEach(btn =>
            btn.classList.remove('active'));
        document.getElementById('status-filter-' + status).classList.add('active');
        document.getElementById('current-status-filter').value = status;
        document.getElementById('current-overdue-filter').value = 'false';
        document.getElementById('overdue-filter').classList.remove('active');
    }
    doSearch(true);
}

// ── Priority filter (clickable badges on cards/rows) ──

const PRIORITY_CONFIG = {
    HIGH:   { msgKey: 'task.priority.high',   css: 'bg-danger text-white',  icon: 'bi-reception-4' },
    MEDIUM: { msgKey: 'task.priority.medium', css: 'bg-warning text-dark',  icon: 'bi-reception-2' },
    LOW:    { msgKey: 'task.priority.low',    css: 'bg-success text-white', icon: 'bi-reception-1' },
};

function setPriorityFilter(priority) {
    const current = document.getElementById('current-priority-filter').value;
    // If clicking a badge on a card/row, toggle off if same priority; otherwise set it
    if (priority && current === priority) {
        document.getElementById('current-priority-filter').value = '';
    } else {
        document.getElementById('current-priority-filter').value = priority;
    }
    // Close the dropdown if open
    const dd = document.getElementById('priorityDropdown');
    if (dd) bootstrap.Dropdown.getOrCreateInstance(dd).hide();
    renderPriorityButton();
    doSearch(true);
}

function renderPriorityButton() {
    const priority = document.getElementById('current-priority-filter').value;
    const label = document.getElementById('priority-filter-label');
    const btn = document.getElementById('priorityDropdown');
    const icon = btn.querySelector('i');
    const baseLabel = APP_CONFIG.messages['task.field.priority'] || 'Priority';

    // Update active item styling
    document.querySelectorAll('.priority-filter-item').forEach(item => {
        item.classList.toggle('active', item.dataset.priority === priority);
    });

    // Reset button to default
    btn.className = 'btn btn-sm btn-outline-secondary dropdown-toggle';
    icon.className = 'bi bi-reception-2';

    if (priority && PRIORITY_CONFIG[priority]) {
        const cfg = PRIORITY_CONFIG[priority];
        label.textContent = APP_CONFIG.messages[cfg.msgKey] || priority;
        icon.className = `bi ${cfg.icon}`;
        // Apply priority color to button
        const colorClass = cfg.css.includes('bg-danger') ? 'btn-danger'
            : cfg.css.includes('bg-warning') ? 'btn-warning'
            : 'btn-success';
        btn.className = `btn btn-sm ${colorClass} dropdown-toggle`;
    } else {
        label.textContent = baseLabel;
    }
}

function updateFilterPillsVisibility() {
    const wrapper = document.getElementById('filter-pills');
    wrapper.classList.toggle('d-none', selectedTagIds.length === 0);
}

// ── Tag filter ──

function toggleTagFilter(tagId) {
    tagId = String(tagId);
    const idx = selectedTagIds.indexOf(tagId);
    if (idx >= 0) {
        selectedTagIds.splice(idx, 1);
    } else {
        selectedTagIds.push(tagId);
    }
    renderTagFilter();
    doSearch(true);
}

function onTagCheckboxChange(checkbox) {
    const tagId = String(checkbox.dataset.tagId);
    if (checkbox.checked) {
        if (!selectedTagIds.includes(tagId)) selectedTagIds.push(tagId);
    } else {
        selectedTagIds = selectedTagIds.filter(id => id !== tagId);
    }
    renderTagFilter();
    doSearch(true);
}

function clearAllTags() {
    selectedTagIds = [];
    renderTagFilter();
    doSearch(true);
}

function renderTagFilter() {
    // Sync dropdown checkboxes
    document.querySelectorAll('.tag-filter-checkbox').forEach(cb => {
        cb.checked = selectedTagIds.includes(String(cb.dataset.tagId));
    });

    // Update dropdown button label
    const label = document.getElementById('tag-filter-label');
    if (label) {
        const baseLabel = label.dataset.defaultLabel || label.textContent.replace(/ \(\d+\)$/, '');
        if (!label.dataset.defaultLabel) label.dataset.defaultLabel = baseLabel;
        label.textContent = selectedTagIds.length > 0
            ? `${baseLabel} (${selectedTagIds.length})`
            : baseLabel;
    }

    // Render tag pills
    const pillsContainer = document.getElementById('tag-pills-container');
    const clearAllLink = document.getElementById('tag-clear-all');
    if (!pillsContainer) return;
    pillsContainer.innerHTML = '';

    if (selectedTagIds.length === 0) {
        clearAllLink.classList.add('d-none');
        updateFilterPillsVisibility();
        return;
    }

    clearAllLink.classList.remove('d-none');
    selectedTagIds.forEach(tagId => {
        const cb = document.querySelector(`.tag-filter-checkbox[data-tag-id="${tagId}"]`);
        const name = cb ? cb.dataset.tagName : `Tag ${tagId}`;
        const pill = document.createElement('span');
        pill.className = 'badge bg-primary me-1';
        pill.innerHTML = `${name} <a href="#" class="text-white text-decoration-none ms-1" onclick="toggleTagFilter('${tagId}'); return false;">&times;</a>`;
        pillsContainer.appendChild(pill);
    });
    updateFilterPillsVisibility();

    // Show "Clear all" only when 2+ tags
    const clearAll = document.getElementById('tag-clear-all');
    if (clearAll) clearAll.classList.toggle('d-none', selectedTagIds.length < 2);

    // Highlight active tags on cards/rows
    highlightActiveTags();
}

function highlightActiveTags() {
    document.querySelectorAll('#tasks-view a.badge[data-tag-id]').forEach(badge => {
        badge.classList.toggle('tag-active', selectedTagIds.includes(String(badge.dataset.tagId)));
    });
}

document.addEventListener('DOMContentLoaded', function() {
    // Store default label and read initial user filter from server-rendered active state
    const mineBtn = document.getElementById('user-filter-mine');
    if (mineBtn) {
        const label = document.getElementById('user-filter-label');
        if (label) mineBtn.dataset.defaultLabel = label.textContent;
        if (!selectedUserId && mineBtn.classList.contains('active')) {
            selectedUserId = mineBtn.dataset.currentUserId;
        }
    }

    initFromUrl();

    // Pagination custom events (dispatched by layouts/pagination fragment)
    const tasksView = document.getElementById('tasks-view');
    tasksView.addEventListener('pagination:navigate', function(e) {
        navigateToPage(e.detail.page);
    });
    tasksView.addEventListener('pagination:resize', function(e) {
        onPageSizeChange(e.detail.size);
    });

    // Debounced live search + clear button visibility
    const searchInput = document.getElementById('search-input');
    const searchClearBtn = document.getElementById('search-clear-btn');

    function updateClearBtn() {
        searchClearBtn.classList.toggle('d-none', searchInput.value === '');
    }

    let searchDebounce;
    searchInput.addEventListener('input', function() {
        updateClearBtn();
        clearTimeout(searchDebounce);
        searchDebounce = setTimeout(() => doSearch(true), 300);
    });

    searchClearBtn.addEventListener('click', function() {
        searchInput.value = '';
        updateClearBtn();
        searchInput.focus();
        doSearch(true);
    });

    // Status filter button click handlers
    document.querySelectorAll('[id^="status-filter-"]').forEach(button => {
        button.addEventListener('click', function() {
            document.querySelectorAll('[id^="status-filter-"]').forEach(btn =>
                btn.classList.remove('active'));
            this.classList.add('active');
            document.getElementById('current-status-filter').value =
                this.id.replace('status-filter-', '');
            // Clear overdue when switching status filters
            document.getElementById('current-overdue-filter').value = 'false';
            document.getElementById('overdue-filter').classList.remove('active');
            doSearch(true);
        });
    });

    // Overdue filter button click handler
    document.getElementById('overdue-filter').addEventListener('click', function() {
        const isActive = this.classList.contains('active');
        if (isActive) {
            // Toggle off: clear overdue, keep current status filter
            this.classList.remove('active');
            document.getElementById('current-overdue-filter').value = 'false';
        } else {
            // Toggle on: set status to All + activate overdue
            document.querySelectorAll('[id^="status-filter-"]').forEach(btn =>
                btn.classList.remove('active'));
            document.getElementById('status-filter-ALL').classList.add('active');
            document.getElementById('current-status-filter').value = 'ALL';
            this.classList.add('active');
            document.getElementById('current-overdue-filter').value = 'true';
        }
        doSearch(true);
    });

    // After HTMX replaces the grid, re-sync the per-page selects in the new HTML.
    // After HTMX loads form content into the modal, show it.
    document.addEventListener('htmx:afterSwap', function(evt) {
        if (evt.detail.target && evt.detail.target.id === 'tasks-view') {
            highlightActiveTags();
        }
        if (evt.detail.target && evt.detail.target.id === 'task-modal-content') {
            bootstrap.Modal.getOrCreateInstance(document.getElementById('task-modal')).show();
        }
    });

    // After task saved: close modal, show toast, and refresh grid
    document.body.addEventListener('taskSaved', function() {
        bootstrap.Modal.getInstance(document.getElementById('task-modal')).hide();
        showToast(APP_CONFIG.messages['toast.task.saved'], 'success');
        doSearch(false);
    });

    // Populate delete modal from the triggering button's data attributes
    document.getElementById('task-delete-modal').addEventListener('show.bs.modal', function(e) {
        const btn = e.relatedTarget;
        document.getElementById('task-delete-modal-title').textContent = btn.dataset.taskTitle;
        const confirmBtn = document.getElementById('delete-confirm-btn');
        confirmBtn.setAttribute('hx-delete', `${TASKS_BASE}/${btn.dataset.taskId}`);
        htmx.process(confirmBtn);
    });

    // After task deleted: show toast and refresh grid
    document.body.addEventListener('taskDeleted', function() {
        showToast(APP_CONFIG.messages['toast.task.deleted'], 'success');
        doSearch(false);
    });

    // Handle browser back/forward — re-sync state from URL and re-fetch the grid
    window.addEventListener('popstate', function() {
        initFromUrl();
        doSearch(false);
    });

    // ── Live task updates via WebSocket ──────────────────────────────────
    const currentUserId = document.querySelector('meta[name="_userId"]')?.content;
    const staleBanner = document.getElementById('stale-banner');
    const staleRefresh = document.getElementById('stale-banner-refresh');

    if (window.stompClient && staleBanner) {
        window.stompClient.onConnect(function(client) {
            client.subscribe('/topic/tasks', function(message) {
                const data = JSON.parse(message.body);
                // Ignore own actions
                if (currentUserId && String(data.userId) === currentUserId) return;
                staleBanner.classList.remove('d-none');
            });
        });

        staleRefresh.addEventListener('click', function(e) {
            e.preventDefault();
            staleBanner.classList.add('d-none');
            doSearch(false);
        });
    }
});
