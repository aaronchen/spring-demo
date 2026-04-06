const _escapeDiv = document.createElement("div");

export function escapeHtml(text) {
    _escapeDiv.textContent = text;
    return _escapeDiv.innerHTML;
}

export function fire(name, detail) {
    document.dispatchEvent(new CustomEvent(name, { detail }));
}
