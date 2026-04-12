# CSS Guide — UI Design Reference

A practical guide for our Bootstrap 5 UI layer. Documents the design decisions, patterns, and lessons learned from polishing the app. This is the single source of truth for how CSS and visual design should be approached in this project.

Applies to: `theme.css`, `base.css`, `tasks.css`, `mentions.css`, component CSS.

---

## 1. Architecture

### File Organization

| File | Scope | Loaded |
|------|-------|--------|
| `base.css` | Every page — global styles, layout, shared components | Always |
| `theme.css` | Theme palettes + shared design tokens + Bootstrap overrides | Always (after Bootstrap) |
| `tasks.css` | Task pages only — filters, kanban, calendar, bulk actions | Via `head(title, cssFile)` fragment |
| `mentions.css` | @mention autocomplete styling | Always |
| `components/searchable-select-bootstrap5.css` | Web Component theme | Always |

### Override Strategy

All customization lives under `[data-theme]` selectors in `theme.css`. Without `data-theme` on `<html>`, the app renders with stock Bootstrap. This means:

- **Never modify Bootstrap source** — override via CSS custom properties
- **`!important` only when Bootstrap uses it** (utility classes like `bg-*`)
- **Use `--bs-*` variable overrides** for buttons, badges, alerts — they're Bootstrap's official extension points

### Token Flow

```
Theme Palette (--theme-*)
    ↓ mapped in Section 2
Bootstrap Variables (--bs-*)
    ↓ consumed by
Bootstrap Components + Our Overrides
```

---

## 2. Color System

### Design Principles

- **WCAG AA minimum** — 4.5:1 for normal text, 3:1 for large text. Every color+text combo must pass.
- **Hue separation** — Status colors must be >40° apart on the hue wheel for distinguishability.
- **Tinted neutrals** — Grays have a subtle brand hue tint (chroma 0.005–0.01 in OKLCH) for cohesion.
- **60-30-10 rule** — 60% neutral surfaces, 30% secondary/text, 10% accent color. Primary works *because* it's rare.

### Workshop Palette

| Role | Color | Hex | Text | WCAG | Hue |
|------|-------|-----|------|------|-----|
| Primary (Cerulean) | Blue | `#2b7de9` | white | 4.56:1 | 220° |
| Info / In Review (Royal Violet) | Purple | `#7c3aed` | white | 7.27:1 | 265° |
| Success / Completed (Jade) | Green | `#16a34a` | white | 4.52:1 | 142° |
| Warning / In Progress (Marigold) | Gold | `#e6a740` | dark | 9.2:1 | 38° |
| Danger / Overdue (Vermilion) | Red | `#dc2626` | white | 5.63:1 | 0° |
| Secondary / Open (Slate) | Gray | `#64748b` | white | 4.54:1 | 215° |
| Backlog (Silver) | Light gray | `#94a3b8` | dark | 7.1:1 | 215° |

### Notebook Palette

| Role | Color | Hex | Text | WCAG |
|------|-------|-----|------|------|
| Primary (Terracotta) | Warm red-brown | `#b35537` | white | 4.92:1 |
| Info (Dusty Plum) | Muted purple | `#8b5e83` | white | 5.22:1 |
| Success (Olive) | Earthy green | `#358254` | white | 4.70:1 |
| Warning (Amber) | Warm gold | `#c48a2c` | dark | 8.6:1 |
| Danger (Brick) | Warm red | `#c4362a` | white | 5.84:1 |
| Secondary (Warm Stone) | Muted neutral | `#78716c` | white | 4.80:1 |

### Titanium Palette

| Role | Color | Hex | Text | WCAG |
|------|-------|-----|------|------|
| Primary (Steel Blue) | Blue | `#0f62fe` | white | 5.00:1 |
| Info (Purple) | Vivid purple | `#8a3ffc` | white | 5.00:1 |
| Success (Industrial Green) | Green | `#198038` | white | 5.02:1 |
| Warning (Signal Yellow) | Bright yellow | `#f1c21b` | dark | 10.75:1 |
| Danger (Engineering Red) | Red | `#da1e28` | white | 5.00:1 |
| Secondary (Graphite) | Neutral gray | `#525252` | white | 7.81:1 |

