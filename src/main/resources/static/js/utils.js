// Backward compatibility shim — re-exports lib/ modules to window.*
// for classic scripts and inline <script> blocks not yet migrated to Stimulus.
// Will be deleted in Phase 8 when all consumers are migrated.

import { requireOk } from "lib/api";
import { showToast } from "lib/toast";
import { showConfirm } from "lib/confirm";
import { getCookie, setCookie } from "lib/cookies";

window.requireOk = requireOk;
window.showToast = showToast;
window.showConfirm = showConfirm;
window.getCookie = getCookie;
window.setCookie = setCookie;
