/**
 * Checks a fetch Response and returns it if ok, otherwise rejects.
 * Usage: fetch(url).then(requireOk).then(r => r.json())
 */
export function requireOk(response) {
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return response;
}
