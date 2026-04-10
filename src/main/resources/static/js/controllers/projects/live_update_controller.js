import { Controller } from "@hotwired/stimulus";
import { onConnect } from "lib/websocket";

// Project page live update — shows stale-data banner when another user
// modifies the project (name, description, archive/unarchive).

export default class extends Controller {
    static values = { projectId: String };
    static targets = ["banner"];

    connect() {
        this.sub = null;
        const currentUserId = document.querySelector('meta[name="_userId"]')?.content;

        this.deregisterWs = onConnect((client) => {
            this.sub = client.subscribe(
                APP_CONFIG.routes.topicProject.params({ projectId: this.projectIdValue }).build(),
                (message) => {
                    const data = JSON.parse(message.body);
                    if (currentUserId && String(data.userId) === currentUserId) return;
                    this.bannerTarget.classList.remove("d-none");
                },
            );
        });
    }

    disconnect() {
        if (this.sub) this.sub.unsubscribe();
        if (this.deregisterWs) this.deregisterWs();
    }

    refresh(event) {
        event.preventDefault();
        location.reload();
    }
}
