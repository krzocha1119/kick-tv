(function() {
    if (window.__kickTvNavInitialized) return;
    window.__kickTvNavInitialized = true;

    let focusedElement = null;

    const SELECTOR = [
        'a[href]',
        'button:not([disabled])',
        'input:not([disabled])',
        '[tabindex="0"]',
        '[role="button"]',
        'video'
    ].join(',');

    function getVisibleCandidates() {
        const candidates = Array.from(document.querySelectorAll(SELECTOR));
        return candidates.filter(el => {
            if (!el || el.disabled) return false;
            // Ignore hidden elements
            if (el.offsetWidth <= 10 || el.offsetHeight <= 10) return false;
            const style = window.getComputedStyle(el);
            if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') return false;
            const r = el.getBoundingClientRect();
            // Must be within or close to visible viewport
            return (r.bottom >= -50 && r.top <= window.innerHeight + 50 && r.right >= 0 && r.left <= window.innerWidth);
        });
    }

    function setFocus(el) {
        if (!el) return;
        if (focusedElement && focusedElement !== el) {
            focusedElement.classList.remove('kick-tv-focused');
        }
        focusedElement = el;
        focusedElement.classList.add('kick-tv-focused');
        focusedElement.focus();
        focusedElement.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' });
    }

    window.tvNavigate = function(dir) {
        const candidates = getVisibleCandidates();
        if (candidates.length === 0) return false;

        // If nothing is focused yet, focus the first good candidate (header or first card)
        if (!focusedElement || !document.contains(focusedElement) || !focusedElement.classList.contains('kick-tv-focused')) {
            // Prefer element around top: 60-150px
            const initial = candidates.find(e => {
                const r = e.getBoundingClientRect();
                return r.top >= 50 && r.top <= 400;
            }) || candidates[0];
            setFocus(initial);
            return true;
        }

        const curRect = focusedElement.getBoundingClientRect();
        const curCenter = {
            x: curRect.left + curRect.width / 2,
            y: curRect.top + curRect.height / 2
        };

        let bestEl = null;
        let minScore = Infinity;

        for (let i = 0; i < candidates.length; i++) {
            const el = candidates[i];
            if (el === focusedElement) continue;

            const r = el.getBoundingClientRect();
            const center = {
                x: r.left + r.width / 2,
                y: r.top + r.height / 2
            };

            let primary = 0;
            let secondary = 0;
            let isValid = false;

            if (dir === 'right') {
                primary = center.x - curCenter.x;
                secondary = Math.abs(center.y - curCenter.y);
                isValid = primary > 12;
                if (isValid) {
                    const score = primary + (secondary * 2.8);
                    if (score < minScore) { minScore = score; bestEl = el; }
                }
            } else if (dir === 'left') {
                primary = curCenter.x - center.x;
                secondary = Math.abs(center.y - curCenter.y);
                isValid = primary > 12;
                if (isValid) {
                    const score = primary + (secondary * 2.8);
                    if (score < minScore) { minScore = score; bestEl = el; }
                }
            } else if (dir === 'down') {
                primary = center.y - curCenter.y;
                secondary = Math.abs(center.x - curCenter.x);
                isValid = primary > 12;
                if (isValid) {
                    const score = primary + (secondary * 2.2);
                    if (score < minScore) { minScore = score; bestEl = el; }
                }
            } else if (dir === 'up') {
                primary = curCenter.y - center.y;
                secondary = Math.abs(center.x - curCenter.x);
                isValid = primary > 12;
                if (isValid) {
                    const score = primary + (secondary * 2.2);
                    if (score < minScore) { minScore = score; bestEl = el; }
                }
            }
        }

        if (bestEl) {
            setFocus(bestEl);
            return true;
        } else {
            // Auto scroll when reaching edge
            if (dir === 'down') {
                window.scrollBy({ top: 350, behavior: 'smooth' });
                setTimeout(() => {
                    const newCandidates = getVisibleCandidates();
                    const next = newCandidates.find(e => e.getBoundingClientRect().top > curRect.bottom);
                    if (next) setFocus(next);
                }, 200);
            } else if (dir === 'up') {
                window.scrollBy({ top: -350, behavior: 'smooth' });
                setTimeout(() => {
                    const newCandidates = getVisibleCandidates();
                    const next = newCandidates.find(e => e.getBoundingClientRect().bottom < curRect.top);
                    if (next) setFocus(next);
                }, 200);
            }
            return false;
        }
    };

    window.tvClick = function() {
        if (!focusedElement) {
            window.tvNavigate('down');
            return;
        }

        if (focusedElement.tagName === 'INPUT' || focusedElement.tagName === 'TEXTAREA') {
            if (window.KickTvBridge && window.KickTvBridge.showInputKeyboard) {
                window.KickTvBridge.showInputKeyboard(
                    focusedElement.id || ('input_' + Math.random().toString(36).substr(2, 9)),
                    focusedElement.placeholder || focusedElement.getAttribute('aria-label') || 'Wpisz tekst',
                    focusedElement.value || '',
                    focusedElement.type || 'text'
                );
                return;
            }
        }

        // Simulate complete click sequence
        const opts = { bubbles: true, cancelable: true, view: window };
        try {
            focusedElement.dispatchEvent(new PointerEvent('pointerdown', opts));
            focusedElement.dispatchEvent(new MouseEvent('mousedown', opts));
            focusedElement.dispatchEvent(new PointerEvent('pointerup', opts));
            focusedElement.dispatchEvent(new MouseEvent('mouseup', opts));
            focusedElement.click();
        } catch(e) {
            focusedElement.click();
        }
    };

    // Listen to click / focus on inputs directly
    document.addEventListener('focusin', function(e) {
        if (e.target && (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA')) {
            if (window.KickTvBridge && window.KickTvBridge.showInputKeyboard) {
                window.KickTvBridge.showInputKeyboard(
                    e.target.id || ('input_' + Math.random().toString(36).substr(2, 9)),
                    e.target.placeholder || e.target.getAttribute('aria-label') || 'Wpisz tekst',
                    e.target.value || '',
                    e.target.type || 'text'
                );
            }
        }
    });

    // Auto highlight first item once DOM ready
    setTimeout(() => {
        if (!focusedElement) {
            window.tvNavigate('down');
        }
    }, 1000);
})();