### Adding a Theme

1. `THEME_*` constant in `Settings.java`
2. `[data-theme="name"]` palette block in `theme.css` (section 1)
3. `ThemeOption` in `SettingsController.THEMES` with swatch colors
4. `admin.settings.theme.<name>.{name,description}` in `messages.properties`

### Lessons Learned

- **Info ≠ Primary** — Bootstrap's default Info (`#0dcaf0`) is only 18° from a blue primary. We moved Info to purple/teal to create clear separation.
- **Don't trust Bootstrap's default contrast** — Stock `bg-info + text-white` fails WCAG at 1.96:1. Always verify.
- **Backlog needs dark text** — Light gray backgrounds (`#94a3b8`) with white text fail at 2.02:1. Use `text-dark`.
- **`bg-*` utilities pick up `--bs-*-rgb`** — Setting `--bs-info-rgb` in the theme automatically flows to `bg-info` class. No need to override each utility individually.
- **Theme token overrides must come after shared tokens** — Same-specificity selectors (`[data-theme="notebook"]` vs `[data-theme]`) resolve by source order. If a theme palette (section 1 in `theme.css`) defines `--radius-*` or `--shadow-*` overrides before the shared `[data-theme]` design tokens section (section 3), the shared defaults silently overwrite them. Put token overrides in the theme's refinement section (sections 5/6), not in the palette section.

---

## 3. Design Tokens

Defined in `theme.css` under `[data-theme]`:

### Motion

```css
--ease-out:    cubic-bezier(0.25, 1, 0.5, 1);     /* Enter — fast start, gentle land */
--ease-in:     cubic-bezier(0.5, 0, 0.75, 0);      /* Exit — gentle start, fast exit */
--ease-in-out: cubic-bezier(0.45, 0, 0.55, 1);     /* State toggles — symmetric */
--ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);  /* Playful overshoot — use sparingly */

--duration-fast:   120ms;   /* Hover, toggle, checkbox */
--duration-normal: 250ms;   /* Modal, dropdown, card lift */
--duration-slow:   400ms;   /* Toast, page reveal */
```

### Shadows (Two-Shadow Technique)

Each shadow combines a tight contact shadow + a soft ambient shadow:

```css
--shadow-xs through --shadow-xl
```

| Level | Token | Use |
|-------|-------|-----|
| 0 | none | Flat surfaces |
| 1 | `--shadow-sm` | Cards at rest |
| 2 | `--shadow-md` | Cards on hover, stat cards |
| 3 | `--shadow-lg` | Dropdowns, popovers |
| 4 | `--shadow-xl` | Modals, toasts, bulk action bar |

### Border Radius

```css
--radius-sm:   0.375rem;  /* Badges, inputs, dropdown items */
--radius-md:   0.625rem;  /* Buttons, dropdowns, small cards */
--radius-lg:   0.875rem;  /* Cards, modals */
--radius-xl:   1.25rem;   /* Hero sections, prominent panels */
--radius-full: 9999px;    /* Pills, avatars */
```

---

## 4. Form Controls

### Three Visual States

| State | Background | Border | Text | Signal |
|-------|-----------|--------|------|--------|
| **Editable** | White `#fff` | Solid | Normal | "Type here" |
| **Readonly** | White `#fff` | Dashed | Muted gray | "See but can't change" |
| **Disabled** | Sunken (`--theme-surface-sunken`) | Transparent | Muted gray | Inert — blends into background |

Applies consistently to `form-control`, `form-select`, and `searchable-select`.

**Note:** Native `<select>` (`form-select`) only supports **editable** and **disabled** — HTML has no `readonly` for selects. The `<searchable-select>` Web Component supports all three states via `readonly` and `disabled` attributes, making it visually consistent with `<input>` and `<textarea>` in read-only forms.

### `form-select` Padding

Never override right padding on `form-select` — Bootstrap reserves ~2.25rem for the dropdown arrow. Only set `padding-top`, `padding-bottom`, and `padding-left`.

