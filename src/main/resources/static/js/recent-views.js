(function () {
    'use strict';

    const wrapper = document.getElementById('recent-views-wrapper');
    if (!wrapper) return;

    const toggle = document.getElementById('recent-views-toggle');
    const panel = document.getElementById('recent-views-panel');
    const listEl = document.getElementById('recent-views-list');
    const emptyEl = document.getElementById('recent-views-empty');
    const MAX_ITEMS = 10;

    // Toggle panel
    toggle.addEventListener('click', function (e) {
        e.preventDefault();
        panel.classList.toggle('d-none');
        toggle.classList.toggle('active', !panel.classList.contains('d-none'));
    });

    // Close on outside click
    document.addEventListener('click', function (e) {
        if (!wrapper.contains(e.target) && !panel.classList.contains('d-none')) {
            panel.classList.add('d-none');
            toggle.classList.remove('active');
        }
    });

    // Load initial data via API
    function loadRecentViews() {
        fetch(APP_CONFIG.routes.apiRecentViews)
            .then(requireOk)
            .then(function (res) { return res.json(); })
            .then(function (items) {
                listEl.innerHTML = '';
                if (items.length === 0) {
                    emptyEl.classList.remove('d-none');
                    return;
                }
                emptyEl.classList.add('d-none');
                items.forEach(function (item) {
                    listEl.appendChild(createItem(item.entityType, item.entityId, item.entityTitle, item.href, item.viewedAt));
                });
            });
    }

    // WebSocket subscription for live updates
    if (window.stompClient) {
        window.stompClient.onConnect(function (client) {
            client.subscribe('/user/queue/recent-views', function (message) {
                const data = JSON.parse(message.body);
                if (data.titleOnly) {
                    updateTitleInPlace(data.entityType, data.entityId, data.entityTitle);
                } else {
                    updateList(data.entityType, data.entityId, data.entityTitle, data.href, data.viewedAt);
                }
            });

            loadRecentViews();
        });
    }

    function formatTime(dateStr) {
        const date = new Date(dateStr);
        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
            + ' ' + date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false });
    }

    function createItem(entityType, entityId, title, href, viewedAt) {
        const iconClass = entityType === 'TASK'
            ? 'bi bi-check2-square text-primary'
            : 'bi bi-folder text-info';
        const timeStr = viewedAt ? formatTime(viewedAt) : '';

        const item = document.createElement('a');
        item.href = href;
        item.className = 'list-group-item list-group-item-action py-2 px-3';
        item.dataset.entityType = entityType;
        item.dataset.entityId = entityId;
        item.innerHTML = `<div class="d-flex align-items-center gap-2">
            <i class="${iconClass}" style="font-size: 0.8rem;"></i>
            <div class="text-truncate" style="min-width: 0;">
                <div class="text-truncate small rv-title">${escapeHtml(title)}</div>
                <small class="text-muted" style="font-size: 0.7rem;">${timeStr}</small>
            </div>
        </div>`;
        return item;
    }

    function updateTitleInPlace(entityType, entityId, title) {
        const existing = listEl.querySelector(
            `[data-entity-type="${entityType}"][data-entity-id="${entityId}"]`);
        if (existing) {
            const titleEl = existing.querySelector('.rv-title');
            if (titleEl) titleEl.textContent = title;
        }
    }

    function updateList(entityType, entityId, title, href, viewedAt) {
        emptyEl.classList.add('d-none');

        // Remove existing entry for this entity (will re-add at top)
        const existing = listEl.querySelector(
            `[data-entity-type="${entityType}"][data-entity-id="${entityId}"]`);
        if (existing) existing.remove();

        listEl.prepend(createItem(entityType, entityId, title, href, viewedAt));

        while (listEl.children.length > MAX_ITEMS) {
            listEl.lastElementChild.remove();
        }
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
})();
