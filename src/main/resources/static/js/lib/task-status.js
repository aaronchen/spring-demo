// Single source of truth for task status badge styling (JS side).
// Mirrors TaskStatus.getCssClass() on the server.
export const STATUS_BADGE = {
    BACKLOG:     { css: "bg-backlog", terminal: false },
    OPEN:        { css: "bg-secondary", terminal: false },
    IN_PROGRESS: { css: "bg-warning text-dark", terminal: false },
    IN_REVIEW:   { css: "bg-info text-white", terminal: false },
    COMPLETED:   { css: "bg-success", terminal: true },
    CANCELLED:   { css: "bg-dark", terminal: true }
};
