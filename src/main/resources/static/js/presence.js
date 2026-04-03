(function () {
    'use strict';

    const countEl = document.getElementById('online-count');
    const listEl = document.getElementById('online-users-list');

    if (!countEl || !window.stompClient) {
        return;
    }

    window.stompClient.onConnect(function (client) {
        client.subscribe(APP_CONFIG.routes.topicPresence.toString(), function (message) {
            const data = JSON.parse(message.body);
            updatePresenceUI(data.count, data.users);
        });

        fetch(APP_CONFIG.routes.apiPresence)
            .then(requireOk)
            .then(function (res) { return res.json(); })
            .then(function (data) { updatePresenceUI(data.count, data.users); });
    });

    function updatePresenceUI(count, users) {
        countEl.textContent = count;

        listEl.innerHTML = '';
        users.forEach(function (name) {
            const li = document.createElement('li');
            const span = document.createElement('span');
            span.className = 'dropdown-item-text';
            span.innerHTML = `<i class="bi bi-circle-fill text-success me-2" style="font-size: 0.5rem;"></i>${escapeHtml(name)}`;
            li.appendChild(span);
            listEl.appendChild(li);
        });
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
})();