### `StringTrimmerEditor` Gotcha

`GlobalBindingConfig` trims blank form strings to `null`. `@RequestParam(defaultValue = "")` only applies when the param is *absent*, not blank. Always null-coalesce string params before calling `.equals()`.

---

## 5. Component Patterns

### Border Radius Tokens

Use `var(--radius-sm)`, `var(--radius-md)`, etc. instead of hardcoded `4px` or `0.375rem` in component CSS. Themes set these tokens — hardcoded values bypass the theme system entirely. Bootstrap components need a "nuclear" `border-radius: 0 !important` selector list because Bootstrap itself hardcodes radius on many components.

### Cards

- **Borderless** (`border: none`) — cleaner look, status shown via colored headers
- **`card-clip`** class — adds `overflow: hidden` to clip header bg to rounded corners. Only use on cards with colored headers (not form cards — would clip dropdowns).
- **`card-lift`** class — `translateY(-5px)` on hover with shadow upgrade

### Dropdowns

- **No animation** — CSS/JS animations on `.dropdown-menu` cause Popper.js repositioning jitter. Let Bootstrap handle show/hide.
- **`scrollbar-gutter: stable`** — prevents content shift when scrollbar appears
- **Scrollable dropdowns** (e.g., tags filter) — put `overflow-y: auto` on an **inner `<div>`**, not the `.dropdown-menu` itself. Otherwise scroll events reach Popper's listeners and cause wobble. Add `overscroll-behavior: contain` on the inner div.
- **Filter checkmarks** — use `<i class="bi bi-check-lg filter-check">` with `visibility: hidden/visible` toggling instead of Bootstrap's `.active` class (which adds a blue background).

### Alerts

- Clean rounded boxes with Bootstrap's default subtle border — no left-border-only accent (clashes with rounded corners).

### Checklist Items

- Delete button sits **outside** the `input-group` as a standalone borderless icon button. Inside the input-group, it inherits squared corners on one side.

### Modals

- Borderless (`border: none`), `--shadow-xl` for floating effect
- Entrance: `translateY(20px) scale(0.96)` → `translateY(0) scale(1)`

### Edit Mode Interaction

- When disabling interactive elements in edit mode, use `opacity: 0.3; pointer-events: none` rather than `display: none` or `visibility: hidden` — avoids layout shift.
- Remove `hx-get` attributes (not just `href`) to prevent HTMX from firing during edit mode.
- Use `pointer-events: none` on cell children (`td.inline-edit-active > *`) to block clicks on nested elements.

### JS-Only Templates

When JS needs to create DOM elements (modals, cards, etc.), use `<template>` elements in Thymeleaf files — never build DOM in JS strings.

- **Global templates** (used on any page): put in the `chrome` fragment in `layouts/base.html`
- **Feature-specific templates**: put in the page template that loads the JS controller
- `<template>` is NOT a Thymeleaf fragment — don't put it in `fragments/`. Nobody `th:replace`s it.
- Thymeleaf processes `#{...}` inside `<template>` for i18n

---

## 6. Typography

### Font: DM Sans

Self-hosted variable font (WOFF2, 54KB total for Latin + Latin-ext). Loaded via `@font-face` in theme.css section 0.

- **Weights**: 400 (body), 500 (badges), 600 (headings)
- **Heading style**: `font-weight: 600; letter-spacing: -0.025em` — semibold feels modern, tight tracking adds density
- **Table headers**: `text-transform: uppercase; font-size: 0.75rem; letter-spacing: 0.05em` — clear section labels

### Why Not Google Fonts CDN

External dependency, privacy concerns, GDPR. Self-hosting eliminates the third-party request and gives full control.

---

## 7. Accessibility

### Reduced Motion

Global rule in `base.css`:

```css
@media (prefers-reduced-motion: reduce) {
    *, *::before, *::after {
        animation-duration: 0.01ms !important;
        transition-duration: 0.01ms !important;
        animation-iteration-count: 1 !important;
    }
}
```

### Focus Rings

