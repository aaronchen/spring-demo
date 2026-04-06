// CSRF header injection for HTMX requests.
// Spring Security requires a CSRF token on every state-changing request.
// Meta tags are cached at module load (they never change during a session).
// Imported as a side-effect in application.js.

const tokenMeta = document.querySelector('meta[name="_csrf"]');
const headerMeta = document.querySelector('meta[name="_csrf_header"]');

if (tokenMeta && headerMeta) {
    document.addEventListener("htmx:configRequest", function (evt) {
        if (tokenMeta.content) {
            evt.detail.headers[headerMeta.content] = tokenMeta.content;
        }
    });
}
