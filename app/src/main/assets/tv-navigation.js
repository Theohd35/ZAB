(function () {
    'use strict';

    if (window.__didvipTvInstalled) return;
    window.__didvipTvInstalled = true;

    var style = document.createElement('style');
    style.textContent =
        'html,body{scrollbar-width:none!important;-ms-overflow-style:none!important}' +
        '::-webkit-scrollbar{display:none!important;width:0!important;height:0!important}' +
        '.didvip-tv-focus{outline:none!important;border-radius:6px!important;' +
        'box-shadow:0 0 10px 3px #E50914!important;' +
        'transition:box-shadow 100ms ease-out!important}';
    (document.head || document.documentElement).appendChild(style);

    var selector = 'a[href],button,input:not([type="hidden"]),select,textarea,' +
        '[role="button"],[role="link"],[tabindex]:not([tabindex="-1"]),video';

    function visible(element) {
        var rect = element.getBoundingClientRect();
        var css = window.getComputedStyle(element);
        return rect.width > 2 && rect.height > 2 && css.visibility !== 'hidden' &&
            css.display !== 'none' && !element.disabled;
    }

    function candidates() {
        return Array.prototype.filter.call(document.querySelectorAll(selector), visible);
    }

    function focus(element) {
        var old = document.querySelector('.didvip-tv-focus');
        if (old) old.classList.remove('didvip-tv-focus');
        element.classList.add('didvip-tv-focus');
        element.focus({preventScroll: true});
        element.scrollIntoView({behavior: 'smooth', block: 'nearest', inline: 'nearest'});
    }

    function initialFocus(items) {
        var active = document.activeElement;
        if (active && active !== document.body && visible(active)) return active;
        var preferred = items.find(function (item) {
            return /poster|synopsis|regarder|nouveaut|tendances/i.test(item.textContent || item.ariaLabel || '');
        });
        return preferred || items[0];
    }

    function move(direction) {
        var items = candidates();
        if (!items.length) return;
        var current = document.querySelector('.didvip-tv-focus') || initialFocus(items);
        if (!current) return;
        if (!current.classList.contains('didvip-tv-focus')) {
            focus(current);
            return;
        }
        var from = current.getBoundingClientRect();
        var fx = from.left + from.width / 2;
        var fy = from.top + from.height / 2;
        var best = null;
        var bestScore = Infinity;
        items.forEach(function (item) {
            if (item === current) return;
            var rect = item.getBoundingClientRect();
            var dx = rect.left + rect.width / 2 - fx;
            var dy = rect.top + rect.height / 2 - fy;
            var forward = direction === 'left' ? -dx : direction === 'right' ? dx :
                direction === 'up' ? -dy : dy;
            if (forward <= 2) return;
            var cross = direction === 'left' || direction === 'right' ? Math.abs(dy) : Math.abs(dx);
            var score = forward + cross * 2.5;
            if (score < bestScore) {
                bestScore = score;
                best = item;
            }
        });
        if (best) focus(best);
    }

    function requestFullscreen(video) {
        if (!video || document.fullscreenElement || document.webkitFullscreenElement) return;
        var request = video.requestFullscreen || video.webkitRequestFullscreen;
        if (request) {
            try {
                var promise = request.call(video);
                if (promise && promise.catch) promise.catch(function () {});
            } catch (ignored) {}
        }
    }

    document.addEventListener('focusin', function (event) {
        if (event.target && event.target.matches && event.target.matches(selector)) focus(event.target);
    });
    document.addEventListener('play', function (event) {
        if (event.target && event.target.tagName === 'VIDEO') requestFullscreen(event.target);
    }, true);
    document.addEventListener('click', function (event) {
        var control = event.target.closest && event.target.closest(selector);
        if (control && /regarder/i.test(control.textContent || control.ariaLabel || '')) {
            setTimeout(function () { requestFullscreen(document.querySelector('video')); }, 0);
        }
    }, true);

    window.__didvipTv = function (action) {
        if (action === 'select') {
            var selected = document.querySelector('.didvip-tv-focus') || initialFocus(candidates());
            if (selected) {
                focus(selected);
                selected.click();
            }
        } else {
            move(action);
        }
    };
})();
