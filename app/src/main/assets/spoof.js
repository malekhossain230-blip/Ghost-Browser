(function () {
  'use strict';
  if (window.__gh) return;
  var B = window.GhostBridge;
  if (!B) return;
  try { Object.defineProperty(window, '__gh', { value: 1 }); } catch (e) {}
  try { delete window.GhostBridge; } catch (e) {}

  // ---- make patched functions look native ----
  var nativeStr = Function.prototype.toString;
  var masked = new WeakMap();
  Function.prototype.toString = function () {
    var m = masked.get(this);
    return m !== undefined ? m : nativeStr.call(this);
  };
  masked.set(Function.prototype.toString, 'function toString() { [native code] }');
  function mask(fn, name) {
    masked.set(fn, 'function ' + name + '() { [native code] }');
    return fn;
  }

  // ---- ask the app (shows popup / applies saved rule). Cached per page ----
  var cache = {};
  function ask(group, detail) {
    if (cache[group]) return cache[group];
    var r;
    try { r = JSON.parse(B.ask(group, detail, String(location.origin))); }
    catch (e) { r = { mode: 'block', v: {} }; }
    cache[group] = r;
    return r;
  }

  // ---- generic getter replacement ----
  function gget(proto, prop, group, detail, fake, blocked) {
    try {
      var d = Object.getOwnPropertyDescriptor(proto, prop);
      if (!d || !d.get) return;
      var og = d.get;
      Object.defineProperty(proto, prop, {
        get: mask(function () {
          var r = ask(group, detail);
          if (r.mode === 'real') return og.call(this);
          if (r.mode === 'fake') return fake(r.v, this);
          return blocked;
        }, 'get ' + prop),
        set: d.set,
        enumerable: d.enumerable,
        configurable: true
      });
    } catch (e) {}
  }

  // ================= DEVICE =================
  var N = Navigator.prototype;
  var dev = 'Reads device model, browser, platform, CPU cores, RAM, touch';

  function buildUad(v) {
    var m = (String(v.ua).match(/Chrome\/(\d+)/) || [])[1] || '126';
    var brands = [
      { brand: 'Not)A;Brand', version: '99' },
      { brand: 'Chromium', version: m },
      { brand: 'Google Chrome', version: m }
    ];
    return {
      brands: brands,
      mobile: true,
      platform: 'Android',
      getHighEntropyValues: mask(function () {
        return Promise.resolve({
          brands: brands, mobile: true, platform: 'Android',
          platformVersion: v.androidVersion + '.0.0',
          model: v.model, architecture: '', bitness: '',
          uaFullVersion: m + '.0.0.0',
          fullVersionList: brands.map(function (b) { return { brand: b.brand, version: b.version + '.0.0.0' }; })
        });
      }, 'getHighEntropyValues'),
      toJSON: mask(function () { return { brands: brands, mobile: true, platform: 'Android' }; }, 'toJSON')
    };
  }

  gget(N, 'userAgent', 'device', dev, function (v) { return v.ua; }, '');
  gget(N, 'appVersion', 'device', dev, function (v) { return String(v.ua).replace(/^Mozilla\//, ''); }, '');
  gget(N, 'platform', 'device', dev, function (v) { return v.platform; }, '');
  gget(N, 'vendor', 'device', dev, function (v) { return v.vendor; }, '');
  gget(N, 'hardwareConcurrency', 'device', dev, function (v) { return +v.cores || 4; }, 2);
  gget(N, 'deviceMemory', 'device', dev, function (v) { return +v.memory || 4; }, 4);
  gget(N, 'maxTouchPoints', 'device', dev, function (v) { return +v.touch || 0; }, 0);
  gget(N, 'doNotTrack', 'device', dev, function (v) { return v.dnt === '' ? null : v.dnt; }, null);
  gget(N, 'userAgentData', 'device', dev, function (v) { return v._u || (v._u = buildUad(v)); }, undefined);

  // ================= SCREEN =================
  var S = Screen.prototype;
  var scr = 'Reads screen size, color depth, pixel density';
  gget(S, 'width', 'screen', scr, function (v) { return +v.screenW || 412; }, 0);
  gget(S, 'height', 'screen', scr, function (v) { return +v.screenH || 915; }, 0);
  gget(S, 'availWidth', 'screen', scr, function (v) { return +v.screenW || 412; }, 0);
  gget(S, 'availHeight', 'screen', scr, function (v) { return +v.screenH || 915; }, 0);
  gget(S, 'colorDepth', 'screen', scr, function (v) { return +v.depth || 24; }, 24);
  gget(S, 'pixelDepth', 'screen', scr, function (v) { return +v.depth || 24; }, 24);
  try {
    var dd = Object.getOwnPropertyDescriptor(window, 'devicePixelRatio');
    if (dd && dd.get) {
      var odg = dd.get;
      Object.defineProperty(window, 'devicePixelRatio', {
        get: mask(function () {
          var r = ask('screen', scr);
          if (r.mode === 'real') return odg.call(this);
          if (r.mode === 'fake') return +r.v.dpr || 2;
          return 1;
        }, 'get devicePixelRatio'),
        set: dd.set, enumerable: dd.enumerable, configurable: true
      });
    }
  } catch (e) {}

  // ================= LOCALE / TIMEZONE =================
  var loc = 'Reads language and timezone';
  gget(N, 'language', 'locale', loc, function (v) { return v.lang; }, 'en-US');
  gget(N, 'languages', 'locale', loc, function (v) {
    return v._l || (v._l = Object.freeze(String(v.langs).split(',').map(function (s) { return s.trim(); })));
  }, Object.freeze(['en-US']));
  try {
    var oRO = Intl.DateTimeFormat.prototype.resolvedOptions;
    Intl.DateTimeFormat.prototype.resolvedOptions = mask(function () {
      var o = oRO.apply(this, arguments);
      var r = ask('locale', loc);
      if (r.mode === 'fake') o.timeZone = r.v.tz;
      else if (r.mode === 'block') o.timeZone = 'UTC';
      return o;
    }, 'resolvedOptions');
  } catch (e) {}
  try {
    var oTZ = Date.prototype.getTimezoneOffset;
    Date.prototype.getTimezoneOffset = mask(function () {
      var r = ask('locale', loc);
      if (r.mode === 'fake') return +r.v.tzOffset || 0;
      if (r.mode === 'block') return 0;
      return oTZ.call(this);
    }, 'getTimezoneOffset');
  } catch (e) {}

  // ================= NETWORK =================
  try {
    if (window.NetworkInformation) {
      var NI = NetworkInformation.prototype;
      var net = 'Reads connection type and speed';
      gget(NI, 'effectiveType', 'network', net, function (v) { return v.netType; }, '4g');
      gget(NI, 'downlink', 'network', net, function (v) { return +v.downlink || 10; }, 10);
      gget(NI, 'rtt', 'network', net, function (v) { return +v.rtt || 50; }, 50);
    }
  } catch (e) {}

  // ================= LOCATION =================
  try {
    var G = Geolocation.prototype;
    var oGet = G.getCurrentPosition, oWatch = G.watchPosition, oClear = G.clearWatch;
    var locD = 'Reads your precise location';
    var denied = { code: 1, message: 'User denied Geolocation', PERMISSION_DENIED: 1, POSITION_UNAVAILABLE: 2, TIMEOUT: 3 };
    var fakePos = function (v) {
      return {
        coords: {
          latitude: +v.lat, longitude: +v.lon, accuracy: +v.acc || 20,
          altitude: null, altitudeAccuracy: null, heading: null, speed: null
        },
        timestamp: Date.now()
      };
    };
    G.getCurrentPosition = mask(function (s, e, o) {
      var r = ask('location', locD);
      if (r.mode === 'real') return oGet.call(this, s, e, o);
      if (r.mode === 'fake') { setTimeout(function () { if (s) s(fakePos(r.v)); }, 30); return; }
      setTimeout(function () { if (e) e(denied); }, 30);
    }, 'getCurrentPosition');
    G.watchPosition = mask(function (s, e, o) {
      var r = ask('location', locD);
      if (r.mode === 'real') return oWatch.call(this, s, e, o);
      if (r.mode === 'fake') {
        var id = setInterval(function () { if (s) s(fakePos(r.v)); }, 1000);
        setTimeout(function () { if (s) s(fakePos(r.v)); }, 30);
        return -id;
      }
      setTimeout(function () { if (e) e(denied); }, 30);
      return 0;
    }, 'watchPosition');
    G.clearWatch = mask(function (id) {
      if (id < 0) { clearInterval(-id); return; }
      return oClear.call(this, id);
    }, 'clearWatch');
  } catch (e) {}

  // ================= FINGERPRINT (canvas / WebGL) =================
  var fp = 'Reads canvas / WebGL fingerprint (GPU name, drawing hash)';
  var C2 = window.CanvasRenderingContext2D && CanvasRenderingContext2D.prototype;
  var oGID = C2 && C2.getImageData;

  function perturb(imgData, v) {
    var s = (+v.canvasNoise || 1) >>> 0;
    var a = imgData.data;
    for (var i = 0; i < a.length; i += 4 * 37) {
      s = (s * 1664525 + 1013904223) >>> 0;
      a[i] = a[i] ^ (s & 1);
    }
  }
  function noisy(src, v) {
    try {
      var w = src.width, h = src.height;
      if (!w || !h) return src;
      var c = document.createElement('canvas');
      c.width = w; c.height = h;
      var x = c.getContext('2d');
      x.drawImage(src, 0, 0);
      var d = oGID.call(x, 0, 0, w, h);
      perturb(d, v);
      x.putImageData(d, 0, 0);
      return c;
    } catch (e) { return src; }
  }
  function blank(src) {
    var c = document.createElement('canvas');
    c.width = src.width; c.height = src.height;
    return c;
  }
  try {
    var HC = HTMLCanvasElement.prototype;
    var oURL = HC.toDataURL, oBlob = HC.toBlob;
    HC.toDataURL = mask(function () {
      var r = ask('fingerprint', fp);
      if (r.mode === 'real') return oURL.apply(this, arguments);
      var c = r.mode === 'fake' ? noisy(this, r.v) : blank(this);
      return oURL.apply(c, arguments);
    }, 'toDataURL');
    HC.toBlob = mask(function () {
      var r = ask('fingerprint', fp);
      if (r.mode === 'real') return oBlob.apply(this, arguments);
      var c = r.mode === 'fake' ? noisy(this, r.v) : blank(this);
      return oBlob.apply(c, arguments);
    }, 'toBlob');
    if (C2 && oGID) {
      C2.getImageData = mask(function () {
        var d = oGID.apply(this, arguments);
        var r = ask('fingerprint', fp);
        if (r.mode === 'fake') perturb(d, r.v);
        else if (r.mode === 'block') { for (var i = 0; i < d.data.length; i++) d.data[i] = 0; }
        return d;
      }, 'getImageData');
    }
  } catch (e) {}
  [window.WebGLRenderingContext, window.WebGL2RenderingContext].forEach(function (Ctx) {
    try {
      if (!Ctx) return;
      var og = Ctx.prototype.getParameter;
      Ctx.prototype.getParameter = mask(function (p) {
        if (p === 37445 || p === 37446) {
          var r = ask('fingerprint', fp);
          if (r.mode === 'fake') return p === 37445 ? r.v.gpuVendor : r.v.gpuRenderer;
          if (r.mode === 'block') return p === 37445 ? 'Google Inc.' : 'ANGLE';
        }
        return og.apply(this, arguments);
      }, 'getParameter');
    } catch (e) {}
  });

  // ================= BATTERY =================
  try {
    if (navigator.getBattery) {
      var oB = Navigator.prototype.getBattery;
      Navigator.prototype.getBattery = mask(function () {
        var r = ask('battery', 'Reads battery level and charging state');
        if (r.mode === 'real') return oB.call(this);
        if (r.mode === 'fake') {
          return Promise.resolve({
            charging: true, chargingTime: 0, dischargingTime: Infinity,
            level: isNaN(+r.v.battery) ? 1 : +r.v.battery,
            addEventListener: function () {}, removeEventListener: function () {}
          });
        }
        return Promise.reject(new DOMException('Not allowed', 'NotAllowedError'));
      }, 'getBattery');
    }
  } catch (e) {}

  // ================= WEBRTC (can leak real IP) =================
  ['RTCPeerConnection', 'webkitRTCPeerConnection'].forEach(function (n) {
    try {
      var d = Object.getOwnPropertyDescriptor(window, n);
      if (!d) return;
      var val = d.value;
      Object.defineProperty(window, n, {
        get: mask(function () {
          var r = ask('webrtc', 'Uses WebRTC (can reveal your real IP address)');
          return r.mode === 'real' ? val : undefined;
        }, 'get ' + n),
        set: function () {},
        configurable: true, enumerable: false
      });
    } catch (e) {}
  });

  // ================= MOTION SENSORS =================
  try {
    var oAdd = window.addEventListener;
    window.addEventListener = mask(function (t) {
      if (t === 'devicemotion' || t === 'deviceorientation' || t === 'deviceorientationabsolute') {
        var r = ask('sensors', 'Reads motion / orientation sensors');
        if (r.mode !== 'real') return;
      }
      return oAdd.apply(this, arguments);
    }, 'addEventListener');
  } catch (e) {}

  // ================= CLIPBOARD =================
  try {
    if (window.Clipboard) {
      ['readText', 'read'].forEach(function (m) {
        var o = Clipboard.prototype[m];
        if (!o) return;
        Clipboard.prototype[m] = mask(function () {
          var r = ask('clipboard', 'Reads your clipboard');
          if (r.mode === 'real') return o.apply(this, arguments);
          return Promise.reject(new DOMException('Read permission denied.', 'NotAllowedError'));
        }, m);
      });
    }
  } catch (e) {}
})();
