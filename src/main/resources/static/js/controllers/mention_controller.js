import { Controller } from "@hotwired/stimulus";
import { requireOk } from "lib/api";
import { initMentionMap, getMentionMap, findMentionAtCursor, isMentionMenuActive, clearMentions } from "lib/mentions";

// Initializes Tribute.js @mention autocomplete on the controlled element.
// Reads data-mention-project-id-value to scope to project members (fetch once, filter client-side)
// or falls back to server-side search across all users.
//
// Tribute is attached in connect() and detached in disconnect(), so HTMX swaps
// that replace the element trigger automatic cleanup and re-initialization.
export default class extends Controller {
    static values = {
        projectId: String,
    };

    connect() {
        if (typeof Tribute === "undefined") return;
        if (this.element.dataset.tribute === "true") return;

        initMentionMap(this.element);

        // Ensure parent is a positioning context for the dropdown
        const parent = this.element.parentElement;
        if (parent && getComputedStyle(parent).position === "static") {
            parent.style.position = "relative";
        }

        let cachedMembers = null;
        const el = this.element;

        this.tribute = new Tribute({
            trigger: "@",
            positionMenu: false,
            menuContainer: parent,
            values: (text, cb) => {
                const projectId = this.projectIdValue;
                if (projectId) {
                    if (cachedMembers) {
                        cb(cachedMembers);
                        return;
                    }
                    const url = APP_CONFIG.routes.apiProjectMembers.resolve({ projectId });
                    fetch(url, { credentials: "same-origin" })
                        .then(requireOk)
                        .then((r) => r.json())
                        .then((users) => {
                            cachedMembers = users;
                            cb(users);
                        })
                        .catch(() => cb([]));
                } else {
                    const url = `${APP_CONFIG.routes.apiUsers}?q=${encodeURIComponent(text)}`;
                    fetch(url, { credentials: "same-origin" })
                        .then(requireOk)
                        .then((r) => r.json())
                        .then((users) => cb(users))
                        .catch(() => cb([]));
                }
            },
            lookup: "name",
            fillAttr: "name",
            selectTemplate: (item) => {
                const mentions = getMentionMap(el);
                if (mentions) mentions.set(item.original.name, item.original.id);
                return `@${item.original.name}`;
            },
            menuItemTemplate: (item) => `<i class="bi bi-person me-1"></i>${item.string}`,
            noMatchTemplate: () => `<li class="tribute-no-match">${APP_CONFIG.messages["user.empty"]}</li>`,
            requireLeadingSpace: true,
        });

        this.tribute.attach(this.element);
        this.element.setAttribute("data-tribute", "true");

        // Backspace/Delete: remove entire mention as an atomic unit
        this.keydownHandler = (e) => {
            if (e.key !== "Backspace" && e.key !== "Delete") return;
            const match = findMentionAtCursor(el, e.key);
            if (!match) return;
            e.preventDefault();
            const val = el.value;
            el.value = val.substring(0, match.start) + val.substring(match.end);
            el.selectionStart = el.selectionEnd = match.start;
            el.dispatchEvent(new Event("input"));
        };
        this.element.addEventListener("keydown", this.keydownHandler);
    }

    disconnect() {
        if (this.tribute) {
            this.tribute.detach(this.element);
            this.tribute = null;
        }
        if (this.keydownHandler) {
            this.element.removeEventListener("keydown", this.keydownHandler);
        }
        this.element.removeAttribute("data-tribute");
    }

    // Enter key guard — submit comment only if mention dropdown is closed
    submitUnlessMentionOpen(event) {
        if (event.key === "Enter" && !isMentionMenuActive()) {
            event.preventDefault();
            document.getElementById("comment-post-btn")?.click();
        }
    }

    // Clear mention state — triggered by the post button after successful submission
    clear() {
        clearMentions(this.element);
    }
}
