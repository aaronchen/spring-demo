// Confirm dialog — Bootstrap modal confirmation replacing window.confirm().
// Created fresh per call, destroyed on hide (avoids backdrop stacking issues).
//
// Options (message is required):
//   message, title, confirmText, cancelText, headerClass, confirmClass, width
//
// onConfirm callback: return false to keep modal open (e.g. for input validation).

const CONFIRM_DEFAULTS = {
    headerClass: "bg-danger text-white",
    confirmClass: "btn btn-danger",
};

export function showConfirm(options, onConfirm) {
    const defaults = window.APP_CONFIG ? APP_CONFIG.messages : {};
    const title = options.title || defaults["action.confirm"] || "Confirm";
    const cancelText = options.cancelText || defaults["action.cancel"] || "Cancel";
    const confirmText = options.confirmText || defaults["action.confirm"] || "Confirm";
    const headerClass = options.headerClass || CONFIRM_DEFAULTS.headerClass;
    const confirmClass = options.confirmClass || CONFIRM_DEFAULTS.confirmClass;
    const width = options.width || "420px";

    const modal = document.createElement("div");
    modal.id = "confirm-modal";
    modal.className = "modal fade";
    modal.tabIndex = -1;
    modal.setAttribute("aria-hidden", "true");
    modal.style.setProperty("--confirm-width", width);
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

    modal.querySelector(".confirm-modal-btn").addEventListener("click", function () {
        if (onConfirm() === false) return;
        bsModal.hide();
    });

    modal.addEventListener("hidden.bs.modal", function () {
        bsModal.dispose();
        modal.remove();
        if (document.querySelector(".modal.show")) {
            document.body.classList.add("modal-open");
            document.body.style.overflow = "";
        }
    });

    document.body.appendChild(modal);
    const bsModal = new bootstrap.Modal(modal);
    bsModal.show();
}

// HTMX integration: intercept hx-confirm and show styled modal.
// Reads data-confirm-* attributes from the triggering element.
document.addEventListener("htmx:confirm", function (evt) {
    if (!evt.detail.question) return;
    evt.preventDefault();
    const el = evt.detail.elt;
    showConfirm(
        {
            message: evt.detail.question,
            title: el.dataset.confirmTitle,
            confirmText: el.dataset.confirmText,
            cancelText: el.dataset.confirmCancelText,
            headerClass: el.dataset.confirmHeaderClass,
            confirmClass: el.dataset.confirmClass,
            width: el.dataset.confirmWidth,
        },
        function () {
            evt.detail.issueRequest(true);
        },
    );
});
