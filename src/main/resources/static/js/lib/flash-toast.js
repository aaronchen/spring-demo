// Flash toasts — two mechanisms for showing toasts without inline scripts:
//
// 1. HX-Trigger: HTMX responses set HX-Trigger header with showToast event.
//    HTMX dispatches it as a DOM event, picked up by the listener below.
//
// 2. Flash attributes: server renders hidden [data-flash-toast] elements with
//    data-message. Scanned on page load for post-redirect toasts.

import { showToast } from "lib/toast";

// HX-Trigger listener
document.addEventListener("showToast", (e) => {
    showToast(e.detail.message, e.detail.type || "success");
});

// Flash attribute scan
document.querySelectorAll("[data-flash-toast]").forEach((el) => {
    showToast(el.dataset.message, el.dataset.type || "success");
});
