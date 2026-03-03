// Shared browser utilities

function getCookie(name) {
    const match = document.cookie.match(new RegExp('(?:^|;\\s*)' + name + '=([^;]*)'));
    return match ? match[1] : null;
}

function setCookie(name, value) {
    document.cookie = `${name}=${value}; path=/; max-age=31536000`;
}

// ── CSRF header injection for HTMX ──────────────────────────────────────────
// Spring Security requires a CSRF token on every state-changing request.
// Thymeleaf auto-injects it into <form th:action> tags as a hidden input.
// HTMX standalone hx-post buttons (toggle, delete confirm) are not inside a
// <form th:action>, so they need the token sent as a request header instead.
// We read it from the meta tags written by base.html and inject it here once,
// globally, covering all HTMX requests on every page.
document.addEventListener('htmx:configRequest', function (evt) {
    const tokenMeta  = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (tokenMeta && headerMeta && tokenMeta.content) {
        evt.detail.headers[headerMeta.content] = tokenMeta.content;
    }
});
