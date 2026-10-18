    var APP_VERSION = '5.6';

    var DEV = {
      name: 'X ZONE BD',
      role: 'Android & Web Developer',
      avatar: ['img/developer.png', 'assets/img/developer.png'],
      links: [
        { icon: 'fa-solid fa-user', label: 'Profile', sub: 'View full profile', url: '' },
        { icon: 'fa-brands fa-facebook-f', label: 'Facebook', sub: 'Follow X ZONE BD', url: 'https://www.facebook.com/share/18Rh8ds8om' },
        { icon: 'fa-brands fa-telegram', label: 'Telegram', sub: 'Updates and support', url: 'https://t.me/x_zone_bangladesh' },
        { icon: 'fa-brands fa-youtube', label: 'YouTube', sub: 'Tutorials and demos', url: 'https://youtube.com/@xzone_bd?si=GclYPy7xufi-LS3I' },
        { icon: 'fa-solid fa-envelope', label: 'Email', sub: 'Business and feedback', url: 'xzone.bangladesh@gmail.com' }
      ]
    };

    var SETTINGS_KEY = 'vaelo_settings';
    var settings = { theme: 'system', pauseHistory: false, suggest: true, bg: '', glass: 5, hiddenDefaults: [] };

    try {
      var rawSettings = JSON.parse(localStorage.getItem(SETTINGS_KEY) || '{}');
      if (rawSettings && typeof rawSettings === 'object') {
        for (var sk in rawSettings) if (Object.prototype.hasOwnProperty.call(settings, sk)) settings[sk] = rawSettings[sk];
      }
    } catch (e) {}

    function saveSettings() {
      try { localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings)); } catch (e) {}
    }

    function $(id) { return document.getElementById(id); }

    function el(tag, cls, html) {
      var n = document.createElement(tag);
      if (cls) n.className = cls;
      if (html !== undefined) n.innerHTML = html;
      return n;
    }

    function escapeHtml(s) {
      return String(s).replace(/[&<>"']/g, function (c) {
        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
      });
    }

    function vibrate(ms) {
      try { if (navigator.vibrate) navigator.vibrate(ms); } catch (e) {}
    }

    var toastEl = $('toast');
    var toastText = $('toastText');
    var toastAct = $('toastAct');
    var toastTimer = null;
    var toastFn = null;

    function showToast(msg, type, actionLabel, actionFn) {
      clearTimeout(toastTimer);
      toastEl.className = 'toast' + (type ? ' ' + type : '');
      toastText.textContent = msg;
      toastFn = actionFn || null;
      toastAct.textContent = actionLabel || '';
      toastAct.classList.toggle('show', !!actionLabel);
      void toastEl.offsetWidth;
      toastEl.classList.add('show');
      toastTimer = setTimeout(hideToast, actionLabel ? 4200 : 2400);
    }

    function hideToast() {
      toastEl.classList.remove('show');
      toastFn = null;
    }

    toastAct.addEventListener('click', function () {
      var f = toastFn;
      hideToast();
      if (f) f();
    });

    function systemDark() {
      try { return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches; } catch (e) { return false; }
    }

    function resolvedTheme() {
      if (settings.theme === 'light' || settings.theme === 'dark') return settings.theme;
      return systemDark() ? 'dark' : 'light';
    }

    function isDark() { return document.documentElement.getAttribute('data-theme') === 'dark'; }

    function paintThemeColor() {
      var m = $('themeColor');
      if (m) m.setAttribute('content', isDark() ? '#0a0c10' : '#f3f4f7');
    }

    var themeSegBtns = $('themeSeg').querySelectorAll('button');

    function paintThemeSeg() {
      for (var i = 0; i < themeSegBtns.length; i++) {
        themeSegBtns[i].classList.toggle('active', themeSegBtns[i].getAttribute('data-theme') === settings.theme);
      }
      $('smAppearanceSub').textContent =
        settings.theme === 'light' ? 'Always light' :
        settings.theme === 'dark' ? 'Always dark' : 'Follows your system';
    }

    function applyTheme() {
      document.documentElement.setAttribute('data-theme', resolvedTheme());
      paintThemeColor();
      applyLogo();
      paintThemeSeg();
    }

    var vtBusy = false;

    function revealChange(x, y, fn) {
      if (vtBusy || typeof document.startViewTransition !== 'function') { fn(); return; }
      var w = window.innerWidth;
      var h = window.innerHeight;
      if (typeof x !== 'number' || typeof y !== 'number') { x = w / 2; y = h / 2; }
      var radius = Math.ceil(Math.sqrt(Math.pow(Math.max(x, w - x), 2) + Math.pow(Math.max(y, h - y), 2))) + 10;
      vtBusy = true;
      document.documentElement.classList.add('vt-run');
      var done = function () { vtBusy = false; document.documentElement.classList.remove('vt-run'); };
      var transition;
      try {
        transition = document.startViewTransition(fn);
      } catch (e) {
        done();
        fn();
        return;
      }
      transition.ready.then(function () {
        document.documentElement.animate(
          { clipPath: ['circle(0px at ' + x + 'px ' + y + 'px)', 'circle(' + radius + 'px at ' + x + 'px ' + y + 'px)'] },
          { duration: 560, easing: 'cubic-bezier(0.25, 0.9, 0.3, 1)', pseudoElement: '::view-transition-new(root)' }
        );
      }).catch(function () {});
      transition.finished.then(done, done);
    }

    function pointOf(e, fallbackEl) {
      if (e && typeof e.clientX === 'number' && (e.clientX || e.clientY)) return { x: e.clientX, y: e.clientY };
      var target = fallbackEl || (e && e.currentTarget);
      if (target && target.getBoundingClientRect) {
        var r = target.getBoundingClientRect();
        return { x: r.left + r.width / 2, y: r.top + r.height / 2 };
      }
      return { x: window.innerWidth / 2, y: window.innerHeight / 2 };
    }

    for (var ti = 0; ti < themeSegBtns.length; ti++) {
      themeSegBtns[ti].addEventListener('click', function (e) {
        var next = this.getAttribute('data-theme');
        if (next === settings.theme) return;
        var before = resolvedTheme();
        var pt = pointOf(e, this);
        settings.theme = next;
        saveSettings();
        if (resolvedTheme() === before) { applyTheme(); return; }
        revealChange(pt.x, pt.y, applyTheme);
      });
    }

    try {
      var mq = window.matchMedia('(prefers-color-scheme: dark)');
      var onScheme = function () { if (settings.theme !== 'light' && settings.theme !== 'dark') applyTheme(); };
      if (mq.addEventListener) mq.addEventListener('change', onScheme);
      else if (mq.addListener) mq.addListener(onScheme);
    } catch (e) {}

    var LOGO_LIGHT = 'assets/img/logo.png';
    var LOGO_DARK = 'assets/img/logo-dark.png';
    var hasDarkLogo = true;

    var LOGOS = [
      { img: $('brandImg'), fb: $('brandFallback') },
      { img: $('aboutImg'), fb: $('aboutFallback') },
      { img: $('menuLogo'), fb: $('menuLogoFb') }
    ];

    function applyLogo() {
      var dark = isDark();
      var src = dark && hasDarkLogo ? LOGO_DARK : LOGO_LIGHT;
      var mono = dark && !hasDarkLogo;
      for (var i = 0; i < LOGOS.length; i++) {
        var img = LOGOS[i].img;
        img.classList.toggle('mono', mono);
        if (img.getAttribute('src') !== src) img.setAttribute('src', src);
      }
    }

    for (var li = 0; li < LOGOS.length; li++) {
      (function (pair) {
        pair.img.addEventListener('error', function () {
          if (hasDarkLogo && isDark() && pair.img.getAttribute('src') === LOGO_DARK) {
            hasDarkLogo = false;
            applyLogo();
            return;
          }
          pair.img.style.display = 'none';
          pair.fb.style.display = pair.fb.classList.contains('about-fallback') ? 'flex' : 'inline';
        });
      })(LOGOS[li]);
    }

    applyTheme();

    function pad(n) { return String(n).padStart(2, '0'); }

    var digits = [];
    for (var di = 0; di < 6; di++) digits.push($('d' + di));

    function setDigit(node, ch) {
      var cur = node.querySelector('.dgi:not(.out)');
      if (cur && cur.textContent === ch) return;
      var olds = node.querySelectorAll('.dgi.out');
      for (var k = 0; k < olds.length; k++) olds[k].remove();
      var nxt = document.createElement('span');
      nxt.className = 'dgi in';
      nxt.textContent = ch;
      if (cur) cur.className = 'dgi out';
      node.appendChild(nxt);
      setTimeout(function () {
        if (cur && cur.parentNode) cur.remove();
        nxt.classList.remove('in');
      }, 480);
    }

    function updateClock() {
      var now = new Date();
      var h = now.getHours();
      var text = $('greetingText');
      var icon = $('greetingIcon');

      if (h >= 5 && h < 12) { text.textContent = 'Good Morning'; icon.className = 'fa-solid fa-cloud-sun'; }
      else if (h >= 12 && h < 17) { text.textContent = 'Good Afternoon'; icon.className = 'fa-solid fa-sun'; }
      else if (h >= 17 && h < 21) { text.textContent = 'Good Evening'; icon.className = 'fa-solid fa-cloud-moon'; }
      else { text.textContent = 'Good Night'; icon.className = 'fa-solid fa-moon'; }

      var stamp = pad(h) + pad(now.getMinutes()) + pad(now.getSeconds());
      for (var i = 0; i < 6; i++) setDigit(digits[i], stamp.charAt(i));

      $('dateText').textContent = now.toLocaleDateString('en-US', { weekday: 'long', day: 'numeric', month: 'long' });
    }

    function scheduleClock() {
      updateClock();
      setTimeout(scheduleClock, 1000 - (Date.now() % 1000));
    }
    scheduleClock();

    var layers = [];

    function syncLock() { document.body.classList.toggle('locked', layers.length > 0); }

    function openLayer(hideFn) {
      layers.push(hideFn);
      try { history.pushState({ v: layers.length }, ''); } catch (e) {}
      syncLock();
    }

    function replaceTopLayer(hideFn) {
      if (!layers.length) { openLayer(hideFn); return; }
      var old = layers[layers.length - 1];
      layers[layers.length - 1] = hideFn;
      if (old) old();
      syncLock();
    }

    var pendingPops = [];

    function closeTopLayer() {
      if (!layers.length) return;
      var f = layers.pop();
      if (f) f();
      syncLock();
      pendingPops.push(Date.now());
      try { history.back(); } catch (e) { pendingPops.pop(); }
    }

    window.addEventListener('popstate', function () {
      var now = Date.now();
      while (pendingPops.length && now - pendingPops[0] > 2500) pendingPops.shift();
      if (pendingPops.length) { pendingPops.shift(); return; }
      var f = layers.pop();
      if (f) f();
      syncLock();
    });

    var ENGINE_ORDER = ['google', 'bing', 'duckduckgo', 'brave', 'yahoo', 'yandex', 'startpage', 'ecosia', 'qwant', 'mojeek', 'baidu', 'perplexity'];

    var ENGINE_ICON_DIR = 'assets/icons/engines/';

    var ENGINES = {
      google: { label: 'Google', url: 'https://www.google.com/search?q=', letter: 'G', color: '#4285f4', domain: 'google.com', icon: 'google.svg' },
      bing: { label: 'Bing', url: 'https://www.bing.com/search?q=', letter: 'b', color: '#1d6cf0', domain: 'bing.com', icon: 'bing.svg' },
      duckduckgo: { label: 'DuckDuckGo', url: 'https://duckduckgo.com/?q=', letter: 'D', color: '#de5833', domain: 'duckduckgo.com', icon: 'duckduckgo.svg' },
      brave: { label: 'Brave', url: 'https://search.brave.com/search?q=', letter: 'B', color: '#fb542b', domain: 'search.brave.com', icon: 'brave.svg' },
      yahoo: { label: 'Yahoo', url: 'https://search.yahoo.com/search?p=', letter: 'Y', color: '#6001d2', domain: 'yahoo.com', icon: 'yahoo.svg' },
      yandex: { label: 'Yandex', url: 'https://yandex.com/search/?text=', letter: 'Я', color: '#fc3f1d', domain: 'yandex.com', icon: 'yandex.svg' },
      startpage: { label: 'Startpage', url: 'https://www.startpage.com/do/search?q=', letter: 'S', color: '#6573ff', domain: 'startpage.com', icon: 'startpage.svg' },
      ecosia: { label: 'Ecosia', url: 'https://www.ecosia.org/search?q=', letter: 'E', color: '#3c9a5f', domain: 'ecosia.org', icon: 'ecosia.svg' },
      qwant: { label: 'Qwant', url: 'https://www.qwant.com/?q=', letter: 'Q', color: '#5c97ff', domain: 'qwant.com', icon: 'qwant.svg' },
      mojeek: { label: 'Mojeek', url: 'https://www.mojeek.com/search?q=', letter: 'M', color: '#0fa14f', domain: 'mojeek.com', icon: 'mojeek.svg' },
      baidu: { label: 'Baidu', url: 'https://www.baidu.com/s?wd=', letter: '百', color: '#2932e1', domain: 'baidu.com', icon: 'baidu.svg' },
      perplexity: { label: 'Perplexity', url: 'https://www.perplexity.ai/search?q=', letter: 'P', color: '#20808d', domain: 'perplexity.ai', icon: 'perplexity.svg' }
    };

    var engine = 'google';
    try {
      var savedEngine = localStorage.getItem('vaelo_engine');
      if (savedEngine && ENGINES[savedEngine]) engine = savedEngine;
    } catch (e) {}

    var LETTER_COLORS = ['#1668f7', '#e11d48', '#f97316', '#16a34a', '#8b5cf6', '#0ea5e9', '#d946ef', '#f59e0b', '#14b8a6', '#ef4444', '#6366f1', '#0d9488'];

    function hashColor(s) {
      var h = 0;
      for (var i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0;
      return LETTER_COLORS[Math.abs(h) % LETTER_COLORS.length];
    }

    function domainOf(url) {
      var h = String(url || '').replace(/^[a-z]+:\/\//i, '').split(/[/?#]/)[0];
      h = h.replace(/^www\./i, '').split('@').pop().split(':')[0];
      return h.toLowerCase();
    }

    function hostLabel(url) {
      var h = domainOf(url);
      var part = h.split('.')[0];
      if (!part) return 'Link';
      return part.charAt(0).toUpperCase() + part.slice(1);
    }

    function iconDomain(domain) {
      var d = String(domain || '').toLowerCase();
      var parts = d.split('.');
      if (parts.length > 2 && /^(www|web|m|mobile|app|touch|wap)$/.test(parts[0])) d = parts.slice(1).join('.');
      return d;
    }

    function iconSources(domain) {
      var d = iconDomain(domain);
      var list = ['https://www.google.com/s2/favicons?sz=64&domain=' + encodeURIComponent(d)];
      if (d !== domain) list.push('https://www.google.com/s2/favicons?sz=64&domain=' + encodeURIComponent(domain));
      list.push('https://icons.duckduckgo.com/ip3/' + d + '.ico');
      return list;
    }

    var iconOk = {};
    var markOk = {};

    function markColor(c) { return c === 'ink' ? 'var(--ink)' : c; }
    function markTint(c) { return c === 'ink' ? 'var(--soft)' : c + '22'; }

    function renderMark(box, src, color, onFail) {
      box.innerHTML = '';
      box.classList.remove('skel');
      box.style.background = markTint(color);
      var mark = el('span', 'eng-mark');
      mark.style.backgroundColor = markColor(color);
      mark.style.webkitMaskImage = 'url("' + src + '")';
      mark.style.maskImage = 'url("' + src + '")';
      box.appendChild(mark);
      if (markOk[src] === undefined) {
        var probe = new Image();
        probe.onload = function () { markOk[src] = true; };
        probe.onerror = function () { markOk[src] = false; if (onFail) onFail(); };
        probe.src = src;
      }
    }

    var EMPTY_IMG = 'assets/img/empty.svg';
    var SITE_ICON_DIR = 'assets/icons/sites/';

    var SITE_ICONS = {
      'youtube.com': { icon: 'youtube.svg', color: '#ff0000' },
      'facebook.com': { icon: 'facebook.svg', color: '#0866ff' },
      'tiktok.com': { icon: 'tiktok.svg', color: 'ink' },
      'telegram.org': { icon: 'telegram.svg', color: '#26a5e4' }
    };

    function siteIcon(domain) {
      var d = iconDomain(domain);
      var hit = SITE_ICONS[d];
      if (!hit) return null;
      var src = SITE_ICON_DIR + hit.icon;
      if (markOk[src] === false) return null;
      return { src: src, color: hit.color };
    }

    function buildIcon(box, opts) {
      box.innerHTML = '';
      box.classList.remove('skel');
      box.style.background = '';

      if (!opts.img && opts.mark) {
        renderMark(box, opts.mark.src, opts.mark.color, function () { render(); });
        return;
      }

      var ltr = el('span', 'ltr', escapeHtml(opts.letter || '?'));
      ltr.style.background = opts.color || '#1668f7';

      if (opts.img) {
        var custom = el('img', 'ico loaded');
        custom.alt = '';
        custom.src = opts.img;
        custom.onerror = function () { custom.remove(); box.appendChild(ltr); };
        box.appendChild(custom);
        return;
      }

      if (!opts.domain) { box.appendChild(ltr); return; }

      var srcs = iconSources(opts.domain);
      var step = 0;
      var known = iconOk[opts.domain];
      var done = false;

      var im = el('img', 'ico');
      im.alt = '';
      if (!known) box.classList.add('skel');

      var fallbackTimer = setTimeout(function () {
        if (done) return;
        box.classList.remove('skel');
        if (!ltr.parentNode) box.appendChild(ltr);
      }, 7000);

      im.onload = function () {
        done = true;
        clearTimeout(fallbackTimer);
        box.classList.remove('skel');
        if (ltr.parentNode) ltr.remove();
        im.classList.add('loaded');
        iconOk[opts.domain] = im.getAttribute('src');
      };

      im.onerror = function () {
        step++;
        if (step < srcs.length) { im.setAttribute('src', srcs[step]); return; }
        done = true;
        clearTimeout(fallbackTimer);
        box.classList.remove('skel');
        im.remove();
        if (!ltr.parentNode) box.appendChild(ltr);
      };

      im.setAttribute('src', known || srcs[0]);
      box.appendChild(im);
    }

    function paintEngineBadge(box, key) {
      var cfg = ENGINES[key];
      var src = cfg.icon ? ENGINE_ICON_DIR + cfg.icon : '';
      if (src && markOk[src] !== false) {
        renderMark(box, src, cfg.color, function () { paintEngine(); });
        return;
      }
      buildIcon(box, { domain: cfg.domain, letter: cfg.letter, color: cfg.color });
      box.style.background = cfg.color + '22';
    }

    var engineBadge = $('engineBadge');
    var spEngineBadge = $('spEngineBadge');
    var smEngineBadge = $('smEngineBadge');
    var engGrid = $('engGrid');
    var engGridBuilt = false;

    function buildEngineGrid() {
      engGrid.innerHTML = '';
      for (var i = 0; i < ENGINE_ORDER.length; i++) {
        (function (key) {
          var cfg = ENGINES[key];
          var b = el('button', 'eng-opt');
          b.type = 'button';
          b.setAttribute('data-engine', key);
          var badge = el('span', 'eng-badge');
          paintEngineBadge(badge, key);
          var lab = el('span', 'el', escapeHtml(cfg.label));
          b.appendChild(badge);
          b.appendChild(lab);
          b.addEventListener('click', function () {
            setEngine(key);
            closeTopLayer();
          });
          engGrid.appendChild(b);
        })(ENGINE_ORDER[i]);
      }
      engGridBuilt = true;
      paintEngineGrid();
    }

    function paintEngineGrid() {
      var opts = engGrid.querySelectorAll('.eng-opt');
      for (var i = 0; i < opts.length; i++) {
        opts[i].classList.toggle('active', opts[i].getAttribute('data-engine') === engine);
      }
    }

    function paintEngine() {
      paintEngineBadge(engineBadge, engine);
      paintEngineBadge(spEngineBadge, engine);
      paintEngineBadge(smEngineBadge, engine);
      $('smEngineSub').textContent = ENGINES[engine].label;
      if (engGridBuilt) paintEngineGrid();
    }

    function setEngine(key) {
      if (!ENGINES[key]) return;
      engine = key;
      try { localStorage.setItem('vaelo_engine', engine); } catch (e) {}
      paintEngine();
      if (searchOpen && spInput.value.trim()) renderList(spInput.value.trim());
    }

    var engineOverlay = $('engineOverlay');

    function openEngineSheet() {
      if (!engGridBuilt) buildEngineGrid();
      paintEngineGrid();
      engineOverlay.classList.add('show');
      openLayer(hideEngineSheet);
    }

    function hideEngineSheet() {
      engineOverlay.classList.remove('show');
      if (searchOpen) setTimeout(function () { spInput.focus(); }, 50);
    }

    $('engineBtn').addEventListener('click', openEngineSheet);
    $('spEngineBtn').addEventListener('click', openEngineSheet);

    paintEngine();

    var DEFAULT_SHORTCUTS = [
      { name: 'YouTube', url: 'https://www.youtube.com' },
      { name: 'Facebook', url: 'https://www.facebook.com' },
      { name: 'TikTok', url: 'https://www.tiktok.com' },
      { name: 'Telegram', url: 'https://web.telegram.org' }
    ];

    var LEGACY_DEFAULT_URLS = [
      'https://www.youtube.com', 'https://www.google.com', 'https://www.facebook.com', 'https://mail.google.com',
      'https://x.com', 'https://www.tiktok.com', 'https://web.whatsapp.com', 'https://web.telegram.org', 'https://github.com'
    ];

    var items = [];
    try {
      var migrated = localStorage.getItem('vaelo_shortcuts_v2') === '1';
      var rawItems = localStorage.getItem('vaelo_shortcuts');
      if (rawItems) {
        var parsed = JSON.parse(rawItems);
        if (Object.prototype.toString.call(parsed) === '[object Array]') {
          for (var pi = 0; pi < parsed.length; pi++) {
            var p = parsed[pi];
            if (!p || typeof p.url !== 'string' || !p.url) continue;
            if (!migrated && LEGACY_DEFAULT_URLS.indexOf(p.url) !== -1) continue;
            items.push({ name: p.name || hostLabel(p.url), url: p.url, img: typeof p.img === 'string' ? p.img : '' });
          }
        }
      }
      if (!migrated) {
        localStorage.setItem('vaelo_shortcuts', JSON.stringify(items));
        localStorage.setItem('vaelo_shortcuts_v2', '1');
      }
    } catch (e) {}

    function saveItems() {
      try { localStorage.setItem('vaelo_shortcuts', JSON.stringify(items)); } catch (e) {}
    }

    function visibleDefaults() {
      var hidden = settings.hiddenDefaults || [];
      return DEFAULT_SHORTCUTS.filter(function (d) { return hidden.indexOf(d.url) === -1; });
    }

    var defCard = $('defCard');
    var defGrid = $('defGrid');
    var cusCard = $('cusCard');
    var grid = $('grid');
    var cusEmpty = $('cusEmpty');
    var moveFrom = -1;

    function buildTile(it, kind, index, delay) {
      var a = el('a', 'tile');
      a.href = it.url;
      a.setAttribute('data-kind', kind);
      a.setAttribute('data-index', index);
      a.style.animationDelay = Math.min(delay, 11) * 0.025 + 's';
      var box = el('div', 'tic');
      var dom = domainOf(it.url);
      buildIcon(box, { domain: dom, img: it.img, mark: siteIcon(dom), letter: (it.name || dom || '?').charAt(0).toUpperCase(), color: hashColor(dom || it.name) });
      var nm = el('span', 'tl');
      nm.textContent = it.name;
      a.appendChild(box);
      a.appendChild(nm);
      return a;
    }

    function render() {
      var defs = visibleDefaults();
      defGrid.innerHTML = '';
      defCard.classList.toggle('hidden', !defs.length);
      for (var d = 0; d < defs.length; d++) {
        var t = buildTile(defs[d], 'def', d, d);
        if (moveFrom >= 0) t.classList.add('dim');
        defGrid.appendChild(t);
      }

      grid.innerHTML = '';
      grid.classList.toggle('hidden', !items.length);
      cusEmpty.classList.toggle('hidden', !!items.length);
      for (var i = 0; i < items.length; i++) {
        var a = buildTile(items[i], 'cus', i, i + defs.length);
        if (moveFrom === i) a.classList.add('moving');
        else if (moveFrom >= 0) a.classList.add('dim');
        grid.appendChild(a);
      }
    }

    render();

    $('cusAdd').addEventListener('click', function () { openEdit(-1); });
    $('cusEmptyAdd').addEventListener('click', function () { openEdit(-1); });

    function bindLongPress(container, selector, onLong) {
      var timer = null;
      var sx = 0;
      var sy = 0;
      var target = null;
      var fired = false;

      function cancel() { clearTimeout(timer); timer = null; target = null; }

      function release() {
        cancel();
        if (fired) {
          fired = false;
          suppressClickUntil = Date.now() + 450;
        }
      }

      container.addEventListener('pointerdown', function (e) {
        if (e.button && e.button !== 0) return;
        var hit = e.target.closest ? e.target.closest(selector) : null;
        if (!hit || !container.contains(hit)) return;
        if (e.target.closest('[data-no-press]')) return;
        cancel();
        fired = false;
        target = hit;
        sx = e.clientX;
        sy = e.clientY;
        timer = setTimeout(function () {
          timer = null;
          var t = target;
          target = null;
          if (!t) return;
          fired = true;
          suppressClickUntil = Date.now() + 1500;
          vibrate(14);
          onLong(t, e);
        }, 460);
      });

      container.addEventListener('pointermove', function (e) {
        if (!timer) return;
        if (Math.abs(e.clientX - sx) > 10 || Math.abs(e.clientY - sy) > 10) cancel();
      });

      container.addEventListener('pointerup', release);
      container.addEventListener('pointercancel', release);
      container.addEventListener('pointerleave', release);
      container.addEventListener('scroll', cancel, { passive: true });
    }

    var suppressClickUntil = 0;

    function clickSuppressed() { return Date.now() < suppressClickUntil; }

    var cardMenu = $('cardMenu');
    var menuOpenedAt = 0;
    var activeIndex = -1;
    var activeKind = 'cus';

    function openCardMenu(index, card, kind) {
      activeIndex = index;
      activeKind = kind;
      var cmAll = cardMenu.querySelectorAll('.cm-item');
      for (var k = 0; k < cmAll.length; k++) {
        var act = cmAll[k].getAttribute('data-act');
        var show = kind === 'def' ? act === 'hide' : act !== 'hide';
        cmAll[k].classList.toggle('hidden', !show);
      }
      cardMenu.classList.add('show');
      menuOpenedAt = Date.now();

      var r = card.getBoundingClientRect();
      var mw = cardMenu.offsetWidth;
      var mh = cardMenu.offsetHeight;
      var left = r.left + r.width / 2 - mw / 2;
      if (left < 10) left = 10;
      if (left + mw > window.innerWidth - 10) left = window.innerWidth - mw - 10;
      var top = r.bottom + 8;
      if (top + mh > window.innerHeight - 10) top = Math.max(10, r.top - mh - 8);

      cardMenu.style.left = left + 'px';
      cardMenu.style.top = top + 'px';
    }

    function closeCardMenu() { cardMenu.classList.remove('show'); }

    function onTileLong(card) {
      if (moveFrom >= 0) return;
      openCardMenu(parseInt(card.getAttribute('data-index'), 10), card, card.getAttribute('data-kind'));
    }

    bindLongPress(grid, '.tile', onTileLong);
    bindLongPress(defGrid, '.tile', onTileLong);

    function onTileClick(e) {
      var card = e.target.closest ? e.target.closest('.tile') : null;
      if (!card) return;
      if (clickSuppressed()) { e.preventDefault(); return; }
      if (moveFrom >= 0) {
        e.preventDefault();
        if (card.getAttribute('data-kind') === 'cus') doMove(parseInt(card.getAttribute('data-index'), 10));
      }
    }

    grid.addEventListener('click', onTileClick);
    defGrid.addEventListener('click', onTileClick);

    window.addEventListener('contextmenu', function (e) {
      e.preventDefault();
      e.stopPropagation();
      return false;
    }, true);

    document.addEventListener('selectstart', function (e) {
      var t = e.target;
      if (t && (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA')) return;
      e.preventDefault();
    }, true);

    document.addEventListener('dragstart', function (e) { e.preventDefault(); }, true);
    document.addEventListener('gesturestart', function (e) { e.preventDefault(); });

    var cmItems = cardMenu.querySelectorAll('.cm-item');
    for (var ci = 0; ci < cmItems.length; ci++) {
      cmItems[ci].addEventListener('click', function (e) {
        e.stopPropagation();
        var act = this.getAttribute('data-act');
        var idx = activeIndex;
        var kind = activeKind;
        closeCardMenu();
        if (kind === 'def') {
          var defs = visibleDefaults();
          if (act !== 'hide' || idx < 0 || idx >= defs.length) return;
          var hidden = settings.hiddenDefaults || [];
          if (hidden.indexOf(defs[idx].url) === -1) hidden.push(defs[idx].url);
          settings.hiddenDefaults = hidden;
          saveSettings();
          render();
          showToast(defs[idx].name + ' hidden', '', 'Undo', function () {
            settings.hiddenDefaults = (settings.hiddenDefaults || []).filter(function (u) { return u !== defs[idx].url; });
            saveSettings();
            render();
          });
          return;
        }
        if (idx < 0 || idx >= items.length) return;
        if (act === 'move') startMove(idx);
        else if (act === 'edit') openEdit(idx);
        else if (act === 'delete') {
          openConfirm({
            title: 'Delete shortcut?',
            text: '"' + items[idx].name + '" will be removed from your home screen.',
            ok: 'Delete',
            danger: true,
            icon: 'fa-trash',
            onOk: function () {
              if (idx < items.length) { items.splice(idx, 1); saveItems(); render(); showToast('Shortcut removed', 'success'); }
            }
          });
        }
      });
    }

    var moveBar = $('moveBar');

    function startMove(index) {
      moveFrom = index;
      moveBar.classList.add('show');
      render();
      openLayer(hideMove);
    }

    function hideMove() {
      moveFrom = -1;
      moveBar.classList.remove('show');
      render();
    }

    function doMove(to) {
      if (moveFrom < 0) return;
      if (to !== moveFrom) {
        var it = items.splice(moveFrom, 1)[0];
        items.splice(to, 0, it);
        saveItems();
      }
      moveFrom = -1;
      closeTopLayer();
      render();
    }

    $('moveCancel').addEventListener('click', function () { closeTopLayer(); });

    function readImageFile(file, maxSide, mime, quality, done) {
      var reader = new FileReader();
      reader.onload = function () {
        var img = new Image();
        img.onload = function () {
          var w = img.naturalWidth || img.width;
          var h = img.naturalHeight || img.height;
          var scale = Math.min(1, maxSide / Math.max(w, h));
          var cw = Math.max(1, Math.round(w * scale));
          var chh = Math.max(1, Math.round(h * scale));
          var c = document.createElement('canvas');
          c.width = cw;
          c.height = chh;
          var ctx = c.getContext('2d');
          if (mime === 'image/jpeg') { ctx.fillStyle = '#000'; ctx.fillRect(0, 0, cw, chh); }
          ctx.drawImage(img, 0, 0, cw, chh);
          try { done(c.toDataURL(mime, quality)); } catch (e) { done(null); }
        };
        img.onerror = function () { done(null); };
        img.src = reader.result;
      };
      reader.onerror = function () { done(null); };
      reader.readAsDataURL(file);
    }

    function squareIconDataUrl(file, done) {
      var reader = new FileReader();
      reader.onload = function () {
        var img = new Image();
        img.onload = function () {
          var size = 128;
          var c = document.createElement('canvas');
          c.width = size;
          c.height = size;
          var ctx = c.getContext('2d');
          var w = img.naturalWidth || img.width;
          var h = img.naturalHeight || img.height;
          var s = Math.min(w, h);
          ctx.drawImage(img, (w - s) / 2, (h - s) / 2, s, s, 0, 0, size, size);
          try { done(c.toDataURL('image/png')); } catch (e) { done(null); }
        };
        img.onerror = function () { done(null); };
        img.src = reader.result;
      };
      reader.onerror = function () { done(null); };
      reader.readAsDataURL(file);
    }

    var editOverlay = $('editOverlay');
    var editTitle = $('editTitle');
    var fName = $('fName');
    var fUrl = $('fUrl');
    var editIcon = $('editIcon');
    var editIconTitle = $('editIconTitle');
    var editIconSub = $('editIconSub');
    var editIconFile = $('editIconFile');
    var editIndex = -1;
    var editImg = '';
    var previewTimer = null;

    function normalizeUrl(raw) {
      var u = String(raw || '').trim();
      if (!u) return '';
      if (!/^[a-z][a-z0-9+.-]*:\/\//i.test(u)) u = 'https://' + u;
      return u;
    }

    function paintEditIcon() {
      var url = normalizeUrl(fUrl.value);
      var dom = domainOf(url);
      var label = fName.value.trim() || (dom ? hostLabel(url) : '');
      fName.placeholder = dom ? hostLabel(url) : 'Auto from link';
      if (editImg) {
        editIconTitle.textContent = 'Custom icon';
        editIconSub.textContent = 'Chosen from your gallery';
      } else {
        editIconTitle.textContent = 'Automatic';
        editIconSub.textContent = dom ? 'Icon from ' + dom : "The site's icon is fetched for you";
      }
      buildIcon(editIcon, { domain: dom, img: editImg, mark: siteIcon(dom), letter: (label || dom || '?').charAt(0).toUpperCase(), color: hashColor(dom || label || 'x') });
    }

    var editExtra = $('editExtra');

    function openEdit(index, replace) {
      editIndex = index;
      fName.classList.remove('err');
      fUrl.classList.remove('err');
      editImg = '';

      if (index >= 0) {
        editTitle.textContent = 'Edit shortcut';
        fName.value = items[index].name || '';
        fUrl.value = items[index].url || '';
        editImg = items[index].img || '';
        editExtra.classList.remove('hidden');
        paintEditIcon();
      } else {
        editTitle.textContent = 'Add shortcut';
        fName.value = '';
        fUrl.value = '';
        editExtra.classList.add('hidden');
      }

      editOverlay.classList.add('show');
      if (replace) replaceTopLayer(hideEdit); else openLayer(hideEdit);
      setTimeout(function () { fUrl.focus(); }, 240);
    }

    function hideEdit() {
      editOverlay.classList.remove('show');
      fName.blur();
      fUrl.blur();
      clearTimeout(previewTimer);
    }

    fUrl.addEventListener('input', function () {
      fUrl.classList.remove('err');
      clearTimeout(previewTimer);
      if (editIndex >= 0) previewTimer = setTimeout(paintEditIcon, 350);
    });

    fName.addEventListener('input', function () {
      var dom = domainOf(normalizeUrl(fUrl.value));
      var ltr = editIcon.querySelector('.ltr');
      if (ltr) ltr.textContent = (fName.value.trim() || dom || '?').charAt(0).toUpperCase();
    });

    $('editIconPick').addEventListener('click', function () { editIconFile.value = ''; editIconFile.click(); });

    $('editIconReset').addEventListener('click', function () {
      if (!editImg) { showToast('Automatic icon is already in use'); return; }
      editImg = '';
      paintEditIcon();
    });

    editIconFile.addEventListener('change', function () {
      var f = editIconFile.files && editIconFile.files[0];
      if (!f) return;
      squareIconDataUrl(f, function (data) {
        if (!data) { showToast("Couldn't read that image", 'error'); return; }
        editImg = data;
        paintEditIcon();
      });
    });

    $('editCancel').addEventListener('click', function () { closeTopLayer(); });

    $('editSave').addEventListener('click', function () {
      var url = normalizeUrl(fUrl.value);
      var bare = url.replace(/^https?:\/\//i, '');
      if (!url || !domainOf(url) || !(URL_RE.test(bare) || IP_RE.test(bare) || /^localhost(:\d+)?(\/.*)?$/i.test(bare))) {
        fUrl.classList.add('err');
        fUrl.focus();
        return;
      }

      var isNew = !(editIndex >= 0 && editIndex < items.length);
      var name = isNew ? hostLabel(url) : (fName.value.trim() || hostLabel(url));
      var entry = { name: name, url: url, img: isNew ? '' : (editImg || '') };
      if (isNew) items.push(entry); else items[editIndex] = entry;

      saveItems();
      render();
      closeTopLayer();
      showToast(isNew ? 'Shortcut added' : 'Shortcut updated', 'success');
    });

    var confirmOverlay = $('confirmOverlay');
    var confirmOk = $('confirmOk');
    var confirmFn = null;

    function openConfirm(opts) {
      $('confirmTitle').textContent = opts.title || 'Are you sure?';
      $('confirmText').textContent = opts.text || '';
      confirmOk.textContent = opts.ok || 'OK';
      confirmOk.className = 'btn ' + (opts.danger ? 'btn-danger' : 'btn-main');
      var ic = $('confirmIc');
      ic.className = 'confirm-ic' + (opts.danger ? '' : ' neutral');
      ic.innerHTML = '<i class="fa-solid ' + (opts.icon || 'fa-circle-question') + '"></i>';
      confirmFn = opts.onOk || null;
      confirmOverlay.classList.add('show');
      openLayer(hideConfirm);
    }

    function hideConfirm() {
      confirmOverlay.classList.remove('show');
    }

    $('confirmCancel').addEventListener('click', function () { confirmFn = null; closeTopLayer(); });

    confirmOk.addEventListener('click', function () {
      var f = confirmFn;
      confirmFn = null;
      closeTopLayer();
      if (f) setTimeout(f, 0);
    });

    var menuBackdrop = $('menuBackdrop');
    var sideMenu = $('sideMenu');

    function openMenu() {
      paintMenu();
      menuBackdrop.classList.add('show');
      sideMenu.classList.add('show');
      openLayer(hideMenu);
    }

    function hideMenu() {
      menuBackdrop.classList.remove('show');
      sideMenu.classList.remove('show');
    }

    function paintMenu() {
      $('togPause').classList.toggle('on', !!settings.pauseHistory);
      $('togSuggest').classList.toggle('on', !!settings.suggest);
      $('smClearSub').textContent = recents.length ? recents.length + (recents.length === 1 ? ' search saved' : ' searches saved') : 'No searches saved';
      $('smThemeSub').textContent = bgLabel();
      $('smFoot').textContent = 'Vaelo v' + APP_VERSION;
      paintThemeSeg();
    }

    $('menuBtn').addEventListener('click', openMenu);
    $('menuClose').addEventListener('click', function () { closeTopLayer(); });
    menuBackdrop.addEventListener('click', function () { closeTopLayer(); });

    $('smAdd').addEventListener('click', function () { openEdit(-1, true); });

    $('smTheme').addEventListener('click', openThemeSheet);
    $('smEngine').addEventListener('click', openEngineSheet);

    $('smPause').addEventListener('click', function () {
      settings.pauseHistory = !settings.pauseHistory;
      saveSettings();
      paintMenu();
      paintPauseState();
      showToast(settings.pauseHistory ? 'History paused' : 'History resumed');
    });

    $('smSuggest').addEventListener('click', function () {
      settings.suggest = !settings.suggest;
      saveSettings();
      paintMenu();
      showToast(settings.suggest ? 'Live suggestions on' : 'Live suggestions off');
    });

    $('smClear').addEventListener('click', function () {
      if (!recents.length) { showToast('No search history to clear'); return; }
      openConfirm({
        title: 'Clear search history?',
        text: recents.length + ' saved ' + (recents.length === 1 ? 'search' : 'searches') + ' will be deleted permanently.',
        ok: 'Clear',
        danger: true,
        icon: 'fa-trash-can',
        onOk: function () {
          recents = [];
          saveRecents();
          paintMenu();
          if (searchOpen) renderList(spInput.value.trim());
          showToast('Search history cleared', 'success');
        }
      });
    });

    $('smReset').addEventListener('click', function () {
      openConfirm({
        title: 'Reset shortcuts?',
        text: 'Hidden quick-access tiles come back and your own shortcuts are removed.',
        ok: 'Reset',
        danger: false,
        icon: 'fa-rotate-left',
        onOk: function () {
          items = [];
          settings.hiddenDefaults = [];
          saveSettings();
          saveItems();
          render();
          showToast('Shortcuts reset', 'success');
        }
      });
    });

    var devOverlay = $('devOverlay');
    var aboutOverlay = $('aboutOverlay');

    function buildDevLinks() {
      var wrap = $('devLinks');
      wrap.innerHTML = '';
      $('devName').textContent = DEV.name;
      $('devRole').textContent = DEV.role;
      for (var i = 0; i < DEV.links.length; i++) {
        var l = DEV.links[i];
        if (!l.url) continue;
        var a = el('a', 'dev-link');
        a.href = l.url;
        a.innerHTML =
          '<span class="sm-badge"><i class="' + l.icon + '"></i></span>' +
          '<span class="dl-text">' + escapeHtml(l.label) + '<small>' + escapeHtml(l.sub || l.url) + '</small></span>' +
          '<i class="fa-solid fa-arrow-up-right-from-square chev"></i>';
        wrap.appendChild(a);
      }
      if (!wrap.children.length) wrap.innerHTML = '<div class="sp-hint" style="padding:14px;">Contact links coming soon</div>';
    }

    function loadDevAvatar() {
      var img = $('devImg');
      var icon = $('devIcon');
      var step = 0;
      img.onload = function () { img.classList.add('loaded'); icon.style.display = 'none'; };
      img.onerror = function () {
        step++;
        if (step < DEV.avatar.length) { img.setAttribute('src', DEV.avatar[step]); return; }
        img.remove();
      };
      img.setAttribute('src', DEV.avatar[0]);
    }

    buildDevLinks();
    loadDevAvatar();
    $('aboutSub').textContent = 'Private browsing, your way - v' + APP_VERSION;

    $('smDeveloper').addEventListener('click', function () {
      devOverlay.classList.add('show');
      openLayer(function () { devOverlay.classList.remove('show'); });
    });

    $('smAbout').addEventListener('click', function () {
      aboutOverlay.classList.add('show');
      openLayer(function () { aboutOverlay.classList.remove('show'); });
    });

    var GRADIENTS = [
      { id: 'aurora', name: 'Aurora', css: 'linear-gradient(135deg, #0ea5e9, #6366f1 50%, #a855f7)' },
      { id: 'sunset', name: 'Sunset', css: 'linear-gradient(135deg, #f97316, #ef4444 50%, #8b5cf6)' },
      { id: 'ocean', name: 'Ocean', css: 'linear-gradient(160deg, #0f172a, #1e3a8a 50%, #0ea5e9)' },
      { id: 'mint', name: 'Mint', css: 'linear-gradient(135deg, #34d399, #06b6d4 55%, #3b82f6)' },
      { id: 'rose', name: 'Rose', css: 'linear-gradient(135deg, #fb7185, #f472b6 50%, #c084fc)' },
      { id: 'night', name: 'Night', css: 'linear-gradient(160deg, #020617, #1e1b4b 60%, #312e81)' },
      { id: 'sand', name: 'Sand', css: 'linear-gradient(135deg, #fde68a, #f59e0b 55%, #b45309)' }
    ];

    var PHOTOS = [1015, 1016, 1018, 1019, 1021, 1022, 1029, 1036, 1039, 1043, 1047, 1053];

    function photoUrl(id, w, h) { return 'https://picsum.photos/id/' + id + '/' + w + '/' + h; }

    var BG_CUSTOM_KEY = 'vaelo_bg_custom';
    var bgLayer = $('bgLayer');
    var themeOverlay = $('themeOverlay');
    var bgFile = $('bgFile');
    var glassSlider = $('glassSlider');
    var glassRow = $('glassRow');

    function customBg() {
      try { return localStorage.getItem(BG_CUSTOM_KEY) || ''; } catch (e) { return ''; }
    }

    function bgLabel() {
      var bg = settings.bg || '';
      if (!bg) return 'Default';
      if (bg === 'custom') return 'Your photo';
      if (bg.indexOf('g:') === 0) {
        for (var i = 0; i < GRADIENTS.length; i++) if (GRADIENTS[i].id === bg.slice(2)) return GRADIENTS[i].name;
      }
      if (bg.indexOf('p:') === 0) return 'Photo';
      return 'Custom';
    }

    function applyGlass(val) {
      val = Math.max(0, Math.min(10, Number(val) || 0));
      var blur = 2 + val * 1.4;
      var aLight = Math.max(0.5, 0.94 - val * 0.04).toFixed(2);
      var aDark = Math.max(0.42, 0.88 - val * 0.04).toFixed(2);
      var root = document.documentElement.style;
      root.setProperty('--glass-blur', blur + 'px');
      root.setProperty('--glass-a-light', aLight);
      root.setProperty('--glass-a-dark', aDark);
      $('glassVal').textContent = String(val);
      glassSlider.value = val;
    }

    function applyBg() {
      var bg = settings.bg || '';
      var image = '';
      if (bg === 'custom') {
        image = customBg();
        if (!image) { settings.bg = ''; saveSettings(); bg = ''; }
        else image = 'url("' + image + '")';
      } else if (bg.indexOf('g:') === 0) {
        for (var i = 0; i < GRADIENTS.length; i++) if (GRADIENTS[i].id === bg.slice(2)) image = GRADIENTS[i].css;
        if (!image) { settings.bg = ''; saveSettings(); bg = ''; }
      } else if (bg.indexOf('p:') === 0) {
        image = 'url("' + photoUrl(bg.slice(2), 900, 1600) + '")';
      }

      if (image) {
        bgLayer.style.backgroundImage = image;
        document.body.classList.add('has-bg');
      } else {
        document.body.classList.remove('has-bg');
        bgLayer.style.backgroundImage = '';
      }
      glassRow.classList.toggle('disabled', !image);
      glassSlider.disabled = !image;
      $('smThemeSub').textContent = bgLabel();
    }

    function setBg(value, silent, pt) {
      if (value === (settings.bg || '')) return;
      settings.bg = value;
      saveSettings();
      var apply = function () { applyBg(); paintThemeCards(); };
      if (pt) revealChange(pt.x, pt.y, apply); else apply();
      if (!silent) showToast(value ? 'Background applied' : 'Default background restored', 'success');
    }

    var themeBuilt = false;

    function themeCard(value, inner, name) {
      var c = el('div', 'th-card');
      c.setAttribute('data-bg', value);
      if (inner) c.appendChild(inner);
      var check = el('div', 'th-check', '<i class="fa-solid fa-check"></i>');
      c.appendChild(check);
      if (name) c.appendChild(el('div', 'th-name', escapeHtml(name)));
      return c;
    }

    function buildThemeSheet() {
      var styles = $('thStyles');
      var photos = $('thPhotos');
      styles.innerHTML = '';
      photos.innerHTML = '';

      var reset = themeCard('', el('div', 'th-center', '<i class="fa-solid fa-ban"></i><span>Default</span>'));
      reset.addEventListener('click', function (e) { setBg('', false, pointOf(e, reset)); });
      styles.appendChild(reset);

      for (var i = 0; i < GRADIENTS.length; i++) {
        (function (g) {
          var c = themeCard('g:' + g.id, null, g.name);
          c.style.background = g.css;
          c.addEventListener('click', function (e) { setBg('g:' + g.id, false, pointOf(e, c)); });
          styles.appendChild(c);
        })(GRADIENTS[i]);
      }

      var custom = themeCard('custom', el('div', 'th-center', '<i class="fa-solid fa-image"></i><span>Gallery</span>'), '');
      custom.id = 'thCustom';
      var change = el('button', 'th-change', '<i class="fa-solid fa-camera"></i>');
      change.type = 'button';
      change.setAttribute('data-no-press', '1');
      change.addEventListener('click', function (e) { e.stopPropagation(); bgFile.value = ''; bgFile.click(); });
      custom.appendChild(change);
      custom.addEventListener('click', function (e) {
        if (customBg()) setBg('custom', false, pointOf(e, custom));
        else { bgFile.value = ''; bgFile.click(); }
      });
      photos.appendChild(custom);

      for (var k = 0; k < PHOTOS.length; k++) {
        (function (id) {
          var c = themeCard('p:' + id, null, '');
          c.classList.add('skel');
          var im = el('img');
          im.alt = '';
          im.addEventListener('load', function () { c.classList.remove('skel'); im.classList.add('loaded'); });
          im.addEventListener('error', function () { c.classList.remove('skel'); c.innerHTML = '<div class="th-center"><i class="fa-solid fa-image"></i></div>'; c.style.opacity = '0.5'; c.style.pointerEvents = 'none'; });
          im.src = photoUrl(id, 192, 272);
          c.appendChild(im);
          c.addEventListener('click', function (e) {
            if ('p:' + id === (settings.bg || '')) return;
            var pt = pointOf(e, c);
            var full = new Image();
            full.onload = function () { setBg('p:' + id, false, pt); };
            full.onerror = function () { showToast("Couldn't load that photo", 'error'); };
            showToast('Applying background...');
            full.src = photoUrl(id, 900, 1600);
          });
          photos.appendChild(c);
        })(PHOTOS[k]);
      }

      themeBuilt = true;
      paintThemeCards();
    }

    function paintThemeCards() {
      if (!themeBuilt) return;
      var cards = themeOverlay.querySelectorAll('.th-card');
      for (var i = 0; i < cards.length; i++) {
        cards[i].classList.toggle('active', cards[i].getAttribute('data-bg') === (settings.bg || ''));
      }
      var custom = $('thCustom');
      if (custom) {
        var data = customBg();
        var center = custom.querySelector('.th-center');
        var old = custom.querySelector('img');
        if (old) old.remove();
        if (data) {
          var im = el('img', 'loaded');
          im.alt = '';
          im.src = data;
          custom.insertBefore(im, custom.firstChild);
          if (center) center.style.display = 'none';
        } else if (center) {
          center.style.display = '';
        }
      }
    }

    function openThemeSheet() {
      if (!themeBuilt) buildThemeSheet();
      paintThemeCards();
      themeOverlay.classList.add('show');
      openLayer(function () { themeOverlay.classList.remove('show'); });
    }

    bgFile.addEventListener('change', function () {
      var f = bgFile.files && bgFile.files[0];
      if (!f) return;
      showToast('Preparing your photo...');
      readImageFile(f, 1280, 'image/jpeg', 0.82, function (data) {
        if (!data) { showToast("Couldn't read that image", 'error'); return; }
        try {
          localStorage.setItem(BG_CUSTOM_KEY, data);
        } catch (e) {
          showToast('That photo is too large to save', 'error');
          return;
        }
        var pt = pointOf(null, $('thCustom'));
        if ((settings.bg || '') === 'custom') {
          revealChange(pt.x, pt.y, function () { applyBg(); paintThemeCards(); });
          showToast('Background applied', 'success');
        } else {
          setBg('custom', false, pt);
        }
      });
    });

    glassSlider.addEventListener('input', function () {
      settings.glass = Number(glassSlider.value);
      applyGlass(settings.glass);
      saveSettings();
    });

    applyGlass(settings.glass);
    applyBg();

    var URL_RE = /^(https?:\/\/)?([a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}(:\d+)?(\/.*)?$/i;
    var IP_RE = /^(https?:\/\/)?(\d{1,3}\.){3}\d{1,3}(:\d+)?(\/.*)?$/;

    function looksLikeUrl(q) {
      if (/\s/.test(q)) return false;
      return URL_RE.test(q) || IP_RE.test(q) || /^https?:\/\/\S+$/i.test(q) || /^localhost(:\d+)?(\/.*)?$/i.test(q);
    }

    function displayTerm(s) {
      if (!looksLikeUrl(s)) return s;
      return String(s).replace(/^https?:\/\//i, '').replace(/\/$/, '');
    }

    function termHtml(s, q) {
      if (looksLikeUrl(s)) return '<span class="acc">' + escapeHtml(displayTerm(s)) + '</span>';
      return q ? highlight(s, q) : escapeHtml(s);
    }

    var recents = [];
    try {
      var rawRec = localStorage.getItem('vaelo_history');
      if (rawRec) {
        var parsedRec = JSON.parse(rawRec);
        if (Object.prototype.toString.call(parsedRec) === '[object Array]') {
          recents = parsedRec.filter(function (x) { return typeof x === 'string' && x; });
        }
      }
    } catch (e) {}

    function saveRecents() {
      try { localStorage.setItem('vaelo_history', JSON.stringify(recents)); } catch (e) {}
    }

    function pushRecent(q) {
      if (settings.pauseHistory) return;
      var low = q.toLowerCase();
      for (var i = recents.length - 1; i >= 0; i--) {
        if (String(recents[i]).toLowerCase() === low) recents.splice(i, 1);
      }
      recents.unshift(q);
      if (recents.length > 40) recents.length = 40;
      saveRecents();
    }

    function removeRecent(text) {
      var idx = -1;
      for (var i = 0; i < recents.length; i++) if (recents[i] === text) { idx = i; break; }
      if (idx < 0) return;
      recents.splice(idx, 1);
      saveRecents();
      showToast('Removed from history', '', 'Undo', function () {
        recents.splice(Math.min(idx, recents.length), 0, text);
        saveRecents();
        if (searchOpen) renderList(spInput.value.trim());
      });
    }

    function runSearch(query) {
      var q = (query || '').trim();
      if (!q) return;
      pushRecent(q);
      if (looksLikeUrl(q)) {
        window.location.href = /^https?:\/\//i.test(q) ? q : 'https://' + q;
        return;
      }
      window.location.href = ENGINES[engine].url + encodeURIComponent(q);
    }

    var searchPage = $('searchPage');
    var spForm = $('spForm');
    var spInput = $('spInput');
    var spClear = $('spClear');
    var spList = $('spList');
    var searchOpen = false;

    function paintPauseState() {
      var ic = $('searchOpenIcon');
      ic.className = settings.pauseHistory ? 'fa-solid fa-user-secret' : 'fa-solid fa-magnifying-glass';
      $('searchOpenText').textContent = settings.pauseHistory ? 'Private search' : 'Search or type a URL';
      spInput.placeholder = settings.pauseHistory ? 'Private search' : 'Search or type a URL';
    }

    paintPauseState();

    function sectionCard(title, actionLabel, actionFn) {
      var card = el('div', 'sp-card');
      if (title) {
        var head = el('div', 'sp-card-head');
        head.appendChild(el('span', '', escapeHtml(title)));
        if (actionLabel) {
          var b = el('button', '', escapeHtml(actionLabel));
          b.type = 'button';
          b.addEventListener('click', function (e) { e.stopPropagation(); actionFn(); });
          head.appendChild(b);
        }
        card.appendChild(head);
      }
      var body = el('div', 'sp-card-body');
      card.appendChild(body);
      return { card: card, body: body };
    }

    function makeRow(text, iconCls, opts) {
      opts = opts || {};
      var row = el('div', 'sug-item' + (opts.primary ? ' primary' : '') + (opts.recent ? ' recent' : ''));
      row.setAttribute('data-text', text);

      var shown = opts.display || text;
      var ic = el('span', 'sug-ic', '<i class="fa-solid ' + iconCls + '"></i>');
      var label = el('span', 'sug-text');
      if (opts.html) label.innerHTML = opts.html; else label.textContent = shown;

      row.appendChild(ic);
      row.appendChild(label);

      if (!opts.noFill) {
        var fill = el('button', 'sug-fill', '<i class="fa-solid fa-arrow-up"></i>');
        fill.type = 'button';
        fill.setAttribute('aria-label', 'Use suggestion');
        fill.setAttribute('data-no-press', '1');
        fill.addEventListener('click', function (e) {
          e.stopPropagation();
          spInput.value = shown;
          spInput.focus();
          onInputChanged();
        });
        row.appendChild(fill);
      }

      row.addEventListener('click', function () {
        if (clickSuppressed()) return;
        runSearch(opts.target || text);
      });
      return row;
    }

    var markRe = null;
    try { markRe = new RegExp('\\p{M}', 'u'); } catch (e) {}

    function graphemeBoundary(s, n) {
      if (n >= s.length) return s.length;
      try {
        if (window.Intl && Intl.Segmenter) {
          var parts = Array.from(new Intl.Segmenter(undefined, { granularity: 'grapheme' }).segment(s));
          for (var i = 0; i < parts.length; i++) if (parts[i].index >= n) return parts[i].index;
          return s.length;
        }
      } catch (e) {}
      var j = n;
      while (j < s.length) {
        var ch = s.charAt(j);
        var prev = s.charAt(j - 1);
        var code = ch.charCodeAt(0);
        var joiner = ch === '‌' || ch === '‍' || prev === '‍' || prev === '্' || prev === '्';
        var lowSurrogate = code >= 0xdc00 && code <= 0xdfff;
        if (joiner || lowSurrogate || (markRe && markRe.test(ch))) j++;
        else break;
      }
      return j;
    }

    function highlight(s, q) {
      var low = s.toLowerCase();
      var ql = q.toLowerCase();
      if (ql && low.indexOf(ql) === 0 && s.length > q.length) {
        var cut = graphemeBoundary(s, q.length);
        if (cut >= s.length) return escapeHtml(s);
        return escapeHtml(s.slice(0, cut)) + '<b>' + escapeHtml(s.slice(cut)) + '</b>';
      }
      return escapeHtml(s);
    }

    function showSkeleton(body, n) {
      body.innerHTML = '';
      var widths = ['62%', '78%', '48%', '70%'];
      for (var i = 0; i < n; i++) {
        var r = el('div', 'skel-row');
        r.appendChild(el('span', 'skel-ic skel'));
        var line = el('span', 'skel-line skel');
        line.style.maxWidth = widths[i % widths.length];
        r.appendChild(line);
        body.appendChild(r);
      }
    }

    function renderRecents() {
      spList.innerHTML = '';

      if (settings.pauseHistory) {
        spList.appendChild(el('div', 'paused-chip', '<i class="fa-solid fa-user-secret"></i>History paused — searches won\'t be saved'));
      }

      if (!recents.length) {
        var card = el('div', 'sp-card sp-empty');
        var pic = el('img', 'empty-img');
        pic.alt = '';
        var hint = el('div', 'sp-hint', '');
        pic.onload = function () { pic.classList.add('loaded'); };
        pic.onerror = function () {
          pic.remove();
          hint.insertBefore(el('i', 'fa-solid fa-magnifying-glass'), hint.firstChild);
        };
        pic.src = EMPTY_IMG;
        card.appendChild(pic);
        card.appendChild(hint);
        spList.appendChild(card);
        return;
      }

      var sec = sectionCard('Recent', 'Clear', function () { $('smClear').click(); });
      for (var i = 0; i < recents.length && i < 10; i++) {
        sec.body.appendChild(makeRow(recents[i], 'fa-clock-rotate-left', { recent: true, html: termHtml(recents[i], ''), display: displayTerm(recents[i]) }));
      }
      spList.appendChild(sec.card);
      spList.scrollTop = 0;
    }

    var sugCache = {};
    var sugSeq = 0;
    var sugTimer = null;
    var pendingBody = null;

    function renderList(q) {
      sugSeq++;
      clearTimeout(sugTimer);
      pendingBody = null;
      spList.innerHTML = '';

      if (!q) { renderRecents(); return; }

      var frag = document.createDocumentFragment();
      var isUrl = looksLikeUrl(q);
      var seen = {};
      seen[q.toLowerCase()] = true;

      var primary = sectionCard('');
      if (isUrl) {
        primary.body.appendChild(makeRow(q, 'fa-link', {
          primary: true, noFill: true,
          html: '<span class="acc">' + escapeHtml(displayTerm(q)) + '</span><span class="dimtxt"> — Open site</span>'
        }));
      } else {
        primary.body.appendChild(makeRow(q, 'fa-magnifying-glass', {
          primary: true, noFill: true,
          html: '<b>' + escapeHtml(q) + '</b><span class="dimtxt"> — ' + escapeHtml(ENGINES[engine].label) + ' Search</span>'
        }));
      }
      frag.appendChild(primary.card);

      var low = q.toLowerCase();
      var matches = [];
      for (var i = 0; i < recents.length && matches.length < 3; i++) {
        var r = String(recents[i]);
        var rk = r.toLowerCase();
        if (seen[rk] || rk.indexOf(low) === -1) continue;
        seen[rk] = true;
        matches.push(r);
      }
      if (matches.length) {
        var recSec = sectionCard('From recent');
        for (var m = 0; m < matches.length; m++) {
          recSec.body.appendChild(makeRow(matches[m], 'fa-clock-rotate-left', { recent: true, html: termHtml(matches[m], q), display: displayTerm(matches[m]) }));
        }
        frag.appendChild(recSec.card);
      }

      if (settings.suggest && !isUrl) {
        var sugSec = sectionCard('Suggestions');
        if (sugCache[low]) {
          fillSuggest(sugSec, q, sugCache[low], seen);
        } else {
          showSkeleton(sugSec.body, 4);
          pendingBody = sugSec;
          var my = sugSeq;
          sugTimer = setTimeout(function () {
            fetchSuggest(q, function (list) {
              if (my !== sugSeq || pendingBody !== sugSec) return;
              sugCache[low] = list;
              fillSuggest(sugSec, q, list, seen);
            });
          }, 80);
        }
        frag.appendChild(sugSec.card);
      }

      spList.appendChild(frag);
      spList.scrollTop = 0;
    }

    function fillSuggest(sec, q, list, seen) {
      sec.body.innerHTML = '';
      var n = 0;
      for (var i = 0; i < list.length && n < 8; i++) {
        var s = list[i];
        if (typeof s !== 'string' || !s) continue;
        var k = s.toLowerCase();
        if (seen[k]) continue;
        seen[k] = true;
        sec.body.appendChild(makeRow(s, looksLikeUrl(s) ? 'fa-globe' : 'fa-magnifying-glass', { html: termHtml(s, q), display: displayTerm(s) }));
        n++;
      }
      if (!n) sec.card.remove();
    }

    var SUG_PROVIDERS = [
      {
        name: 'google',
        url: function (q, cb) { return 'https://suggestqueries.google.com/complete/search?client=chrome&q=' + encodeURIComponent(q) + '&callback=' + cb; }
      },
      {
        name: 'bing',
        url: function (q, cb) { return 'https://api.bing.com/osjson.aspx?query=' + encodeURIComponent(q) + '&JsonType=callback&JsonCallback=' + cb; }
      },
      {
        name: 'yahoo',
        url: function (q, cb) { return 'https://search.yahoo.com/sugg/gossip/gossip-us-ura/?output=sd1&command=' + encodeURIComponent(q) + '&callback=' + cb; }
      }
    ];

    var providerDown = {};
    var jsonpId = 0;

    function parseSuggest(data) {
      var out = [];
      if (!data) return out;
      var list = null;
      if (Object.prototype.toString.call(data) === '[object Array]') {
        list = Object.prototype.toString.call(data[1]) === '[object Array]' ? data[1] : data;
      } else if (typeof data === 'object') {
        if (data.r) list = data.r;
        else if (data.gossip && data.gossip.results) list = data.gossip.results;
        else if (data.suggestions) list = data.suggestions;
      }
      if (!list) return out;
      for (var i = 0; i < list.length; i++) {
        var x = list[i];
        var s = null;
        if (typeof x === 'string') s = x;
        else if (Object.prototype.toString.call(x) === '[object Array]' && typeof x[0] === 'string') s = x[0];
        else if (x && typeof x === 'object') s = x.k || x.key || x.phrase || x.text || x.query || null;
        if (typeof s === 'string') {
          s = s.replace(/<[^>]+>/g, '').trim();
          if (s) out.push(s);
        }
      }
      return out;
    }

    function jsonp(url, name, done) {
      var script = document.createElement('script');
      var finished = false;

      function cleanup() {
        if (finished) return;
        finished = true;
        clearTimeout(timer);
        try { delete window[name]; } catch (e) { window[name] = undefined; }
        if (script.parentNode) script.parentNode.removeChild(script);
      }

      var timer = setTimeout(function () { cleanup(); done(null, true); }, 2800);

      window[name] = function (data) { cleanup(); done(data, false); };
      script.onerror = function () { cleanup(); done(null, true); };
      script.async = true;
      script.src = url;
      document.head.appendChild(script);
    }

    function fetchSuggest(q, done) {
      var order = SUG_PROVIDERS.slice();
      var now = Date.now();

      function tryNext() {
        while (order.length && providerDown[order[0].name] && providerDown[order[0].name] > now) order.shift();
        if (!order.length) { done([]); return; }
        var prov = order.shift();
        var name = 'vaeloSug' + (++jsonpId);
        jsonp(prov.url(q, name), name, function (data, failed) {
          if (failed) {
            providerDown[prov.name] = Date.now() + 60000;
            tryNext();
            return;
          }
          var list = parseSuggest(data);
          done(list);
        });
      }

      tryNext();
    }

    var warmed = false;

    function warmSuggest() {
      if (warmed || !settings.suggest) return;
      warmed = true;
      fetchSuggest('a', function (list) { if (list.length) sugCache['a'] = list; });
    }

    function openSearchPage(prefill) {
      if (searchOpen) return;
      searchOpen = true;
      closeCardMenu();
      searchPage.classList.add('show');
      document.body.classList.add('sp-open');
      spInput.value = prefill || '';
      spClear.classList.toggle('show', !!spInput.value);
      renderList(spInput.value.trim());
      openLayer(hideSearchPage);
      warmSuggest();
      setTimeout(function () { spInput.focus(); }, 60);
    }

    function hideSearchPage() {
      searchOpen = false;
      searchPage.classList.remove('show');
      document.body.classList.remove('sp-open');
      spInput.blur();
      clearTimeout(sugTimer);
      sugSeq++;
      pendingBody = null;
    }

    $('searchOpen').addEventListener('click', function (e) {
      if (e.target.closest && e.target.closest('.mic-btn')) return;
      openSearchPage('');
    });

    function onInputChanged() {
      var q = spInput.value.trim();
      spClear.classList.toggle('show', !!spInput.value);
      renderList(q);
    }

    spInput.addEventListener('input', onInputChanged);

    spClear.addEventListener('click', function () {
      spInput.value = '';
      onInputChanged();
      spInput.focus();
    });

    spForm.addEventListener('submit', function (e) {
      e.preventDefault();
      runSearch(spInput.value);
    });

    bindLongPress(spList, '.sug-item.recent', function (row) {
      var text = row.getAttribute('data-text');
      row.classList.add('removing');
      setTimeout(function () {
        var body = row.parentNode;
        row.remove();
        removeRecent(text);
        if (body && !body.children.length) renderList(spInput.value.trim());
      }, 190);
    });

    var voice = $('voice');
    var voiceBars = $('voiceBars');
    var voiceStatus = $('voiceStatus');
    var voiceText = $('voiceText');
    var voiceOpen = false;
    var voiceRaf = null;
    var voiceStart = 0;
    var voiceRec = null;
    var voiceBarEls = [];

    for (var vb = 0; vb < 24; vb++) {
      var bar = document.createElement('span');
      voiceBars.appendChild(bar);
      voiceBarEls.push(bar);
    }

    function animateBars(ts) {
      if (!voiceStart) voiceStart = ts;
      var t = (ts - voiceStart) / 1000;
      for (var i = 0; i < voiceBarEls.length; i++) {
        var wave = Math.sin(t * 3 + i * 0.6) * 0.5 + 0.5;
        var jitter = Math.random() * 0.3;
        var v = Math.min(1, wave * 0.7 + jitter);
        voiceBarEls[i].style.height = (6 + v * 46).toFixed(1) + 'px';
      }
      voiceRaf = requestAnimationFrame(animateBars);
    }

    function stopBars() {
      if (voiceRaf) cancelAnimationFrame(voiceRaf);
      voiceRaf = null;
      voiceStart = 0;
      for (var i = 0; i < voiceBarEls.length; i++) voiceBarEls[i].style.height = '6px';
    }

    function stopRecognition() {
      if (voiceRec) {
        voiceRec.onresult = null;
        voiceRec.onerror = null;
        voiceRec.onend = null;
        try { voiceRec.stop(); } catch (e) {}
        voiceRec = null;
      }
      if (window.AndroidVoice) { try { window.AndroidVoice.stopListening(); } catch (e) {} }
    }

    function hideVoice() {
      voiceOpen = false;
      voice.classList.remove('show');
      voice.classList.add('idle');
      stopRecognition();
      stopBars();
    }

    function voiceSupported() {
      return !!(window.AndroidVoice || window.SpeechRecognition || window.webkitSpeechRecognition);
    }

    function finishVoice(text) {
      voiceStatus.textContent = 'Searching...';
      voiceText.textContent = text;
      stopBars();
      setTimeout(function () { runSearch(text); }, 450);
    }

    function startVoice() {
      if (!voiceOpen) {
        voiceOpen = true;
        voice.classList.add('show');
        openLayer(hideVoice);
      }
      beginListening();
    }

    function beginListening() {
      stopRecognition();
      stopBars();
      voiceText.textContent = '';

      if (!voiceSupported()) {
        voice.classList.add('idle');
        voiceStatus.textContent = "Voice search isn't available in this browser yet.";
        return;
      }

      voice.classList.remove('idle');
      voiceStatus.textContent = 'Listening...';
      animateBars(performance.now());

      if (window.AndroidVoice) {
        try { window.AndroidVoice.startListening('bn-BD'); return; } catch (e) {}
      }

      var SR = window.SpeechRecognition || window.webkitSpeechRecognition;
      voiceRec = new SR();
      voiceRec.lang = 'bn-BD';
      voiceRec.continuous = false;
      voiceRec.interimResults = true;

      voiceRec.onresult = function (e) {
        var finalText = '';
        var interim = '';
        for (var i = 0; i < e.results.length; i++) {
          if (e.results[i].isFinal) finalText += e.results[i][0].transcript;
          else interim += e.results[i][0].transcript;
        }
        voiceText.textContent = finalText || interim;
        if (finalText.trim()) finishVoice(finalText.trim());
      };
      voiceRec.onerror = function () {
        voiceStatus.textContent = "Didn't catch that. Tap the mic to try again.";
        voice.classList.add('idle');
        stopBars();
      };
      voiceRec.onend = function () {
        if (voiceOpen && voiceStatus.textContent === 'Listening...') {
          voiceStatus.textContent = 'No speech detected. Tap the mic to try again.';
          voice.classList.add('idle');
          stopBars();
        }
      };
      try { voiceRec.start(); } catch (e) {}
    }

    window.onNativeVoiceResult = function (text) {
      if (!voiceOpen) return;
      voiceText.textContent = text || '';
      if (text && text.trim()) finishVoice(text.trim());
      else { voiceStatus.textContent = 'No speech detected. Tap the mic to try again.'; voice.classList.add('idle'); stopBars(); }
    };

    window.onNativeVoiceError = function (message) {
      if (!voiceOpen) return;
      voiceStatus.textContent = message || "Didn't catch that. Tap the mic to try again.";
      voice.classList.add('idle');
      stopBars();
    };

    voice.querySelector('.voice-mic').addEventListener('click', function () {
      if (!voiceOpen || !voiceSupported()) return;
      beginListening();
    });

    $('voiceClose').addEventListener('click', function () { closeTopLayer(); });
    $('micHome').addEventListener('click', function (e) { e.stopPropagation(); startVoice(); });
    $('micSearch').addEventListener('click', function (e) { e.stopPropagation(); startVoice(); });

    var overlays = [editOverlay, engineOverlay, themeOverlay, devOverlay, aboutOverlay, confirmOverlay];
    for (var oi = 0; oi < overlays.length; oi++) {
      (function (ov) {
        ov.addEventListener('click', function (e) {
          if (e.target === ov) {
            if (ov === confirmOverlay) confirmFn = null;
            closeTopLayer();
          }
        });
      })(overlays[oi]);
    }

    document.addEventListener('click', function (e) {
      var t = e.target;
      if (cardMenu.classList.contains('show') && !clickSuppressed() && Date.now() - menuOpenedAt > 350) {
        if (!t.closest || !t.closest('.card-menu')) closeCardMenu();
      }
    });

    window.addEventListener('scroll', function () {
      if (cardMenu.classList.contains('show')) closeCardMenu();
    }, { passive: true });

    document.addEventListener('keydown', function (e) {
      if (e.key !== 'Escape') return;
      if (cardMenu.classList.contains('show')) { closeCardMenu(); return; }
      if (layers.length) closeTopLayer();
    });
