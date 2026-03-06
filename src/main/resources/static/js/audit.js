// Audit log page — category filter, search, date range, pagination

let currentPage = 0;
let pageSize = 50;
let currentCategory = '';
const AUDIT_BASE = APP_CONFIG.routes.audit;

function buildUrl(page) {
    const params = new URLSearchParams();
    const search = document.getElementById('audit-search-input').value;
    const from = document.getElementById('audit-date-from').value;
    const to = document.getElementById('audit-date-to').value;
    if (search) params.set('search', search);
    if (currentCategory) params.set('category', currentCategory);
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    params.set('size', pageSize);
    if (page > 0) params.set('page', page);
    return `${AUDIT_BASE}?${params.toString()}`;
}

function doSearch(resetPage) {
    if (resetPage) currentPage = 0;
    const url = buildUrl(currentPage);
    htmx.ajax('GET', url, {target: '#audit-view', swap: 'innerHTML'});
    window.history.replaceState({}, '', url);
}

function navigateToPage(page) {
    if (page < 0) return;
    currentPage = page;
    const url = buildUrl(page);
    htmx.ajax('GET', url, {target: '#audit-view', swap: 'innerHTML'});
    window.history.pushState({}, '', url);
}

function onPageSizeChange(newSize) {
    pageSize = parseInt(newSize);
    currentPage = 0;
    doSearch(false);
}

function initFromUrl() {
    const params = new URLSearchParams(window.location.search);

    currentPage = parseInt(params.get('page') || '0');
    if (params.has('size')) pageSize = parseInt(params.get('size'));

    document.getElementById('audit-search-input').value = params.get('search') || '';
    document.getElementById('audit-date-from').value = params.get('from') || '';
    document.getElementById('audit-date-to').value = params.get('to') || '';

    currentCategory = params.get('category') || '';
    document.querySelectorAll('.audit-category-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.category === currentCategory);
    });

    const clearBtn = document.getElementById('audit-search-clear-btn');
    if (clearBtn) clearBtn.classList.toggle('d-none', !params.get('search'));
}

document.addEventListener('DOMContentLoaded', function() {
    initFromUrl();

    const auditView = document.getElementById('audit-view');

    // Pagination custom events
    auditView.addEventListener('pagination:navigate', function(e) {
        navigateToPage(e.detail.page);
    });
    auditView.addEventListener('pagination:resize', function(e) {
        onPageSizeChange(e.detail.size);
    });

    // Category buttons
    document.querySelectorAll('.audit-category-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            document.querySelectorAll('.audit-category-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            currentCategory = this.dataset.category;
            doSearch(true);
        });
    });

    // Debounced search
    const searchInput = document.getElementById('audit-search-input');
    const searchClearBtn = document.getElementById('audit-search-clear-btn');
    let searchDebounce;

    function updateClearBtn() {
        searchClearBtn.classList.toggle('d-none', searchInput.value === '');
    }

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

    // Date range
    document.getElementById('audit-date-from').addEventListener('change', () => doSearch(true));
    document.getElementById('audit-date-to').addEventListener('change', () => doSearch(true));

    // Browser back/forward
    window.addEventListener('popstate', function() {
        initFromUrl();
        doSearch(false);
    });
});
