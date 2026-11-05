package com.nhbox.htmltovideo

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/**
 * v3.2: "My Voice" engine. Aapki apni awaaz ke clips (khud record kiye ya Colab/AI se bane) phone mein save rehte hain.
 * Har line ka clip us line ke text ke hash se pehchana jata hai, isliye script same rahe to dobara record nahi karna padta.
 * Clips raw 16-bit PCM, 48 kHz mono mein store hote hain (TtsRenderer jaisa hi format), baaki pipeline (timeline sync, mix) same rehti hai.
 */
object MyVoice {
    fun dir(ctx: Context): File = File(ctx.filesDir, "myvoice").also { it.mkdirs() }

    fun keyOf(text: String): String {
        val d = MessageDigest.getInstance("SHA-1").digest(text.trim().toByteArray(Charsets.UTF_8))
        val sb = StringBuilder()
        for (i in 0 until 8) sb.append(String.format("%02x", d[i]))
        return sb.toString()
    }

    private fun fileOf(ctx: Context, text: String) = File(dir(ctx), keyOf(text) + ".pcm")

    fun has(ctx: Context, text: String): Boolean {
        val f = fileOf(ctx, text)
        return f.exists() && f.length() > AudioUtils.SR / 10   // 0.1 sec se chhota clip maana nahi jata
    }

    fun load(ctx: Context, text: String): ShortArray? {
        val f = fileOf(ctx, text)
        if (!f.exists()) return null
        val b = f.readBytes()
        val n = b.size / 2
        if (n <= 0) return null
        val o = ShortArray(n)
        ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(o)
        return o
    }

    fun save(ctx: Context, text: String, pcm: ShortArray) {
        val bb = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        bb.asShortBuffer().put(pcm)
        fileOf(ctx, text).writeBytes(bb.array())
    }

    fun delete(ctx: Context, text: String) {
        fileOf(ctx, text).delete()
    }

    fun clearAll(ctx: Context): Int {
        var n = 0
        for (f in dir(ctx).listFiles() ?: return 0) if (f.delete()) n++
        return n
    }

    fun countReady(ctx: Context, lines: List<String>): Int = lines.count { has(ctx, it) }

    /** Saare clips ki awaaz ek jaisi rakhne ke liye: peak ko ~22000 tak le jata hai (bahut dheemi/shor wali clip ko nahi chhedta). */
    fun normalize(pcm: ShortArray): ShortArray {
        var peak = 0
        for (v in pcm) {
            val a = Math.abs(v.toInt())
            if (a > peak) peak = a
        }
        if (peak < 200) return pcm
        val g = minOf(8.0, 22000.0 / peak)
        if (Math.abs(g - 1.0) < 0.05) return pcm
        val o = ShortArray(pcm.size)
        for (i in pcm.indices) o[i] = (pcm[i] * g).toInt().coerceIn(-32768, 32767).toShort()
        return o
    }

    /** Koi bhi audio file (wav / mp3 / m4a / ogg / aac...) ko 48 kHz mono, trim + normalize karke deta hai. */
    fun decodeToPcm(f: File): ShortArray? {
        var r: Pair<ShortArray, Int>? = null
        try {
            r = AudioUtils.readWav(f)
        } catch (_: Exception) {
        }
        if (r == null) r = decodeWithCodec(f.absolutePath)
        if (r == null) return null
        val pcm = AudioUtils.resample(r.first, r.second, AudioUtils.SR)
        return normalize(AudioUtils.trimSilence(pcm))
    }

    private fun decodeWithCodec(path: String): Pair<ShortArray, Int>? {
        val ex = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            ex.setDataSource(path)
            var track = -1
            var fmt: MediaFormat? = null
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    track = i
                    fmt = f
                    break
                }
            }
            if (track < 0 || fmt == null) return null
            ex.selectTrack(track)
            var sr = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var ch = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val c = MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME)!!)
            codec = c
            c.configure(fmt, null, null, 0)
            c.start()
            val info = MediaCodec.BufferInfo()
            val out = ByteArrayOutputStream()
            var inDone = false
            var outDone = false
            var guard = 0
            while (!outDone && guard < 2_000_000) {
                guard++
                if (!inDone) {
                    val ii = c.dequeueInputBuffer(10_000)
                    if (ii >= 0) {
                        val buf = c.getInputBuffer(ii)!!
                        val n = ex.readSampleData(buf, 0)
                        if (n < 0) {
                            c.queueInputBuffer(ii, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inDone = true
                        } else {
                            c.queueInputBuffer(ii, 0, n, ex.sampleTime, 0)
                            ex.advance()
                        }
                    }
                }
                val oi = c.dequeueOutputBuffer(info, 10_000)
                if (oi >= 0) {
                    if (info.size > 0) {
                        val ob = c.getOutputBuffer(oi)!!
                        ob.position(info.offset)
                        ob.limit(info.offset + info.size)
                        val chunk = ByteArray(info.size)
                        ob.get(chunk)
                        out.write(chunk)
                    }
                    c.releaseOutputBuffer(oi, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outDone = true
                } else if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val nf = c.outputFormat
                    if (nf.containsKey(MediaFormat.KEY_SAMPLE_RATE)) sr = nf.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    if (nf.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) ch = nf.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
            }
            val bytes = out.toByteArray()
            if (ch < 1) ch = 1
            val frames = bytes.size / 2 / ch
            if (frames <= 0) return null
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val mono = ShortArray(frames)
            for (i in 0 until frames) {
                var sum = 0
                for (k in 0 until ch) sum += bb.getShort((i * ch + k) * 2).toInt()
                mono[i] = (sum / ch).toShort()
            }
            return Pair(mono, sr)
        } catch (e: Exception) {
            return null
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { ex.release() } catch (_: Exception) {}
        }
    }
}
