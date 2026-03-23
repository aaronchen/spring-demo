// Task list keyboard shortcuts — loaded on task list and project pages

document.addEventListener('DOMContentLoaded', function() {
    document.addEventListener('keydown', function(e) {
        // Skip when focus is in an editable element
        const tag = e.target.tagName;
        if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || e.target.isContentEditable) {
            if (e.key === 'Escape') {
                e.target.blur();
            }
            return;
        }

        // Skip if modifier keys are held
        if (e.ctrlKey || e.metaKey || e.altKey) return;

        switch (e.key) {
            case 'h':
                e.preventDefault();
                toggleKeyboardHelp();
                break;

            case 'n':
                e.preventDefault();
                triggerNewTask();
                break;

            case 's':
                e.preventDefault();
                focusSearch();
                break;

            case '1':
                e.preventDefault();
                switchViewIfAvailable('cards');
                break;

            case '2':
                e.preventDefault();
                switchViewIfAvailable('table');
                break;

            case '3':
                e.preventDefault();
                switchViewIfAvailable('calendar');
                break;

            case '4':
                e.preventDefault();
                switchViewIfAvailable('board');
                break;

            case 'e':
                if (typeof currentView !== 'undefined' && currentView === 'table'
                        && typeof toggleEditMode === 'function') {
                    e.preventDefault();
                    toggleEditMode();
                }
                break;

            case 'Escape':
                closeOpenModal();
                break;
        }
    });
});

function toggleKeyboardHelp() {
    const modal = document.getElementById('keyboard-help-modal');
    if (!modal) return;
    const instance = bootstrap.Modal.getOrCreateInstance(modal);
    if (modal.classList.contains('show')) {
        instance.hide();
    } else {
        instance.show();
    }
}

function triggerNewTask() {
    const newBtn = document.querySelector('a[hx-get$="/tasks/new"], button[hx-get*="/tasks/new"]');
    if (newBtn) newBtn.click();
}

function focusSearch() {
    const input = document.getElementById('search-input');
    if (input) {
        input.focus();
        input.select();
    }
}

function switchViewIfAvailable(view) {
    const btn = document.getElementById(`view-${view}`);
    if (btn && typeof switchView === 'function') {
        switchView(view);
    }
}

function closeOpenModal() {
    const openModals = document.querySelectorAll('.modal.show');
    if (openModals.length > 0) {
        const topModal = openModals[openModals.length - 1];
        bootstrap.Modal.getInstance(topModal)?.hide();
    }
}
