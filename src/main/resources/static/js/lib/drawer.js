/**
 * Shared drawer behavior for left-side panel drawers (recent views, pins).
 * Handles outside-click close, mutual exclusion via drawer:opened event,
 * and toggle/close panel logic.
 *
 * @param {object} options
 * @param {HTMLElement} options.element - The drawer wrapper element (for contains check)
 * @param {string} options.name - Unique drawer name (e.g., "pins", "recent-views")
 * @param {function} options.getPanel - Returns the panel target element
 * @param {function} options.getToggle - Returns the toggle target element
 * @returns {{ toggle: function(Event), close: function, destroy: function }}
 */
export function setupDrawer({ element, name, getPanel, getToggle }) {
    const outsideClickHandler = (e) => {
        const panel = getPanel();
        if (panel && !element.contains(e.target)) {
            close();
        }
    };

    const drawerOpenedHandler = (e) => {
        if (e.detail !== name) close();
    };

    document.addEventListener("click", outsideClickHandler);
    document.addEventListener("drawer:opened", drawerOpenedHandler);

    function close() {
        const panel = getPanel();
        const toggle = getToggle();
        if (panel) panel.classList.add("d-none");
        if (toggle) toggle.classList.remove("active");
    }

    function toggle(event) {
        event.preventDefault();
        event.stopPropagation();
        const panel = getPanel();
        const toggleEl = getToggle();
        if (!panel) return;

        const isOpening = panel.classList.contains("d-none");
        panel.classList.toggle("d-none");
        if (toggleEl) toggleEl.classList.toggle("active", isOpening);
        if (isOpening) {
            document.dispatchEvent(new CustomEvent("drawer:opened", { detail: name }));
        }
    }

    function destroy() {
        document.removeEventListener("click", outsideClickHandler);
        document.removeEventListener("drawer:opened", drawerOpenedHandler);
    }

    return { toggle, close, destroy };
}
