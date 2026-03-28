// Shared browser utilities

// Substitute {placeholder} tokens in a URL template with values from a params object.
// Example: resolveRoute("/api/projects/{projectId}/members", { projectId: 42 })
//       → "/api/projects/42/members"
function resolveRoute(template, params) {
    let url = template;
    for (const [key, value] of Object.entries(params)) {
        url = url.replace(`{${key}}`, value);
    }
    return url;
}

function getCookie(name) {
    const match = document.cookie.match(new RegExp(`(?:^|;\\s*)${name}=([^;]*)`));
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

function showToast(message, type, options) {
    type = type || 'info';
    options = options || {};
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
    if (options.href) {
        toastEl.style.cursor = 'pointer';
    }
    toastEl.setAttribute('role', 'alert');
    toastEl.setAttribute('aria-live', 'assertive');
    toastEl.setAttribute('aria-atomic', 'true');
    toastEl.innerHTML = `
        <div class="d-flex align-items-center">
            <div class="toast-icon fs-4 px-3">
                <i class="bi ${icon}"></i>
            </div>
            <div class="toast-divider"></div>
            <div class="toast-body flex-grow-1">${message}</div>
            <button type="button" class="btn-close btn-close-white me-3 flex-shrink-0" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>`;

    if (options.href) {
        toastEl.addEventListener('click', function (e) {
            if (e.target.closest('.btn-close')) return;
            window.location.href = options.href;
        });
    }

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

// ── Confirm dialog ──────────────────────────────────────────────────────────
// showConfirm(options, onConfirm) — shows a Bootstrap modal confirmation dialog.
// A fresh modal is created each call and destroyed on hide (avoids Bootstrap
// backdrop stacking issues with nested modals).
//
// Options (message is required):
//   message      — modal body content; accepts plain text or HTML (required)
//   title        — header title (default: APP_CONFIG 'action.confirm' || 'Confirm')
//   confirmText  — confirm button label (default: APP_CONFIG 'action.confirm' || 'Confirm')
//   cancelText   — cancel button label (default: APP_CONFIG 'action.cancel' || 'Cancel')
//   headerClass  — header CSS classes (default: 'bg-danger text-white')
//   confirmClass — confirm button CSS classes (default: 'btn btn-danger')
//   width        — modal width (default: '420px', via CSS custom property)
//
// onConfirm callback: return false to keep the modal open (e.g. for input validation).
//
// HTMX integration: intercepts htmx:confirm so any element with hx-confirm gets
// the styled modal. Use data-confirm-* attributes to pass options from HTML:
//   <button hx-confirm="Delete this?" data-confirm-title="Delete"
//           data-confirm-header-class="bg-warning text-dark">

const CONFIRM_DEFAULTS = {
    headerClass:  'bg-danger text-white',
    confirmClass: 'btn btn-danger',
};

function showConfirm(options, onConfirm) {
    const defaults = window.APP_CONFIG ? APP_CONFIG.messages : {};
    const title       = options.title || defaults['action.confirm'] || 'Confirm';
    const cancelText  = options.cancelText || defaults['action.cancel'] || 'Cancel';
    const confirmText = options.confirmText || defaults['action.confirm'] || 'Confirm';
    const headerClass  = options.headerClass || CONFIRM_DEFAULTS.headerClass;
    const confirmClass = options.confirmClass || CONFIRM_DEFAULTS.confirmClass;
    const width = options.width || '420px';

    // Create a fresh modal each time — Bootstrap's backdrop cleanup doesn't
    // handle nested modals well, so we destroy the entire element on hide.
    const modal = document.createElement('div');
    modal.id = 'confirm-modal';
    modal.className = 'modal fade';
    modal.tabIndex = -1;
    modal.setAttribute('aria-hidden', 'true');
    modal.style.setProperty('--confirm-width', width);
    modal.innerHTML = `
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content shadow-lg border-0">
                <div class="modal-header border-0 ${headerClass}">
                    <h5 class="modal-title">
                        <i class="bi bi-exclamation-triangle"></i> ${title}
                    </h5>
                    <button type="button" class="btn-close btn-close-white"
                            data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body py-4">
                    ${options.message}
                </div>
                <div class="modal-footer border-0">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                        <i class="bi bi-x-circle"></i> ${cancelText}
                    </button>
                    <button type="button" class="${confirmClass} confirm-modal-btn">
                        <i class="bi bi-check-circle"></i> ${confirmText}
                    </button>
                </div>
            </div>
        </div>`;

    modal.querySelector('.confirm-modal-btn').addEventListener('click', function () {
        if (onConfirm() === false) return; // return false to keep modal open
        bsModal.hide();
    });

    modal.addEventListener('hidden.bs.modal', function () {
        bsModal.dispose();
        modal.remove();
        // Restore body state if another modal is still open
        if (document.querySelector('.modal.show')) {
            document.body.classList.add('modal-open');
            document.body.style.overflow = '';
        }
    });

    document.body.appendChild(modal);
    const bsModal = new bootstrap.Modal(modal);
    bsModal.show();
}

// HTMX integration: intercept hx-confirm and show styled modal.
// Reads data-confirm-* attributes from the triggering element for per-element customization.
document.addEventListener('htmx:confirm', function (evt) {
    if (!evt.detail.question) return;
    evt.preventDefault();
    const el = evt.detail.elt;
    showConfirm({
        message:      evt.detail.question,
        title:        el.dataset.confirmTitle,
        confirmText:  el.dataset.confirmText,
        cancelText:   el.dataset.confirmCancelText,
        headerClass:  el.dataset.confirmHeaderClass,
        confirmClass: el.dataset.confirmClass,
        width:        el.dataset.confirmWidth,
    }, function () {
        evt.detail.issueRequest(true);
    });
});

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
    const status = evt.detail.xhr.status;
    const msg = evt.detail.xhr.responseText;
    if (status === 400) {
        showToast(msg || APP_CONFIG.messages['error.400.heading'], 'danger');
    } else if (status === 404) {
        showToast(msg || APP_CONFIG.messages['error.404.message'], 'warning');
    } else if (status === 409) {
        showToast(msg || APP_CONFIG.messages['error.409.message'], 'danger');
    } else if (status >= 500) {
        showToast(APP_CONFIG.messages['error.500.message'], 'danger');
    }
});

document.addEventListener('htmx:configRequest', function (evt) {
    const tokenMeta  = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (tokenMeta && headerMeta && tokenMeta.content) {
        evt.detail.headers[headerMeta.content] = tokenMeta.content;
    }
});

