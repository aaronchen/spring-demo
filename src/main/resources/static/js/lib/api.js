/**
 * Checks a fetch Response and returns it if ok, otherwise rejects.
 * Usage: fetch(url).then(requireOk).then(r => r.json())
 */
export function requireOk(response) {
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return response;
}

/** Returns CSRF headers for raw fetch() calls (not needed for HTMX — lib/htmx-csrf handles that). */
let _csrfToken, _csrfHeader, _csrfResolved = false;

export function csrfHeaders() {
    if (!_csrfResolved) {
        _csrfToken = document.querySelector('meta[name="_csrf"]')?.content || null;
        _csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "X-CSRF-TOKEN";
        _csrfResolved = true;
    }
    return _csrfToken ? { [_csrfHeader]: _csrfToken } : {};
}
