(function () {
  /* HTML2Video v3.5: DOM adapter (No Background Music — Voice + Sound Effects Only).
     Un HTML pages ke liye jo canvas nahi, balki scene (section.scene, data-start/data-end, .rv data-at)
     aur CAPTIONS + render() se bane hain (jaise MovieBox promo). */
  try {
    if (window.__h2vDom) return 'dom-exists';
    var isDom = false;
    try {
      isDom = (typeof scenes !== 'undefined' && Array.isArray(scenes) && scenes.length > 0 &&
        typeof CAPTIONS !== 'undefined' && typeof render === 'function' &&
        typeof TOTAL !== 'undefined' && typeof t === 'number');
    } catch (e0) { isDom = false; }
    if (!isDom) return 'nodom';
    if (window.NH && typeof window.NH.run === 'function') return 'nh-native';

    var SR = 48000;
    var PV = { master: 1, voice: 1, music: 0, sfx: 1 };
    var AT = new WeakMap();
    var holdLoop = false, heldLoop = null;
    var rafReal = window.requestAnimationFrame.bind(window);
    var segs = [];
    var TOTALN = TOTAL;
    var ITEMS = [];

    /* Disable background music completely in DOM HTML */
    try { window.musicStart = function () {}; window.musicOn = false; } catch (eM) {}

    /* ---------- HUD (page ke chhote buttons) app ke andar chhupa do ---------- */
    (function () {
      var st = document.createElement('style');
      st.id = 'h2v-dom-style';
      st.textContent = '#hud{display:none!important}';
      document.head.appendChild(st);
    })();

    /* ---------- requestAnimationFrame wrapper: export mein particle loop ko hum chalate hain ---------- */
    window.requestAnimationFrame = function (cb) {
      if (holdLoop && cb && cb.name === 'loop') { heldLoop = cb; return 0; }
      return rafReal(cb);
    };

    /* ---------- preview ki volume (WebAudio ka output ek gain se guzarta hai) ---------- */
    (function () {
      try {
        var oc = AudioNode.prototype.connect;
        AudioNode.prototype.connect = function (dest) {
          try {
            if (dest && typeof AudioDestinationNode !== 'undefined' && dest instanceof AudioDestinationNode &&
              !(typeof OfflineAudioContext !== 'undefined' && this.context instanceof OfflineAudioContext)) {
              var c = this.context;
              if (!c.__h2vVol) {
                var g = c.createGain();
                var v = PV.master * PV.sfx;
                try {
                  if (typeof ac !== 'undefined' && c === ac) v = 0; // Mute music context completely
                  else if (typeof sx !== 'undefined' && c === sx) v = PV.master * PV.sfx;
                } catch (e1) {}
                g.gain.value = v;
                oc.call(g, c.destination);
                c.__h2vVol = g;
              }
              arguments[0] = c.__h2vVol;
            }
          } catch (e2) {}
          return oc.apply(this, arguments);
        };
      } catch (e3) {}
    })();
    function applyVol() {
      try { if (typeof ac !== 'undefined' && ac && ac.__h2vVol) ac.__h2vVol.gain.value = 0; } catch (e) {}
      try { if (typeof sx !== 'undefined' && sx && sx.__h2vVol) sx.__h2vVol.gain.value = PV.master * PV.sfx; } catch (e) {}
    }
    setInterval(applyVol, 300);

    /* ---------- narration items (har caption = ek voice clip) ---------- */
    function clean(s) {
      return String(s).replace(/["\u201C\u201D]/g, '').replace(/_/g, ' ')
        .replace(/([a-z])([A-Z])/g, '$1 $2').replace(/\u2192/g, ' ').replace(/\s+/g, ' ').trim();
    }
    scenes.forEach(function (s, si) {
      var list = CAPTIONS[s.id] || [];
      list.forEach(function (c) {
        ITEMS.push({ idx: ITEMS.length, si: si, text: String(c[1]), abs: s.start + (+c[0]) });
      });
    });

    /* ---------- time warp: voice lambi ho to us caption ka hissa utna lamba ho jata hai ---------- */
    function buildWarp(durs) {
      segs = [];
      var ns = 0;
      scenes.forEach(function (s, si) {
        var list = ITEMS.filter(function (it) { return it.si === si; });
        var edges = [{ p: s.start, item: null }];
        list.forEach(function (it) { edges.push({ p: it.abs, item: it }); });
        for (var k = 0; k < edges.length; k++) {
          var os = edges[k].p;
          var oe = (k + 1 < edges.length) ? edges[k + 1].p : s.end;
          var L = oe - os;
          if (L <= 0) continue;
          var item = edges[k].item;
          var vd = item ? (durs[item.idx] || 0) : 0;
          var need = vd > 0 ? vd + (k === edges.length - 1 ? 0.7 : 0.35) : 0;
          var NL = Math.max(L, need);
          segs.push({ os: os, oe: oe, ns: ns, ne: ns + NL, L: L, NL: NL, item: item });
          ns += NL;
        }
      });
      TOTALN = ns;
    }
    function n2o(T) {
      if (T <= 0) return segs[0].os;
      for (var i = 0; i < segs.length; i++) {
        var g = segs[i];
        if (T < g.ne) return g.os + (T - g.ns) / g.NL * g.L;
      }
      return Math.max(0, segs[segs.length - 1].oe - 0.001);
    }
    function o2n(x) {
      for (var i = 0; i < segs.length; i++) {
        var g = segs[i];
        if (x < g.oe) return g.ns + Math.max(0, x - g.os) / g.L * g.NL;
      }
      return TOTALN;
    }
    buildWarp([]);

    /* ---------- helpers ---------- */
    function sleepMs(ms) { return new Promise(function (r) { setTimeout(r, ms); }); }
    function settle() {
      return new Promise(function (res) {
        var done = false;
        function fin() { if (!done) { done = true; res(); } }
        try { window.requestAnimationFrame(function () { setTimeout(fin, 0); }); } catch (e) {}
        setTimeout(fin, 250);
      });
    }
    function flush() { void document.documentElement.offsetHeight; }
    function finishFinite() {
      document.getAnimations().forEach(function (a) {
        try {
          var tm = a.effect && a.effect.getComputedTiming();
          if (tm && isFinite(tm.endTime)) a.finish();
        } catch (e) {}
      });
    }
    function releaseAnims() {
      document.getAnimations().forEach(function (a) {
        try { if (a.playState === 'paused') a.play(); } catch (e) {}
      });
    }
    function resetPageState() {
      scenes.forEach(function (s) {
        s.el.classList.remove('active');
        s.rv.forEach(function (n) { n.classList.remove('on'); });
        s.seq.forEach(function (kids) { kids.forEach(function (k) { k.classList.remove('cur', 'done'); }); });
      });
      try { stage.classList.remove('lb'); } catch (e) {}
      try { capEl.classList.add('hide'); } catch (e) {}
      flush();
      finishFinite();
      t = 0; prevT = 0; lastSceneId = ''; lastCapKey = '';
    }
    function stepParticles(fps) {
      var k = Math.max(1, Math.round(60 / fps));
      for (var i = 0; i < k; i++) {
        if (heldLoop) { var f = heldLoop; heldLoop = null; try { f(); } catch (e) {} }
      }
    }
    function seekFrame(Tn) {
      t = n2o(Tn);
      playing = false;
      render();
      flush();
      var list = document.getAnimations();
      for (var i = 0; i < list.length; i++) {
        var a = list[i];
        if (!AT.has(a)) AT.set(a, Tn);
        try { a.pause(); a.currentTime = (Tn - AT.get(a)) * 1000; } catch (e) {}
      }
    }
    var expStyle = null;
    function setExportStyle(on) {
      if (on) {
        if (!expStyle) {
          expStyle = document.createElement('style');
          expStyle.id = 'h2v-export-style';
          expStyle.textContent = '#grain{animation:none!important;transform:none!important}';
          document.head.appendChild(expStyle);
        }
      } else if (expStyle) { expStyle.remove(); expStyle = null; }
    }
    async function prepareExport() {
      setExportStyle(true);
      playing = false;
      try { musicStop(); } catch (e) {}
      try { speechSynthesis.cancel(); } catch (e) {}
      try { overlay.style.display = 'none'; } catch (e) {}
      holdLoop = true;
      await settle(); await settle();
      resetPageState();
      AT = new WeakMap();
    }
    function endExport() {
      setExportStyle(false);
      holdLoop = false;
      if (heldLoop) { var f = heldLoop; heldLoop = null; try { rafReal(f); } catch (e) {} }
      releaseAnims();
    }

    /* ---------- SFX Only (Music completely removed) ---------- */
    var WIN = 60;
    var NODES = 0, PEAK = {}, SFXERR = 0, SFXLAST = '';
    function S(tm, rate) { return Math.round(tm * rate); }

    function renderSfxWin(OAC, len, rate, st, en) {
      var off = new OAC(1, len, rate);
      var out = off.createGain(); out.gain.value = 0.65;
      var comp = off.createDynamicsCompressor(); out.connect(comp); comp.connect(off.destination);
      var nb = off.createBuffer(1, off.sampleRate * 2, off.sampleRate);
      var d = nb.getChannelData(0);
      for (var i = 0; i < d.length; i++) d[i] = Math.random() * 2 - 1;
      var CUR = 0, CURO = 0, lastAt = {};
      var proxy = {
        createOscillator: function () { NODES++; return off.createOscillator(); },
        createGain: function () { return off.createGain(); },
        createBufferSource: function () { NODES++; return off.createBufferSource(); },
        createBiquadFilter: function () { return off.createBiquadFilter(); },
        get currentTime() { return CUR; },
        sampleRate: rate, state: 'running'
      };
      var Sv = {
        t: t, prevT: prevT, playing: playing, lsi: lastSceneId, lck: lastCapKey, vo: voiceOn, so: sfxOn,
        sx: sx, sxo: sxOut, nb: noiseBuf, ok: window.ok, riser: sfx.riser
      };
      var origRiser = sfx.riser;
      var step = 1 / 60;
      try {
        resetPageState();
        sx = proxy; sxOut = out; noiseBuf = nb; sfxOn = true; voiceOn = false;
        window.ok = function (name, gap) {
          var ms = CURO * 1000;
          if (lastAt[name] !== undefined && ms - lastAt[name] < gap) return false;
          lastAt[name] = ms; return true;
        };
        sfx.riser = function (dd) {
          dd = dd || 2.4;
          var r = (o2n(CURO + dd) - o2n(CURO)) / dd;
          return origRiser.call(sfx, dd * (r > 0 ? r : 1));
        };
        var x0 = st > 0 ? n2o(st) : 0, x1 = (en >= TOTALN) ? TOTAL : n2o(en);
        if (x0 > 0) {
          playing = false; t = Math.max(0, x0 - step); render();
        }
        playing = true;
        for (var x = x0; x <= x1; x += step) {
          CURO = x; CUR = o2n(x) - st; t = x; render();
        }
      } finally {
        sx = Sv.sx; sxOut = Sv.sxo; noiseBuf = Sv.nb; sfxOn = Sv.so; voiceOn = Sv.vo; playing = Sv.playing;
        window.ok = Sv.ok; sfx.riser = origRiser;
        resetPageState();
      }
      return off.startRendering();
    }

    function b64(u8) {
      var s = '', n = 8192;
      for (var i = 0; i < u8.length; i += n) s += String.fromCharCode.apply(null, u8.subarray(i, i + n));
      return btoa(s);
    }
    async function sendSlice(name, ch, from, to, rate) {
      for (var p = from; p < to; p += rate) {
        var m = Math.min(rate, to - p), i16 = new Int16Array(m);
        for (var i = 0; i < m; i++) {
          var v = ch[p + i];
          v = v < -1 ? -1 : (v > 1 ? 1 : v);
          i16[i] = Math.round(v * 32767);
          var av = v < 0 ? -v : v;
          if (av > (PEAK[name] || 0)) PEAK[name] = av;
        }
        Android.audioChunk(name, b64(new Uint8Array(i16.buffer)));
        await sleepMs(0);
      }
    }
    async function sendSfx(OAC, rate) {
      var total = TOTALN, N = S(total, rate), OV = 8;
      Android.audioBegin('sfx', N, rate);
      for (var a = 0; a < total; a += WIN) {
        var b = Math.min(total, a + WIN), st = Math.max(0, a - OV);
        var s0 = S(st, rate), ia = S(a, rate), ib = (b >= total) ? N : S(b, rate);
        var ch;
        try {
          var buf = await renderSfxWin(OAC, ib - s0 + 2, rate, st, b);
          ch = buf.getChannelData(0);
        } catch (e) {
          SFXERR++; SFXLAST = String(e && e.message ? e.message : e);
          ch = new Float32Array(ib - s0 + 2);
        }
        await sendSlice('sfx', ch, ia - s0, ib - s0, rate);
      }
      if (SFXERR > 0 && SFXERR >= Math.ceil(total / WIN)) throw new Error('SFX render fail: ' + SFXLAST);
    }

    /* Extract DOM SFX cues mapped to voice-warped timeline o2n(x) */
    window.__h2vExtractDomCues = function () {
      var cues = [];
      scenes.forEach(function (s, idx) {
        cues.push({ t: Math.round(o2n(s.start) * 1000) / 1000, k: 'whoosh', d: idx === 0 ? 1.0 : 0.78 });
        s.rv.forEach(function (n) {
          var at = s.start + (+n.dataset.at || 0);
          var tn = Math.round(o2n(at) * 1000) / 1000;
          var c = n.classList, p = n.parentElement ? n.parentElement.classList : null;
          var k = 'popup';
          if (c.contains('letter') || (p && (p.contains('log') || p.contains('code')))) k = 'key';
          else if (c.contains('apk')) k = 'success';
          else if (c.contains('shield')) k = 'lock';
          else if (c.contains('logo') || c.contains('banner')) k = 'hit';
          else if (c.contains('bell')) k = 'notif';
          else if (c.contains('subbtn') || c.contains('step')) k = 'click';
          else if (c.contains('phone') || c.contains('admin') || c.contains('buildbox') || c.contains('code') || c.contains('left') || c.contains('right')) k = 'slide';
          else if (c.contains('tr') || c.contains('nav') || c.contains('sc') || c.contains('card')) k = 'tick';
          else if (c.contains('sec') || c.contains('sub') || c.contains('tag') || c.contains('endline')) return;
          cues.push({ t: tn, k: k });
        });
      });
      if (typeof CUES !== 'undefined' && Array.isArray(CUES)) {
        CUES.forEach(function (c) {
          if (c && typeof c.at === 'number') cues.push({ t: Math.round(o2n(c.at) * 1000) / 1000, k: 'riser', d: 2.0 });
        });
      }
      cues.sort(function (a, b) { return a.t - b.t; });
      return JSON.stringify(cues);
    };

    /* ---------- NH API ---------- */
    window.NH = {
      lang: 'hi-IN',
      domMode: true,
      narration: function () {
        return JSON.stringify(ITEMS.map(function (it) { return { a: clean(it.text), b: '' }; }));
      },
      setDurations: function (j) {
        var d = JSON.parse(j);
        var durs = ITEMS.map(function (it, i) { return (d[i] && d[i].a > 0) ? +d[i].a : 0; });
        buildWarp(durs);
        var ev = [];
        segs.forEach(function (g) {
          if (g.item && durs[g.item.idx] > 0) ev.push({ key: g.item.idx + 'a', at: g.ns + 0.08 });
        });
        return JSON.stringify({ total: TOTALN, events: ev, stems: true });
      },
      renderAudio: async function () {
        try {
          var OAC = window.OfflineAudioContext || window.webkitOfflineAudioContext;
          if (!OAC) throw new Error('OfflineAudioContext is WebView mein nahi hai');
          var SRT = 24000;
          NODES = 0; PEAK = {}; SFXERR = 0; SFXLAST = '';
          await sendSfx(OAC, SRT);
          Android.audioInfo('sfx', PEAK.sfx || 0, NODES);
          Android.audioInfo('sfxerr', SFXERR, 0);
          Android.audioDone();
        } catch (e) {
          Android.audioFail(String(e && e.message ? e.message : e));
        }
      },
      run: async function (fps) {
        try {
          await prepareExport();
          var N = Math.ceil(TOTALN * fps);
          for (var n = 0; n < N; n++) {
            var Tn = n / fps;
            stepParticles(fps);
            seekFrame(Tn);
            await settle();
            if (Android.captureFrame(n) === false) { Android.fail('Cancel ho gaya'); return; }
            if (n % 10 === 0) { Android.progress(n, N); await sleepMs(0); }
          }
          Android.progress(N, N);
          Android.done();
        } catch (e) {
          Android.fail(String(e && e.message ? e.message : e));
        } finally {
          endExport();
        }
      },
      endExport: function () { endExport(); window.__h2vPrep = false; return 'ok'; },
      seekTo: async function (Tn) {
        if (!window.__h2vPrep) { await prepareExport(); window.__h2vPrep = true; }
        stepParticles(30);
        seekFrame(Tn);
        await settle();
        return 'ok';
      },
      preview: function () {
        try {
          if (playing) {
            playing = false;
            try { musicStop(); } catch (e) {}
            try { speechSynthesis.cancel(); } catch (e) {}
            return 'stopped';
          }
          endExport();
          resetPageState();
          lastCapKey = '';
          musicOn = false;
          startPlay();
          applyVol();
          return 'playing';
        } catch (e) { return 'err:' + e.message; }
      },
      setVolumes: function (m, v, mu, s) {
        PV.master = +m; PV.voice = +v; PV.music = 0; PV.sfx = +s;
        applyVol();
        return 'ok';
      }
    };
    window.__h2vDom = true;
    return 'dom';
  } catch (err) {
    return 'err:' + (err && err.message ? err.message : err);
  }
})()
