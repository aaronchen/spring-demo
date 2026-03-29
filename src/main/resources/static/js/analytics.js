(function () {
    'use strict';

    const apiUrl = document.querySelector('meta[name="_analyticsApi"]')?.content;
    if (!apiUrl) return;

    const msg = window.APP_CONFIG?.messages || {};

    const STATUS_COLORS = {
        BACKLOG: '#adb5bd',
        OPEN: '#0d6efd',
        IN_PROGRESS: '#ffc107',
        IN_REVIEW: '#0dcaf0',
        COMPLETED: '#198754',
        CANCELLED: '#dc3545'
    };

    const PRIORITY_COLORS = {
        LOW: '#198754',
        MEDIUM: '#ffc107',
        HIGH: '#dc3545'
    };

    // Track chart instances for cleanup on re-render
    const charts = {};

    function statusLabel(key) {
        return msg[`task.status.${statusKeyToMessageSuffix(key)}`] || key;
    }

    function statusKeyToMessageSuffix(key) {
        const map = {
            BACKLOG: 'backlog',
            OPEN: 'open',
            IN_PROGRESS: 'inProgress',
            IN_REVIEW: 'inReview',
            COMPLETED: 'completed',
            CANCELLED: 'cancelled'
        };
        return map[key] || key.toLowerCase();
    }

    function priorityLabel(key) {
        const map = { LOW: 'low', MEDIUM: 'medium', HIGH: 'high' };
        return msg[`task.priority.${map[key]}`] || key;
    }

    // ── Sprint Filter ────────────────────────────────────────────────────

    let currentSprintId = '';

    window.setAnalyticsSprintFilter = function (sprintId) {
        currentSprintId = sprintId;
        const container = document.getElementById('sprint-filter');
        if (container) {
            container.querySelectorAll('.btn').forEach(btn => {
                btn.classList.remove('active');
            });
            if (sprintId === '') {
                container.querySelector('.btn')?.classList.add('active');
            } else {
                const match = container.querySelector(`[data-sprint-id="${sprintId}"]`);
                if (match) match.classList.add('active');
            }
        }
        fetchAndRender();
    };

    // ── Project Filter ───────────────────────────────────────────────────

    function getSelectedProjectIds() {
        const checkboxes = document.querySelectorAll('.project-filter-checkbox');
        if (checkboxes.length === 0) return null; // No filter UI (project-scoped page)
        const selected = [];
        checkboxes.forEach(cb => {
            if (cb.checked) selected.push(cb.value);
        });
        return selected;
    }

    function buildFetchUrl() {
        const projectIds = getSelectedProjectIds();
        const params = new URLSearchParams();

        if (projectIds !== null) {
            if (projectIds.length === 0) return null; // Nothing selected
            projectIds.forEach(id => params.append('projectIds', id));
        }

        if (currentSprintId) {
            params.append('sprintId', currentSprintId);
        }

        const qs = params.toString();
        return qs ? `${apiUrl}?${qs}` : apiUrl;
    }

    function initFilterListeners() {
        const checkboxes = document.querySelectorAll('.project-filter-checkbox');
        if (checkboxes.length === 0) return;

        const selectAll = document.getElementById('filter-select-all');

        checkboxes.forEach(cb => {
            cb.addEventListener('change', () => {
                // Sync "Select All" checkbox with individual state
                if (selectAll) {
                    selectAll.checked = [...checkboxes].every(c => c.checked);
                }
                fetchAndRender();
            });
        });

        if (selectAll) {
            selectAll.addEventListener('change', () => {
                checkboxes.forEach(cb => { cb.checked = selectAll.checked; });
                fetchAndRender();
            });
        }
    }

    // ── Fetch & Render ───────────────────────────────────────────────────

    function destroyCharts() {
        Object.values(charts).forEach(chart => chart.destroy());
        Object.keys(charts).forEach(key => delete charts[key]);
    }

    function fetchAndRender() {
        const url = buildFetchUrl();

        destroyCharts();

        if (url === null) {
            // No projects selected — clear canvases
            return;
        }

        fetch(url, {
            headers: { 'Accept': 'application/json' }
        })
            .then(response => response.json())
            .then(data => {
                renderStatusChart(data.statusBreakdown);
                renderPriorityChart(data.priorityBreakdown);
                renderWorkloadChart(data.workloadDistribution);
                renderBurndownChart(data.burndown);
                renderVelocityChart(data.velocity);
                renderOverdueChart(data.overdueAnalysis);
                renderEffortChart(data.effortDistribution);
            });
    }

    // ── Chart Renderers ──────────────────────────────────────────────────

    function renderStatusChart(breakdown) {
        const ctx = document.getElementById('statusChart');
        const labels = Object.keys(breakdown.counts).map(statusLabel);
        const values = Object.values(breakdown.counts);
        const colors = Object.keys(breakdown.counts).map(k => STATUS_COLORS[k] || '#999');

        charts.status = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: values,
                    backgroundColor: colors,
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'right' }
                }
            }
        });
    }

    function renderPriorityChart(breakdown) {
        const ctx = document.getElementById('priorityChart');
        const labels = Object.keys(breakdown.counts).map(priorityLabel);
        const values = Object.values(breakdown.counts);
        const colors = Object.keys(breakdown.counts).map(k => PRIORITY_COLORS[k] || '#999');

        charts.priority = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: values,
                    backgroundColor: colors,
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'right' }
                }
            }
        });
    }

    function renderWorkloadChart(distribution) {
        const ctx = document.getElementById('workloadChart');
        const datasets = Object.entries(distribution.statusCounts).map(([status, counts]) => ({
            label: statusLabel(status),
            data: counts,
            backgroundColor: STATUS_COLORS[status] || '#999'
        }));

        charts.workload = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: distribution.assignees,
                datasets: datasets
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: { stacked: true },
                    y: { stacked: true, beginAtZero: true, ticks: { stepSize: 1 } }
                },
                plugins: {
                    legend: { position: 'top' }
                }
            }
        });
    }

    function renderBurndownChart(burndown) {
        const ctx = document.getElementById('burndownChart');
        const labels = burndown.map(p => p.date);
        const values = burndown.map(p => p.remaining);
        const remaining = msg['analytics.label.remaining'] || 'Remaining Tasks';

        charts.burndown = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: remaining,
                    data: values,
                    borderColor: '#0d6efd',
                    backgroundColor: 'rgba(13, 110, 253, 0.1)',
                    fill: true,
                    tension: 0.3
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: { beginAtZero: true, ticks: { stepSize: 1 } }
                },
                plugins: {
                    legend: { display: false }
                }
            }
        });
    }

    function renderVelocityChart(velocity) {
        const ctx = document.getElementById('velocityChart');
        const labels = velocity.map(p => p.weekStart);
        const completedValues = velocity.map(p => p.completed);
        const effortValues = velocity.map(p => p.effortCompleted);
        const completedLabel = msg['analytics.label.completed'] || 'Completed';
        const effortLabel = msg['analytics.label.effort'] || 'Effort';
        const hasEffort = effortValues.some(v => v !== null && v > 0);

        const datasets = [{
            label: completedLabel,
            data: completedValues,
            borderColor: '#198754',
            backgroundColor: 'rgba(25, 135, 84, 0.1)',
            fill: true,
            tension: 0.3,
            yAxisID: 'y'
        }];

        if (hasEffort) {
            datasets.push({
                label: effortLabel,
                data: effortValues,
                borderColor: '#6f42c1',
                backgroundColor: 'rgba(111, 66, 193, 0.1)',
                fill: true,
                tension: 0.3,
                yAxisID: 'y1'
            });
        }

        const scales = {
            y: { beginAtZero: true, ticks: { stepSize: 1 }, position: 'left' }
        };
        if (hasEffort) {
            scales.y1 = {
                beginAtZero: true,
                position: 'right',
                grid: { drawOnChartArea: false },
                ticks: { stepSize: 1 }
            };
        }

        charts.velocity = new Chart(ctx, {
            type: 'line',
            data: { labels: labels, datasets: datasets },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: scales,
                plugins: {
                    legend: { display: hasEffort }
                }
            }
        });
    }

    function renderOverdueChart(overdueAnalysis) {
        const ctx = document.getElementById('overdueChart');
        const tasks = msg['analytics.label.tasks'] || 'Tasks';

        charts.overdue = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: overdueAnalysis.assignees,
                datasets: [{
                    label: tasks,
                    data: overdueAnalysis.counts,
                    backgroundColor: '#dc3545'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: { beginAtZero: true, ticks: { stepSize: 1 } }
                },
                plugins: {
                    legend: { display: false }
                }
            }
        });
    }

    function renderEffortChart(effortDistribution) {
        const ctx = document.getElementById('effortChart');
        const effortLabel = msg['analytics.label.effort'] || 'Effort';

        charts.effort = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: effortDistribution.assignees,
                datasets: [{
                    label: effortLabel,
                    data: effortDistribution.efforts,
                    backgroundColor: '#6f42c1'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                indexAxis: 'y',
                scales: {
                    x: { beginAtZero: true, ticks: { stepSize: 1 } },
                    y: { ticks: { autoSkip: false } }
                },
                plugins: {
                    legend: { display: false }
                }
            }
        });
    }

    // ── Init ─────────────────────────────────────────────────────────────

    initFilterListeners();
    fetchAndRender();
})();
