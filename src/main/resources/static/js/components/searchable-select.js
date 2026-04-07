// <searchable-select> — reusable searchable dropdown Web Component.
//
// Three modes:
//
// 1. Local (static options, client-side filter):
//    <searchable-select name="status" placeholder="Pick one...">
//        <option value="">-- None --</option>
//        <option value="OPEN" selected>Open</option>
//        <option value="CLOSED">Closed</option>
//    </searchable-select>
//
// 2. Remote prefetch (fetch once, client-side filter from cache):
//    <searchable-select name="userId" placeholder="Search users..."
//                       src="/api/users" value-field="id" text-field="name" prefetch>
//        <option value="">Unassigned</option>
//        <option value="1" selected>Alice Johnson</option>
//    </searchable-select>
//
// 3. Remote server search (debounced fetch per keystroke):
//    <searchable-select name="taskId" placeholder="Search tasks..."
//                       src="/api/tasks/search" value-field="id" text-field="title"
//                       query-param="q" debounce="300">
//        <option value="">---</option>
//    </searchable-select>
//
// Attributes:
//   name          — form field name (carried by hidden <input>)
//   placeholder   — input placeholder text
//   disabled      — disables the control (grayed out, no interaction, value NOT submitted)
//   readonly      — read-only display (dashed border, no interaction, value IS submitted)
//   src           — remote endpoint URL (enables remote mode)
//   prefetch      — boolean; when present with src/fetchFn, fetch once and filter client-side
//   value-field   — JSON field for option values (default: "id")
//   text-field    — JSON field for option display text (default: "name")
//   query-param   — URL parameter name for search query (default: "q")
//   debounce      — milliseconds to debounce remote searches (default: "300")
//
// Public properties:
//   value         — get/set selected value (no change event on set)
//   fetchFn       — async (query, signal) => Array<Object>; overrides src-based fetching
//
// Public methods:
//   reset()           — clear selection, no change event
//   clear()           — clear selection, fires change event
//   setValue(v, text)  — set selection programmatically, no change event
//   getValue()        — returns { value, text }
//   setSrc(url)       — change remote URL, clears cache
//   setOptions(opts)  — replace local dataset with [{value, text}, ...]
//   enable()          — remove disabled attribute
//   disable()         — set disabled attribute
//
// Events:
//   change — { detail: { value, text, data }, bubbles: true } on user selection
//            data: original item object — API response item (remote), input object (setOptions),
//            dataset object (static <option data-*>), or undefined (static <option> without data-*)
//
// CSS: load searchable-select-bootstrap5.css (or your own theme).

class SearchableSelect extends HTMLElement {

    connectedCallback() {
        this._src = this.getAttribute('src');
        this._isRemote = !!this._src;
        this._fetchFn = null;
        this._valueField = this.getAttribute('value-field') || 'id';
        this._textField = this.getAttribute('text-field') || 'name';
        this._queryParam = this.getAttribute('query-param') || 'q';
        this._debounceMs = parseInt(this.getAttribute('debounce') || '300', 10);

        // Parse <option> children before replacing innerHTML.
        // In remote mode, only the empty option and any selected option matter.
        this._staticOptions = [];
        this._emptyOption = null;
        this.querySelectorAll('option').forEach(opt => {
            const data = Object.keys(opt.dataset).length > 0 ? { ...opt.dataset } : undefined;
            const entry = {
                value: opt.value,
                text: opt.textContent.trim(),
                data,
                selected: opt.selected || opt.hasAttribute('selected')
            };
            this._staticOptions.push(entry);
            if (opt.value === '') this._emptyOption = entry;
        });

        // In local mode, all static options are the working set
        this._options = this._isRemote ? [] : this._staticOptions.slice();

        this._selectedValue = '';
        this._selectedText = '';
        const selected = this._staticOptions.find(o => o.selected);
        if (selected) {
            this._selectedValue = selected.value;
            this._selectedText = selected.text;
        }

        // Remote state
        this._cache = null;        // cached full-list response (prefetch mode only)
        this._debounceTimer = null;
        this._abortController = null;

        // Keyboard navigation
        this._highlightIndex = -1;

        this._buildDOM();
        this._attachEvents();
    }

