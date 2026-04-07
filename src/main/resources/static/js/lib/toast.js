// Toast notifications — creates Bootstrap toasts and appends to #toast-container.
// Types: 'success', 'danger', 'warning', 'info'.
// Success/info auto-dismiss after 4 seconds; danger/warning stay until manually closed.

const TOAST_ICONS = {
    success: "bi-check-circle-fill",
    danger: "bi-exclamation-triangle-fill",
    warning: "bi-exclamation-triangle-fill",
    info: "bi-info-circle-fill",
};

export function showToast(message, type, options) {
    type = type || "info";
    options = options || {};
    const autohide = type === "success" || type === "info";
    const icon = TOAST_ICONS[type] || TOAST_ICONS.info;

    // Lazy-create the container on first use
    let container = document.getElementById("toast-container");
    if (!container) {
        container = document.createElement("div");
        container.id = "toast-container";
        container.className = "toast-container position-fixed top-0 end-0 p-3";
        document.body.appendChild(container);
    }

    const toastEl = document.createElement("div");
    toastEl.className = `toast align-items-center text-bg-${type} border-0`;
    if (options.href) {
        toastEl.style.cursor = "pointer";
    }
    toastEl.setAttribute("role", "alert");
    toastEl.setAttribute("aria-live", "assertive");
    toastEl.setAttribute("aria-atomic", "true");
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
        toastEl.addEventListener("click", function (e) {
            if (e.target.closest(".btn-close")) return;
            window.location.href = options.href;
        });
    }

    container.appendChild(toastEl);

    const toast = new bootstrap.Toast(toastEl, {
        autohide: autohide,
        delay: 4000,
    });

    toastEl.addEventListener("hidden.bs.toast", function () {
        toastEl.remove();
    });

    toast.show();
}
