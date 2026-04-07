import { Controller } from "@hotwired/stimulus";

// Task list keyboard shortcuts.
// Communicates with sibling controllers via custom DOM events.

export default class extends Controller {
    connect() {
        this.keydownHandler = (e) => this.handleKeydown(e);
        document.addEventListener("keydown", this.keydownHandler);
    }

    disconnect() {
        document.removeEventListener("keydown", this.keydownHandler);
    }

    handleKeydown(e) {
        const tag = e.target.tagName;
        if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT" || e.target.isContentEditable) {
            if (e.key === "Escape") e.target.blur();
            return;
        }
        if (e.ctrlKey || e.metaKey || e.altKey) return;

        switch (e.key) {
            case "h":
                e.preventDefault();
                this.toggleHelp();
                break;
            case "n":
                e.preventDefault();
                this.triggerNewTask();
                break;
            case "s":
                e.preventDefault();
                this.focusSearch();
                break;
            case "1":
                e.preventDefault();
                this.dispatchSwitchView("cards");
                break;
            case "2":
                e.preventDefault();
                this.dispatchSwitchView("table");
                break;
            case "3":
                e.preventDefault();
                this.dispatchSwitchView("calendar");
                break;
            case "4":
                e.preventDefault();
                this.dispatchSwitchView("board");
                break;
            case "e":
                e.preventDefault();
                this.element.dispatchEvent(new CustomEvent("tasks:toggle-edit-mode", { bubbles: true }));
                break;
            case "Escape":
                this.closeOpenModal();
                break;
        }
    }

    toggleHelp() {
        const modal = document.getElementById("keyboard-help-modal");
        if (!modal) return;
        const instance = bootstrap.Modal.getOrCreateInstance(modal);
        if (modal.classList.contains("show")) {
            instance.hide();
        } else {
            instance.show();
        }
    }

    triggerNewTask() {
        const newBtn = document.querySelector('a[hx-get$="/tasks/new"], button[hx-get*="/tasks/new"]');
        if (newBtn) newBtn.click();
    }

    focusSearch() {
        const input = document.getElementById("search-input");
        if (input) {
            input.focus();
            input.select();
        }
    }

    dispatchSwitchView(view) {
        this.element.dispatchEvent(new CustomEvent("tasks:switch-view", { bubbles: true, detail: { view } }));
    }

    closeOpenModal() {
        const openModals = document.querySelectorAll(".modal.show");
        if (openModals.length > 0) {
            const topModal = openModals[openModals.length - 1];
            bootstrap.Modal.getInstance(topModal)?.hide();
        }
    }
}
