package com.nhbox.htmltovideo

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/**
 * v3.5: Complete Studio SFX Synthesizer (No Background Music).
 * Supports all UI/UX & tutorial sound effects:
 * - whoosh (screen in / out transition)
 * - slide (side cards, bottom sheets, drawers, tab slides)
 * - popup / pop (cards, banners, callout boxes, OSD pills, highlight boxes)
 * - key / type (realistic mechanical/tactile keyboard typing per character)
 * - tap (mobile finger screen tap)
 * - click / step (button press click)
 * - tick (counter & fine nudge tick)
 * - copy (clipboard copy double-chirp)
 * - lock / alarm (smart lock metallic clunk + confirmation ping)
 * - notif / bell (two-tone notification ding)
 * - chime (harmonic chord arpeggio)
 * - success (4-note ascending major celebration arpeggio)
 * - riser (cinematic synth/noise sweep)
 * - hit / impact (deep sub-bass impact + chord)
 */
object AutoSfx {
    const val SFX_RATE = 24000
    const val MUSIC_RATE = 12000

    private class Rng(var s: Int = 0x1234567) {
        fun next(): Float {
            s = s xor (s shl 13)
            s = s xor (s ushr 17)
            s = s xor (s shl 5)
            return (s and 0xFFFFFF) / 8388608f - 1f
        }
    }

    /** Per-sample soft limiter so loud whooshes/hits never shrink quiet typing/popup/slide sounds. */
    private fun toShortsSoft(b: FloatArray): ShortArray {
        val o = ShortArray(b.size)
        val knee = 0.72f
        for (i in b.indices) {
            val x = b[i]
            val a = abs(x)
            val y = if (a <= knee) a else knee + (0.96f - knee) * tanh((a - knee) / (0.96f - knee))
            val s = ((if (x < 0f) -y else y) * 32767f).toInt().coerceIn(-32768, 32767)
            o[i] = s.toShort()
        }
        return o
    }

