package com.nhbox.htmltovideo

import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * ElevenLabs.io API integration for NH Video Edit (v4.2).
 * Supports:
 * - Fetching all Hindi & Multilingual voices from https://api.elevenlabs.io/v1/voices (plus shared Hindi voices)
 * - Curated starter catalog of 14 verified ElevenLabs Hindi-capable voices
 * - Live Hindi voice preview (MP3 generation + preview_url fallback)
 * - Full 48kHz mono PCM synthesis for MP4 video export
 */
class ElevenLabsTts(
    private val cacheDir: File,
    private val apiKey: String,
    private val voiceSpec: String,
    private val modelId: String = "eleven_multilingual_v2"
) {

    data class VoiceItem(
        val voiceId: String,
        val name: String,
        val gender: String,
        val badge: String,
        val description: String,
        val previewUrl: String = "",
        val isHindiPriority: Boolean = false
    ) {
        fun toSpecString(): String = "$name | $voiceId"
    }

    fun synth(text: String): ShortArray {
        val bytes = synthMp3Bytes(apiKey, text, voiceSpec, modelId)
        val decoded = AudioUtils.decodeAudioBytes(cacheDir, bytes)
            ?: throw Exception("ElevenLabs audio decode nahi ho paya.")
        val pcm = AudioUtils.resample(decoded.first, decoded.second, AudioUtils.SR)
        return AudioUtils.trimSilence(pcm)
    }

    companion object {
        const val MODEL_MULTILINGUAL_V2 = "eleven_multilingual_v2"
        const val MODEL_FLASH_V2_5 = "eleven_flash_v2_5"

        val defaultHindiVoices = listOf(
            VoiceItem("9BWtsMINqrJLrRacOk9x", "Aria", "Female", "🇮🇳 Hindi & Hinglish • Expressive", "Best for natural Hinglish tutorials & app showcases", "", true),
            VoiceItem("nPczCjzI2devNBz1zQrb", "Brian", "Male", "🇮🇳 Hindi & Hinglish • Deep Studio", "Deep, resonant & clear male voice for tech tutorials", "", true),
            VoiceItem("EXAVITQu4vr4xnSDxMaL", "Sarah", "Female", "🇮🇳 Hindi & Hinglish • Friendly Guide", "Warm, friendly & crystal-clear step-by-step narration", "", true),
            VoiceItem("TX3LPaxmHKxFdv7VOQHJ", "Liam", "Male", "🇮🇳 Hindi & Hinglish • YouTube Creator", "Energetic & engaging creator voice for promo videos", "", true),
            VoiceItem("CwhRBWXzGAHq8TQ4Fs17", "Roger", "Male", "🇮🇳 Hindi & Hinglish • Calm Mentor", "Balanced, confident & smooth male narrator", "", true),
            VoiceItem("FGY2WhTYpPnrIDTdsKH5", "Laura", "Female", "🇮🇳 Hindi & Hinglish • Upbeat Modern", "Bright, energetic female voice for UI/UX showcases", "", true),
            VoiceItem("onwK4e9ZLuTAKqWW03F9", "Daniel", "Male", "🇮🇳 Hindi & Hinglish • Studio Anchor", "Articulate, crisp professional broadcast voice", "", true),
            VoiceItem("XrExE9yKIg1WjnnlVkGX", "Matilda", "Female", "🇮🇳 Hindi & Hinglish • Professional", "Knowledgeable & warm instructor voice", "", true),
            VoiceItem("IKne3meq5aSn9XLyUdCD", "Charlie", "Male", "🇮🇳 Hindi & Hinglish • Fast & Natural", "Conversational & lively narration style", "", false),
            VoiceItem("Xb7hH8MSUJpSbSDYk0k2", "Alice", "Female", "🇮🇳 Hindi & Hinglish • Clear Educator", "Clean pronunciation for coding & setup guides", "", false),
            VoiceItem("JBFqnCBsd6RMkjVDRZzb", "George", "Male", "🇮🇳 Hindi & Hinglish • Warm Storyteller", "Rich & authoritative male voiceover", "", false),
            VoiceItem("pFZP5JQG7iQjIQuC4Bku", "Lily", "Female", "🇮🇳 Hindi & Hinglish • Smooth Velvety", "Soft, pleasant narration for product demos", "", false),
            VoiceItem("iP95p4xoKVk53GoZ742B", "Chris", "Male", "🇮🇳 Hindi & Hinglish • Trusted Guide", "Natural everyday male voice for walkthroughs", "", false),
            VoiceItem("pqHfZKP75CvOlQylNhV4", "Bill", "Male", "🇮🇳 Hindi & Hinglish • Documentary", "Deep, mature & impactful voiceover", "", false)
        )

        fun resolveVoiceId(spec: String): String {
            val trimmed = spec.trim()
            if (trimmed.isEmpty()) return defaultHindiVoices.first().voiceId
            if (trimmed.contains("|")) {
                val afterPipe = trimmed.substringAfterLast("|").trim()
                if (afterPipe.isNotEmpty()) return afterPipe
            }
            val byName = defaultHindiVoices.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
            if (byName != null) return byName.voiceId
            return trimmed
        }

        /**
         * Fetches all available voices from the user's ElevenLabs account (/v1/voices)
         * plus shared Hindi library voices (/v1/shared-voices?language=hi) when accessible,
         * sorting Hindi/Indian/Cloned voices to the top.
         */
        fun fetchVoicesFromApi(apiKey: String): List<VoiceItem> {
            val cleanKey = apiKey.trim()
            if (cleanKey.isEmpty()) {
                throw Exception("Pehle ElevenLabs API Key enter karein.")
            }

            val conn = (URL("https://api.elevenlabs.io/v1/voices").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20_000
                readTimeout = 30_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("xi-api-key", cleanKey)
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val respText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val msg = try {
                    val j = JSONObject(respText)
                    val det = j.opt("detail")
                    when (det) {
                        is JSONObject -> det.optString("message", respText.take(160))
                        is String -> det
                        else -> respText.take(160)
                    }
                } catch (_: Exception) {
                    respText.take(160)
                }
                throw Exception("ElevenLabs API Error ($code): $msg")
            }

            val fetched = ArrayList<VoiceItem>()
            val seenIds = HashSet<String>()

            val root = JSONObject(respText)
            val arr = root.optJSONArray("voices")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val v = arr.optJSONObject(i) ?: continue
                    val id = v.optString("voice_id", "").trim()
                    val name = v.optString("name", "").trim()
                    if (id.isEmpty() || name.isEmpty() || !seenIds.add(id)) continue

                    val category = v.optString("category", "premade").lowercase()
                    val previewUrl = v.optString("preview_url", "").trim()
                    val labels = v.optJSONObject("labels")
                    val gender = labels?.optString("gender", "")?.replaceFirstChar { it.uppercase() } ?: ""
                    val accent = labels?.optString("accent", "") ?: ""
                    val useCase = labels?.optString("use_case", labels.optString("use case", "")) ?: ""
                    val desc = labels?.optString("description", "") ?: v.optString("description", "")
                    val langLabel = labels?.optString("language", "") ?: ""

                    val combinedMeta = "$name $category $accent $useCase $desc $langLabel".lowercase()
                    val isHindiOrCustom = combinedMeta.contains("hindi") ||
                        combinedMeta.contains("india") ||
                        combinedMeta.contains("hinglish") ||
                        langLabel.equals("hi", ignoreCase = true) ||
                        category == "cloned" ||
                        category == "generated" ||
                        category == "professional"

                    val badgeParts = ArrayList<String>()
                    if (category == "cloned" || category == "professional") {
                        badgeParts.add("🎙 My Custom Voice")
                    } else if (combinedMeta.contains("hindi") || combinedMeta.contains("india")) {
                        badgeParts.add("🇮🇳 Native Hindi / Indian")
                    } else {
                        badgeParts.add("🇮🇳 Hindi Multilingual v2")
                    }
                    if (gender.isNotBlank()) badgeParts.add(gender)
                    if (accent.isNotBlank()) badgeParts.add(accent.replaceFirstChar { it.uppercase() })

                    val subDesc = listOf(useCase, desc).filter { it.isNotBlank() }.joinToString(" • ").ifBlank {
                        "Supports natural Hindi & Hinglish speech"
                    }

                    fetched.add(
                        VoiceItem(
                            voiceId = id,
                            name = name,
                            gender = gender.ifBlank { "Voice" },
                            badge = badgeParts.joinToString(" • "),
                            description = subDesc.take(72),
                            previewUrl = previewUrl,
                            isHindiPriority = isHindiOrCustom
                        )
                    )
                }
            }

            // Also try fetching shared Hindi voices from ElevenLabs Voice Library if key has permission
            try {
                val shConn = (URL("https://api.elevenlabs.io/v1/shared-voices?language=hi&page_size=20").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("xi-api-key", cleanKey)
                }
                if (shConn.responseCode in 200..299) {
                    val shTxt = shConn.inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                    val shArr = JSONObject(shTxt).optJSONArray("voices")
                    if (shArr != null) {
                        for (i in 0 until shArr.length()) {
                            val sv = shArr.optJSONObject(i) ?: continue
                            val id = sv.optString("voice_id", "").trim()
                            val name = sv.optString("name", "").trim()
                            if (id.isEmpty() || name.isEmpty() || !seenIds.add(id)) continue
                            val gender = sv.optString("gender", "").replaceFirstChar { it.uppercase() }
                            val accent = sv.optString("accent", "Indian").replaceFirstChar { it.uppercase() }
                            val desc = sv.optString("description", "Native Hindi Voice Library").take(72)
                            val prev = sv.optString("preview_url", "").trim()
                            fetched.add(
                                VoiceItem(
                                    voiceId = id,
                                    name = name,
                                    gender = gender.ifBlank { "Hindi" },
                                    badge = "🇮🇳 Hindi Library • ${gender.ifBlank { accent }}",
                                    description = desc,
                                    previewUrl = prev,
                                    isHindiPriority = true
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore if shared-voices requires higher tier permission
            }

            // Merge with defaultHindiVoices so none of the core voices are missed
            for (dv in defaultHindiVoices) {
                if (seenIds.add(dv.voiceId)) {
                    fetched.add(dv)
                }
            }

            return fetched.sortedWith(
                compareByDescending<VoiceItem> { it.isHindiPriority }
                    .thenBy { it.name.lowercase() }
            )
        }

        /**
         * Synthesizes Hindi/Hinglish text into MP3 bytes using ElevenLabs Text-to-Speech API.
         */
        fun synthMp3Bytes(
            apiKey: String,
            text: String,
            voiceSpec: String,
            modelId: String = MODEL_MULTILINGUAL_V2
        ): ByteArray {
            val cleanKey = apiKey.trim()
            if (cleanKey.isEmpty()) {
                throw Exception("ElevenLabs API key khali hai. Settings mein ElevenLabs API key enter karein.")
            }
            val vId = resolveVoiceId(voiceSpec)
            val useModel = modelId.trim().ifEmpty { MODEL_MULTILINGUAL_V2 }

            val endpoint = "https://api.elevenlabs.io/v1/text-to-speech/$vId?output_format=mp3_44100_128"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 20_000
                readTimeout = 90_000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "audio/mpeg")
                setRequestProperty("xi-api-key", cleanKey)
            }

            val voiceSettings = JSONObject()
                .put("stability", 0.50)
                .put("similarity_boost", 0.78)
                .put("style", 0.15)
                .put("use_speaker_boost", true)

            val body = JSONObject()
                .put("text", text)
                .put("model_id", useModel)
                .put("voice_settings", voiceSettings)

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                val errTxt = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                val msg = try {
                    val j = JSONObject(errTxt)
                    val det = j.opt("detail")
                    when (det) {
                        is JSONObject -> det.optString("message", errTxt.take(160))
                        is String -> det
                        else -> errTxt.take(160)
                    }
                } catch (_: Exception) {
                    errTxt.take(160)
                }
                throw Exception("ElevenLabs TTS Error ($code): $msg")
            }

            val bytes = conn.inputStream.use { it.readBytes() }
            if (bytes.size < 128) {
                throw Exception("ElevenLabs ne khali audio return kiya.")
            }
            return bytes
        }
    }
}