- `outline: 2px solid var(--bs-primary)` with `outline-offset: 2px` via `:focus-visible`
- Buttons and form controls use box-shadow focus rings instead (Bootstrap convention)
- 3:1 minimum contrast against adjacent colors

### Color + Icon

Never use color alone to convey meaning. All status indicators pair color with an icon (e.g., `bi-check-circle-fill` for Completed, `bi-exclamation-circle-fill` for Overdue).

---

## 8. Anti-Patterns (Lessons Learned)

| Don't | Why | Do Instead |
|-------|-----|------------|
| Animate dropdowns with CSS | Popper.js recalculates position during animation, causing wobble | Let Bootstrap handle show/hide |
| Use View Transitions with HTMX fragments | Causes full-page cross-fade flash on every fragment swap | View Transitions are for full-page navigations only |
| Put `overflow-y: auto` on `.dropdown-menu` | Scroll events bubble to Popper, causing repositioning | Scroll an inner `<div>` instead |
| Override right padding on `form-select` | Clips the dropdown arrow into the text | Only set top/bottom/left padding |
| Use `.active` class on filter dropdown items | Bootstrap styles it with blue background | Use checkmark icon with visibility toggle |
| Put delete buttons inside `input-group` | Inherits squared corners, border merging | Place outside as standalone icon button |
| Set `border-width: 0; border-left-width: 4px` on alerts | Left border clips into rounded corners | Use default border all around |
| Use `text-white` on light backgrounds | WCAG failure (e.g., backlog `#94a3b8` + white = 2.02:1) | Always verify contrast ratios |
| Use Bootstrap's default dropdown hover on themed apps | `#f8f9fa` is nearly invisible on white backgrounds | Override with `rgba(var(--theme-body-color-rgb), 0.06)` for themed, `rgba(0, 0, 0, 0.06)` for unthemed |
| Put `scrollbar-gutter: stable` on `.dropdown-menu` | Reserves scrollbar space even when content doesn't scroll | Only use on actually-scrolling containers |
| Override `padding`, `border-width`, or `height` on form controls in a theme | Bootstrap's sizing system relies on these being consistent across `.form-control`, `.form-select`, `.input-group-text`, and `.btn` | Only change visual properties: `background-color`, `border-color`, `border-style`, `box-shadow`, `color` |
| Use raw `padding` to align buttons with inputs in the same row | Different elements have different computed padding | Use `--bs-btn-padding-y` and `--bs-btn-padding-x` (Bootstrap's own CSS variables) |
| Hardcode `border-radius` values (`4px`, `0.375rem`) in component CSS | Bypasses the theme token system | Use `var(--radius-sm)`, `var(--radius-md)`, etc. |

### Theme-Safe Component Rules

- **Never override sizing properties** (`padding`, `border-width`, `height`) on form controls in a theme. Bootstrap's sizing system relies on these being consistent across `.form-control`, `.form-select`, `.input-group-text`, and `.btn`. Only change visual properties: `background-color`, `border-color`, `border-style`, `box-shadow`, `color`.
- **Button/input alignment** — To align buttons with inputs in the same row, use `--bs-btn-padding-y` and `--bs-btn-padding-x` (Bootstrap's own CSS variables), not raw `padding`.
- **`input-group-text` vertical padding** must match `form-control` padding. The shared `[data-theme]` section handles this.
- **Transparent border overlap** — Bootstrap's `input-group > :not(:first-child)` uses `margin-left: -1px` to overlap borders. Themes with transparent borders may need `margin-left: 0` to prevent visual shifts.

---

## 9. Navbar

- **Frosted glass** — `backdrop-filter: blur(14px) saturate(180%)` with semi-transparent background
- **Sticky** — `position: sticky; top: 0; z-index: 1030`
- **Nav links**: `nav-link-bright` class for higher-contrast white links with underline on active

---

## 10. Z-Index Layers

| z-index | Element |
|---------|---------|
| 1030 | Navbar, Recently Viewed wrapper |
| 1050 | Modal backdrop |
| 1055 | Modal |
| 1065 | Confirm modal backdrop |
| 1070 | Confirm modal |
| 1090 | Toast container |
