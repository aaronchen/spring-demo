// Task form — checklist management (shared by task list modal and full-page task view)

function addChecklistItem() {
    const container = document.getElementById('checklist-container');
    if (!container) return;
    const empty = document.getElementById('checklist-empty');
    if (empty) empty.classList.add('d-none');
    const placeholder = APP_CONFIG.messages['task.checklist.placeholder'] || 'Enter checklist item';
    const div = document.createElement('div');
    div.className = 'checklist-item input-group input-group-sm mb-1';
    div.innerHTML = `<div class="input-group-text">
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
