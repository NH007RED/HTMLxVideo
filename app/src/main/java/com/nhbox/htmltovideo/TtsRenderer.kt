package com.nhbox.htmltovideo

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TtsMissingException(msg: String) : Exception(msg)

/**
 * Android ka apna Text-to-Speech engine use karke har line ki awaaz WAV file mein banata hai.
 * Isse awaaz seedha file mein aati hai, isliye audio capture ki koi dikkat nahi hoti.
 * Ye class background thread se chalani hai (main thread se nahi).
 */
class TtsRenderer(private val ctx: Context) {
    private var tts: TextToSpeech? = null
    private val latches = ConcurrentHashMap<String, CountDownLatch>()
    private val errors = ConcurrentHashMap<String, Boolean>()

    fun init(lang: String, rate: Float) {
        val latch = CountDownLatch(1)
        var ok = false
        tts = TextToSpeech(ctx.applicationContext) { status ->
            ok = (status == TextToSpeech.SUCCESS)
            latch.countDown()
        }
        if (!latch.await(15, TimeUnit.SECONDS) || !ok) {
            throw TtsMissingException("TTS engine start nahi hua. Phone mein Google Text-to-speech install/enable karo.")
        }
        val t = tts!!
        val r = t.setLanguage(Locale.forLanguageTag(lang))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            throw TtsMissingException("Hindi (TTS) voice is phone mein nahi hai. 'TTS settings' kholo aur Hindi voice data download karo.")
        }
        t.setSpeechRate(rate)
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                latches[utteranceId ?: return]?.countDown()
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                val id = utteranceId ?: return
                errors[id] = true
                latches[id]?.countDown()
            }
            override fun onError(utteranceId: String?, errorCode: Int) {
                onError(utteranceId)
            }
        })
    }

    /** Ek line ki awaaz banakar 48 kHz mono PCM deta hai. Fail hone par null. */
    fun synth(key: String, text: String): ShortArray? {
        val t = tts ?: return null
        val f = File(ctx.cacheDir, "tts_$key.wav")
        f.delete()
        val id = "u_$key"
        val l = CountDownLatch(1)
        latches[id] = l
        val r = t.synthesizeToFile(text, Bundle(), f, id)
        if (r != TextToSpeech.SUCCESS) return null
        l.await(90, TimeUnit.SECONDS)
        if (errors[id] == true || !f.exists() || f.length() < 100) return null
        val wav = AudioUtils.readWav(f) ?: return null
        f.delete()
        val pcm = AudioUtils.resample(wav.first, wav.second, AudioUtils.SR)
        return AudioUtils.trimSilence(pcm)
    }

    fun shutdown() {
        try { tts?.stop() } catch (_: Exception) {}
        try { tts?.shutdown() } catch (_: Exception) {}
        tts = null
    }
}
