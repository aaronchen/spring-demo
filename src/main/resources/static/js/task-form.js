// Task form — checklist management (shared by task list modal and full-page task view)

function addChecklistItem() {
    const container = document.getElementById('checklist-container');
    if (!container) return;
    const empty = document.getElementById('checklist-empty');
    if (empty) empty.classList.add('d-none');
    const placeholder = APP_CONFIG.messages['task.checklist.placeholder'] || 'Enter checklist item';
    const div = document.createElement('div');
    div.className = 'checklist-item input-group input-group-sm mb-1';
    div.draggable = true;
    div.ondragstart = checklistDragStart;
    div.ondragover = checklistDragOver;
    div.ondragend = checklistDragEnd;
    div.ondrop = checklistDrop;
    div.innerHTML = `<span class="input-group-text checklist-drag-handle">
            <i class="bi bi-grip-vertical"></i>
        </span>
        <div class="input-group-text">
            <input type="hidden" name="checklistChecked" value="false">
            <input type="checkbox" class="form-check-input mt-0"
                   onchange="this.previousElementSibling.value = this.checked">
        </div>
        <input type="text" class="form-control" name="checklistTexts" autocomplete="off" placeholder="${placeholder}">
        <button type="button" class="btn btn-outline-danger" onclick="removeChecklistItem(this)">
            <i class="bi bi-x"></i>
        </button>`;
    container.appendChild(div);
    div.querySelector('input[type="text"]').focus();
    updateChecklistHeading();
}

function removeChecklistItem(btn) {
    const item = btn.closest('.checklist-item');
    if (item) item.remove();
    const container = document.getElementById('checklist-container');
    const empty = document.getElementById('checklist-empty');
    if (empty && container && container.children.length === 0) {
        empty.classList.remove('d-none');
    }
    updateChecklistHeading();
}

function updateChecklistHeading() {
    const heading = document.getElementById('task-checklist-heading');
    const container = document.getElementById('checklist-container');
    if (!heading || !container) return;
    const count = container.children.length;
    const template = APP_CONFIG.messages['task.field.checklist.heading'] || 'Checklist ({0})';
    heading.textContent = template.replace('{0}', count);
}

// ── Checklist drag-and-drop reordering ─────────────────────────────────────

let checklistDragItem = null;

function checklistDragStart(e) {
    checklistDragItem = e.currentTarget;
    checklistDragItem.classList.add('checklist-dragging');
    e.dataTransfer.effectAllowed = 'move';
}

function checklistDragOver(e) {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    const target = e.currentTarget;
    if (target === checklistDragItem || !checklistDragItem) return;
    const container = document.getElementById('checklist-container');
    const items = [...container.children];
    const dragIdx = items.indexOf(checklistDragItem);
    const targetIdx = items.indexOf(target);
    if (dragIdx < targetIdx) {
        container.insertBefore(checklistDragItem, target.nextSibling);
    } else {
        container.insertBefore(checklistDragItem, target);
    }
}

function checklistDrop(e) {
    e.preventDefault();
}

function checklistDragEnd() {
    if (checklistDragItem) {
        checklistDragItem.classList.remove('checklist-dragging');
        checklistDragItem = null;
    }
}
