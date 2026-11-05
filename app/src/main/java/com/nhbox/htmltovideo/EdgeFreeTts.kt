package com.nhbox.htmltovideo

import android.util.Base64
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * 100% Free & Unlimited Microsoft Edge Neural Text-to-Speech (No API Key Required).
 * Supports all Hindi (hi-IN) and Indian English / Hinglish (en-IN) Neural voices:
 * Swara, Madhur, Aarav, Ananya, Kavya, Kunal, Rehaan, Aarti, Arjun, Neerja, Prabhat, etc.
 *
 * Includes a 3-tier resilient synthesis pipeline:
 * 1. Microsoft Edge v2 Cognitive Services WebSocket (api.msedgeservices.com)
 * 2. Microsoft Edge v1 ReadAloud WebSocket with Sec-MS-GEC SHA-256 token (speech.platform.bing.com)
 * 3. Multi-chunk HTTPS Hindi TTS fallback if a mobile network blocks WebSocket upgrades
 */
class EdgeFreeTts(
    private val cacheDir: File,
    private val voiceSpec: String,
    private val ratePercent: Int = 0
) {

    data class EdgeVoiceItem(
        val shortName: String,
        val displayName: String,
        val gender: String,
        val locale: String,
        val badge: String,
        val description: String,
        val isHindiPrimary: Boolean = true
    ) {
        fun toSpecString(): String = "$displayName | $shortName"
    }

    fun synth(text: String): ShortArray {
        val mp3Bytes = synthMp3Bytes(text, voiceSpec, ratePercent)
        val decoded = AudioUtils.decodeAudioBytes(cacheDir, mp3Bytes)
            ?: throw Exception("Edge Neural TTS audio decode nahi ho paya.")
        val pcm = AudioUtils.resample(decoded.first, decoded.second, AudioUtils.SR)
        return AudioUtils.trimSilence(pcm)
    }

    companion object {
        private const val TRUSTED_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
        private const val CHROMIUM_VERSION = "130.0.2849.68"
        private const val GEC_VERSION = "1-$CHROMIUM_VERSION"

        val defaultHindiVoices = listOf(
            EdgeVoiceItem("hi-IN-SwaraNeural", "Swara (Hindi)", "Female", "hi-IN", "🇮🇳 100% Free • Female • Studio Hindi", "Most popular expressive Hindi voice for YouTube & tutorials", true),
            EdgeVoiceItem("hi-IN-MadhurNeural", "Madhur (Hindi)", "Male", "hi-IN", "🇮🇳 100% Free • Male • Deep Hindi", "Clear, natural & authoritative male Hindi narrator", true),
            EdgeVoiceItem("hi-IN-AaravNeural", "Aarav (Hindi)", "Male", "hi-IN", "🇮🇳 100% Free • Male • Energetic", "Modern, energetic voice for app showcases & tech videos", true),
            EdgeVoiceItem("hi-IN-AnanyaNeural", "Ananya (Hindi)", "Female", "hi-IN", "🇮🇳 100% Free • Female • Conversational", "Warm, friendly & natural Hinglish storytelling voice", true),
            EdgeVoiceItem("hi-IN-KavyaNeural", "Kavya (Hindi)", "Female", "hi-IN", "🇮🇳 100% Free • Female • Crisp Guide", "Clean & upbeat pronunciation for step-by-step guides", true),
            EdgeVoiceItem("hi-IN-KunalNeural", "Kunal (Hindi)", "Male", "hi-IN", "🇮🇳 100% Free • Male • Professional", "Balanced & confident studio explainer voice", true),
            EdgeVoiceItem("hi-IN-RehaanNeural", "Rehaan (Hindi)", "Male", "hi-IN", "🇮🇳 100% Free • Male • Creator Style", "Smooth & engaging voiceover for promo & reels", true),
            EdgeVoiceItem("hi-IN-AartiNeural", "Aarti (Hindi)", "Female", "hi-IN", "🇮🇳 100% Free • Female • Educator", "Calm & clear voice for educational tutorials", true),
            EdgeVoiceItem("hi-IN-ArjunNeural", "Arjun (Hindi)", "Male", "hi-IN", "🇮🇳 100% Free • Male • Bold Documentary", "Strong & resonant voice for intros & presentations", true),
            EdgeVoiceItem("en-IN-NeerjaNeural", "Neerja (Hinglish)", "Female", "en-IN", "🇮🇳 100% Free • Female • Indian English", "Natural Indian accent for Hinglish + technical scripts", false),
            EdgeVoiceItem("en-IN-PrabhatNeural", "Prabhat (Hinglish)", "Male", "en-IN", "🇮🇳 100% Free • Male • Indian English", "Crisp Indian male accent for coding & tech tutorials", false),
            EdgeVoiceItem("en-IN-AaravNeural", "Aarav (Hinglish)", "Male", "en-IN", "🇮🇳 100% Free • Male • Tech Hinglish", "Fast & modern Hinglish developer voice", false),
            EdgeVoiceItem("en-IN-AashiNeural", "Aashi (Hinglish)", "Female", "en-IN", "🇮🇳 100% Free • Female • Youthful", "Bright & lively Indian English / Hinglish voice", false),
            EdgeVoiceItem("en-IN-AnanyaNeural", "Ananya (Hinglish)", "Female", "en-IN", "🇮🇳 100% Free • Female • Smooth Hinglish", "Pleasant Indian accent for product demos", false)
        )

        fun resolveShortName(spec: String): String {
            val trimmed = spec.trim()
            if (trimmed.isEmpty()) return defaultHindiVoices.first().shortName
            if (trimmed.contains("|")) {
                val afterPipe = trimmed.substringAfterLast("|").trim()
                if (afterPipe.isNotEmpty()) return afterPipe
            }
            val byDisplay = defaultHindiVoices.firstOrNull {
                it.displayName.equals(trimmed, ignoreCase = true) || it.shortName.equals(trimmed, ignoreCase = true)
            }
            if (byDisplay != null) return byDisplay.shortName
            return if (trimmed.contains("-")) trimmed else "hi-IN-SwaraNeural"
        }

        /**
         * Fetches all Hindi (hi-IN) and Indian English (en-IN) Neural voices from Microsoft Edge's public voice list API,
         * merging with the 14 built-in Hindi/Hinglish voices.
         */
        fun fetchHindiVoicesList(): List<EdgeVoiceItem> {
            val result = ArrayList<EdgeVoiceItem>()
            val seen = HashSet<String>()

            val urls = listOf(
                "https://api.msedgeservices.com/tts/cognitiveservices/voices/list?Ocp-Apim-Subscription-Key=$TRUSTED_TOKEN",
                "https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/voices/list?trustedclienttoken=$TRUSTED_TOKEN&Sec-MS-GEC=${generateSecMsGec()}&Sec-MS-GEC-Version=$GEC_VERSION"
            )

            for (u in urls) {
                try {
                    val conn = (URL(u).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 12_000
                        readTimeout = 15_000
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$CHROMIUM_VERSION Safari/537.36 Edg/$CHROMIUM_VERSION")
                    }
                    if (conn.responseCode in 200..299) {
                        val txt = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        val arr = JSONArray(txt)
                        for (i in 0 until arr.length()) {
                            val o = arr.optJSONObject(i) ?: continue
                            val locale = o.optString("Locale", "")
                            if (!locale.equals("hi-IN", ignoreCase = true) && !locale.equals("en-IN", ignoreCase = true)) continue
                            val shortName = o.optString("ShortName", "").trim()
                            if (shortName.isEmpty() || !seen.add(shortName)) continue
                            val gender = o.optString("Gender", "Voice")
                            val cleanPerson = shortName
                                .removePrefix("hi-IN-")
                                .removePrefix("en-IN-")
                                .removeSuffix("Neural")
                            val langTag = if (locale.startsWith("hi", ignoreCase = true)) "Hindi" else "Hinglish"
                            val disp = "$cleanPerson ($langTag)"
                            val isHi = locale.startsWith("hi", ignoreCase = true)
                            result.add(
                                EdgeVoiceItem(
                                    shortName = shortName,
                                    displayName = disp,
                                    gender = gender,
                                    locale = locale,
                                    badge = "🇮🇳 100% Free • $gender • ${if (isHi) "Native Hindi" else "Indian Hinglish"}",
                                    description = o.optString("FriendlyName", "Microsoft Edge Neural Voice ($shortName)").take(68),
                                    isHindiPrimary = isHi
                                )
                            )
                        }
                        if (result.isNotEmpty()) break
                    }
                } catch (_: Exception) {
                    // Try next URL or fallback to built-in list
                }
            }

            for (dv in defaultHindiVoices) {
                if (seen.add(dv.shortName)) {
                    result.add(dv)
                }
            }

            return result.sortedWith(
                compareByDescending<EdgeVoiceItem> { it.isHindiPrimary }
                    .thenBy { it.displayName.lowercase() }
            )
        }

        /**
         * Generates the 64-char uppercase SHA-256 Sec-MS-GEC token from Windows file time (5-min rounded).
         */
        fun generateSecMsGec(clockSkewSec: Long = 0L): String {
            val unixSec = (System.currentTimeMillis() / 1000L) + clockSkewSec
            val winSec = unixSec + 11644473600L
            val rounded = winSec - (winSec % 300L)
            val ticks100ns = rounded * 10_000_000L
            val strToHash = "$ticks100ns$TRUSTED_TOKEN"
            val digest = MessageDigest.getInstance("SHA-256").digest(strToHash.toByteArray(Charsets.US_ASCII))
            val sb = StringBuilder(64)
            for (b in digest) {
                sb.append(String.format("%02X", b.toInt() and 0xFF))
            }
            return sb.toString()
        }

        /**
         * Synthesizes Hindi/Hinglish text into MP3 bytes (100% Free, Zero API Key).
         */
        fun synthMp3Bytes(text: String, voiceSpec: String, ratePercent: Int = 0): ByteArray {
            val cleanText = text.trim()
            if (cleanText.isEmpty()) throw Exception("Text khali hai.")
            val shortVoice = resolveShortName(voiceSpec)
            val locale = if (shortVoice.startsWith("en-IN", ignoreCase = true)) "en-IN" else "hi-IN"

            // Split long text into safe chunks (~900 chars) if needed
            val chunks = splitTextIntoChunks(cleanText, 900)
            val combinedOut = ByteArrayOutputStream()

            for (chunk in chunks) {
                val chunkBytes = synthSingleChunkWithFallbacks(chunk, shortVoice, locale, ratePercent)
                combinedOut.write(chunkBytes)
            }

            val finalBytes = combinedOut.toByteArray()
            if (finalBytes.size < 128) {
                throw Exception("Free Voice audio generate nahi ho paya. Internet connection check karein.")
            }
            return finalBytes
        }

        private fun synthSingleChunkWithFallbacks(
            chunkText: String,
            shortVoice: String,
            locale: String,
            ratePercent: Int
        ): ByteArray {
            // Tier 1: Microsoft Edge v2 WebSocket endpoint (api.msedgeservices.com)
            try {
                val b1 = synthViaEdgeWebSocket(
                    host = "api.msedgeservices.com",
                    pathAndQuery = "/tts/cognitiveservices/websocket/v1?Ocp-Apim-Subscription-Key=$TRUSTED_TOKEN&Sec-MS-GEC=${generateSecMsGec()}&Sec-MS-GEC-Version=$GEC_VERSION&ConnectionId=${UUID.randomUUID().toString().replace("-", "")}",
                    text = chunkText,
                    shortVoice = shortVoice,
                    locale = locale,
                    ratePercent = ratePercent
                )
                if (b1.size >= 128) return b1
            } catch (_: Exception) {}

            // Tier 2: Classic Microsoft Edge ReadAloud WebSocket endpoint (speech.platform.bing.com)
            try {
                val b2 = synthViaEdgeWebSocket(
                    host = "speech.platform.bing.com",
                    pathAndQuery = "/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=$TRUSTED_TOKEN&Sec-MS-GEC=${generateSecMsGec()}&Sec-MS-GEC-Version=$GEC_VERSION&ConnectionId=${UUID.randomUUID().toString().replace("-", "")}",
                    text = chunkText,
                    shortVoice = shortVoice,
                    locale = locale,
                    ratePercent = ratePercent
                )
                if (b2.size >= 128) return b2
            } catch (_: Exception) {}

            // Tier 3: HTTPS Free Hindi TTS fallback (if carrier blocks raw WebSocket upgrade)
            return synthViaHttpsFallback(chunkText, locale)
        }

        private fun synthViaEdgeWebSocket(
            host: String,
            pathAndQuery: String,
            text: String,
            shortVoice: String,
            locale: String,
            ratePercent: Int
        ): ByteArray {
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val sock = factory.createSocket(host, 443) as SSLSocket
            sock.soTimeout = 25_000
            try {
                sock.startHandshake()
                val ins = sock.inputStream
                val outs = sock.outputStream

                val wsKeyBytes = ByteArray(16)
                SecureRandom().nextBytes(wsKeyBytes)
                val wsKey = Base64.encodeToString(wsKeyBytes, Base64.NO_WRAP)

                val handshake = StringBuilder()
                    .append("GET ").append(pathAndQuery).append(" HTTP/1.1\r\n")
                    .append("Host: ").append(host).append("\r\n")
                    .append("Origin: chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold\r\n")
                    .append("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/").append(CHROMIUM_VERSION).append(" Safari/537.36 Edg/").append(CHROMIUM_VERSION).append("\r\n")
                    .append("Pragma: no-cache\r\n")
                    .append("Cache-Control: no-cache\r\n")
                    .append("Upgrade: websocket\r\n")
                    .append("Connection: Upgrade\r\n")
                    .append("Sec-WebSocket-Key: ").append(wsKey).append("\r\n")
                    .append("Sec-WebSocket-Version: 13\r\n\r\n")
                    .toString()

                outs.write(handshake.toByteArray(Charsets.US_ASCII))
                outs.flush()

                val statusHeader = readHttpHeader(ins)
                if (!statusHeader.contains("101")) {
                    throw Exception("WS handshake rejected: ${statusHeader.lines().firstOrNull()}")
                }

                val nowStr = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT+0000 (Coordinated Universal Time)'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())

                // 1. Send speech.config frame
                val configPayload = "X-Timestamp:$nowStr\r\n" +
                    "Content-Type:application/json; charset=utf-8\r\n" +
                    "Path:speech.config\r\n\r\n" +
                    "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"},\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}"
                writeMaskedTextFrame(outs, configPayload)

                // 2. Send SSML frame
                val reqId = UUID.randomUUID().toString().replace("-", "")
                val rateStr = if (ratePercent >= 0) "+$ratePercent%" else "$ratePercent%"
                val safeXmlText = escapeXml(text)
                val ssmlPayload = "X-RequestId:$reqId\r\n" +
                    "Content-Type:application/ssml+xml\r\n" +
                    "X-Timestamp:${nowStr}Z\r\n" +
                    "Path:ssml\r\n\r\n" +
                    "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='$locale'>" +
                    "<voice name='$shortVoice'>" +
                    "<prosody pitch='+0Hz' rate='$rateStr' volume='+0%'>$safeXmlText</prosody>" +
                    "</voice></speak>"
                writeMaskedTextFrame(outs, ssmlPayload)

                // 3. Read binary audio chunks until Path:turn.end
                val audioStream = ByteArrayOutputStream()
                while (true) {
                    val b0 = ins.read()
                    val b1 = ins.read()
                    if (b0 < 0 || b1 < 0) break
                    val opcode = b0 and 0x0F
                    val isMasked = (b1 and 0x80) != 0
                    var payloadLen = (b1 and 0x7F).toLong()
                    if (payloadLen == 126L) {
                        val hi = ins.read()
                        val lo = ins.read()
                        if (hi < 0 || lo < 0) break
                        payloadLen = ((hi shl 8) or lo).toLong()
                    } else if (payloadLen == 127L) {
                        val lenBuf = ByteArray(8)
                        readFully(ins, lenBuf)
                        payloadLen = ByteBuffer.wrap(lenBuf).long
                    }
                    val maskKey = if (isMasked) {
                        val mk = ByteArray(4)
                        readFully(ins, mk)
                        mk
                    } else null

                    val payload = ByteArray(payloadLen.toInt())
                    readFully(ins, payload)
                    if (maskKey != null) {
                        for (i in payload.indices) {
                            payload[i] = (payload[i].toInt() xor maskKey[i % 4].toInt()).toByte()
                        }
                    }

                    if (opcode == 0x02) {
                        // Binary frame: first 2 bytes are big-endian header length
                        if (payload.size > 2) {
                            val hdrLen = ((payload[0].toInt() and 0xFF) shl 8) or (payload[1].toInt() and 0xFF)
                            if (2 + hdrLen <= payload.size) {
                                val hdrStr = String(payload, 2, hdrLen, Charsets.US_ASCII)
                                if (hdrStr.contains("Path:audio")) {
                                    val audioOffset = 2 + hdrLen
                                    val audioLen = payload.size - audioOffset
                                    if (audioLen > 0) {
                                        audioStream.write(payload, audioOffset, audioLen)
                                    }
                                }
                            }
                        }
                    } else if (opcode == 0x01) {
                        val msg = String(payload, Charsets.UTF_8)
                        if (msg.contains("Path:turn.end")) {
                            break
                        }
                    } else if (opcode == 0x08) {
                        break
                    }
                }
                return audioStream.toByteArray()
            } finally {
                try { sock.close() } catch (_: Exception) {}
            }
        }

        private fun synthViaHttpsFallback(text: String, locale: String): ByteArray {
            val subChunks = splitTextIntoChunks(text, 180)
            val out = ByteArrayOutputStream()
            val tl = if (locale.startsWith("en", ignoreCase = true)) "en-IN" else "hi"
            for (sc in subChunks) {
                val q = URLEncoder.encode(sc, "UTF-8")
                val u = "https://translate.googleapis.com/translate_tts?ie=UTF-8&q=$q&tl=$tl&client=tw-ob"
                val conn = (URL(u).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 20_000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                }
                if (conn.responseCode in 200..299) {
                    out.write(conn.inputStream.use { it.readBytes() })
                }
            }
            return out.toByteArray()
        }

        private fun readHttpHeader(ins: InputStream): String {
            val sb = StringBuilder()
            var state = 0
            while (true) {
                val c = ins.read()
                if (c < 0) break
                val ch = c.toChar()
                sb.append(ch)
                state = when {
                    state == 0 && ch == '\r' -> 1
                    state == 1 && ch == '\n' -> 2
                    state == 2 && ch == '\r' -> 3
                    state == 3 && ch == '\n' -> 4
                    else -> if (ch == '\r') 1 else 0
                }
                if (state == 4 || sb.length > 8192) break
            }
            return sb.toString()
        }

        private fun readFully(ins: InputStream, buf: ByteArray) {
            var off = 0
            while (off < buf.size) {
                val n = ins.read(buf, off, buf.size - off)
                if (n < 0) throw Exception("Unexpected EOF")
                off += n
            }
        }

        private fun writeMaskedTextFrame(outs: OutputStream, text: String) {
            val data = text.toByteArray(Charsets.UTF_8)
            val mask = ByteArray(4)
            SecureRandom().nextBytes(mask)

            val frame = ByteArrayOutputStream(data.size + 14)
            frame.write(0x81) // FIN + Text opcode
            when {
                data.size <= 125 -> {
                    frame.write(0x80 or data.size)
                }
                data.size <= 65535 -> {
                    frame.write(0x80 or 126)
                    frame.write((data.size shr 8) and 0xFF)
                    frame.write(data.size and 0xFF)
                }
                else -> {
                    frame.write(0x80 or 127)
                    val lb = ByteBuffer.allocate(8).putLong(data.size.toLong()).array()
                    frame.write(lb)
                }
            }
            frame.write(mask)
            val maskedData = ByteArray(data.size)
            for (i in data.indices) {
                maskedData[i] = (data[i].toInt() xor mask[i % 4].toInt()).toByte()
            }
            frame.write(maskedData)
            outs.write(frame.toByteArray())
            outs.flush()
        }

        private fun escapeXml(s: String): String {
            return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
        }

        private fun splitTextIntoChunks(text: String, maxLen: Int): List<String> {
            if (text.length <= maxLen) return listOf(text)
            val out = ArrayList<String>()
            val sentences = text.split(Regex("(?<=[.!?।])\\s+"))
            var cur = StringBuilder()
            for (s in sentences) {
                if (cur.length + s.length + 1 > maxLen && cur.isNotEmpty()) {
                    out.add(cur.toString().trim())
                    cur = StringBuilder()
                }
                if (cur.isNotEmpty()) cur.append(" ")
                cur.append(s)
            }
            if (cur.isNotEmpty()) out.add(cur.toString().trim())
            return if (out.isEmpty()) listOf(text.take(maxLen)) else out
        }
    }
}
