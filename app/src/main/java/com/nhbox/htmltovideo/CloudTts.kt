package com.nhbox.htmltovideo

import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Natural insaani awaaz ke liye Google Cloud Text-to-Speech (Neural2 / Wavenet / Chirp voices).
 * Internet aur aapki apni API key chahiye. Awaaz WAV (LINEAR16) mein aati hai.
 * Background thread se chalao.
 */
class CloudTts(
    private val apiKey: String,
    private val lang: String,
    private val voice: String,
    private val rate: Double
) {
    fun synth(text: String): ShortArray {
        val url = URL("https://texttospeech.googleapis.com/v1/text:synthesize?key=" + URLEncoder.encode(apiKey.trim(), "UTF-8"))
        val body = JSONObject()
            .put("input", JSONObject().put("text", text))
            .put("voice", JSONObject().put("languageCode", lang).put("name", voice.trim()))
            .put("audioConfig", JSONObject()
                .put("audioEncoding", "LINEAR16")
                .put("sampleRateHertz", 24000)
                .put("speakingRate", rate))
        val c = url.openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.doOutput = true
        c.connectTimeout = 15000
        c.readTimeout = 60000
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val txt = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) {
            val m = try { JSONObject(txt).getJSONObject("error").getString("message") } catch (e: Exception) { txt.take(150) }
            throw Exception("Google TTS error $code: $m")
        }
        val bytes = Base64.decode(JSONObject(txt).getString("audioContent"), Base64.DEFAULT)
        val wav = AudioUtils.readWavBytes(bytes) ?: throw Exception("Google TTS ki audio samajh nahi aayi")
        val pcm = AudioUtils.resample(wav.first, wav.second, AudioUtils.SR)
        return AudioUtils.trimSilence(pcm)
    }
}
