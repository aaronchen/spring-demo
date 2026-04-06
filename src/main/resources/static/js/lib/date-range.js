// Date range constraint — when a .date-range-start input changes,
// sets the min attribute on the target end-date input.
// Works with HTMX-swapped content (delegated listener on document).
// Imported as a side-effect in application.js.

document.addEventListener("change", function (evt) {
    if (!evt.target.classList.contains("date-range-start")) return;
    const endInput = document.getElementById(evt.target.dataset.dateMinFor);
    if (endInput) endInput.min = evt.target.value;
});
