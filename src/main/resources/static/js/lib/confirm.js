// Confirm dialog — Bootstrap modal confirmation replacing window.confirm().
// Clones <template id="confirm-dialog-template"> from base.html, sets dynamic
// content, then shows. Destroyed on hide (avoids backdrop stacking issues).
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
    const tpl = document.getElementById("confirm-dialog-template");
    const modal = tpl.content.firstElementChild.cloneNode(true);
    modal.id = "confirm-modal";

    const width = options.width || "420px";
    modal.style.setProperty("--confirm-width", width);

    const header = modal.querySelector("[data-confirm-header]");
    header.className = `modal-header border-0 ${options.headerClass || CONFIRM_DEFAULTS.headerClass}`;

    if (options.title) {
        modal.querySelector("[data-confirm-title]").textContent = options.title;
    }

    modal.querySelector("[data-confirm-message]").innerHTML = options.message;

    if (options.cancelText) {
        modal.querySelector("[data-confirm-cancel]").textContent = options.cancelText;
    }

    const confirmBtn = modal.querySelector("[data-confirm-action]");
    confirmBtn.className = options.confirmClass || CONFIRM_DEFAULTS.confirmClass;
    if (options.confirmText) {
        modal.querySelector("[data-confirm-ok]").textContent = options.confirmText;
    }

    confirmBtn.addEventListener("click", function () {
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
