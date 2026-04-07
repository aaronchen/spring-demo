// Global mention encoding — intercepts HTMX requests and form submits to encode
// @Name tokens to @[Name](userId:N) in all [data-mention] fields.
// Imported as a side-effect in application.js.

import { encodeMentions } from "lib/mentions";

document.addEventListener("htmx:configRequest", function (evt) {
    document.querySelectorAll("[data-mention][data-tribute]").forEach(function (el) {
        const paramName = el.name;
        if (paramName && evt.detail.parameters[paramName] != null) {
            evt.detail.parameters[paramName] = encodeMentions(evt.detail.parameters[paramName], el);
        }
    });
});

document.addEventListener(
    "submit",
    function (evt) {
        evt.target.querySelectorAll("[data-mention][data-tribute]").forEach(function (el) {
            el.value = encodeMentions(el.value, el);
        });
    },
    true,
);
