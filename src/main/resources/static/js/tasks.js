// Task list page — sort, filter, search, pagination, and modal state management

const SORT_LABELS = {
    'title,asc':        'Title ↑',
    'title,desc':       'Title ↓',
    'createdAt,asc':    'Oldest First',
    'createdAt,desc':   'Newest First',
    'description,asc':  'Desc ↑',
    'description,desc': 'Desc ↓',
};

let activeSorts = [{field: 'createdAt', direction: 'desc'}];
let currentPage = 0;
let pageSize = parseInt(getCookie('pageSize') || '25');
let currentView = 'cards';

// Build the URL for the current state
function buildUrl(page) {
    const params = new URLSearchParams();
    const search = document.getElementById('search-input').value;
    const filter = document.getElementById('current-filter').value;
    if (search) params.set('search', search);
    if (filter && filter !== 'all') params.set('filter', filter);
    activeSorts.forEach(s => params.append('sort', `${s.field},${s.direction}`));
    params.set('size', pageSize);
    if (page > 0) params.set('page', page);
    if (currentView !== 'cards') params.set('view', currentView);
    return `/web/tasks?${params.toString()}`;
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

// Switch between card and table views
function switchView(view) {
    currentView = view;
    setCookie('view', view);
    renderViewToggle();
    doSearch(false);
}

// Update active state on view toggle buttons
function renderViewToggle() {
    document.getElementById('view-cards').classList.toggle('active', currentView === 'cards');
    document.getElementById('view-table').classList.toggle('active', currentView === 'table');
}

// Per-page selector change — syncs all instances (top and bottom bars)
function onPageSizeChange(newSize) {
    pageSize = parseInt(newSize);
    setCookie('pageSize', pageSize);
    syncPageSizeSelects();
    currentPage = 0;
    doSearch(false);
}

function syncPageSizeSelects() {
    document.querySelectorAll('.page-size-select').forEach(el => el.value = pageSize);
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
    syncPageSizeSelects();

    // Filter
    const filter = params.get('filter') || 'all';
    document.getElementById('current-filter').value = filter;
    document.querySelectorAll('[id^="filter-"]').forEach(btn => {
        btn.classList.toggle('active', btn.id === 'filter-' + filter);
    });

    // Search
    document.getElementById('search-input').value = params.get('search') || '';

    // View: URL takes precedence; fall back to cookie (mirrors server-side logic)
    currentView = params.get('view') || getCookie('view') || 'cards';
    renderViewToggle();

    // Render sort checkmarks and label
    renderSorts();
}

document.addEventListener('DOMContentLoaded', function() {
    initFromUrl();

    // Debounced live search
    let searchDebounce;
    document.getElementById('search-input').addEventListener('input', function() {
        clearTimeout(searchDebounce);
        searchDebounce = setTimeout(() => doSearch(true), 300);
    });

    // Filter button click handlers
    document.querySelectorAll('[id^="filter-"]').forEach(button => {
        button.addEventListener('click', function() {
            document.querySelectorAll('[id^="filter-"]').forEach(btn =>
                btn.classList.remove('active'));
            this.classList.add('active');
            document.getElementById('current-filter').value =
                this.id.replace('filter-', '');
            doSearch(true);
        });
    });

    // After HTMX replaces the grid, re-sync the per-page selects in the new HTML.
    // After HTMX loads form content into the modal, show it.
    document.addEventListener('htmx:afterSwap', function(evt) {
        if (evt.detail.target && evt.detail.target.id === 'tasks-view') {
            syncPageSizeSelects();
        }
        if (evt.detail.target && evt.detail.target.id === 'task-modal-content') {
            bootstrap.Modal.getOrCreateInstance(document.getElementById('task-modal')).show();
        }
    });

    // After task saved: close modal and refresh grid
    document.body.addEventListener('taskSaved', function() {
        bootstrap.Modal.getInstance(document.getElementById('task-modal')).hide();
        doSearch(false);
    });

    // Populate delete modal from the triggering button's data attributes
    document.getElementById('task-delete-modal').addEventListener('show.bs.modal', function(e) {
        const btn = e.relatedTarget;
        document.getElementById('task-delete-modal-title').textContent = btn.dataset.taskTitle;
        const confirmBtn = document.getElementById('delete-confirm-btn');
        confirmBtn.setAttribute('hx-post', '/web/tasks/' + btn.dataset.taskId + '/delete');
        htmx.process(confirmBtn);
    });

    // After task deleted: refresh grid
    document.body.addEventListener('taskDeleted', function() {
        doSearch(false);
    });

    // Handle browser back/forward — re-sync state from URL and re-fetch the grid
    window.addEventListener('popstate', function() {
        initFromUrl();
        doSearch(false);
    });
});
