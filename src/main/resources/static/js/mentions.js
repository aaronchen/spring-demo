// @Mention autocomplete via Tribute.js
// Attaches to any element with [data-mention] (input or textarea).
// Shows clean "@Name" in the field; encodes to @[Name](userId:N) before submission.
// Mentions behave as atomic tokens: Backspace/Delete removes the entire "@Name".
//
// Positioning: Tribute's built-in caret calculation is unreliable inside flex/modal
// layouts. We disable it (positionMenu: false) and use pure CSS instead — the dropdown
// is appended to the input's parent and positioned with bottom: 100%.

const mentionMap = new WeakMap(); // element → Map<name, id>

// Returns true if any Tribute dropdown is currently visible (used to suppress
// Enter-to-post while the user is selecting a mention from the dropdown).
function isMentionMenuActive() {
    const containers = document.querySelectorAll('.tribute-container');
    for (const c of containers) {
        if (c.style.display !== 'none' && c.childElementCount > 0) return true;
    }
    return false;
}

// Find the mention token that the cursor is inside or at the boundary of.
// Returns { start, end } or null.
function findMentionAtCursor(el, key) {
    const mentions = mentionMap.get(el);
    if (!mentions || mentions.size === 0) return null;
    const pos = el.selectionStart;
    const val = el.value;
    for (const [name] of mentions) {
        const token = `@${name}`;
        let idx = val.indexOf(token);
        while (idx !== -1) {
            const end = idx + token.length;
            const inside = (key === 'Backspace')
                ? (pos > idx && pos <= end)
                : (pos >= idx && pos < end);
            if (inside) return { start: idx, end: end };
            idx = val.indexOf(token, idx + 1);
        }
    }
    return null;
}

function initMentionInputs(root) {
    if (typeof Tribute === 'undefined') return;
    const els = (root || document).querySelectorAll('[data-mention]:not([data-tribute])');
    els.forEach(function(el) {
        mentionMap.set(el, new Map());

        // Ensure the parent is a positioning context for the dropdown
        const parent = el.parentElement;
        if (parent && getComputedStyle(parent).position === 'static') {
            parent.style.position = 'relative';
        }

        let cachedMembers = null;

        const tribute = new Tribute({
            trigger: '@',
            positionMenu: false,
            menuContainer: parent,
            values: function(text, cb) {
                const projectId = el.dataset.projectId;
                if (projectId) {
                    // Project-scoped: fetch once, filter client-side via Tribute lookup
                    if (cachedMembers) { cb(cachedMembers); return; }
                    const url = APP_CONFIG.routes.apiProjectMembers.resolve({ projectId });
                    fetch(url, { credentials: 'same-origin' })
                        .then(requireOk)
                        .then(function(r) { return r.json(); })
                        .then(function(users) { cachedMembers = users; cb(users); })
                        .catch(function() { cb([]); });
                } else {
                    // No project context: server-side search across all users
                    const url = `${APP_CONFIG.routes.apiUsers}?q=${encodeURIComponent(text)}`;
                    fetch(url, { credentials: 'same-origin' })
                        .then(requireOk)
                        .then(function(r) { return r.json(); })
                        .then(function(users) { cb(users); })
                        .catch(function() { cb([]); });
                }
            },
            lookup: 'name',
            fillAttr: 'name',
            selectTemplate: function(item) {
                const mentions = mentionMap.get(el);
                if (mentions) mentions.set(item.original.name, item.original.id);
                return `@${item.original.name}`;
            },
            menuItemTemplate: function(item) {
                return `<i class="bi bi-person me-1"></i>${item.string}`;
            },
            noMatchTemplate: function() { return '<li class="tribute-no-match">No users found</li>'; },
            requireLeadingSpace: true,
        });
        tribute.attach(el);
        el.setAttribute('data-tribute', 'true');

        // Backspace/Delete: remove entire mention as an atomic unit
        el.addEventListener('keydown', function(e) {
            if (e.key !== 'Backspace' && e.key !== 'Delete') return;
            const match = findMentionAtCursor(el, e.key);
            if (!match) return;
            e.preventDefault();
            const val = el.value;
            el.value = val.substring(0, match.start) + val.substring(match.end);
            el.selectionStart = el.selectionEnd = match.start;
            el.dispatchEvent(new Event('input'));
        });
    });
}

function encodeMentions(text, el) {
    const mentions = el ? mentionMap.get(el) : null;
    if (!mentions || mentions.size === 0) return text;
    mentions.forEach(function(id, name) {
        const clean = `@${name}`;
        const encoded = `@[${name}](userId:${id})`;
        while (text.includes(clean)) {
            text = text.replace(clean, encoded);
        }
    });
    return text;
}

function clearMentions(el) {
    if (mentionMap.has(el)) mentionMap.set(el, new Map());
}

// Encode mentions before HTMX sends any request that includes a [data-mention] field.
// Matches by the input's name attribute against the request parameters.
document.addEventListener('htmx:configRequest', function(evt) {
    document.querySelectorAll('[data-mention][data-tribute]').forEach(function(el) {
        const paramName = el.name;
        if (paramName && evt.detail.parameters[paramName] != null) {
            evt.detail.parameters[paramName] = encodeMentions(evt.detail.parameters[paramName], el);
        }
    });
});

// Encode mentions before regular form submissions
document.addEventListener('submit', function(evt) {
    const form = evt.target;
    form.querySelectorAll('[data-mention][data-tribute]').forEach(function(el) {
        el.value = encodeMentions(el.value, el);
    });
}, true);

document.addEventListener('DOMContentLoaded', function() { initMentionInputs(); });
document.addEventListener('htmx:afterSwap', function(evt) { initMentionInputs(evt.detail.target); });