    /** 1. Screen in-out transition whoosh: resonant bandpass sweep + subtle sub-air body. */
    private fun whoosh(buf: FloatArray, t0: Double, dur: Double = 0.78, vol: Float = 0.62f, rng: Rng) {
        val d = dur.coerceIn(0.25, 2.5)
        val s0 = (t0 * SFX_RATE).toInt()
        val n = (d * SFX_RATE).toInt()
        var lp = 0f
        var bp = 0f
        var subPh = 0.0
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx < 0 || idx >= buf.size) continue
            val p = i / n.toFloat()
            val env = sin(PI * p).toFloat()
            val cut = 0.04f + 0.44f * sin(PI * p).toFloat()
            val x = rng.next()
            lp += cut * (x - lp)
            bp += cut * (lp - bp)
            val subF = 130.0 - 75.0 * p
            subPh += 2.0 * PI * subF / SFX_RATE
            val sub = (sin(subPh) * 0.16 * env).toFloat()
            buf[idx] += ((lp - bp) * 1.15f * env + sub) * vol
        }
    }

    /** 2. Slide (sideCard, sheet, drawer, tab transition): silky air-swipe + soft landing pop. */
    private fun slide(buf: FloatArray, t0: Double, rng: Rng) {
        val dur = 0.19
        val s0 = (t0 * SFX_RATE).toInt()
        val n = (dur * SFX_RATE).toInt()
        var y1 = 0f
        var y2 = 0f
        var ph = 0.0
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx < 0 || idx >= buf.size) continue
            val p = i / n.toFloat()
            val t = i / SFX_RATE.toDouble()
            val env = sin(PI * p).toFloat() * exp(-t * 6.5).toFloat()
            val cut = 0.08f + 0.42f * sin(PI * p).toFloat()
            val x = rng.next()
            y1 += cut * (x - y1)
            y2 += cut * 0.5f * (y1 - y2)
            // Soft upward pitch glide (240 Hz -> 480 Hz)
            val f = 240.0 + 240.0 * p
            ph += 2.0 * PI * f / SFX_RATE
            val body = (sin(ph) * exp(-t * 14.0) * 0.32).toFloat()
            buf[idx] += (y1 - y2) * env * 0.65f + body
        }
    }

    /** 3. Popup / Pop (UI card, banner, OSD pill, highlight box, callout): crisp bubbly upward pitch pop. */
    private fun popup(buf: FloatArray, t0: Double) {
        val s0 = (t0 * SFX_RATE).toInt()
        val n = (0.095 * SFX_RATE).toInt()
        var ph = 0.0
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx < 0 || idx >= buf.size) continue
            val t = i / SFX_RATE.toDouble()
            val p = i / n.toDouble()
            // Fast upward pitch sweep 340 Hz -> 820 Hz for modern UI bubble pop
            val f = 340.0 + 480.0 * (1.0 - exp(-p * 4.5))
            ph += 2.0 * PI * f / SFX_RATE
            val env = sin(PI * minOf(1.0, p * 4.0)) * exp(-t * 24.0)
            val sample = (sin(ph) + 0.35 * sin(ph * 2.0)) * env * 0.52
            buf[idx] += sample.toFloat()
        }
    }

    /** 4. Key (Single character typing sound): tactile mechanical keystroke + highpass click transient. */
    private fun key(buf: FloatArray, t0: Double, rng: Rng) {
        val s0 = (t0 * SFX_RATE).toInt()
        val n = (0.036 * SFX_RATE).toInt()
        // Subtle pitch variation per keystroke so typing sounds natural and human
        val detune = 1.0 + 0.12 * sin(t0 * 43.7)
        var lp = 0f
        var ph = 0.0
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx < 0 || idx >= buf.size) continue
            val t = i / SFX_RATE.toDouble()
            val x = rng.next()
            lp += 0.25f * (x - lp)
            val hp = x - lp // High-passed crisp keycap click
            val clickEnv = exp(-t * 115.0)
            val f = (1350.0 * detune) * exp(-t * 38.0) + 420.0
            ph += 2.0 * PI * f / SFX_RATE
            val bodyEnv = exp(-t * 75.0)
            val sample = hp * clickEnv * 0.48 + sin(ph) * bodyEnv * 0.34
            buf[idx] += sample.toFloat()
        }
    }

    /** Multi-character typing burst. */
    private fun typeBurst(buf: FloatArray, t0: Double, count: Int, gap: Double, rng: Rng) {
        val n = count.coerceIn(1, 40)
        val gp = if (gap > 0.02) gap else 0.072
        for (k in 0 until n) {
            val jitter = ((k * 37) % 5) * 0.004
            key(buf, t0 + k * gp + jitter, rng)
        }
    }

    /** 5. Tap (Mobile finger touch on screen): warm pitch-drop thud + crisp touch transient. */
    private fun tap(buf: FloatArray, t0: Double, rng: Rng) {
        val s0 = (t0 * SFX_RATE).toInt()
        val n = (0.095 * SFX_RATE).toInt()
        var ph = 0.0
        var lp = 0f
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx < 0 || idx >= buf.size) continue
            val t = i / SFX_RATE.toDouble()
            val f = 140.0 + 240.0 * exp(-t * 36.0)
            ph += 2.0 * PI * f / SFX_RATE
            val x = rng.next()
            lp += 0.3f * (x - lp)
            val transient = (x - lp) * exp(-t * 140.0) * 0.32
            val body = sin(ph) * exp(-t * 28.0) * 0.54
            buf[idx] += (body + transient).toFloat()
        }
    }

    /** 6. Click (UI button press): snappy two-frequency click. */
    private fun click(buf: FloatArray, t0: Double, rng: Rng) {
        val s0 = (t0 * SFX_RATE).toInt()
        val n = (0.052 * SFX_RATE).toInt()
        var ph = 0.0
        var lp = 0f
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx < 0 || idx >= buf.size) continue
            val t = i / SFX_RATE.toDouble()
            val f = 820.0 + 920.0 * exp(-t * 55.0)
            ph += 2.0 * PI * f / SFX_RATE
            val x = rng.next()
            lp += 0.22f * (x - lp)
            val hp = (x - lp) * exp(-t * 130.0) * 0.38
            val tone = sin(ph) * exp(-t * 50.0) * 0.44
            buf[idx] += (hp + tone).toFloat()
        }
    }

    /** 7. Tick (Counter / fine nudge tick): subtle high-tech micro-tick. */
    private fun tick(buf: FloatArray, t0: Double, amp: Float = 0.38f) {
        val s0 = (t0 * SFX_RATE).toInt()
        val n = (0.042 * SFX_RATE).toInt()
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx < 0 || idx >= buf.size) continue
            val t = i / SFX_RATE.toDouble()
            buf[idx] += (amp * exp(-t * 68.0) * (sin(2.0 * PI * 2100.0 * t) + 0.4 * sin(2.0 * PI * 3400.0 * t))).toFloat()
        }
    }

    /** 8. Copy (Clipboard copy double-chirp). */
    private fun copy(buf: FloatArray, t0: Double, rng: Rng) {
        click(buf, t0, rng)
        val s0 = ((t0 + 0.035) * SFX_RATE).toInt()
        val n = (0.085 * SFX_RATE).toInt()
        var ph = 0.0
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx < 0 || idx >= buf.size) continue
            val t = i / SFX_RATE.toDouble()
            val f = 580.0 + 520.0 * (i / n.toDouble())
            ph += 2.0 * PI * f / SFX_RATE
            buf[idx] += (0.46 * exp(-t * 24.0) * sin(ph)).toFloat()
        }
    }

    /** 9. Lock (Smart lock metallic clunk + high confirmation ping). */
    private fun lock(buf: FloatArray, t0: Double) {
        val s0 = (t0 * SFX_RATE).toInt()
        val n1 = (0.14 * SFX_RATE).toInt()
        var ph1 = 0.0
        for (i in 0 until n1) {
            val idx = s0 + i
            if (idx < 0 || idx >= buf.size) continue
            val t = i / SFX_RATE.toDouble()
            val f = 110.0 + 95.0 * exp(-t * 35.0)
            ph1 += 2.0 * PI * f / SFX_RATE
            val sq = if (sin(ph1) >= 0.0) 0.6 else -0.6
            buf[idx] += ((sq * 0.35 + sin(ph1 * 0.5) * 0.45) * exp(-t * 22.0)).toFloat()
        }
        val s1 = ((t0 + 0.095) * SFX_RATE).toInt()
        val n2 = (0.32 * SFX_RATE).toInt()
        for (i in 0 until n2) {
            val idx = s1 + i
            if (idx < 0 || idx >= buf.size) continue
            val t = i / SFX_RATE.toDouble()
            buf[idx] += (0.42 * exp(-t * 10.0) * (sin(2.0 * PI * 1318.5 * t) + 0.35 * sin(2.0 * PI * 2637.0 * t))).toFloat()
        }
    }

    /** 10. Notif / Bell (Two-tone bright notification ding). */
    private fun notif(buf: FloatArray, t0: Double) {
        val notes = doubleArrayOf(987.77, 1318.51)
        for (k in notes.indices) {
            val s0 = ((t0 + k * 0.105) * SFX_RATE).toInt()
            val n = (0.38 * SFX_RATE).toInt()
            val f = notes[k]
            for (i in 0 until n) {
                val idx = s0 + i
                if (idx < 0 || idx >= buf.size) continue
                val t = i / SFX_RATE.toDouble()
                val env = exp(-t * 8.5)
                buf[idx] += (0.42 * env * (sin(2.0 * PI * f * t) + 0.3 * sin(2.0 * PI * f * 2.0 * t))).toFloat()
            }
        }
    }

    /** 11. Chime (Harmonic chord arpeggio). */
    private fun chime(buf: FloatArray, t0: Double, customNotes: JSONArray? = null) {
        val notes = if (customNotes != null && customNotes.length() > 0) {
            DoubleArray(customNotes.length()) { customNotes.optDouble(it, 784.0) }
        } else {
            doubleArrayOf(523.25, 659.25, 1046.50)
        }
        for (k in notes.indices) {
            val s0 = ((t0 + k * 0.095) * SFX_RATE).toInt()
            val n = (0.75 * SFX_RATE).toInt()
            val f = notes[k]
            for (i in 0 until n) {
                val idx = s0 + i
                if (idx < 0 || idx >= buf.size) continue
                val t = i / SFX_RATE.toDouble()
                val env = exp(-t * 4.8)
                buf[idx] += (0.34 * env * (sin(2.0 * PI * f * t) + 0.25 * sin(2.0 * PI * f * 2.0 * t))).toFloat()
            }
        }
    }

    /** 12. Success (4-note ascending major celebration arpeggio C5-E5-G5-C6). */
    private fun success(buf: FloatArray, t0: Double) {
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        for (k in notes.indices) {
            val s0 = ((t0 + k * 0.078) * SFX_RATE).toInt()
            val n = (0.48 * SFX_RATE).toInt()
            val f = notes[k]
            for (i in 0 until n) {
                val idx = s0 + i
                if (idx < 0 || idx >= buf.size) continue
                val t = i / SFX_RATE.toDouble()
                val env = exp(-t * 6.5)
                buf[idx] += (0.38 * env * (sin(2.0 * PI * f * t) + 0.3 * sin(2.0 * PI * f * 2.0 * t))).toFloat()
            }
        }
    }

    /** 13. Riser (Cinematic synth + filter sweep leading to climax). */
    private fun riser(buf: FloatArray, t0: Double, dur: Double = 2.0, rng: Rng) {
        val d = dur.coerceIn(0.6, 4.0)
        whoosh(buf, t0, d, 0.45f, rng)
        val s0 = (t0 * SFX_RATE).toInt()
        val n = (d * SFX_RATE).toInt()
        var ph = 0.0
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx < 0 || idx >= buf.size) continue
            val p = i / n.toDouble()
            val f = 85.0 * Math.pow(7.5, p)
            ph += 2.0 * PI * f / SFX_RATE
            val env = p * p * (1.0 - 0.5 * maxOf(0.0, (p - 0.92) / 0.08))
            buf[idx] += (sin(ph) * env * 0.28).toFloat()
        }
    }

    /** 14. Hit / Impact (Deep cinematic sub-bass drop + warm triad chord). */
    private fun hit(buf: FloatArray, t0: Double, rng: Rng) {
        val s0 = (t0 * SFX_RATE).toInt()
        val n = (1.35 * SFX_RATE).toInt()
        var ph = 0.0
        var lp = 0f
        val chord = doubleArrayOf(261.63, 329.63, 392.00, 523.25)
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx < 0 || idx >= buf.size) continue
            val t = i / SFX_RATE.toDouble()
            val f = 36.0 + 48.0 * exp(-t * 5.5)
            ph += 2.0 * PI * f / SFX_RATE
            val sub = sin(ph) * exp(-t * 2.6) * 0.58
            val x = rng.next()
            lp += 0.08f * (x - lp)
            val punch = lp * exp(-t * 9.0) * 0.42
            var chSum = 0.0
            for (cf in chord) chSum += sin(2.0 * PI * cf * t)
            val ch = chSum * 0.08 * exp(-t * 2.2)
            buf[idx] += (sub + punch + ch).toFloat()
        }
    }

    /**
     * Synthesizes a complete 24 kHz mono SFX track from extracted HTML cues (`cues`).
     * Falls back to `events` if `cues` is empty or null.
     */
    fun sfxFromCues(totalSec: Double, cues: JSONArray?, events: JSONArray?, sceneStyle: Boolean): ShortArray {
        if (cues == null || cues.length() == 0) {
            return sfx(totalSec, events, sceneStyle)
        }
        val n = Math.ceil(totalSec * SFX_RATE).toInt()
        val buf = FloatArray(n)
        val rng = Rng()
        for (i in 0 until cues.length()) {
            val e: JSONObject = cues.optJSONObject(i) ?: continue
            val t = maxOf(0.0, e.optDouble("t", 0.0))
            when (e.optString("k", "")) {
                "whoosh" -> whoosh(buf, t, e.optDouble("d", 0.78), e.optDouble("v", 0.62).toFloat(), rng)
                "slide" -> slide(buf, t, rng)
                "popup", "pop" -> popup(buf, t)
                "key" -> key(buf, t, rng)
                "type" -> typeBurst(buf, t, e.optInt("n", 10), e.optDouble("gap", 0.072), rng)
                "tap" -> tap(buf, t, rng)
                "click", "step" -> click(buf, t, rng)
                "tick" -> tick(buf, t)
                "copy" -> copy(buf, t, rng)
                "lock", "alarm" -> lock(buf, t)
                "notif", "bell" -> notif(buf, t)
                "chime" -> chime(buf, t, e.optJSONArray("notes"))
                "success" -> success(buf, t)
                "riser" -> riser(buf, t, e.optDouble("d", 2.0), rng)
                "hit", "impact" -> hit(buf, t, rng)
            }
        }
        return toShortsSoft(buf)
    }

    /** Fallback when no detailed cue list is available. */
    fun sfx(totalSec: Double, events: JSONArray?, sceneStyle: Boolean): ShortArray {
        val n = Math.ceil(totalSec * SFX_RATE).toInt()
        val buf = FloatArray(n)
        val rng = Rng()
        if (events != null) {
            var first = true
            for (i in 0 until events.length()) {
                val e = events.getJSONObject(i)
                val keyStr = e.getString("key")
                val at = e.getDouble("at")
                if (sceneStyle) {
                    if (keyStr.endsWith("a")) {
                        if (first) {
                            whoosh(buf, maxOf(0.0, at - 0.35), 0.9, 0.60f, rng)
                            chime(buf, maxOf(0.0, at + 0.05))
                            first = false
                        } else {
                            whoosh(buf, maxOf(0.0, at - 0.35), 0.78, 0.62f, rng)
                            slide(buf, maxOf(0.0, at + 0.15), rng)
                        }
                    } else {
                        popup(buf, maxOf(0.0, at - 0.03))
                    }
                } else {
                    popup(buf, maxOf(0.0, at - 0.02))
                }
            }
        }
        if (totalSec > 3.0) success(buf, totalSec - 2.2)
        return toShortsSoft(buf)
    }

    /** v3.5: Background music removed completely per user preference (only Voice + Sound Effects). */
    fun music(totalSec: Double): ShortArray = ShortArray(0)
}
