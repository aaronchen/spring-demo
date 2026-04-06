import { Controller } from "@hotwired/stimulus";

export default class extends Controller {
    static targets = ["view", "searchInput", "searchClear", "dateFrom", "dateTo", "categoryLabel"];

    connect() {
        this.currentPage = 0;
        this.pageSize = 50;
        this.currentCategory = "";
        this.searchDebounce = null;

        this.initFromUrl();

        // Browser back/forward
        this.popstateHandler = () => {
            this.initFromUrl();
            this.doSearch(false);
        };
        window.addEventListener("popstate", this.popstateHandler);
    }

    disconnect() {
        window.removeEventListener("popstate", this.popstateHandler);
    }

    // ── Actions ──────────────────────────────────────────────────────────

    search() {
        clearTimeout(this.searchDebounce);
        this.updateClearBtn();
        this.searchDebounce = setTimeout(() => this.doSearch(true), 300);
    }

    clearSearch() {
        this.searchInputTarget.value = "";
        this.updateClearBtn();
        this.searchInputTarget.focus();
        this.doSearch(true);
    }

    filterCategory(event) {
        event.preventDefault();
        this.currentCategory = event.currentTarget.dataset.category || "";
        this.updateCategoryDropdown();
        this.doSearch(true);
    }

    filterDateFrom() {
        this.doSearch(true);
    }

    filterDateTo() {
        this.doSearch(true);
    }

    navigate(event) {
        const page = event.detail.page;
        if (page < 0) return;
        this.currentPage = page;
        const url = this.buildUrl(page);
        htmx.ajax("GET", url, { target: `#${this.viewTarget.id}`, swap: "innerHTML" });
        window.history.pushState({}, "", url);
    }

    resize(event) {
        this.pageSize = parseInt(event.detail.size);
        this.currentPage = 0;
        this.doSearch(false);
    }

    // ── Internal ─────────────────────────────────────────────────────────

    buildUrl(page) {
        const params = new URLSearchParams();
        const search = this.searchInputTarget.value;
        const from = this.dateFromTarget.value;
        const to = this.dateToTarget.value;
        if (search) params.set("search", search);
        if (this.currentCategory) params.set("category", this.currentCategory);
        if (from) params.set("from", from);
        if (to) params.set("to", to);
        params.set("size", this.pageSize);
        if (page > 0) params.set("page", page);
        return `${APP_CONFIG.routes.audit}?${params.toString()}`;
    }

    doSearch(resetPage) {
        if (resetPage) this.currentPage = 0;
        const url = this.buildUrl(this.currentPage);
        htmx.ajax("GET", url, { target: `#${this.viewTarget.id}`, swap: "innerHTML" });
        window.history.replaceState({}, "", url);
    }

    updateClearBtn() {
        this.searchClearTarget.classList.toggle("d-none", this.searchInputTarget.value === "");
    }

    updateCategoryDropdown() {
        this.element.querySelectorAll(".audit-category-item").forEach((item) => {
            const isActive = (item.dataset.category || "") === this.currentCategory;
            item.classList.toggle("active", isActive);
            if (isActive) this.categoryLabelTarget.textContent = item.textContent;
        });
    }

    initFromUrl() {
        const params = new URLSearchParams(window.location.search);

        this.currentPage = parseInt(params.get("page") || "0");
        if (params.has("size")) this.pageSize = parseInt(params.get("size"));

        this.searchInputTarget.value = params.get("search") || "";
        this.dateFromTarget.value = params.get("from") || "";
        this.dateToTarget.value = params.get("to") || "";

        this.currentCategory = params.get("category") || "";
        this.updateCategoryDropdown();
        this.updateClearBtn();
    }
}
