package com.nhbox.htmltovideo

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * v3.3: free cloud server (Hugging Face Space) se aapki awaaz mein clip banwane ka client.
 * Server ki files project mein hf_space/ folder mein hain. Android par koi library nahi chahiye (sirf HttpURLConnection).
 *   GET  /health                      -> {"ok":true,...}
 *   POST /register  (multipart sample) -> {"voice_id":"..."}
 *   POST /synth     (json voice_id,text) -> audio/wav
 */
class CloudVoice(baseUrl: String, private val token: String) {
    class UnknownVoice : Exception("Server ko ye voice yaad nahi (restart hua hoga)")
    class HttpFail(val code: Int, msg: String) : IOException(msg)

    private val base = baseUrl.trim().trimEnd('/')

    private fun open(path: String, method: String, readMs: Int): HttpURLConnection {
        val c = URL(base + path).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 30_000
        c.readTimeout = readMs
        c.setRequestProperty("X-Token", token)
        c.setRequestProperty("User-Agent", "HTML2Video/3.3")
        return c
    }

    private fun readAll(c: HttpURLConnection): ByteArray {
        val ins = if (c.responseCode in 200..299) c.inputStream else (c.errorStream ?: return ByteArray(0))
        val out = ByteArrayOutputStream()
        ins.use { s ->
            val buf = ByteArray(16 * 1024)
            while (true) {
                val n = s.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
            }
        }
        return out.toByteArray()
    }

    private fun fail(c: HttpURLConnection, body: ByteArray): Nothing {
        val code = c.responseCode
        val txt = String(body, Charsets.UTF_8).take(200).replace(Regex("\\s+"), " ")
        when (code) {
            401 -> throw HttpFail(code, "Token galat hai")
            else -> throw HttpFail(code, "Server error $code: $txt")
        }
    }

    fun health(): String {
        val c = open("/health", "GET", 120_000)
        val body = readAll(c)
        if (c.responseCode !in 200..299) fail(c, body)
        return String(body, Charsets.UTF_8)
    }

    fun register(sample: ByteArray, filename: String): String {
        val boundary = "----h2v" + System.nanoTime()
        val c = open("/register", "POST", 300_000)
        c.doOutput = true
        c.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        val safeName = filename.replace("\"", "_").ifEmpty { "sample.bin" }
        c.outputStream.use { o ->
            o.write(("--$boundary\r\nContent-Disposition: form-data; name=\"sample\"; filename=\"$safeName\"\r\nContent-Type: application/octet-stream\r\n\r\n").toByteArray())
            o.write(sample)
            o.write("\r\n--$boundary--\r\n".toByteArray())
        }
        val body = readAll(c)
        if (c.responseCode !in 200..299) fail(c, body)
        val m = Regex("\"voice_id\"\\s*:\\s*\"([^\"]+)\"").find(String(body, Charsets.UTF_8))
            ?: throw IOException("Server ne voice_id nahi diya")
        return m.groupValues[1]
    }

    fun synth(voiceId: String, text: String, baseVoice: String = "", rate: String = ""): ByteArray {
        val json = StringBuilder("{")
        json.append("\"voice_id\":").append(q(voiceId)).append(",\"text\":").append(q(text))
        if (baseVoice.isNotBlank()) json.append(",\"base_voice\":").append(q(baseVoice))
        if (rate.isNotBlank()) json.append(",\"rate\":").append(q(rate))
        json.append("}")
        val c = open("/synth", "POST", 240_000)
        c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        c.outputStream.use { it.write(json.toString().toByteArray(Charsets.UTF_8)) }
        val body = readAll(c)
        if (c.responseCode == 404) throw UnknownVoice()
        if (c.responseCode !in 200..299) fail(c, body)
        if (body.size < 1000) throw IOException("Server ne khali audio diya")
        return body
    }

    private fun q(s: String): String {
        val sb = StringBuilder("\"")
        for (ch in s) {
            when {
                ch == '\\' -> sb.append("\\\\")
                ch == '"' -> sb.append("\\\"")
                ch == '\n' -> sb.append("\\n")
                ch == '\r' -> sb.append("\\r")
                ch == '\t' -> sb.append("\\t")
                ch < ' ' -> sb.append(String.format("\\u%04x", ch.code))
                else -> sb.append(ch)
            }
        }
        return sb.append('"').toString()
    }
}
