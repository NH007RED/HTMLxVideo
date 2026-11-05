package com.nhbox.htmltovideo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Base64
import android.view.Surface
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 1920x1080 H.264 video (60 fps) + AAC audio ko ek MP4 file mein likhta hai.
 * Video ka time frame number se tay hota hai (frame / fps), isliye awaaz aur video hamesha sync rehte hain.
 */
class VideoExporter(
    private val outFile: File,
    private val fps: Int,
    private val audio: ShortArray?,
    /** true = 9:16 (1080x1920) vertical video. false = 16:9 (1920x1080), pehle jaisa. */
    private val portrait: Boolean = false,
    /** Sirf 9:16 mein: true = poori screen bharo (side se crop), false = poora frame beech mein + blur background. */
    private val fill: Boolean = false
) {
    companion object {
        const val W = 1920
        const val H = 1080
    }

    // Output size: 16:9 ya 9:16
    private val outW = if (portrait) H else W
    private val outH = if (portrait) W else H

    private class Sample(val data: ByteArray, val pts: Long, val flags: Int)

    private val aSamples = ArrayList<Sample>()
    private var aFormat: MediaFormat? = null
    private var venc: MediaCodec? = null
    private var surface: Surface? = null
    private var muxer: MediaMuxer? = null
    private var vTrack = -1
    private var aTrack = -1
    private var started = false
    private var frameIdx = 0L
    private var aIdx = 0
    private val info = MediaCodec.BufferInfo()
    private val ainfo = MediaCodec.BufferInfo()
    private val dst = Rect(0, 0, outW, outH)
    // 9:16 ke liye helpers
    private val pPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val shade = Paint().apply { color = Color.argb(110, 0, 0, 0) }
    private var bgMid: Bitmap? = null
    private var bgSmall: Bitmap? = null
    private var reuse: Bitmap? = null
    private val opts = BitmapFactory.Options()

    fun prepare() {
        if (audio != null) encodeAudio(audio)

        var created: MediaCodec? = null
        var lastErr: Exception? = null
        for (high in listOf(true, false)) {
            try {
                val c = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                val f = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, outW, outH)
                f.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                f.setInteger(MediaFormat.KEY_BIT_RATE, 18_000_000)
                f.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                f.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                if (high) {
                    f.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
                    f.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel42)
                }
                try {
                    c.configure(f, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    created = c
                    break
                } catch (e: Exception) {
                    lastErr = e
                    c.release()
                }
            } catch (e: Exception) {
                lastErr = e
            }
        }
        val enc = created ?: throw Exception("Phone 1080p 60fps H.264 encoder nahi chala paya: ${lastErr?.message}")
        venc = enc
        surface = enc.createInputSurface()
        enc.start()
        muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    /** JS se aaya JPEG frame (data URL) encoder ko deta hai. */
    fun addFrame(dataUrl: String) {
        val comma = dataUrl.indexOf(',')
        val bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT)
        opts.inMutable = true
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888
        opts.inBitmap = reuse
        val bmp = try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (e: IllegalArgumentException) {
            opts.inBitmap = null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } ?: throw Exception("Frame decode nahi hua")
        reuse = bmp
        drawFrame(bmp)
    }

    /** v3: DOM/WebView se seedha Bitmap aata hai (JPEG ke bina). Bitmap ko recycle mat karo, caller dobara use karta hai. */
    fun addBitmap(bmp: Bitmap) {
        drawFrame(bmp)
    }

    private fun drawFrame(bmp: Bitmap) {
        val s = surface ?: throw Exception("Encoder surface nahi hai")
        val c = try {
            s.lockHardwareCanvas()
        } catch (e: Exception) {
            s.lockCanvas(null)
        }
        if (!portrait) {
            c.drawBitmap(bmp, null, dst, null)
        } else {
            drawPortrait(c, bmp)
        }
        s.unlockCanvasAndPost(c)
        drain(false)
    }

    /** 16:9 frame ko 1080x1920 canvas par rakhta hai (Fill = crop, warna Fit = beech mein + blur background). */
    private fun drawPortrait(c: Canvas, bmp: Bitmap) {
        val bw = bmp.width
        val bh = bmp.height
        // Beech ka 9:16 hissa (crop area)
        var cw = bh * 9 / 16
        if (cw > bw) cw = bw
        val cl = (bw - cw) / 2
        val cropRect = Rect(cl, 0, cl + cw, bh)
        if (fill) {
            c.drawBitmap(bmp, cropRect, dst, pPaint)
            return
        }
        // Blur background: crop ko chhota karke (2 step) wapas bada karte hain
        val mid = bgMid ?: Bitmap.createBitmap(216, 384, Bitmap.Config.ARGB_8888).also { bgMid = it }
        val small = bgSmall ?: Bitmap.createBitmap(54, 96, Bitmap.Config.ARGB_8888).also { bgSmall = it }
        Canvas(mid).drawBitmap(bmp, cropRect, Rect(0, 0, mid.width, mid.height), pPaint)
        Canvas(small).drawBitmap(mid, null, Rect(0, 0, small.width, small.height), pPaint)
        c.drawBitmap(small, null, dst, pPaint)
        c.drawRect(dst, shade)
        // Poora 16:9 frame beech mein (width = 1080)
        val fh = (outW.toLong() * bh / bw).toInt()
        val top = (outH - fh) / 2
        c.drawBitmap(bmp, null, Rect(0, top, outW, top + fh), pPaint)
    }

    private fun startMuxer(fmt: MediaFormat) {
        val m = muxer ?: return
        vTrack = m.addTrack(fmt)
        aFormat?.let { aTrack = m.addTrack(it) }
        m.start()
        started = true
    }

    private fun writeAudioUpTo(limitUs: Long) {
        val m = muxer ?: return
        if (!started || aTrack < 0) return
        while (aIdx < aSamples.size && aSamples[aIdx].pts <= limitUs) {
            val s = aSamples[aIdx]
            ainfo.set(0, s.data.size, s.pts, s.flags)
            m.writeSampleData(aTrack, ByteBuffer.wrap(s.data), ainfo)
            aIdx++
        }
    }

    private fun drain(eos: Boolean) {
        val enc = venc ?: return
        val m = muxer ?: return
        var idle = 0
        while (true) {
            val idx = enc.dequeueOutputBuffer(info, if (eos) 10_000L else 0L)
            if (idx == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!eos) return
                idle++
                if (idle > 600) throw Exception("Video encoder band nahi hua (timeout)")
            } else if (idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                startMuxer(enc.outputFormat)
            } else if (idx >= 0) {
                idle = 0
                val buf = enc.getOutputBuffer(idx)
                if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) info.size = 0
                if (info.size > 0 && started && buf != null) {
                    buf.position(info.offset)
                    buf.limit(info.offset + info.size)
                    info.presentationTimeUs = frameIdx * 1_000_000L / fps
                    frameIdx++
                    m.writeSampleData(vTrack, buf, info)
                    writeAudioUpTo(info.presentationTimeUs)
                }
                val end = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                enc.releaseOutputBuffer(idx, false)
                if (end) return
            }
        }
    }

    /** Sab frames dene ke baad MP4 band karta hai aur file wapas deta hai. */
    fun finish(): File {
        if (frameIdx == 0L && !started) {
            // koi frame nahi aaya
        }
        venc?.signalEndOfInputStream()
        drain(true)
        writeAudioUpTo(Long.MAX_VALUE)
        if (!started) throw Exception("Koi video frame encode nahi hua")
        muxer?.stop()
        release()
        return outFile
    }

    fun abort() {
        release()
        outFile.delete()
    }

    private fun release() {
        try { muxer?.release() } catch (_: Exception) {}
        try { venc?.stop() } catch (_: Exception) {}
        try { venc?.release() } catch (_: Exception) {}
        try { surface?.release() } catch (_: Exception) {}
        muxer = null
        venc = null
        surface = null
    }

    private fun encodeAudio(pcm: ShortArray) {
        val fmt = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, AudioUtils.SR, 1)
        fmt.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        fmt.setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
        fmt.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        val c = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        c.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        c.start()
        val bi = MediaCodec.BufferInfo()
        var pos = 0
        var inDone = false
        var outDone = false
        var guard = 0
        while (!outDone) {
            if (!inDone) {
                val ii = c.dequeueInputBuffer(10_000)
                if (ii >= 0) {
                    val b = c.getInputBuffer(ii)!!
                    b.clear()
                    b.order(ByteOrder.nativeOrder())
                    val n = minOf(pcm.size - pos, b.capacity() / 2)
                    val pts = pos * 1_000_000L / AudioUtils.SR
                    if (n <= 0) {
                        c.queueInputBuffer(ii, 0, 0, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inDone = true
                    } else {
                        b.asShortBuffer().put(pcm, pos, n)
                        c.queueInputBuffer(ii, 0, n * 2, pts, 0)
                        pos += n
                    }
                }
            }
            val oi = c.dequeueOutputBuffer(bi, 10_000)
            if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                aFormat = c.outputFormat
            } else if (oi >= 0) {
                if (bi.size > 0 && (bi.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    val ob = c.getOutputBuffer(oi)!!
                    ob.position(bi.offset)
                    ob.limit(bi.offset + bi.size)
                    val arr = ByteArray(bi.size)
                    ob.get(arr)
                    aSamples.add(Sample(arr, bi.presentationTimeUs, bi.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM.inv()))
                }
                if ((bi.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outDone = true
                c.releaseOutputBuffer(oi, false)
            } else {
                guard++
                if (guard > 3000) throw Exception("Audio encoder atak gaya")
            }
        }
        c.stop()
        c.release()
        if (aFormat == null) throw Exception("AAC audio format nahi mila")
    }
}
