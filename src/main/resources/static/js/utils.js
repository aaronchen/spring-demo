// Shared browser utilities

function getCookie(name) {
    const match = document.cookie.match(new RegExp('(?:^|;\\s*)' + name + '=([^;]*)'));
    return match ? match[1] : null;
}

function setCookie(name, value) {
    document.cookie = `${name}=${value}; path=/; max-age=31536000`;
}

// ── Toast notifications ─────────────────────────────────────────────────────
// showToast(message, type) — creates a Bootstrap toast and appends it to #toast-container.
// Types: 'success', 'danger', 'warning', 'info'.
// Success/info auto-dismiss after 4 seconds; danger/warning stay until manually closed.

const TOAST_ICONS = {
    success: 'bi-check-circle-fill',
    danger:  'bi-exclamation-triangle-fill',
    warning: 'bi-exclamation-triangle-fill',
    info:    'bi-info-circle-fill',
};

function showToast(message, type) {
    type = type || 'info';
    const autohide = type === 'success' || type === 'info';
    const icon = TOAST_ICONS[type] || TOAST_ICONS.info;

    // Lazy-create the container on first use
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container position-fixed top-0 end-0 p-3';
        document.body.appendChild(container);
    }

    const toastEl = document.createElement('div');
    toastEl.className = `toast align-items-center text-bg-${type} border-0`;
    toastEl.setAttribute('role', 'alert');
    toastEl.setAttribute('aria-live', 'assertive');
    toastEl.setAttribute('aria-atomic', 'true');
    toastEl.innerHTML =
        '<div class="d-flex align-items-center">' +
            '<div class="toast-icon fs-4 px-3">' +
                '<i class="bi ' + icon + '"></i>' +
            '</div>' +
            '<div class="toast-divider"></div>' +
            '<div class="toast-body flex-grow-1">' + message + '</div>' +
            '<button type="button" class="btn-close btn-close-white me-3 flex-shrink-0" data-bs-dismiss="toast" aria-label="Close"></button>' +
        '</div>';

    container.appendChild(toastEl);

    const toast = new bootstrap.Toast(toastEl, {
        autohide: autohide,
        delay: 4000,
    });

    toastEl.addEventListener('hidden.bs.toast', function () {
        toastEl.remove();
    });

    toast.show();
}

// ── CSRF header injection for HTMX ──────────────────────────────────────────
// Spring Security requires a CSRF token on every state-changing request.
// Thymeleaf auto-injects it into <form th:action> tags as a hidden input.
// HTMX standalone hx-post buttons (toggle, delete confirm) are not inside a
// <form th:action>, so they need the token sent as a request header instead.
// We read it from the meta tags written by base.html and inject it here once,
// globally, covering all HTMX requests on every page.
// ── 409 Conflict handler for HTMX ────────────────────────────────────────────
// When an HTMX request gets a 409 (optimistic locking conflict), the response
// is not swapped by default — the user sees nothing. This handler catches it
// globally and shows a toast so the user knows to reload.
document.addEventListener('htmx:responseError', function (evt) {
    if (evt.detail.xhr.status === 409) {
        showToast(APP_CONFIG.messages['error.409.message'], 'danger');
    }
});

document.addEventListener('htmx:configRequest', function (evt) {
    const tokenMeta  = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (tokenMeta && headerMeta && tokenMeta.content) {
        evt.detail.headers[headerMeta.content] = tokenMeta.content;
    }
});
