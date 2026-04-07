import { Controller } from "@hotwired/stimulus";
import { onConnect } from "lib/websocket";

// Live task/comment updates via WebSocket — used on task detail page and task modal.
// Subscribes to project task changes and comment changes for a specific task.
// Shows stale-data banner when another user modifies the task or adds a comment.
//
// Values:
//   taskId     — the task being viewed
//   projectId  — the task's project (for project-level change subscription)
//   refreshUrl — URL to reload content (modal uses HTMX, full page uses location.reload)
//   modal      — "true" if this is a modal (uses htmx refresh instead of page reload)

export default class extends Controller {
    static values = {
        taskId: String,
        projectId: String,
        refreshUrl: String,
        modal: { type: Boolean, default: false },
    };

    static targets = ["banner", "refresh"];

    connect() {
        this.subs = [];
        const currentUserId = document.querySelector('meta[name="_userId"]')?.content;

        onConnect((client) => {
            // Task change subscription
            this.subs.push(
                client.subscribe(
                    APP_CONFIG.routes.topicProjectTasks.params({ projectId: this.projectIdValue }).build(),
                    (message) => {
                        const data = JSON.parse(message.body);
                        if (currentUserId && String(data.userId) === currentUserId) return;
                        if (data.taskId !== this.taskIdValue) return;
                        this.bannerTarget.classList.remove("d-none");
                    },
                ),
            );

            // Comment subscription — refresh activity timeline
            this.subs.push(
                client.subscribe(
                    APP_CONFIG.routes.topicTaskComments.params({ taskId: this.taskIdValue }).build(),
                    (message) => {
                        const data = JSON.parse(message.body);
                        if (currentUserId && String(data.userId) === currentUserId) return;
                        htmx.ajax("GET", `${APP_CONFIG.routes.tasks}/${this.taskIdValue}/activity`, {
                            target: "#task-activity",
                            swap: "outerHTML",
                        });
                    },
                ),
            );
        });

        // Modal: unsubscribe on close
        if (this.modalValue) {
            const modal = document.getElementById("task-modal");
            if (modal) {
                modal.addEventListener("hidden.bs.modal", () => this.unsubscribe(), { once: true });
            }
        }
    }

    disconnect() {
        this.unsubscribe();
    }

    refresh(event) {
        event.preventDefault();
        if (this.modalValue) {
            htmx.ajax("GET", this.refreshUrlValue, { target: "#task-modal-content", swap: "innerHTML" });
        } else {
            location.reload();
        }
    }

    unsubscribe() {
        this.subs.forEach((s) => s.unsubscribe());
        this.subs = [];
    }
}
