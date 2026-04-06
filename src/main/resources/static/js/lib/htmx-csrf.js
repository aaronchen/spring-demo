// CSRF header injection for HTMX requests.
// Spring Security requires a CSRF token on every state-changing request.
// Reads token from meta tags written by base.html and injects as a request header.
// Imported as a side-effect in application.js.

document.addEventListener("htmx:configRequest", function (evt) {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (tokenMeta && headerMeta && tokenMeta.content) {
        evt.detail.headers[headerMeta.content] = tokenMeta.content;
    }
});