    disconnectedCallback() {
        if (this._outsideClickHandler) {
            document.removeEventListener('click', this._outsideClickHandler);
        }
        if (this._debounceTimer) clearTimeout(this._debounceTimer);
        if (this._abortController) this._abortController.abort();
    }

    static get observedAttributes() { return ['disabled', 'readonly', 'src', 'prefetch']; }

    attributeChangedCallback(name) {
        // Guard: callbacks fire during parsing, before connectedCallback builds the DOM
        if (!this._input) return;

        if (name === 'disabled') {
            const isDisabled = this.hasAttribute('disabled');
            this._input.disabled = isDisabled;
            if (isDisabled) this._close();
            this._updateClear();
        }
        if (name === 'readonly') {
            const isReadonly = this.hasAttribute('readonly');
            this._input.readOnly = isReadonly;
            if (isReadonly) this._close();
            this._updateClear();
        }
        if (name === 'src') {
            this._src = this.getAttribute('src');
            this._isRemote = !!this._src;
            this._cache = null;
        }
        // 'prefetch' — no runtime action needed; read via hasAttribute at fetch time
    }

    // --- Public properties ---

    get value() { return this._selectedValue; }

    set value(val) {
        const strVal = String(val ?? '');
        // Look up display text from current options
        const all = [...this._staticOptions, ...this._options];
        const match = all.find(o => String(o.value) === strVal);
        const text = match ? match.text : '';
        this._selectedValue = strVal;
        this._selectedText = text;
        if (this._hidden) this._hidden.value = strVal;
        if (this._input) this._input.value = text;
        this._updateClear();
    }

    get fetchFn() { return this._fetchFn; }

    set fetchFn(fn) {
        this._fetchFn = fn;
        this._isRemote = true;
        this._cache = null;
    }

    // --- Public methods ---

    /** Clear selection without firing change event. */
    reset() {
        const empty = this._emptyOption;
        this._selectedValue = empty ? empty.value : '';
        this._selectedText = empty ? empty.text : '';
        if (this._hidden) this._hidden.value = this._selectedValue;
        if (this._input) this._input.value = this._selectedText;
        this._updateClear();
    }

    /** Clear selection and fire change event. */
    clear() {
        const empty = this._emptyOption;
        this._select(empty ? empty.value : '', empty ? empty.text : '');
    }

    /** Set selection programmatically without firing change event. */
    setValue(value, text) {
        const strVal = String(value ?? '');
        if (text === undefined || text === null) {
            // Look up display text from current options
            const all = [...this._staticOptions, ...this._options];
            const match = all.find(o => String(o.value) === strVal);
            text = match ? match.text : '';
        }
        this._selectedValue = strVal;
        this._selectedText = String(text);
        if (this._hidden) this._hidden.value = strVal;
        if (this._input) this._input.value = this._selectedText;
        this._updateClear();
    }

    /** Returns current selection as { value, text }. */
    getValue() {
        return { value: this._selectedValue, text: this._selectedText };
    }

    /** Change remote URL. Clears cache and sets remote mode. */
    setSrc(url) {
        // Use setAttribute so attributeChangedCallback handles _src, _isRemote, _cache
        if (url) {
            this.setAttribute('src', url);
        } else {
            this.removeAttribute('src');
            this._src = null;
            this._isRemote = !!this._fetchFn;
            this._cache = null;
        }
    }

    /** Replace local dataset with an array of { value, text, ...extra } objects. Switches to local mode. */
    setOptions(options) {
        this._isRemote = false;
        this._src = null;
        this._fetchFn = null;
        this._cache = null;
        this._emptyOption = null;
        this._staticOptions = options.map(o => {
            const entry = { value: String(o.value ?? ''), text: String(o.text ?? ''), data: o, selected: false };
            if (entry.value === '') this._emptyOption = entry;
            return entry;
        });
        this._options = this._staticOptions.slice();
        if (this._isOpen) {
            this._buildItems(this._options);
            this._updateDropDirection();
        }
    }

