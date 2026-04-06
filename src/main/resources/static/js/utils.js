// Backward compatibility shim — exposes showToast and showConfirm on window
// for inline <script> blocks in admin pages and OOB HTMX panel responses.
// Will be deleted when those are converted to HX-Trigger headers or Stimulus controllers.

import { showToast } from "lib/toast";
import { showConfirm } from "lib/confirm";

window.showToast = showToast;
window.showConfirm = showConfirm;
