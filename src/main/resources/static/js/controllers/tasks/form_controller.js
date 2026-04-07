import { Controller } from "@hotwired/stimulus";
import { requireOk } from "lib/api";

// Task form — project-aware assignee list + sprint dropdown + checklist management.
// Shared by task list modal and full-page task view.
//

export default class extends Controller {
    connect() {
        this.checklistDragItem = null;
        this.bindProjectChange();
    }

    // Re-bind after HTMX swaps modal content
    afterSwap(evt) {
        if (evt.detail.target?.id === "task-modal-content") {
            this.bindProjectChange();
        }
    }

    bindProjectChange() {
        const projectSelect = document.getElementById("projectId");
        if (!projectSelect || projectSelect.dataset.formBound) return;
        projectSelect.dataset.formBound = "true";
        projectSelect.addEventListener("change", () => {
            const assigneeSelect = document.getElementById("assigneeId");
            if (!assigneeSelect) return;
            const projectId = projectSelect.value;
            assigneeSelect.setSrc(projectId
                ? APP_CONFIG.routes.apiProjectMembersAssignable.resolve({ projectId })
                : APP_CONFIG.routes.apiUsers);
            assigneeSelect.reset();
            this.updateSprintDropdown(projectId);
        });
    }

    updateSprintDropdown(projectId) {
        const container = document.getElementById("sprint-selector");
        const select = document.getElementById("sprintId");
        if (!container || !select) return;

        if (!projectId) {
            container.classList.add("d-none");
            select.innerHTML = "";
            return;
        }

        const url = APP_CONFIG.routes.apiProjectSprints.resolve({ projectId });
        fetch(url)
            .then(requireOk)
            .then((r) => r.json())
            .then((sprints) => {
                if (!sprints.length) {
                    container.classList.add("d-none");
                    select.innerHTML = "";
                    return;
                }
                const noneLabel = APP_CONFIG.messages["task.field.sprint.none"] || "No Sprint";
                let options = `<option value="">${noneLabel}</option>`;
                for (const s of sprints) {
                    if (s.status !== "past") {
                        options += `<option value="${s.id}">${s.name}</option>`;
                    }
                }
                select.innerHTML = options;
                container.classList.remove("d-none");
            })
            .catch((err) => {
                console.error("Failed to load sprints:", err);
                container.classList.add("d-none");
                select.innerHTML = "";
            });
    }

    // ── Checklist ────────────────────────────────────────────────────────

    addChecklistItem() {
        const container = document.getElementById("checklist-container");
        if (!container) return;
        const template = document.getElementById("checklist-item-template");
        if (!template) return;
        const empty = document.getElementById("checklist-empty");
        if (empty) empty.classList.add("d-none");
        const clone = template.content.firstElementChild.cloneNode(true);
        container.appendChild(clone);
        clone.querySelector('input[type="text"]').focus();
        this.updateChecklistHeading();
    }

    removeChecklistItem(event) {
        const btn = event.currentTarget || event;
        const item = btn.closest(".checklist-item");
        if (item) item.remove();
        const container = document.getElementById("checklist-container");
        const empty = document.getElementById("checklist-empty");
        if (empty && container && container.children.length === 0) {
            empty.classList.remove("d-none");
        }
        this.updateChecklistHeading();
    }

    syncChecklistCheckbox(event) {
        const checkbox = event.currentTarget;
        const hidden = checkbox.previousElementSibling;
        if (hidden) hidden.value = checkbox.checked;
    }

    updateChecklistHeading() {
        const heading = document.getElementById("task-checklist-heading");
        const container = document.getElementById("checklist-container");
        if (!heading || !container) return;
        const count = container.children.length;
        const template = APP_CONFIG.messages["task.field.checklist.heading"] || "Checklist ({0})";
        heading.textContent = template.replace("{0}", count);
    }

    checklistDragStart(e) {
        this.checklistDragItem = e.currentTarget;
        this.checklistDragItem.classList.add("checklist-dragging");
        e.dataTransfer.effectAllowed = "move";
    }

    checklistDragOver(e) {
        e.preventDefault();
        e.dataTransfer.dropEffect = "move";
        const target = e.currentTarget;
        if (target === this.checklistDragItem || !this.checklistDragItem) return;
        const container = document.getElementById("checklist-container");
        const items = [...container.children];
        const dragIdx = items.indexOf(this.checklistDragItem);
        const targetIdx = items.indexOf(target);
        if (dragIdx < targetIdx) {
            container.insertBefore(this.checklistDragItem, target.nextSibling);
        } else {
            container.insertBefore(this.checklistDragItem, target);
        }
    }

    checklistDrop(e) {
        e.preventDefault();
    }

    checklistDragEnd() {
        if (this.checklistDragItem) {
            this.checklistDragItem.classList.remove("checklist-dragging");
            this.checklistDragItem = null;
        }
    }

}
