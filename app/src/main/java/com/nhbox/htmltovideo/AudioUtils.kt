package com.nhbox.htmltovideo

import org.json.JSONArray
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.tanh

/** WAV padhna, 48 kHz mono banana, silence trim karna aur poori timeline ka audio mix banana. */
object AudioUtils {
    const val SR = 48000

    /** WAV (16-bit PCM) ko mono ShortArray + sample rate mein badalta hai. */
    fun readWav(f: File): Pair<ShortArray, Int>? = readWavBytes(f.readBytes())

    /**
     * Decodes WAV or MP3/AAC/OGG audio bytes into (mono 16-bit PCM ShortArray, sampleRate)
     * using Android's built-in MediaExtractor + MediaCodec (works for ElevenLabs MP3 & WAV streams).
     */
    fun decodeAudioBytes(cacheDir: File, b: ByteArray): Pair<ShortArray, Int>? {
        readWavBytes(b)?.let { return it }
        val tmp = File(cacheDir, "h2v_dec_${System.nanoTime()}.mp3")
        var ex: android.media.MediaExtractor? = null
        var codec: android.media.MediaCodec? = null
        try {
            tmp.writeBytes(b)
            ex = android.media.MediaExtractor()
            ex.setDataSource(tmp.absolutePath)
            var trackIdx = -1
            var fmt: android.media.MediaFormat? = null
            var mime = ""
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                val m = f.getString(android.media.MediaFormat.KEY_MIME) ?: ""
                if (m.startsWith("audio/")) {
                    trackIdx = i
                    fmt = f
                    mime = m
                    break
                }
            }
            if (trackIdx < 0 || fmt == null) return null
            ex.selectTrack(trackIdx)
            var sr = if (fmt.containsKey(android.media.MediaFormat.KEY_SAMPLE_RATE)) fmt.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE) else 44100
            var ch = if (fmt.containsKey(android.media.MediaFormat.KEY_CHANNEL_COUNT)) fmt.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT) else 1
            codec = android.media.MediaCodec.createDecoderByType(mime)
            codec.configure(fmt, null, null, 0)
            codec.start()

            val outSamples = java.io.ByteArrayOutputStream(b.size * 6)
            val info = android.media.MediaCodec.BufferInfo()
            var inDone = false
            var outDone = false
            while (!outDone) {
                if (!inDone) {
                    val inIdx = codec.dequeueInputBuffer(10000)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)
                        if (inBuf != null) {
                            val sz = ex.readSampleData(inBuf, 0)
                            if (sz < 0) {
                                codec.queueInputBuffer(inIdx, 0, 0, 0L, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inDone = true
                            } else {
                                codec.queueInputBuffer(inIdx, 0, sz, ex.sampleTime, 0)
                                ex.advance()
                            }
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, 10000)
                when {
                    outIdx >= 0 -> {
                        val outBuf = codec.getOutputBuffer(outIdx)
                        if (outBuf != null && info.size > 0) {
                            outBuf.position(info.offset)
                            outBuf.limit(info.offset + info.size)
                            val chunk = ByteArray(info.size)
                            outBuf.get(chunk)
                            outSamples.write(chunk)
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if ((info.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outDone = true
                        }
                    }
                    outIdx == android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val of = codec.outputFormat
                        if (of.containsKey(android.media.MediaFormat.KEY_SAMPLE_RATE)) sr = of.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE)
                        if (of.containsKey(android.media.MediaFormat.KEY_CHANNEL_COUNT)) ch = of.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT)
                    }
                }
            }

            val rawPcm = outSamples.toByteArray()
            val safeCh = ch.coerceAtLeast(1)
            val frameCount = rawPcm.size / 2 / safeCh
            if (frameCount <= 0) return null
            val bb = ByteBuffer.wrap(rawPcm).order(ByteOrder.LITTLE_ENDIAN)
            val mono = ShortArray(frameCount)
            for (i in 0 until frameCount) {
                var sum = 0
                for (c in 0 until safeCh) {
                    sum += bb.getShort((i * safeCh + c) * 2).toInt()
                }
                mono[i] = (sum / safeCh).toShort()
            }
            return Pair(mono, sr)
        } catch (_: Exception) {
            return null
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { ex?.release() } catch (_: Exception) {}
            try { tmp.delete() } catch (_: Exception) {}
        }
    }

    fun readWavBytes(b: ByteArray): Pair<ShortArray, Int>? {
        if (b.size < 44) return null
        if (String(b, 0, 4, Charsets.US_ASCII) != "RIFF") return null
        val bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
        var p = 12
        var ch = 1
        var sr = 22050
        var bits = 16
        var dataOff = -1
        var dataLen = 0
        while (p + 8 <= b.size) {
            val id = String(b, p, 4, Charsets.US_ASCII)
            val sz = bb.getInt(p + 4)
            if (id == "fmt ") {
                ch = bb.getShort(p + 10).toInt()
                sr = bb.getInt(p + 12)
                bits = bb.getShort(p + 22).toInt()
            }
            if (id == "data") {
                dataOff = p + 8
                dataLen = if (sz < 0 || dataOff + sz > b.size) b.size - dataOff else sz
                break
            }
            if (sz < 0) break
            p += 8 + sz + (sz and 1)
        }
        if (dataOff < 0 || bits != 16 || ch < 1) return null
        val n = dataLen / 2 / ch
        if (n <= 0) return null
        val out = ShortArray(n)
        for (i in 0 until n) {
            var sum = 0
            for (c in 0 until ch) sum += bb.getShort(dataOff + (i * ch + c) * 2).toInt()
            out[i] = (sum / ch).toShort()
        }
        return Pair(out, sr)
    }

    fun resample(src: ShortArray, sr: Int, dst: Int): ShortArray {
        if (sr == dst) return src
        val n = (src.size.toLong() * dst / sr).toInt()
        val o = ShortArray(n)
        val r = sr.toDouble() / dst
        for (i in 0 until n) {
            val x = i * r
            val i0 = x.toInt()
            val f = x - i0
            val a = src[minOf(i0, src.size - 1)].toInt()
            val b = src[minOf(i0 + 1, src.size - 1)].toInt()
            o[i] = (a + (b - a) * f).toInt().toShort()
        }
        return o
    }

    /** Shuru aur end ki khamoshi hatata hai taaki timing sahi rahe. */
    fun trimSilence(s: ShortArray): ShortArray {
        val th = 300
        var a = 0
        while (a < s.size && Math.abs(s[a].toInt()) < th) a++
        var e = s.size - 1
        while (e > a && Math.abs(s[e].toInt()) < th) e--
        if (a >= e) return s
        val from = maxOf(0, a - SR / 20)
        val to = minOf(s.size, e + SR / 10)
        return s.copyOfRange(from, to)
    }

    /** Sab clips ko unke time par rakhkar ek mono 48 kHz track banata hai. */
    fun mix(totalSec: Double, ev: JSONArray, clips: Map<String, ShortArray>): ShortArray {
        val n = Math.ceil(totalSec * SR).toInt()
        val acc = IntArray(n)
        for (i in 0 until ev.length()) {
            val e = ev.getJSONObject(i)
            val c = clips[e.getString("key")] ?: continue
            val s = Math.round(e.getDouble("at") * SR).toInt()
            for (j in c.indices) {
                val p = s + j
                if (p >= n) break
                acc[p] += c[j].toInt()
            }
        }
        var peak = 1
        for (v in acc) {
            val a = Math.abs(v)
            if (a > peak) peak = a
        }
        val g = minOf(2.5, 26000.0 / peak)
        return ShortArray(n) { (acc[it] * g).toInt().coerceIn(-32768, 32767).toShort() }
    }

    /**
     * v3.5: Voice-over + Sound Effects (Music completely removed).
     * Har stem ki apni volume (1.0 = 100%), upar se master.
     * Block-wise (1 sec) kaam karta hai taaki lambi video par bhi memory kam lage.
     */
    fun mixStems(
        totalSec: Double,
        ev: JSONArray?,
        clips: Map<String, ShortArray>,
        @Suppress("UNUSED_PARAMETER") music: ShortArray?,
        @Suppress("UNUSED_PARAMETER") musicRate: Int,
        sfx: ShortArray?,
        sfxRate: Int,
        master: Float,
        voiceVol: Float,
        @Suppress("UNUSED_PARAMETER") musicVol: Float,
        sfxVol: Float,
        duck: Boolean
    ): ShortArray {
        val n = Math.ceil(totalSec * SR).toInt()

        class VEv(val start: Int, val clip: ShortArray)
        val evs = ArrayList<VEv>()
        if (ev != null) {
            for (i in 0 until ev.length()) {
                val e = ev.getJSONObject(i)
                val c = clips[e.getString("key")] ?: continue
                evs.add(VEv(Math.round(e.getDouble("at") * SR).toInt(), c))
            }
        }
        evs.sortBy { it.start }
        val hasVoice = evs.isNotEmpty()

        val bsz = SR                       // 1 second block
        val tmp = FloatArray(bsz)
        fun fillBlock(b0: Int) {
            java.util.Arrays.fill(tmp, 0f)
            val b1 = b0 + bsz
            for (e in evs) {
                if (e.start >= b1) break
                val eEnd = e.start + e.clip.size
                if (eEnd <= b0) continue
                val s0 = maxOf(e.start, b0)
                val t0 = minOf(eEnd, b1)
                for (p in s0 until t0) tmp[p - b0] += e.clip[p - e.start].toFloat()
            }
        }

        // pass 1: voice peak -> normalization gain
        var peak = 1f
        if (hasVoice) {
            var b0 = 0
            while (b0 < n) {
                fillBlock(b0)
                val lim = minOf(bsz, n - b0)
                for (i in 0 until lim) {
                    val a = Math.abs(tmp[i])
                    if (a > peak) peak = a
                }
                b0 += bsz
            }
        }
        val g = minOf(2.5, 26000.0 / peak).toFloat() / 32768f

        // pass 2: duck envelope (10 ms blocks)
        val blk = SR / 100
        val nb = n / blk + 2
        val env = FloatArray(nb + 1)
        if (duck && hasVoice) {
            var cur = 0f
            var b0 = 0
            while (b0 < n) {
                fillBlock(b0)
                val sub = bsz / blk
                for (k in 0 until sub) {
                    val idx = b0 / blk + k
                    if (idx >= nb) break
                    var pk = 0f
                    val from = k * blk
                    val to = minOf(from + blk, minOf(bsz, n - b0))
                    for (i in from until to) {
                        val a = Math.abs(tmp[i]) * g
                        if (a > pk) pk = a
                    }
                    val target = if (pk > 0.02f) 1f else 0f
                    cur += (target - cur) * (if (target > cur) 0.35f else 0.02f)
                    env[idx] = cur
                }
                b0 += bsz
            }
            env[nb] = cur
        }

        // pass 3: mix Voice + SFX (Music removed) + soft limiter
        val out = ShortArray(n)
        var b0 = 0
        while (b0 < n) {
            if (hasVoice) fillBlock(b0)
            val lim = minOf(bsz, n - b0)
            for (i in 0 until lim) {
                val gi = b0 + i
                val b = gi / blk
                val f = (gi - b * blk) / blk.toFloat()
                val e = env[b] + (env[b + 1] - env[b]) * f
                var x = if (hasVoice) tmp[i] * g * voiceVol else 0f
                if (sfx != null) x += sampleAt(sfx, sfxRate, gi) * SFX_BOOST * sfxVol * (1f - 0.08f * e)
                x *= master
                out[gi] = softLimit(x)
            }
            b0 += bsz
        }
        return out
    }

    /** Stem ka sample (kam rate ho to linear interpolation se 48k par). */
    private fun sampleAt(a: ShortArray, rate: Int, i: Int): Float {
        if (rate == SR) return if (i < a.size) a[i] / 32768f else 0f
        val p = i.toDouble() * rate / SR
        val i0 = p.toInt()
        if (i0 >= a.size - 1) return if (i0 < a.size) a[i0] / 32768f else 0f
        val fr = (p - i0).toFloat()
        return (a[i0] * (1f - fr) + a[i0 + 1] * fr) / 32768f
    }

    /** SFX boost taaki voice ke saath typing, popup, slide, tap saaf sunai dein. */
    private const val SFX_BOOST = 1.55f

    private fun softLimit(x: Float): Short {
        val th = 0.85f
        val a = Math.abs(x)
        val y = if (a <= th) a else th + (1f - th) * tanh((a - th) / (1f - th))
        val v = ((if (x < 0f) -y else y) * 32767f).toInt().coerceIn(-32768, 32767)
        return v.toShort()
    }
}
