package com.nhbox.htmltovideo

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cartesia.ai (https://play.cartesia.ai/text-to-speech) Sonic Hindi & Hinglish TTS API integration for NH Video Edit (v4.4).
 * Supports:
 * - Fetching all Hindi (hi), Indian-accented, Cloned & Multilingual voices from https://api.cartesia.ai/voices
 * - Pre-loaded catalog of 14 verified Cartesia Native Hindi, Indian & Sonic-3.6 Hindi/Hinglish voices
 * - Live Hindi voice preview (24kHz WAV synthesis via POST https://api.cartesia.ai/tts/bytes)
 * - Full 48kHz mono PCM synthesis for MP4 video export
 */
class CartesiaTts(
    private val cacheDir: File,
    private val apiKey: String,
    private val voiceSpec: String,
    private val modelId: String = MODEL_SONIC_3
) {

    data class CartesiaVoiceItem(
        val voiceId: String,
        val name: String,
        val gender: String,
        val badge: String,
        val description: String,
        val isHindiPriority: Boolean = true
    ) {
        fun toSpecString(): String = "$name | $voiceId"
    }

    fun synth(text: String): ShortArray {
        val wavBytes = synthWavBytes(apiKey, text, voiceSpec, modelId)
        val decoded = AudioUtils.readWavBytes(wavBytes)
            ?: AudioUtils.decodeAudioBytes(cacheDir, wavBytes)
            ?: throw Exception("Cartesia audio decode nahi ho paya.")
        val pcm = AudioUtils.resample(decoded.first, decoded.second, AudioUtils.SR)
        return AudioUtils.trimSilence(pcm)
    }

    companion object {
        const val MODEL_SONIC_3 = "sonic-3"
        const val MODEL_SONIC_2 = "sonic-2"
        const val MODEL_SONIC_TURBO = "sonic-turbo"

        val defaultHindiVoices = listOf(
            CartesiaVoiceItem(
                "c1abd502-9231-4558-a054-10ac950c356d",
                "Hindi Narrator Woman",
                "Female",
                "🇮🇳 Native Hindi • Female • Expressive Narrator",
                "Official Cartesia native Hindi female narrator for tutorials & stories",
                true
            ),
            CartesiaVoiceItem(
                "bdab08ad-4137-4548-b9db-6142854c7525",
                "Hindi Reporter Man",
                "Male",
                "🇮🇳 Native Hindi • Male • Crisp Explainer",
                "Official Cartesia native Hindi male reporter & tech presenter",
                true
            ),
            CartesiaVoiceItem(
                "638efaaa-4d0c-442e-b701-3fae16aad012",
                "Indian Man (Hinglish)",
                "Male",
                "🇮🇳 Indian Accent • Male • Natural Hinglish",
                "Warm Indian male voice ideal for Hinglish & coding tutorials",
                true
            ),
            CartesiaVoiceItem(
                "3b554273-4299-48b9-9aaf-eefd438e3941",
                "Indian Lady (Hinglish)",
                "Female",
                "🇮🇳 Indian Accent • Female • Clear Guide",
                "Natural Indian female voice for app showcases & walkthroughs",
                true
            ),
            CartesiaVoiceItem(
                "db6b0ed5-d5d3-463d-ae85-518a07d3c2b4",
                "Skylar (Sonic Hindi)",
                "Female",
                "🇮🇳 Sonic-3.6 Hindi & Hinglish • Flagship Female",
                "Cartesia flagship voice with native-grade Hindi & Hinglish code-switching",
                true
            ),
            CartesiaVoiceItem(
                "47c38ca4-5f35-497b-b1a3-415245fb35e1",
                "Daniel (Sonic Hindi)",
                "Male",
                "🇮🇳 Sonic-3.6 Hindi & Hinglish • Flagship Male",
                "Steady, pleasant & realistic male voice for Hindi/Hinglish guides",
                true
            ),
            CartesiaVoiceItem(
                "9626c31c-bec5-4cca-baa8-f8ba9e84c8bc",
                "Jacqueline (Sonic Hindi)",
                "Female",
                "🇮🇳 Sonic-3.6 Hindi & Hinglish • Expressive",
                "Lively & engaging female voice for product promos",
                true
            ),
            CartesiaVoiceItem(
                "62ae83ad-4f6a-430b-af41-a9bede9286ca",
                "Gemma (Sonic Hindi)",
                "Female",
                "🇮🇳 Sonic-3.6 Hindi & Hinglish • Crisp Studio",
                "Clean & articulate female voice for UI/UX showcases",
                true
            ),
            CartesiaVoiceItem(
                "ef191366-f52f-447a-a398-ed8c0f2943a1",
                "Archie (Sonic Hindi)",
                "Male",
                "🇮🇳 Sonic-3.6 Hindi & Hinglish • Confident",
                "Energetic male presenter for YouTube & social videos",
                true
            ),
            CartesiaVoiceItem(
                "694f9389-aac1-45b6-b726-9d9369183238",
                "Sarah (Sonic Hindi)",
                "Female",
                "🇮🇳 Sonic Multilingual • Friendly",
                "Warm conversational tone for step-by-step instructions",
                false
            ),
            CartesiaVoiceItem(
                "bf991597-6c13-47e4-8411-91ec2de5c466",
                "Newslady (Sonic Hindi)",
                "Female",
                "🇮🇳 Sonic Multilingual • Broadcast Anchor",
                "Professional news & tutorial anchor delivery",
                false
            ),
            CartesiaVoiceItem(
                "d46abd1d-2d02-43e8-819f-51fb652c1c61",
                "Newsman (Sonic Hindi)",
                "Male",
                "🇮🇳 Sonic Multilingual • Studio Anchor",
                "Clear, authoritative male broadcast voice",
                false
            ),
            CartesiaVoiceItem(
                "156fb8d2-335b-4950-9cb3-a2d33befec77",
                "Helpful Woman (Hindi)",
                "Female",
                "🇮🇳 Sonic Multilingual • Helpful Guide",
                "Polite & supportive voice for onboarding tutorials",
                false
            ),
            CartesiaVoiceItem(
                "42b39f37-515f-4eee-8546-73e841679c1d",
                "Wise Guide Man (Hindi)",
                "Male",
                "🇮🇳 Sonic Multilingual • Deep Mentor",
                "Rich, mature male narration for technical explainers",
                false
            )
        )

        fun resolveVoiceId(spec: String): String {
            val trimmed = spec.trim()
            if (trimmed.isEmpty()) return defaultHindiVoices.first().voiceId
            if (trimmed.contains("|")) {
                val afterPipe = trimmed.substringAfterLast("|").trim()
                if (afterPipe.isNotEmpty()) return afterPipe
            }
            val byName = defaultHindiVoices.firstOrNull {
                it.name.equals(trimmed, ignoreCase = true) || it.voiceId.equals(trimmed, ignoreCase = true)
            }
            if (byName != null) return byName.voiceId
            return trimmed
        }

        /**
         * Fetches all available voices from https://api.cartesia.ai/voices
         * and sorts Native Hindi (hi), Indian-accented, and custom cloned voices to the top.
         */
        fun fetchVoicesFromApi(apiKey: String): List<CartesiaVoiceItem> {
            val cleanKey = apiKey.trim()
            if (cleanKey.isEmpty()) {
                throw Exception("Pehle Cartesia API Key (play.cartesia.ai) enter karein.")
            }

            val versionsToTry = listOf("2025-04-16", "2024-11-13", "2026-08-14")
            var respText = ""
            var success = false
            var lastCode = 0

            for (ver in versionsToTry) {
                try {
                    val conn = (URL("https://api.cartesia.ai/voices").openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 20_000
                        readTimeout = 30_000
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("X-API-Key", cleanKey)
                        setRequestProperty("Authorization", "Bearer $cleanKey")
                        setRequestProperty("Cartesia-Version", ver)
                    }
                    lastCode = conn.responseCode
                    val stream = if (lastCode in 200..299) conn.inputStream else conn.errorStream
                    respText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                    if (lastCode in 200..299 && respText.isNotBlank()) {
                        success = true
                        break
                    }
                } catch (_: Exception) {}
            }

            if (!success) {
                val errMsg = try {
                    JSONObject(respText).optString("message", respText.take(160))
                } catch (_: Exception) {
                    respText.take(160)
                }
                throw Exception("Cartesia API Error ($lastCode): $errMsg")
            }

            val fetched = ArrayList<CartesiaVoiceItem>()
            val seenIds = HashSet<String>()

            val trimmedResp = respText.trim()
            val arr: JSONArray? = if (trimmedResp.startsWith("[")) {
                JSONArray(trimmedResp)
            } else {
                val root = JSONObject(trimmedResp)
                root.optJSONArray("data") ?: root.optJSONArray("voices")
            }

            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val v = arr.optJSONObject(i) ?: continue
                    val id = v.optString("id", v.optString("voice_id", "")).trim()
                    val name = v.optString("name", "").trim()
                    if (id.isEmpty() || name.isEmpty() || !seenIds.add(id)) continue

                    val lang = v.optString("language", "").lowercase()
                    val desc = v.optString("description", v.optString("tagline", "")).trim()
                    val genderRaw = v.optString("gender", "").replaceFirstChar { it.uppercase() }
                    val isPublic = v.optBoolean("is_public", true)

                    val combined = "$name $desc $lang".lowercase()
                    val isHindiNative = lang == "hi" ||
                        lang.startsWith("hi-") ||
                        combined.contains("hindi") ||
                        combined.contains("indian") ||
                        combined.contains("india") ||
                        combined.contains("hinglish") ||
                        !isPublic

                    val gender = when {
                        genderRaw.isNotBlank() -> genderRaw
                        combined.contains("woman") || combined.contains("lady") || combined.contains("female") || combined.contains("girl") -> "Female"
                        combined.contains("man") || combined.contains("male") || combined.contains("guy") || combined.contains("boy") -> "Male"
                        else -> "Sonic"
                    }

                    val badge = when {
                        !isPublic -> "🎙 My Custom / Cloned Voice • $gender"
                        lang == "hi" || combined.contains("hindi") -> "🇮🇳 Native Hindi • $gender • Sonic"
                        combined.contains("indian") || combined.contains("india") -> "🇮🇳 Indian Hinglish • $gender • Sonic"
                        else -> "🇮🇳 Hindi Multilingual • $gender • Sonic"
                    }

                    fetched.add(
                        CartesiaVoiceItem(
                            voiceId = id,
                            name = name,
                            gender = gender,
                            badge = badge,
                            description = desc.ifBlank { "Supports Hindi (hi) & Hinglish speech on Cartesia Sonic" }.take(72),
                            isHindiPriority = isHindiNative
                        )
                    )
                }
            }

            // Ensure all 14 curated Hindi & Sonic voices are included
            for (dv in defaultHindiVoices) {
                if (seenIds.add(dv.voiceId)) {
                    fetched.add(dv)
                }
            }

            return fetched.sortedWith(
                compareByDescending<CartesiaVoiceItem> { it.isHindiPriority }
                    .thenBy { it.name.lowercase() }
            )
        }

        /**
         * Synthesizes Hindi/Hinglish text into 24kHz 16-bit PCM WAV bytes via POST https://api.cartesia.ai/tts/bytes.
         * Automatically handles both Cartesia API schema versions (2025-04-16 / 2024-11-13 and 2026-08-14)
         * and model fallback (sonic-3 -> sonic-3.6 -> sonic-2) for 100% compatibility.
         */
        fun synthWavBytes(
            apiKey: String,
            text: String,
            voiceSpec: String,
            preferredModel: String = MODEL_SONIC_3
        ): ByteArray {
            val cleanKey = apiKey.trim()
            if (cleanKey.isEmpty()) {
                throw Exception("Cartesia API key khali hai. Settings mein play.cartesia.ai ki API key enter karein.")
            }
            val vId = resolveVoiceId(voiceSpec)

            data class AttemptSpec(val apiVersion: String, val model: String, val voiceAsObject: Boolean)

            val attempts = listOf(
                AttemptSpec("2025-04-16", preferredModel.ifBlank { MODEL_SONIC_3 }, true),
                AttemptSpec("2024-11-13", MODEL_SONIC_2, true),
                AttemptSpec("2026-08-14", "sonic-3.6", false),
                AttemptSpec("2024-11-13", "sonic-multilingual", true)
            )

            var lastErr = "Unknown Cartesia TTS error"

            for (att in attempts) {
                try {
                    val conn = (URL("https://api.cartesia.ai/tts/bytes").openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        connectTimeout = 20_000
                        readTimeout = 90_000
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        setRequestProperty("X-API-Key", cleanKey)
                        setRequestProperty("Authorization", "Bearer $cleanKey")
                        setRequestProperty("Cartesia-Version", att.apiVersion)
                    }

                    val outFmt = JSONObject()
                        .put("container", "wav")
                        .put("encoding", "pcm_s16le")
                        .put("sample_rate", 24000)

                    val body = JSONObject()
                        .put("model_id", att.model)
                        .put("transcript", text)
                        .put("language", "hi")
                        .put("output_format", outFmt)

                    if (att.voiceAsObject) {
                        body.put("voice", JSONObject().put("mode", "id").put("id", vId))
                    } else {
                        body.put("voice", vId)
                    }

                    conn.outputStream.use { os ->
                        os.write(body.toString().toByteArray(Charsets.UTF_8))
                    }

                    val code = conn.responseCode
                    if (code in 200..299) {
                        val bytes = conn.inputStream.use { it.readBytes() }
                        if (bytes.size >= 128) {
                            return bytes
                        }
                    } else {
                        val errTxt = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                        val msg = try {
                            JSONObject(errTxt).optString("message", errTxt.take(160))
                        } catch (_: Exception) {
                            errTxt.take(160)
                        }
                        lastErr = "HTTP $code (${att.model}): $msg"
                        // If 401/403 invalid API key, fail immediately without retrying other models
                        if (code == 401 || code == 403) break
                    }
                } catch (e: Exception) {
                    lastErr = e.message ?: "Network error"
                }
            }

            throw Exception("Cartesia TTS Error: $lastErr")
        }
    }
}
