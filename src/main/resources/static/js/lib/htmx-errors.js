// Global HTMX error handler — shows toast on non-2xx responses.
// Without this, failed HTMX requests are silently swallowed.
// Imported as a side-effect in application.js.

import { t } from "lib/i18n";
import { showToast } from "lib/toast";

document.addEventListener("htmx:responseError", function (evt) {
    const status = evt.detail.xhr.status;
    const msg = evt.detail.xhr.responseText;
    if (status === 400) {
        showToast(msg || t("error.400.heading"), "danger");
    } else if (status === 404) {
        showToast(msg || t("error.404.message"), "warning");
    } else if (status === 409) {
        showToast(msg || t("error.409.message"), "danger");
    } else if (status >= 500) {
        showToast(t("error.500.message"), "danger");
    }
});