    /** Remove disabled attribute. */
    enable() { this.removeAttribute('disabled'); }

    /** Set disabled attribute. */
    disable() { this.setAttribute('disabled', ''); }

    // --- DOM construction ---

    _buildDOM() {
        const name = this.getAttribute('name') || '';
        const placeholder = this.getAttribute('placeholder') || 'Search...';
        const isDisabled = this.hasAttribute('disabled');
        const isReadonly = this.hasAttribute('readonly');

        this.innerHTML = '';

        // Hidden input for form submission
        this._hidden = document.createElement('input');
        this._hidden.type = 'hidden';
        this._hidden.name = name;
        this._hidden.value = this._selectedValue;
        this.appendChild(this._hidden);

        // Wrapper — receives unified focus ring via CSS
        this._wrapper = document.createElement('div');
        this._wrapper.className = 'ss-wrapper';
        this.appendChild(this._wrapper);

        // Visible text input
        this._input = document.createElement('input');
        this._input.type = 'text';
        this._input.className = 'form-control ss-input';
        this._input.placeholder = placeholder;
        this._input.autocomplete = 'off';
        this._input.disabled = isDisabled;
        this._input.readOnly = isReadonly;
        this._input.value = this._selectedText;
        this._wrapper.appendChild(this._input);

        // Clear button
        this._clearBtn = document.createElement('button');
        this._clearBtn.type = 'button';
        this._clearBtn.className = 'ss-clear';
        this._clearBtn.innerHTML = '&times;';
        this._clearBtn.tabIndex = -1;
        this._wrapper.appendChild(this._clearBtn);

        // Dropdown menu
        this._menu = document.createElement('ul');
        this._menu.className = 'dropdown-menu w-100 ss-menu';
        this._menu.style.maxHeight = '250px';
        this._menu.style.overflowY = 'auto';
        this._wrapper.appendChild(this._menu);

        // Clear keyboard highlight on real mouse movement (not scroll-triggered mouseenter)
        this._menu.addEventListener('mousemove', () => {
            if (this.classList.contains('ss-keyboard-nav')) {
                this.classList.remove('ss-keyboard-nav');
                this._resetHighlight();
                this._getSelectableItems().forEach(el => el.classList.remove('ss-highlighted'));
            }
        });

        this._buildItems(this._options);
        this._updateClear();
    }

    _buildItems(options, filter) {
        this._menu.innerHTML = '';
        this._resetHighlight();
        const query = (filter || '').toLowerCase();
        let hasMatch = false;

        // In remote mode, always prepend the empty option (e.g., "Unassigned")
        const allOptions = this._isRemote && this._emptyOption
            ? [this._emptyOption, ...options.filter(o => o.value !== '')]
            : options;

        allOptions.forEach(opt => {
            if (query && !opt.text.toLowerCase().includes(query)) return;
            hasMatch = true;

            const li = document.createElement('li');
            const a = document.createElement('a');
            a.className = 'dropdown-item';
            a.href = '#';
            a.dataset.value = opt.value;
            a._optionData = opt.data; // expando — data may be a complex object, not just strings
            a.textContent = opt.text;
            if (String(opt.value) === String(this._selectedValue)) {
                a.classList.add('active');
            }
            a.addEventListener('mousedown', (e) => {
                e.preventDefault(); // prevent blur before click fires
                this._select(opt.value, opt.text, opt.data);
                this._close();
            });
            li.appendChild(a);
            this._menu.appendChild(li);
        });

        if (!hasMatch) {
            const li = document.createElement('li');
            const span = document.createElement('span');
            span.className = 'dropdown-item text-muted';
            span.textContent = 'No results';
            li.appendChild(span);
            this._menu.appendChild(li);
        }
    }

