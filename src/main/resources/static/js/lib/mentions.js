// @Mention encoding and state management.
// Shared by mention_controller.js (Tribute init) and global event listeners
// (htmx:configRequest encoding, form submit encoding).

const mentionMap = new WeakMap(); // element -> Map<name, id>

export function getMentionMap(el) {
    return mentionMap.get(el);
}

export function initMentionMap(el) {
    mentionMap.set(el, new Map());
}

export function clearMentions(el) {
    if (mentionMap.has(el)) mentionMap.set(el, new Map());
}

export function encodeMentions(text, el) {
    const mentions = el ? mentionMap.get(el) : null;
    if (!mentions || mentions.size === 0) return text;
    mentions.forEach(function (id, name) {
        const clean = `@${name}`;
        const encoded = `@[${name}](userId:${id})`;
        while (text.includes(clean)) {
            text = text.replace(clean, encoded);
        }
    });
    return text;
}

export function isMentionMenuActive() {
    const containers = document.querySelectorAll(".tribute-container");
    for (const c of containers) {
        if (c.style.display !== "none" && c.childElementCount > 0) return true;
    }
    return false;
}

export function findMentionAtCursor(el, key) {
    const mentions = mentionMap.get(el);
    if (!mentions || mentions.size === 0) return null;
    const pos = el.selectionStart;
    const val = el.value;
    for (const [name] of mentions) {
        const token = `@${name}`;
        let idx = val.indexOf(token);
        while (idx !== -1) {
            const end = idx + token.length;
            const inside = key === "Backspace" ? pos > idx && pos <= end : pos >= idx && pos < end;
            if (inside) return { start: idx, end: end };
            idx = val.indexOf(token, idx + 1);
        }
    }
    return null;
}
