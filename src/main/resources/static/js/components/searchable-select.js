// <searchable-select> — reusable searchable dropdown Web Component.
//
// Local mode (static options):
//   <searchable-select name="userId" placeholder="Search users...">
//       <option value="">Unassigned</option>
//       <option value="1" selected>Alice Johnson</option>
//       <option value="2">Bob Smith</option>
//   </searchable-select>
//
// Remote mode (fetches from API):
//   <searchable-select name="userId" placeholder="Search users..."
//                      src="/api/users" value-field="id" text-field="name">
//       <option value="">Unassigned</option>
//       <option value="1" selected>Alice Johnson</option>
//   </searchable-select>
//
// Attributes: name, placeholder, disabled, src, value-field, text-field, query-param, debounce
// Form submission: hidden <input> carries the selected value.
// CSS: load searchable-select-bootstrap5.css (or your own theme).

class SearchableSelect extends HTMLElement {

    connectedCallback() {
        this._src = this.getAttribute('src');
        this._isRemote = !!this._src;
        this._valueField = this.getAttribute('value-field') || 'id';
        this._textField = this.getAttribute('text-field') || 'name';
        this._queryParam = this.getAttribute('query-param') || 'q';
        this._debounceMs = parseInt(this.getAttribute('debounce') || '300', 10);

        // Parse <option> children before replacing innerHTML.
        // In remote mode, only the empty option and any selected option matter.
        this._staticOptions = [];
        this._emptyOption = null;
        this.querySelectorAll('option').forEach(opt => {
            const entry = {
                value: opt.value,
                text: opt.textContent.trim(),
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
        this._cache = null;        // cached full-list response
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

    static get observedAttributes() { return ['disabled']; }

    attributeChangedCallback(name) {
        if (name === 'disabled' && this._input) {
            const isDisabled = this.hasAttribute('disabled');
            this._input.disabled = isDisabled;
            if (isDisabled) this._close();
            this._updateClear();
        }
    }

    _buildDOM() {
        const name = this.getAttribute('name') || '';
        const placeholder = this.getAttribute('placeholder') || 'Search...';
        const isDisabled = this.hasAttribute('disabled');

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
            a.textContent = opt.text;
            if (String(opt.value) === String(this._selectedValue)) {
                a.classList.add('active');
            }
            a.addEventListener('mousedown', (e) => {
                e.preventDefault(); // prevent blur before click fires
                this._select(opt.value, opt.text);
                this._close();
            });
            li.appendChild(a);
            this._menu.appendChild(li);
        });

        if (!hasMatch) {
            const li = document.createElement('li');
            li.innerHTML = '<span class="dropdown-item text-muted">No results</span>';
            this._menu.appendChild(li);
        }
    }

    _showStatus(message) {
        this._menu.innerHTML = '';
        const li = document.createElement('li');
        li.innerHTML = '<span class="dropdown-item text-muted">' + message + '</span>';
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
                this._debouncedFetch(this._input.value);
            } else {
                this._buildItems(this._options, this._input.value);
                this._updateRingHeight();
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
            this._select(target.dataset.value, target.textContent);
            this._close();
        }
    }

    _select(value, text) {
        this._selectedValue = value;
        this._selectedText = text;
        this._hidden.value = value;
        this._input.value = text;
        this._updateClear();
    }

    _updateClear() {
        const show = this._selectedValue !== '' && !this.hasAttribute('disabled');
        this._clearBtn.style.display = show ? '' : 'none';
        this._input.classList.toggle('has-clear', show);
    }

    _open() {
        if (this._input.disabled) return;
        this._isOpen = true;
        this._input.value = '';
        this._wrapper.classList.add('open');

        if (this._isRemote) {
            if (this._cache) {
                // Use cached full list
                this._options = this._cache;
                this._buildItems(this._options);
                this._menu.classList.add('show');
                this._updateRingHeight();
            } else {
                // Fetch full list
                this._menu.classList.add('show');
                this._fetchRemote('');
            }
        } else {
            this._buildItems(this._options);
            this._menu.classList.add('show');
            this._updateRingHeight();
        }
    }

    _close() {
        this._isOpen = false;
        this._menu.classList.remove('show');
        this._wrapper.classList.remove('open');
        this._wrapper.style.removeProperty('--ss-ring-height');
        if (this._input) this._input.value = this._selectedText;
        if (this._debounceTimer) clearTimeout(this._debounceTimer);
    }

    _updateRingHeight() {
        // Defer to next frame so the menu has rendered and has its final height
        requestAnimationFrame(() => {
            if (!this._isOpen) return;
            const inputH = this._input.offsetHeight;
            const menuH = this._menu.offsetHeight;
            this._wrapper.style.setProperty('--ss-ring-height', (inputH + menuH - 1) + 'px');
        });
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

        const url = new URL(this._src, window.location.origin);
        if (query) url.searchParams.set(this._queryParam, query);

        this._showStatus('Loading\u2026');
        if (this._isOpen) {
            this._menu.classList.add('show');
            this._updateRingHeight();
        }

        try {
            const resp = await fetch(url.toString(), {
                signal: this._abortController.signal,
                headers: { 'Accept': 'application/json' }
            });
            if (!resp.ok) throw new Error(resp.statusText);
            const data = await resp.json();

            const options = data.map(item => ({
                value: String(item[this._valueField] ?? ''),
                text: String(item[this._textField] ?? '')
            }));

            // Cache the full-list response (no query)
            if (!query) this._cache = options;

            this._options = options;
            this._buildItems(options);
            if (this._isOpen) this._updateRingHeight();
        } catch (err) {
            if (err.name === 'AbortError') return; // request was superseded
            this._showStatus('Error loading results');
            if (this._isOpen) this._updateRingHeight();
        }
    }
}

customElements.define('searchable-select', SearchableSelect);
