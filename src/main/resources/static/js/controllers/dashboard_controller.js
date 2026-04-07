import { Controller } from "@hotwired/stimulus";
import { onConnect } from "lib/websocket";

// Dashboard — refreshes stats via HTMX when tasks change or presence updates.

export default class extends Controller {
    static values = {
        wsProjectIds: String,
    };

    static targets = ["stats"];

    connect() {
        onConnect((client) => {
            if (this.wsProjectIdsValue) {
                this.wsProjectIdsValue.split(",").forEach((id) => {
                    client.subscribe(APP_CONFIG.routes.topicProjectTasks.resolve({ projectId: id.trim() }), () =>
                        this.refreshStats(),
                    );
                });
            }

            client.subscribe(APP_CONFIG.routes.topicPresence.toString(), () => this.refreshStats());
        });
    }

    refreshStats() {
        htmx.ajax("GET", `${APP_CONFIG.routes.dashboard}/stats`, {
            target: `#${this.statsTarget.id}`,
            swap: "outerHTML",
        });
    }
}
