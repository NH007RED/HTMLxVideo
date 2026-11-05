(function () {
  /* HTML2Video v3.5: Universal Canvas & DOM SFX Extractor + Live Preview SFX Engine (No Background Music).
     Works automatically with every HTML2Video showcase/tutorial HTML file. */
  try {
    if (window.__h2vSfxReady) return 'sfx-exists';
    window.__h2vSfxReady = true;

    /* 1. Completely disable any background music in the page */
    window.bedSched = function () {};
    window.musicStart = function () {};
    try { window.musicOn = false; } catch (e0) {}
    try {
      var sfInput = document.getElementById('sf');
      if (sfInput && sfInput.parentElement) {
        var nodes = sfInput.parentElement.childNodes;
        for (var ni = 0; ni < nodes.length; ni++) {
          if (nodes[ni].nodeType === 3 && /music/i.test(nodes[ni].nodeValue)) {
            nodes[ni].nodeValue = ' SFX (browser preview)';
          }
        }
      }
      var bm = document.getElementById('bMusic');
      if (bm) bm.style.display = 'none';
    } catch (e1) {}

    /* 2. Complete WebAudio SFX Synthesizer (for Live Preview & WAV Download) */
    function mkNoise(ctx) {
      var n = ctx.sampleRate * 2, b = ctx.createBuffer(1, n, ctx.sampleRate), d = b.getChannelData(0), s = 1234567;
      for (var i = 0; i < n; i++) { s = (s * 1664525 + 1013904223) >>> 0; d[i] = (s / 4294967296) * 2 - 1; }
      return b;
    }
    function safeT(x, t) {
      var minT = (x && x.ctx && typeof x.ctx.currentTime === 'number') ? x.ctx.currentTime : 0;
      return Math.max(minT + 0.001, t || 0);
    }
    function envG(g, t, a, d, pk) {
      g.gain.setValueAtTime(0.0001, t);
      g.gain.exponentialRampToValueAtTime(Math.max(0.0002, pk), t + a);
      g.gain.exponentialRampToValueAtTime(0.0001, t + a + d);
    }
    function tone(x, type, f, t, d, v, f2) {
      t = safeT(x, t);
      var o = x.ctx.createOscillator(), g = x.ctx.createGain();
      o.type = type;
      o.frequency.setValueAtTime(Math.max(20, f), t);
      if (f2) o.frequency.exponentialRampToValueAtTime(Math.max(20, f2), t + d);
      envG(g, t, 0.005, d, v);
      o.connect(g); g.connect(x.out);
      o.start(t); o.stop(t + d + 0.04);
    }
    function nz(x, t, d, v, ft, fq, q, off) {
      t = safeT(x, t);
      var n = x.ctx.createBufferSource(); n.buffer = x.noise;
      var f = x.ctx.createBiquadFilter(); f.type = ft; f.frequency.value = fq; f.Q.value = q || 0.7;
      var g = x.ctx.createGain(); envG(g, t, 0.003, d, v);
      n.connect(f); f.connect(g); g.connect(x.out);
      n.start(t, Math.max(0, (off || 0) % 1.5), d + 0.04);
      return f;
    }

    var FULL_SFX = {
      whoosh: function (x, t, e) {
        t = safeT(x, t);
        var d = (e && e.d) ? e.d : 0.78, vol = (e && e.v) ? e.v * 0.32 : 0.30;
        var n = x.ctx.createBufferSource(); n.buffer = x.noise;
        var f = x.ctx.createBiquadFilter(); f.type = 'bandpass'; f.Q.value = 1.1;
        f.frequency.setValueAtTime(260, t);
        f.frequency.exponentialRampToValueAtTime(3200, t + d * 0.6);
        f.frequency.exponentialRampToValueAtTime(480, t + d);
        var g = x.ctx.createGain(); envG(g, t, d * 0.42, d * 0.58, vol);
        n.connect(f); f.connect(g); g.connect(x.out);
        n.start(t, 0, d + 0.05);
      },
      slide: function (x, t) {
        t = safeT(x, t);
        var d = 0.18, n = x.ctx.createBufferSource(); n.buffer = x.noise;
        var f = x.ctx.createBiquadFilter(); f.type = 'bandpass'; f.Q.value = 1.3;
        f.frequency.setValueAtTime(420, t);
        f.frequency.exponentialRampToValueAtTime(2400, t + d * 0.55);
        f.frequency.exponentialRampToValueAtTime(800, t + d);
        var g = x.ctx.createGain(); envG(g, t, 0.03, d - 0.03, 0.24);
        n.connect(f); f.connect(g); g.connect(x.out);
        n.start(t, 0, d + 0.04);
        tone(x, 'sine', 240, t, 0.11, 0.16, 480);
      },
      popup: function (x, t) {
        t = safeT(x, t);
        tone(x, 'sine', 340, t, 0.085, 0.26, 820);
        tone(x, 'triangle', 680, t + 0.01, 0.065, 0.10, 1560);
      },
      pop: function (x, t) { FULL_SFX.popup(x, t); },
      key: function (x, t) {
        t = safeT(x, t);
        var det = 1 + 0.11 * Math.sin(t * 43.7);
        nz(x, t, 0.024, 0.22, 'highpass', 2200 * det, 0.85, (t * 17) % 1.4);
        tone(x, 'triangle', 1300 * det, t, 0.026, 0.14, 460);
      },
      type: function (x, t, e) {
        t = safeT(x, t);
        var n = (e && e.n) ? e.n : 10, gp = (e && e.gap) ? e.gap : 0.072;
        for (var k = 0; k < n; k++) {
          FULL_SFX.key(x, t + k * gp + ((k * 37) % 5) * 0.004);
        }
      },
      tap: function (x, t) {
        t = safeT(x, t);
        tone(x, 'sine', 320, t, 0.09, 0.30, 135);
        nz(x, t, 0.02, 0.15, 'highpass', 2300, 0.7, (t * 19) % 1.4);
      },
      click: function (x, t) {
        t = safeT(x, t);
        nz(x, t, 0.022, 0.22, 'highpass', 2100, 0.7, (t * 11) % 1.4);
        tone(x, 'sine', 1550, t, 0.042, 0.16, 820);
      },
      step: function (x, t) { FULL_SFX.click(x, t); },
      tick: function (x, t) {
        t = safeT(x, t);
        nz(x, t, 0.025, 0.18, 'highpass', 2500, 0.7, (t * 13) % 1.4);
        tone(x, 'sine', 2100, t, 0.03, 0.12, 1600);
      },
      chime: function (x, t, e) {
        t = safeT(x, t);
        var N = (e && Array.isArray(e.notes) && e.notes.length) ? e.notes : [523.25, 659.25, 1046.5];
        N.forEach(function (f, k) {
          tone(x, 'sine', f, t + k * 0.095, 0.75, 0.20);
          tone(x, 'sine', f * 2, t + k * 0.095, 0.45, 0.06);
        });
      },
      success: function (x, t) {
        t = safeT(x, t);
        [523.25, 659.25, 783.99, 1046.5].forEach(function (f, k) {
          tone(x, 'triangle', f, t + k * 0.078, 0.48, 0.22);
          tone(x, 'sine', f * 2, t + k * 0.078, 0.32, 0.06);
        });
      },
      notif: function (x, t) {
        t = safeT(x, t);
        [987.77, 1318.51].forEach(function (f, k) {
          tone(x, 'triangle', f, t + k * 0.105, 0.36, 0.22);
        });
      },
      bell: function (x, t) { FULL_SFX.notif(x, t); },
      copy: function (x, t) {
        t = safeT(x, t);
        FULL_SFX.click(x, t);
        tone(x, 'sine', 580, t + 0.035, 0.08, 0.24, 1100);
      },
      lock: function (x, t) {
        t = safeT(x, t);
        tone(x, 'square', 190, t, 0.07, 0.16, 115);
        tone(x, 'sine', 95, t, 0.16, 0.28, 65);
        tone(x, 'triangle', 1320, t + 0.095, 0.35, 0.18);
      },
      alarm: function (x, t) { FULL_SFX.lock(x, t); },
      riser: function (x, t, e) {
        t = safeT(x, t);
        var d = (e && e.d) ? e.d : 2.0;
        var o = x.ctx.createOscillator(), f = x.ctx.createBiquadFilter(), g = x.ctx.createGain();
        o.type = 'sawtooth';
        o.frequency.setValueAtTime(80, t);
        o.frequency.exponentialRampToValueAtTime(680, t + d);
        f.type = 'lowpass';
        f.frequency.setValueAtTime(220, t);
        f.frequency.exponentialRampToValueAtTime(4800, t + d);
        g.gain.setValueAtTime(0.0001, t);
        g.gain.exponentialRampToValueAtTime(0.16, t + d);
        g.gain.exponentialRampToValueAtTime(0.0001, t + d + 0.12);
        o.connect(f); f.connect(g); g.connect(x.out);
        o.start(t); o.stop(t + d + 0.15);
        FULL_SFX.whoosh(x, t, { d: d, v: 0.75 });
      },
      hit: function (x, t) {
        t = safeT(x, t);
        tone(x, 'sine', 74, t, 1.35, 0.48, 34);
        nz(x, t, 0.42, 0.30, 'lowpass', 420, 0.7, 0);
        [261.63, 329.63, 392.0, 523.25].forEach(function (f) {
          tone(x, 'triangle', f, t + 0.04, 1.4, 0.09);
        });
      },
      impact: function (x, t) { FULL_SFX.hit(x, t); }
    };

    window.SFX = Object.assign(window.SFX || {}, FULL_SFX);

    /* 3. Universal Cue Extractor (combines explicit sfxPlan + automatic Canvas & DOM animation scanning) */
    var origSfxPlan = typeof window.sfxPlan === 'function' ? window.sfxPlan : null;
    var cachedCues = null;
    var cachedTotal = -1;

    function extractAllCuesArray() {
      try {
        if (typeof buildPlan === 'function' && !window.__h2vScanning) {
          try { buildPlan(); } catch (eBp) {}
        }
        var curTotal = typeof TOTAL === 'number' ? TOTAL : 0;
        if (cachedCues && Math.abs(curTotal - cachedTotal) < 0.01 && curTotal > 0) {
          return cachedCues;
        }
        window.__h2vScanning = true;
        var cues = [];
        var explicitTypeRanges = [];

        function addCue(t, k, extra) {
          if (typeof t !== 'number' || isNaN(t) || t < 0) return;
          var o = { t: Math.round(t * 1000) / 1000, k: k };
          if (extra) {
            if (typeof extra.d === 'number') o.d = extra.d;
            if (typeof extra.v === 'number') o.v = extra.v;
            if (Array.isArray(extra.notes)) o.notes = extra.notes;
          }
          cues.push(o);
        }

        // A. Explicit sfxPlan() from the HTML page (if present)
        if (origSfxPlan) {
          try {
            var sp = origSfxPlan();
            if (Array.isArray(sp)) {
              sp.forEach(function (e) {
                if (!e || typeof e.t !== 'number' || !e.k) return;
                if (e.k === 'type') {
                  var n = e.n || 10, gp = e.gap || 0.072;
                  explicitTypeRanges.push([e.t - 0.15, e.t + n * gp + 0.15]);
                  for (var k = 0; k < n; k++) {
                    addCue(e.t + k * gp, 'key');
                  }
                } else {
                  addCue(e.t, e.k, e);
                }
              });
            }
          } catch (eSp) {}
        }

        // B. Scene transitions from PLAN
        if (typeof PLAN !== 'undefined' && Array.isArray(PLAN) && PLAN.length > 0) {
          PLAN.forEach(function (p, idx) {
            if (p && typeof p.start === 'number') {
              addCue(p.start + 0.05, 'whoosh', { d: idx === 0 ? 1.1 : 0.76 });
              if (idx === 0) addCue(p.start + 0.45, 'chime');
              if (p.len && p.len > 2.2) {
                addCue(p.start + p.len - 0.38, 'whoosh', { d: 0.45, v: 0.55 });
              }
            }
          });
        }

        // C. Fast dry-run scan across frameAt(T) for Canvas HTMLs
        var realFrameAt = window.__h2vOrigFrameAt || window.frameAt;
        if (typeof realFrameAt === 'function' && curTotal > 0) {
          var seen = {};
          var lastTime = {};
          var prevTx = {};
          var prevFld = {};
          var prevBlur = {};
          var prevOsd = '';
          var prevScene = -1;
          var insideWrap = 0;
          var curT = 0;

          function cool(key, gap) {
            if (lastTime[key] !== undefined && curT - lastTime[key] < gap) return false;
            lastTime[key] = curT;
            return true;
          }
          function inExplicitType(t) {
            for (var i = 0; i < explicitTypeRanges.length; i++) {
              if (t >= explicitTypeRanges[i][0] && t <= explicitTypeRanges[i][1]) return true;
            }
            return false;
          }

          var orig = {};
          var fnNames = [
            'sideCard', 'cbBanner', 'pop', 'panel', 'osd', 'hlBox', 'mark', 'markS',
            'sheetBase', 'pickScr', 'sheet', 'browser', 'tap', 'ripple', 'btn', 'ghBtn',
            'field', 'ghField', 'tx', 'blurBar', 'designerAppScr', 'promptAiAppScr',
            'exportPngAppScr', 'loadingScr2', 'homeAppScr', 'fixesSheet', 'tile', 'button'
          ];
          fnNames.forEach(function (nm) {
            if (typeof window[nm] === 'function') orig[nm] = window[nm];
          });

          var noopFns = ['backdrop', 'caption', 'rr', 'sbar', 'bottomNavBar', 'ico', 'icoX', 'glow', 'noglow', 'gradH', 'gradDiag'];
          var savedNoops = {};
          noopFns.forEach(function (fn) {
            if (typeof window[fn] === 'function') {
              savedNoops[fn] = window[fn];
              window[fn] = function () {};
            }
          });

          var origC = window.c, origCtx = window.ctx;
          var fastC = {
            save: function () {}, restore: function () {}, translate: function () {}, scale: function () {},
            rotate: function () {}, setTransform: function () {}, beginPath: function () {}, moveTo: function () {},
            lineTo: function () {}, arcTo: function () {}, closePath: function () {}, arc: function () {},
            rect: function () {}, clip: function () {}, fill: function () {}, stroke: function () {},
            fillRect: function () {}, strokeRect: function () {}, clearRect: function () {}, fillText: function () {},
            drawImage: function () {}, setLineDash: function () {},
            bezierCurveTo: function () {}, quadraticCurveTo: function () {},
            measureText: function (s) { return { width: String(s).length * 9 }; },
            createLinearGradient: function () { return { addColorStop: function () {} }; },
            createRadialGradient: function () { return { addColorStop: function () {} }; }
          };
          try { if (window.c) window.c = fastC; } catch (eC1) {}
          try { if (window.ctx) window.ctx = fastC; } catch (eC2) {}

          if (orig.sideCard) {
            window.sideCard = function (x, y, w, h, a, title) {
              if (a > 0.02) {
                var k = 'sc:' + prevScene + ':' + (title || (Math.round(x) + ':' + Math.round(y)));
                if (!seen[k]) { seen[k] = 1; if (cool('slide', 0.12)) addCue(curT, 'slide'); }
              }
              insideWrap++;
              try { return orig.sideCard.apply(this, arguments); } finally { insideWrap--; }
            };
          }
          if (orig.cbBanner) {
            window.cbBanner = function (x, y, w, h, a, col, icon, title) {
              if (a > 0.02) {
                var k = 'cb:' + prevScene + ':' + (title || Math.round(y));
                if (!seen[k]) { seen[k] = 1; if (cool('popup', 0.12)) addCue(curT, 'popup'); }
              }
              insideWrap++;
              try { return orig.cbBanner.apply(this, arguments); } finally { insideWrap--; }
            };
          }
          if (orig.pop) {
            window.pop = function (cx, cy, a, fn) {
              if (a > 0.02) {
                var k = 'pop:' + prevScene + ':' + Math.round(cx) + ':' + Math.round(cy);
                if (!seen[k]) { seen[k] = 1; if (cool('popup', 0.09)) addCue(curT, 'popup'); }
              }
              insideWrap++;
              try { return orig.pop.apply(this, arguments); } finally { insideWrap--; }
            };
          }
          if (orig.panel) {
            window.panel = function (x, y, w, h, a) {
              if (insideWrap === 0 && a > 0.02) {
                var k = 'pn:' + prevScene + ':' + Math.round(x) + ':' + Math.round(y) + ':' + Math.round(w);
                if (!seen[k]) { seen[k] = 1; if (cool('popup', 0.12)) addCue(curT, 'popup'); }
              }
              return orig.panel.apply(this, arguments);
            };
          }
          if (orig.osd) {
            window.osd = function (items, q) {
              var cur = null;
              if (Array.isArray(items)) items.forEach(function (it) { if (q >= it[0]) cur = it; });
              if (cur && cur[1] !== prevOsd) {
                prevOsd = cur[1];
                if (cur[0] > 0.02 && cool('osd', 0.18)) addCue(curT, 'popup');
              }
              return orig.osd.apply(this, arguments);
            };
          }
          if (orig.hlBox) {
            window.hlBox = function (x, y, w, h, a) {
              if (a > 0.02) {
                var k = 'hl:' + prevScene + ':' + Math.round(x) + ':' + Math.round(y) + ':' + Math.round(w);
                if (!seen[k]) { seen[k] = 1; if (cool('popup', 0.12)) addCue(curT, 'popup'); }
              }
              return orig.hlBox.apply(this, arguments);
            };
          }
          if (orig.mark) {
            window.mark = function (r, al) {
              if (al > 0.02 && r) {
                var k = 'mk:' + prevScene + ':' + Math.round(r[0]) + ':' + Math.round(r[1]);
                if (!seen[k]) { seen[k] = 1; if (cool('popup', 0.15)) addCue(curT, 'popup'); }
              }
              return orig.mark.apply(this, arguments);
            };
          }
          if (orig.markS) {
            window.markS = function (r, c, al) {
              if (al > 0.02 && r) {
                var k = 'mks:' + prevScene + ':' + Math.round(r[0]) + ':' + Math.round(r[1]);
                if (!seen[k]) { seen[k] = 1; if (cool('popup', 0.15)) addCue(curT, 'popup'); }
              }
              return orig.markS.apply(this, arguments);
            };
          }
          if (orig.sheetBase) {
            window.sheetBase = function (T, p, title, h) {
              if (p > 0.02) {
                var k = 'shb:' + prevScene + ':' + (title || h);
                if (!seen[k]) { seen[k] = 1; if (cool('slide', 0.18)) addCue(curT, 'slide'); }
              }
              return orig.sheetBase.apply(this, arguments);
            };
          }
          if (orig.pickScr) {
            window.pickScr = function (T, p) {
              if (p > 0.02) {
                var k = 'pks:' + prevScene;
                if (!seen[k]) { seen[k] = 1; if (cool('slide', 0.18)) addCue(curT, 'slide'); }
              }
              return orig.pickScr.apply(this, arguments);
            };
          }
          if (orig.sheet) {
            window.sheet = function (title, sub, u, at) {
              if (u >= at) {
                var k = 'sht:' + prevScene + ':' + title;
                if (!seen[k]) { seen[k] = 1; if (cool('slide', 0.18)) addCue(curT, 'slide'); }
              }
              return orig.sheet.apply(this, arguments);
            };
          }
          if (orig.browser) {
            window.browser = function (x, y, w, h, url, a) {
              if (a > 0.02) {
                var k = 'brw:' + prevScene + ':' + url;
                if (!seen[k]) { seen[k] = 1; if (cool('slide', 0.2)) addCue(curT, 'slide'); }
              }
              return orig.browser.apply(this, arguments);
            };
          }
          if (orig.tap) {
            window.tap = function (x, y, p) {
              var k = 'tap:' + prevScene + ':' + Math.round(x / 8) + ':' + Math.round(y / 8);
              if (p > 0.005 && p < 0.95) {
                if (!seen[k]) { seen[k] = 1; if (cool('anytap', 0.12)) addCue(curT, 'tap'); }
              } else {
                seen[k] = 0;
              }
              return orig.tap.apply(this, arguments);
            };
          }
          if (orig.ripple) {
            window.ripple = function (x, y, dt) {
              var k = 'rip:' + prevScene + ':' + Math.round(x / 10) + ':' + Math.round(y / 10);
              if (dt >= 0 && dt < 0.55) {
                if (!seen[k]) { seen[k] = 1; if (cool('anytap', 0.12)) addCue(curT, 'tap'); }
              } else {
                seen[k] = 0;
              }
              return orig.ripple.apply(this, arguments);
            };
          }
          if (orig.btn) {
            window.btn = function (x, y, w, h, label, pri, pr) {
              var kp = 'btnp:' + prevScene + ':' + label;
              if ((pr || 0) > 0.005) {
                if (!seen[kp]) { seen[kp] = 1; if (cool('anyclick', 0.12)) addCue(curT, 'click'); }
              } else {
                seen[kp] = 0;
              }
              if (typeof label === 'string' && (/^✓|Active ✓|Copied|Saved/.test(label))) {
                var ks = 'btns:' + prevScene + ':' + label;
                if (!seen[ks]) { seen[ks] = 1; if (cool('success', 0.25)) addCue(curT, 'success'); }
              }
              return orig.btn.apply(this, arguments);
            };
          }
          if (orig.ghBtn) {
            window.ghBtn = function (x, y, w, h, label, col, pr) {
              var kp = 'ghbp:' + prevScene + ':' + label;
              if ((pr || 0) > 0.005) {
                if (!seen[kp]) { seen[kp] = 1; if (cool('anyclick', 0.12)) addCue(curT, 'click'); }
              } else {
                seen[kp] = 0;
              }
              return orig.ghBtn.apply(this, arguments);
            };
          }
          if (orig.field) {
            window.field = function (x, y, w, label, val) {
              var s = String(val || '');
              var kf = 'fld:' + prevScene + ':' + label;
              var pv = prevFld[kf] || '';
              if (s.length > pv.length && !inExplicitType(curT)) {
                if (cool('key', 0.045)) addCue(curT, 'key');
              }
              prevFld[kf] = s;
              return orig.field.apply(this, arguments);
            };
          }
          if (orig.ghField) {
            window.ghField = function (x, y, w, label, val) {
              var s = String(val || '');
              var kf = 'ghf:' + prevScene + ':' + label;
              var pv = prevFld[kf] || '';
              if (s.length > pv.length && !inExplicitType(curT)) {
                if (cool('key', 0.045)) addCue(curT, 'key');
              }
              prevFld[kf] = s;
              return orig.ghField.apply(this, arguments);
            };
          }
          if (orig.blurBar) {
            window.blurBar = function (x, y, w) {
              var kb = 'blb:' + prevScene + ':' + Math.round(x) + ':' + Math.round(y);
              var pw = prevBlur[kb] || 0;
              if (w > pw + 3 && !inExplicitType(curT)) {
                if (cool('key', 0.055)) addCue(curT, 'key');
              }
              prevBlur[kb] = w;
              return orig.blurBar.apply(this, arguments);
            };
          }
          if (orig.loadingScr2) {
            window.loadingScr2 = function () {
              var kl = 'ld2:' + prevScene;
              if (!seen[kl]) { seen[kl] = 1; if (cool('slide', 0.2)) addCue(curT, 'slide'); }
              return orig.loadingScr2.apply(this, arguments);
            };
          }
          if (orig.homeAppScr) {
            window.homeAppScr = function () {
              var kh = 'hma:' + prevScene;
              if (!seen[kh]) { seen[kh] = 1; if (cool('slide', 0.2)) addCue(curT, 'slide'); }
              return orig.homeAppScr.apply(this, arguments);
            };
          }
          var prevDes = {};
          if (orig.designerAppScr) {
            window.designerAppScr = function (T, o) {
              o = o || {};
              var kd = 'des:' + prevScene;
              var pd = prevDes[kd];
              if (pd) {
                if (o.frameStyle !== pd.frameStyle) { if (cool('click', 0.15)) addCue(curT, 'click'); }
                if (o.themeIdx !== pd.themeIdx) { if (cool('slide', 0.15)) addCue(curT, 'slide'); }
                if ((o.numEl || 5) > (pd.numEl || 5)) { if (cool('popup', 0.15)) addCue(curT, 'popup'); }
                if (o.panelMode !== pd.panelMode) { if (cool('slide', 0.15)) addCue(curT, 'slide'); }
                if (Boolean(o.hasGalleryPhoto) && !Boolean(pd.hasGalleryPhoto)) { if (cool('popup', 0.15)) addCue(curT, 'popup'); }
                if (Boolean(o.fitMode) !== Boolean(pd.fitMode)) { if (cool('click', 0.15)) addCue(curT, 'click'); }
                if ((o.subTab || 0) !== (pd.subTab || 0)) { if (cool('slide', 0.15)) addCue(curT, 'slide'); }
                if ((o.navTabs || 4) !== (pd.navTabs || 4)) { if (cool('click', 0.15)) addCue(curT, 'click'); }
                if (o.customColor !== pd.customColor && o.customColor) { if (cool('popup', 0.15)) addCue(curT, 'popup'); }
                if (Boolean(o.slimPreset) && !Boolean(pd.slimPreset)) { if (cool('tap', 0.15)) addCue(curT, 'tap'); }
                if ((o.nudgeX || 0) !== (pd.nudgeX || 0)) { if (cool('tick', 0.12)) addCue(curT, 'tick'); }
                if (Boolean(o.locked) && !Boolean(pd.locked)) { if (cool('lock', 0.2)) addCue(curT, 'lock'); }
                if (Boolean(o.showExplainBox) && !Boolean(pd.showExplainBox)) { if (cool('popup', 0.15)) addCue(curT, 'popup'); }
                if (Boolean(o.previewMode) && !Boolean(pd.previewMode)) { if (cool('slide', 0.15)) addCue(curT, 'slide'); }
              }
              prevDes[kd] = {
                frameStyle: o.frameStyle, themeIdx: o.themeIdx, numEl: o.numEl,
                panelMode: o.panelMode, hasGalleryPhoto: o.hasGalleryPhoto, fitMode: o.fitMode,
                subTab: o.subTab, navTabs: o.navTabs, customColor: o.customColor,
                slimPreset: o.slimPreset, nudgeX: o.nudgeX, locked: o.locked,
                showExplainBox: o.showExplainBox, previewMode: o.previewMode
              };
              return orig.designerAppScr.apply(this, arguments);
            };
          }
          if (orig.promptAiAppScr) {
            window.promptAiAppScr = function (T, o) {
              o = o || {};
              var kp = 'pai:' + prevScene + ':' + (o.aiTab ? '1' : '0');
              if (!seen[kp]) { seen[kp] = 1; if (cool('slide', 0.18)) addCue(curT, 'slide'); }
              return orig.promptAiAppScr.apply(this, arguments);
            };
          }
          if (orig.exportPngAppScr) {
            window.exportPngAppScr = function (T, o) {
              o = o || {};
              var ke = 'exp:' + prevScene;
              if (!seen[ke]) { seen[ke] = 1; if (cool('popup', 0.18)) addCue(curT, 'popup'); }
              if (o.licActive && !seen[ke + ':lic']) { seen[ke + ':lic'] = 1; if (cool('success', 0.2)) addCue(curT, 'success'); }
              if ((o.saved || 0) > 0.02 && !seen[ke + ':sav']) { seen[ke + ':sav'] = 1; if (cool('notif', 0.2)) addCue(curT, 'notif'); }
              return orig.exportPngAppScr.apply(this, arguments);
            };
          }
          if (orig.tx) {
            window.tx = function (s, x, y) {
              var str = String(s == null ? '' : s);
              var hasPipe = str.length > 1 && str.charCodeAt(str.length - 1) === 124; // '|'
              var clean = hasPipe ? str.slice(0, -1) : str;
              var kt = 'tx:' + prevScene + ':' + Math.round(x) + ':' + Math.round(y);
              var pv = prevTx[kt];
              if (clean.length > 0 && clean.length <= 64 && !inExplicitType(curT)) {
                if (hasPipe && pv !== undefined && clean !== pv && clean.length > pv.length) {
                  if (cool('key', 0.045)) addCue(curT, 'key');
                } else if (pv !== undefined && clean.length > pv.length && (clean.length - pv.length) <= 3 && pv.length >= 1 && clean.indexOf(pv) === 0 && !/^\d+[%+]?$/.test(clean)) {
                  if (cool('key', 0.045)) addCue(curT, 'key');
                } else if (pv !== undefined && pv.length >= 8 && (clean.length === 1 || clean.length === 2) && !/^\d+[%+]?$/.test(clean)) {
                  if (cool('key', 0.045)) addCue(curT, 'key');
                }
              }
              prevTx[kt] = clean;
            };
          }

          var step = 0.05;
          for (var t = 0; t <= curTotal; t += step) {
            curT = t;
            if (typeof PLAN !== 'undefined' && Array.isArray(PLAN)) {
              for (var k = 0; k < PLAN.length; k++) {
                if (t < PLAN[k].start + PLAN[k].len) {
                  if (k !== prevScene) { prevScene = k; prevTx = {}; }
                  break;
                }
              }
            }
            try { realFrameAt(t); } catch (eFr) {}
          }

          try { if (origC) window.c = origC; } catch (eC3) {}
          try { if (origCtx) window.ctx = origCtx; } catch (eC4) {}
          for (var fn in savedNoops) window[fn] = savedNoops[fn];
          for (var nm in orig) window[nm] = orig[nm];
          try { realFrameAt(Math.min(2.2, Math.max(0, curTotal - 0.05))); } catch (eRest) {}
        }

        // Sort & deduplicate cues
        cues.sort(function (a, b) { return a.t - b.t; });
        var out = [];
        var lastByKind = {};
        for (var i = 0; i < cues.length; i++) {
          var c = cues[i];
          var minGap = (c.k === 'key' || c.k === 'tick') ? 0.04 : 0.08;
          if (lastByKind[c.k] !== undefined && c.t - lastByKind[c.k] < minGap) continue;
          lastByKind[c.k] = c.t;
          out.push(c);
        }
        cachedCues = out;
        cachedTotal = curTotal;
        window.__h2vScanning = false;
        return out;
      } catch (err) {
        window.__h2vScanning = false;
        return [];
      }
    }

    window.__h2vExtractSfxCues = function () {
      cachedCues = null; // Force fresh scan with current DUR / PLAN
      return JSON.stringify(extractAllCuesArray());
    };

    // Upgrade sfxPlan() so WAV download (renderSfx) also gets all extracted cues
    window.sfxPlan = function () {
      return extractAllCuesArray();
    };

    /* 4. Live Preview Real-Time SFX Player (triggered directly by frameAt(T) during preview) */
    var liveAC = null, liveMG = null, liveNoise = null;
    var liveVol = { master: 1, sfx: 1 };
    var prevLiveT = -1;

    function ensureLiveAC() {
      var sfEl = document.getElementById('sf');
      if (sfEl && !sfEl.checked) return null;
      var AC0 = window.AudioContext || window.webkitAudioContext;
      if (!AC0) return null;
      if (!liveAC) {
        try {
          liveAC = new AC0();
          liveMG = liveAC.createGain();
          liveMG.gain.value = 0.9 * liveVol.master * liveVol.sfx;
          liveMG.connect(liveAC.destination);
          liveNoise = mkNoise(liveAC);
        } catch (eAc) { liveAC = null; return null; }
      }
      if (liveAC && liveAC.state === 'suspended') {
        try { liveAC.resume(); } catch (eRes) {}
      }
      return { ctx: liveAC, out: liveMG, noise: liveNoise };
    }

    // Replace page's bulk sfxStart with lightweight live AC initializer
    window.sfxStart = function () {
      prevLiveT = -1;
      ensureLiveAC();
    };
    window.sfxStop = function () {
      prevLiveT = -1;
    };

    // Hook frameAt(T) to play SFX cues in real time during Preview
    if (typeof window.frameAt === 'function' && !window.__h2vOrigFrameAt) {
      window.__h2vOrigFrameAt = window.frameAt;
      window.frameAt = function (T) {
        if (!window.__h2vExporting && !window.__h2vScanning && typeof T === 'number') {
          var dt = T - prevLiveT;
          if (prevLiveT >= 0 && dt > 0 && dt < 0.40) {
            var x = ensureLiveAC();
            if (x) {
              var list = extractAllCuesArray();
              var now = x.ctx.currentTime + 0.005;
              for (var i = 0; i < list.length; i++) {
                var e = list[i];
                if (e.t > prevLiveT && e.t <= T) {
                  var fn = FULL_SFX[e.k] || (window.SFX && window.SFX[e.k]);
                  if (fn) { try { fn(x, now, e); } catch (ePlay) {} }
                } else if (e.t > T) {
                  break;
                }
              }
            }
          }
          prevLiveT = T;
        }
        return window.__h2vOrigFrameAt.apply(this, arguments);
      };
    }

    // Wrap NH.run so live preview SFX are muted during fast frame-by-frame MP4 export
    if (window.NH) {
      if (typeof window.NH.run === 'function' && !window.NH.__h2vWrappedRun) {
        var origRun = window.NH.run;
        window.NH.run = async function (fps) {
          window.__h2vExporting = true;
          try {
            return await origRun.call(this, fps);
          } finally {
            window.__h2vExporting = false;
          }
        };
        window.NH.__h2vWrappedRun = true;
      }
      if (!window.NH.setVolumes) {
        window.NH.setVolumes = function (m, v, mu, s) {
          liveVol.master = +m || 1;
          liveVol.sfx = +s || 1;
          if (liveMG) liveMG.gain.value = 0.9 * liveVol.master * liveVol.sfx;
          return 'ok';
        };
      }
    }

    return 'sfx-ok';
  } catch (err) {
    return 'sfx-err:' + (err && err.message ? err.message : err);
  }
})();
