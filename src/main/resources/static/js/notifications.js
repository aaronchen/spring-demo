(function () {
    'use strict';

    const badgeEl = document.getElementById('notification-badge');
    const listEl = document.getElementById('notification-list');
    const emptyEl = document.getElementById('notification-empty');

    if (!badgeEl || !window.stompClient) {
        return;
    }

    // ── Event bus ────────────────────────────────────────────────────────────
    // Custom DOM events decouple producers (WebSocket, dropdown, page) from
    // consumers (badge, dropdown list, notifications page).
    //
    // Events:
    //   notification:received  — new notification arrived (detail: notification object)
    //   notification:read      — single notification marked read (detail: { id })
    //   notification:allRead   — all notifications marked read
    //   notification:cleared   — all notifications deleted

    function fire(name, detail) {
        document.dispatchEvent(new CustomEvent(name, { detail: detail }));
    }

    // ── WebSocket subscription ──────────────────────────────────────────────

    window.stompClient.onConnect(function (client) {
        client.subscribe('/user/queue/notifications', function (message) {
            const data = JSON.parse(message.body);
            fire('notification:received', data);
            showToast(data.message, 'info', { href: data.link });
        });

        refreshBadge();
        loadRecentNotifications();
    });

    // ── Badge ───────────────────────────────────────────────────────────────

    function refreshBadge() {
        fetch(APP_CONFIG.routes.apiNotificationsUnreadCount)
            .then(requireOk)
            .then(function (res) { return res.json(); })
            .then(function (data) { updateBadge(data.count); });
    }

    function updateBadge(count) {
        if (count > 0) {
            badgeEl.textContent = count > 99 ? '99+' : count;
            badgeEl.classList.remove('d-none');
        } else {
            badgeEl.classList.add('d-none');
        }
    }

    document.addEventListener('notification:received', function () {
        const current = badgeEl.classList.contains('d-none') ? 0 : parseInt(badgeEl.textContent) || 0;
        updateBadge(current + 1);
    });

    document.addEventListener('notification:read', function () {
        refreshBadge();
    });

    document.addEventListener('notification:allRead', function () {
        updateBadge(0);
    });

    document.addEventListener('notification:cleared', function () {
        updateBadge(0);
    });

    // ── Dropdown list ───────────────────────────────────────────────────────

    function loadRecentNotifications() {
        fetch(`${APP_CONFIG.routes.apiNotifications}?size=10`)
            .then(requireOk)
            .then(function (res) { return res.json(); })
            .then(function (data) { renderNotificationList(data.content); });
    }

    function renderNotificationList(notifications) {
        listEl.innerHTML = '';
        if (notifications.length === 0) {
            emptyEl.classList.remove('d-none');
            return;
        }
        emptyEl.classList.add('d-none');
        notifications.forEach(function (n) {
            listEl.appendChild(createNotificationItem(n));
        });
    }

    document.addEventListener('notification:received', function (e) {
        emptyEl.classList.add('d-none');
        listEl.prepend(createNotificationItem(e.detail));
        while (listEl.children.length > 10) {
            listEl.removeChild(listEl.lastChild);
        }
    });

    document.addEventListener('notification:read', function (e) {
        const item = listEl.querySelector(`[data-notification-id="${e.detail.id}"]`);
        if (item) item.classList.remove('fw-semibold');
    });

    document.addEventListener('notification:allRead', function () {
        listEl.querySelectorAll('.fw-semibold').forEach(function (el) {
            el.classList.remove('fw-semibold');
        });
    });

    document.addEventListener('notification:cleared', function () {
        listEl.innerHTML = '';
        emptyEl.classList.remove('d-none');
    });

    // ── Dropdown item rendering ─────────────────────────────────────────────

    function createNotificationItem(n) {
        const item = document.createElement('div');
        item.className = `dropdown-item py-2 ${n.read ? '' : 'fw-semibold'}`;
        item.style.cursor = 'pointer';
        item.dataset.notificationId = n.id;

        const icon = getNotificationIcon(n.type);
        const time = formatRelativeTime(n.createdAt);

        const actionLink = n.link
            ? `<a href="${n.link}" class="text-muted ms-2 flex-shrink-0" title="${n.link}"><i class="bi bi-box-arrow-up-right"></i></a>`
            : '';

        item.innerHTML = `<div class="d-flex align-items-start">
            <i class="bi ${icon} me-2 mt-1"></i>
            <div class="flex-grow-1" style="min-width: 0;">
                <div class="text-truncate" style="max-width: 280px;">${escapeHtml(n.message)}</div>
                <small class="text-muted">${time}</small>
            </div>
            ${actionLink}
        </div>`;

        item.addEventListener('click', function () {
            if (!n.read) {
                fetch(APP_CONFIG.routes.apiNotificationRead.resolve({ id: n.id }), { method: 'PATCH' })
                    .then(function () { fire('notification:read', { id: n.id }); });
                item.classList.remove('fw-semibold');
                n.read = true;
            }
        });

        return item;
    }

    // Mark all as read (dropdown header link)
    document.getElementById('notification-mark-all-read')?.addEventListener('click', function (e) {
        e.preventDefault();
        e.stopPropagation();
        fetch(APP_CONFIG.routes.apiNotificationsReadAll, { method: 'PATCH' })
            .then(function () { fire('notification:allRead'); });
    });

    // ── Shared helpers (exposed for notifications page) ─────────────────────

    function getNotificationIcon(type) {
        switch (type) {
            case 'TASK_ASSIGNED': return 'bi-person-plus text-primary';
            case 'TASK_UPDATED': return 'bi-pencil-square text-primary';
            case 'COMMENT_ADDED': return 'bi-chat-dots text-success';
            case 'COMMENT_MENTIONED': return 'bi-at text-info';
            case 'TASK_DUE_REMINDER': return 'bi-calendar-event text-warning';
            case 'TASK_OVERDUE': return 'bi-clock text-danger';
            case 'SYSTEM': return 'bi-megaphone text-warning';
            default: return 'bi-bell text-secondary';
        }
    }

    function formatRelativeTime(dateStr) {
        const date = new Date(dateStr);
        const now = new Date();
        const diffMs = now - date;
        const diffMin = Math.floor(diffMs / 60000);
        const diffHr = Math.floor(diffMs / 3600000);
        const diffDay = Math.floor(diffMs / 86400000);

        if (diffMin < 1) return APP_CONFIG.messages['notification.time.now'];
        if (diffMin < 60) return APP_CONFIG.messages['notification.time.minutes'].replace('{0}', diffMin);
        if (diffHr < 24) return APP_CONFIG.messages['notification.time.hours'].replace('{0}', diffHr);
        return APP_CONFIG.messages['notification.time.days'].replace('{0}', diffDay);
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // Expose helpers for the notifications page to build rows with the same look
    window.notificationHelpers = {
        getIcon: getNotificationIcon,
        formatTime: formatRelativeTime,
        escapeHtml: escapeHtml,
        fire: fire,
    };
})();
