import { Controller } from "@hotwired/stimulus";
import { requireOk } from "lib/api";
import { enumToCamelCase } from "lib/i18n";

const STATUS_COLORS = {
    BACKLOG: "#adb5bd",
    OPEN: "#0d6efd",
    IN_PROGRESS: "#ffc107",
    IN_REVIEW: "#0dcaf0",
    COMPLETED: "#198754",
    CANCELLED: "#dc3545",
};

const PRIORITY_COLORS = {
    LOW: "#198754",
    MEDIUM: "#ffc107",
    HIGH: "#dc3545",
};

export default class extends Controller {
    static values = {
        apiUrl: String,
    };

    connect() {
        this.msg = window.APP_CONFIG?.messages || {};
        this.charts = {};
        this.currentSprintId = "";

        this.initFilterListeners();
        this.fetchAndRender();
    }

    disconnect() {
        this.destroyCharts();
    }

    // ── Actions ──────────────────────────────────────────────────────────

    setSprintFilter(event) {
        this.currentSprintId = event.target.value;
        this.fetchAndRender();
    }

    // ── Label helpers ────────────────────────────────────────────────────

    statusLabel(key) {
        return this.msg[`task.status.${enumToCamelCase(key)}`] || key;
    }

    priorityLabel(key) {
        return this.msg[`task.priority.${key.toLowerCase()}`] || key;
    }

    // ── Project filter ───────────────────────────────────────────────────

    getSelectedProjectIds() {
        const checkboxes = this.element.querySelectorAll(".project-filter-checkbox");
        if (checkboxes.length === 0) return null;
        const selected = [];
        checkboxes.forEach((cb) => {
            if (cb.checked) selected.push(cb.value);
        });
        return selected;
    }

    buildFetchUrl() {
        const projectIds = this.getSelectedProjectIds();
        const params = new URLSearchParams();

        if (projectIds !== null) {
            if (projectIds.length === 0) return null;
            projectIds.forEach((id) => params.append("projectIds", id));
        }

        if (this.currentSprintId) {
            params.append("sprintId", this.currentSprintId);
        }

        const qs = params.toString();
        return qs ? `${this.apiUrlValue}?${qs}` : this.apiUrlValue;
    }

    initFilterListeners() {
        const checkboxes = this.element.querySelectorAll(".project-filter-checkbox");
        if (checkboxes.length === 0) return;

        const selectAll = this.element.querySelector("#filter-select-all");

        checkboxes.forEach((cb) => {
            cb.addEventListener("change", () => {
                if (selectAll) {
                    selectAll.checked = [...checkboxes].every((c) => c.checked);
                }
                this.fetchAndRender();
            });
        });

        if (selectAll) {
            selectAll.addEventListener("change", () => {
                checkboxes.forEach((cb) => { cb.checked = selectAll.checked; });
                this.fetchAndRender();
            });
        }
    }

    // ── Fetch & Render ───────────────────────────────────────────────────

    destroyCharts() {
        Object.values(this.charts).forEach((chart) => chart.destroy());
        this.charts = {};
    }

    fetchAndRender() {
        const url = this.buildFetchUrl();

        this.destroyCharts();

        if (url === null) return;

        fetch(url, { headers: { Accept: "application/json" } })
            .then(requireOk)
            .then((response) => response.json())
            .then((data) => {
                this.renderStatusChart(data.statusBreakdown);
                this.renderPriorityChart(data.priorityBreakdown);
                this.renderWorkloadChart(data.workloadDistribution);
                this.renderBurndownChart(data.burndown);
                this.renderVelocityChart(data.velocity);
                this.renderOverdueChart(data.overdueAnalysis);
                this.renderEffortChart(data.effortDistribution);
            });
    }

    // ── Chart Renderers ──────────────────────────────────────────────────