    _showStatus(message) {
        this._menu.innerHTML = '';
        const li = document.createElement('li');
        const span = document.createElement('span');
        span.className = 'dropdown-item text-muted';
        span.textContent = message;
        li.appendChild(span);
        this._menu.appendChild(li);
    }

    _attachEvents() {
        this._input.addEventListener('focus', () => this._open());
        this._input.addEventListener('click', () => {
            if (!this._isOpen) this._open();
        });

        this._input.addEventListener('input', () => {
            if (!this._isOpen) {
                // Re-open dropdown on any typing (e.g., backspace after Enter selected an item)
                this._isOpen = true;
                this._wrapper.classList.add('open');
                this._menu.classList.add('show');
            }
            if (this._isRemote) {
                if (this._isPrefetch() && this._cache) {
                    // Prefetch mode with cache: filter client-side, no server call
                    this._buildItems(this._cache, this._input.value);
                    this._updateDropDirection();
                } else {
                    this._debouncedFetch(this._input.value);
                }
            } else {
                this._buildItems(this._options, this._input.value);
                this._updateDropDirection();
            }
        });

        this._input.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                this._close();
                this._input.blur();
                return;
            }
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                this._moveHighlight(1);
                return;
            }
            if (e.key === 'ArrowUp') {
                e.preventDefault();
                this._moveHighlight(-1);
                return;
            }
            if (e.key === 'Enter') {
                e.preventDefault();
                this._selectHighlighted();
                return;
            }
        });

        this._clearBtn.addEventListener('mousedown', (e) => {
            e.preventDefault(); // prevent focus shift
            if (this.hasAttribute('disabled') || this.hasAttribute('readonly')) return;
            const empty = this._emptyOption;
            this._select(empty ? empty.value : '', empty ? empty.text : '');
            this._close();
            this._input.blur();
        });

        // Close on click outside
        this._outsideClickHandler = (e) => {
            if (!this.contains(e.target)) this._close();
        };
        document.addEventListener('click', this._outsideClickHandler);
    }

    // --- Keyboard navigation ---

    _getSelectableItems() {
        return this._menu.querySelectorAll('a.dropdown-item');
    }

    _moveHighlight(delta) {
        if (!this._isOpen) { this._open(); return; }
        const items = this._getSelectableItems();
        if (!items.length) return;
        this.classList.add('ss-keyboard-nav');
        this._highlightIndex = Math.max(0, Math.min(items.length - 1, this._highlightIndex + delta));
        this._applyHighlight(items);
    }

    _applyHighlight(items) {
        items.forEach((el, i) => el.classList.toggle('ss-highlighted', i === this._highlightIndex));
        const target = items[this._highlightIndex];
        if (target) target.scrollIntoView({ block: 'nearest' });
    }

    _resetHighlight() {
        this._highlightIndex = -1;
    }

    _selectHighlighted() {
        if (!this._isOpen) return;
        const items = this._getSelectableItems();
        const target = items[this._highlightIndex];
        if (target) {
            this._select(target.dataset.value, target.textContent, target._optionData);
            this._close();
        }
    }

    _select(value, text, data) {
        const previous = this._selectedValue;
        this._selectedValue = value;
        this._selectedText = text;
        this._hidden.value = value;
        this._input.value = text;
        this._updateClear();
        if (value !== previous) {
            this.dispatchEvent(new CustomEvent('change', {
                detail: { value, text, data },
                bubbles: true
            }));
        }
    }

    _updateClear() {
        if (!this._clearBtn) return;
        const show = this._selectedValue !== ''
            && !this.hasAttribute('disabled')
            && !this.hasAttribute('readonly');
        this._clearBtn.style.display = show ? '' : 'none';
        if (this._input) this._input.classList.toggle('has-clear', show);
    }

    _open() {
        if (this._input.disabled || this._input.readOnly) return;
        if (this._isOpen) return; // guard against Safari focus+click double-fire
        this._isOpen = true;
        this._input.value = '';
        this._wrapper.classList.add('open');

        if (this._isRemote) {
            if (this._isPrefetch() && this._cache) {
                // Prefetch mode with cache: show cached list, filter client-side
                this._options = this._cache;
                this._buildItems(this._options);
                this._menu.classList.add('show');
                this._updateDropDirection();
            } else {
                // Fetch from server (prefetch cache miss, or server-search mode)
                this._menu.classList.add('show');
                this._fetchRemote('');
            }
        } else {
            this._buildItems(this._options);
            this._menu.classList.add('show');
            this._updateDropDirection();
        }
    }

    _close() {
        this._isOpen = false;
        this._menu.classList.remove('show');
        this._wrapper.classList.remove('open');
        this._wrapper.classList.remove('dropup');
        this._wrapper.style.removeProperty('--ss-ring-height');
        if (this._input) this._input.value = this._selectedText;
        if (this._debounceTimer) clearTimeout(this._debounceTimer);
    }

    _updateDropDirection() {
        // Defer to next frame so the menu has rendered and has its final height
        requestAnimationFrame(() => {
            if (!this._isOpen) return;
            const inputRect = this._input.getBoundingClientRect();
            const menuH = this._menu.offsetHeight;
            const container = this._findScrollParent();
            const containerBottom = container
                ? container.getBoundingClientRect().bottom
                : window.innerHeight;
            const containerTop = container
                ? container.getBoundingClientRect().top
                : 0;
            const spaceBelow = containerBottom - inputRect.bottom;
            const spaceAbove = inputRect.top - containerTop;

            // Flip to dropup if not enough space below but more space above
            const shouldFlip = menuH > spaceBelow && spaceAbove > spaceBelow;
            this._wrapper.classList.toggle('dropup', shouldFlip);

            const inputH = this._input.offsetHeight;
            this._wrapper.style.setProperty('--ss-ring-height', `${inputH + menuH - 1}px`);
        });
    }

    /** Walk up the DOM to find the nearest scrollable ancestor. */
    _findScrollParent() {
        let el = this.parentElement;
        while (el && el !== document.body) {
            const overflow = getComputedStyle(el).overflowY;
            if (overflow === 'auto' || overflow === 'scroll') return el;
            el = el.parentElement;
        }
        return null;
    }

    _isPrefetch() {
        return this.hasAttribute('prefetch');
    }

    // --- Remote data ---

    _debouncedFetch(query) {
        if (this._debounceTimer) clearTimeout(this._debounceTimer);
        this._debounceTimer = setTimeout(() => {
            this._fetchRemote(query);
        }, this._debounceMs);
    }

    async _fetchRemote(query) {
        // Abort any in-flight request
        if (this._abortController) this._abortController.abort();
        this._abortController = new AbortController();

        this._showStatus('Loading\u2026');
        if (this._isOpen) {
            this._menu.classList.add('show');
            this._updateDropDirection();
        }

        try {
            let data;
            if (this._fetchFn) {
                // Custom fetch function — dev controls the request entirely
                data = await this._fetchFn(query, this._abortController.signal);
            } else {
                // Default: build URL from src + query-param
                const url = new URL(this._src, window.location.origin);
                if (query) url.searchParams.set(this._queryParam, query);

                const resp = await fetch(url.toString(), {
                    signal: this._abortController.signal,
                    headers: { 'Accept': 'application/json' }
                });
                if (!resp.ok) throw new Error(resp.statusText);
                data = await resp.json();
            }

            const options = data.map(item => ({
                value: String(item[this._valueField] ?? ''),
                text: String(item[this._textField] ?? ''),
                data: item
            }));

            // Cache only in prefetch mode on initial (empty query) fetch
            if (!query && this._isPrefetch()) this._cache = options;

            this._options = options;
            this._buildItems(options);
            if (this._isOpen) this._updateDropDirection();
        } catch (err) {
            if (err.name === 'AbortError') return; // request was superseded
            this._showStatus('Error loading results');
            if (this._isOpen) this._updateDropDirection();
        }
    }
}

customElements.define('searchable-select', SearchableSelect);
