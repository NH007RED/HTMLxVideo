package com.nhbox.htmltovideo

import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sarvam AI (Bulbul v3) se natural Hindi awaaz. Signup par free credits milte hain.
 * Awaaz WAV (base64) mein aati hai. Background thread se chalao.
 */
class SarvamTts(
    private val apiKey: String,
    private val lang: String,
    private val speaker: String,
    private val pace: Double
) {
    fun synth(text: String): ShortArray {
        val body = JSONObject()
            .put("text", text)
            .put("language_code", lang)
            .put("speaker", speaker.trim().lowercase())
            .put("model", "bulbul:v3")
            .put("pace", pace)
            .put("speech_sample_rate", 24000)
            .put("output_audio_codec", "wav")
        val c = URL("https://api.sarvam.ai/text-to-speech").openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.doOutput = true
        c.connectTimeout = 15000
        c.readTimeout = 90000
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        c.setRequestProperty("api-subscription-key", apiKey.trim())
        c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val txt = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) {
            val m = try {
                val e = JSONObject(txt).get("error")
                if (e is JSONObject) e.optString("message", txt.take(150)) else e.toString()
            } catch (ex: Exception) {
                txt.take(150)
            }
            throw Exception("Sarvam TTS error $code: $m")
        }
        val arr = JSONObject(txt).getJSONArray("audios")
        val parts = ArrayList<ShortArray>()
        var sr = 24000
        for (i in 0 until arr.length()) {
            val wav = AudioUtils.readWavBytes(Base64.decode(arr.getString(i), Base64.DEFAULT))
                ?: throw Exception("Sarvam audio samajh nahi aayi")
            sr = wav.second
            parts.add(wav.first)
        }
        if (parts.isEmpty()) throw Exception("Sarvam ne audio nahi di")
        val all = ShortArray(parts.sumOf { it.size })
        var p = 0
        for (a in parts) {
            System.arraycopy(a, 0, all, p, a.size)
            p += a.size
        }
        return AudioUtils.trimSilence(AudioUtils.resample(all, sr, AudioUtils.SR))
    }
}