    renderStatusChart(breakdown) {
        const ctx = this.element.querySelector("#statusChart");
        const labels = Object.keys(breakdown.counts).map((k) => this.statusLabel(k));
        const values = Object.values(breakdown.counts);
        const colors = Object.keys(breakdown.counts).map((k) => STATUS_COLORS[k] || "#999");

        this.charts.status = new Chart(ctx, {
            type: "doughnut",
            data: {
                labels,
                datasets: [{ data: values, backgroundColor: colors, borderWidth: 2 }],
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: "right" } },
            },
        });
    }

    renderPriorityChart(breakdown) {
        const ctx = this.element.querySelector("#priorityChart");
        const labels = Object.keys(breakdown.counts).map((k) => this.priorityLabel(k));
        const values = Object.values(breakdown.counts);
        const colors = Object.keys(breakdown.counts).map((k) => PRIORITY_COLORS[k] || "#999");

        this.charts.priority = new Chart(ctx, {
            type: "doughnut",
            data: {
                labels,
                datasets: [{ data: values, backgroundColor: colors, borderWidth: 2 }],
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: "right" } },
            },
        });
    }

    renderWorkloadChart(distribution) {
        const ctx = this.element.querySelector("#workloadChart");
        const datasets = Object.entries(distribution.statusCounts).map(([status, counts]) => ({
            label: this.statusLabel(status),
            data: counts,
            backgroundColor: STATUS_COLORS[status] || "#999",
        }));

        this.charts.workload = new Chart(ctx, {
            type: "bar",
            data: { labels: distribution.assignees, datasets },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: { stacked: true },
                    y: { stacked: true, beginAtZero: true, ticks: { stepSize: 1 } },
                },
                plugins: { legend: { position: "top" } },
            },
        });
    }

    renderBurndownChart(burndown) {
        const ctx = this.element.querySelector("#burndownChart");
        const remaining = this.msg["analytics.label.remaining"] || "Remaining Tasks";

        this.charts.burndown = new Chart(ctx, {
            type: "line",
            data: {
                labels: burndown.map((p) => p.date),
                datasets: [{
                    label: remaining,
                    data: burndown.map((p) => p.remaining),
                    borderColor: "#0d6efd",
                    backgroundColor: "rgba(13, 110, 253, 0.1)",
                    fill: true,
                    tension: 0.3,
                }],
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } },
                plugins: { legend: { display: false } },
            },
        });
    }

    renderVelocityChart(velocity) {
        const ctx = this.element.querySelector("#velocityChart");
        const completedLabel = this.msg["analytics.label.completed"] || "Completed";
        const effortLabel = this.msg["analytics.label.effort"] || "Effort";
        const effortValues = velocity.map((p) => p.effortCompleted);
        const hasEffort = effortValues.some((v) => v !== null && v > 0);

        const datasets = [{
            label: completedLabel,
            data: velocity.map((p) => p.completed),
            borderColor: "#198754",
            backgroundColor: "rgba(25, 135, 84, 0.1)",
            fill: true,
            tension: 0.3,
            yAxisID: "y",
        }];

        if (hasEffort) {
            datasets.push({
                label: effortLabel,
                data: effortValues,
                borderColor: "#6f42c1",
                backgroundColor: "rgba(111, 66, 193, 0.1)",
                fill: true,
                tension: 0.3,
                yAxisID: "y1",
            });
        }

        const scales = { y: { beginAtZero: true, ticks: { stepSize: 1 }, position: "left" } };
        if (hasEffort) {
            scales.y1 = {
                beginAtZero: true,
                position: "right",
                grid: { drawOnChartArea: false },
                ticks: { stepSize: 1 },
            };
        }

        this.charts.velocity = new Chart(ctx, {
            type: "line",
            data: { labels: velocity.map((p) => p.weekStart), datasets },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales,
                plugins: { legend: { display: hasEffort } },
            },
        });
    }

    renderOverdueChart(overdueAnalysis) {
        const ctx = this.element.querySelector("#overdueChart");
        const tasks = this.msg["analytics.label.tasks"] || "Tasks";

        this.charts.overdue = new Chart(ctx, {
            type: "bar",
            data: {
                labels: overdueAnalysis.assignees,
                datasets: [{ label: tasks, data: overdueAnalysis.counts, backgroundColor: "#dc3545" }],
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } },
                plugins: { legend: { display: false } },
            },
        });
    }

    renderEffortChart(effortDistribution) {
        const ctx = this.element.querySelector("#effortChart");
        const effortLabel = this.msg["analytics.label.effort"] || "Effort";

        this.charts.effort = new Chart(ctx, {
            type: "bar",
            data: {
                labels: effortDistribution.assignees,
                datasets: [{ label: effortLabel, data: effortDistribution.efforts, backgroundColor: "#6f42c1" }],
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                indexAxis: "y",
                scales: {
                    x: { beginAtZero: true, ticks: { stepSize: 1 } },
                    y: { ticks: { autoSkip: false } },
                },
                plugins: { legend: { display: false } },
            },
        });
    }
}
