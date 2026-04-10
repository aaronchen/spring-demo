import { Controller } from "@hotwired/stimulus";
import { requireOk } from "lib/api";
import { onConnect } from "lib/websocket";
import { escapeHtml } from "lib/html";

export default class extends Controller {
    static targets = ["count", "list"];

    connect() {
        onConnect((client) => {
            client.subscribe(APP_CONFIG.routes.topicPresence.toString(), (message) => {
                const data = JSON.parse(message.body);
                this.updateUI(data.count, data.users);
            });

            fetch(APP_CONFIG.routes.apiPresence.build())
                .then(requireOk)
                .then((res) => res.json())
                .then((data) => this.updateUI(data.count, data.users));
        });
    }

    updateUI(count, users) {
        this.countTarget.textContent = count;

        this.listTarget.innerHTML = "";
        users.forEach((name) => {
            const li = document.createElement("li");
            const span = document.createElement("span");
            span.className = "dropdown-item-text";
            span.innerHTML = `<i class="bi bi-circle-fill text-success me-2" style="font-size: 0.5rem;"></i>${escapeHtml(name)}`;
            li.appendChild(span);
            this.listTarget.appendChild(li);
        });
    }
}
