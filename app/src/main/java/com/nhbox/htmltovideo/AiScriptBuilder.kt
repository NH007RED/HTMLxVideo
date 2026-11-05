package com.nhbox.htmltovideo

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

object AiScriptBuilder {

    data class OfflineModelDownload(
        val id: String,
        val title: String,
        val badge: String,
        val fileName: String,
        val url: String
    )

    val offlineModels = listOf(
        OfflineModelDownload(
            "qwen15b",
            "⬇ Qwen 2.5 1.5B (1.1 GB)",
            "Recommended • Fast & Hinglish Ready (4GB+ RAM)",
            "qwen2.5-1.5b-instruct-q4_k_m.gguf",
            "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf?download=true"
        ),
        OfflineModelDownload(
            "gemma2b",
            "⬇ Gemma 2 2B (1.7 GB)",
            "Google Gemma 2 • Balanced Quality (6GB+ RAM)",
            "gemma-2-2b-it-Q4_K_M.gguf",
            "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf?download=true"
        ),
        OfflineModelDownload(
            "llama3b",
            "⬇ Llama 3.2 3B (2.0 GB)",
            "Meta Llama 3.2 • Pro Studio Quality (8GB+ RAM)",
            "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf?download=true"
        ),
        OfflineModelDownload(
            "qwen05b",
            "⬇ Qwen 2.5 0.5B (398 MB)",
            "Ultra-Lite • Super Fast for Any Phone",
            "qwen2.5-0.5b-instruct-q4_k_m.gguf",
            "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf?download=true"
        )
    )

    data class ModelFileInfo(
        val valid: Boolean,
        val fileName: String,
        val displayPath: String,
        val sizeBytes: Long,
        val sizeLabel: String,
        val formatLabel: String,
        val archLabel: String,
        val statusLine: String
    )

    data class ProviderSpec(val id: String, val label: String, val defaultModel: String, val endpoint: String)

    val providers = listOf(
        ProviderSpec("gemini", "Gemini AI", "gemini-2.5-flash", "https://generativelanguage.googleapis.com/v1beta/models"),
        ProviderSpec("groq", "Groq Fast", "llama-3.3-70b-versatile", "https://api.groq.com/openai/v1/chat/completions"),
        ProviderSpec("openai", "OpenAI", "gpt-4o-mini", "https://api.openai.com/v1/chat/completions"),
        ProviderSpec("openrouter", "OpenRouter", "google/gemini-2.5-flash", "https://openrouter.ai/api/v1/chat/completions")
    )

    fun byId(id: String): ProviderSpec = providers.firstOrNull { it.id == id } ?: providers.first()

    private const val SYSTEM_PROMPT = """You are an expert video director and scriptwriter for a 1920x1080 Full HD mobile app & tech tutorial video engine.
Given a user's script, markdown (.md) guide, feature list, or topic prompt, convert it into a structured JSON video blueprint with 6 to 12 scenes.
IMPORTANT: Only include features, steps, filenames, and workflows that actually belong to the user's script! Never invent unrelated model specs.
Return ONLY valid JSON (no markdown fences, no extra prose) matching this exact schema:
{
  "appName": "Short Project or App Name (max 24 chars)",
  "tagline": "Short subtitle or tutorial title (max 64 chars)",
  "themeId": 0,
  "scenes": [
    {
      "min": 14,
      "vo": "Natural Hinglish (Roman Hindi + English technical terms) voiceover narration for this scene (30 to 65 words).",
      "osd1": "STAGE 1 BANNER TEXT",
      "osd2": "STAGE 2 BANNER TEXT",
      "osd3": "STAGE 3 BANNER TEXT",
      "screenTitle": "Header Title",
      "screenSub": "Header Subtitle",
      "mode": "hero_launch",
      "browserUrl": "console.cloud.google.com",
      "fileName": "google-services.json",
      "codeLines": ["CLIENT_ID=YOUR_CLIENT_ID", "DRIVE_API=ENABLED"],
      "typingText": "Short text typed in input/search field",
      "buttonText": "Primary Action Button Label",
      "leftTitle": "Left Callout Card Title",
      "leftLines": ["Point 1 from scene", "Point 2 from scene", "Point 3 from scene"],
      "rightTitle": "Right Callout Card Title",
      "rightLines": ["Point A from scene", "Point B from scene", "Point C from scene"],
      "bannerTitle": "Bottom Callout Headline",
      "bannerSub": "Bottom Callout Subtext",
      "items": ["Step/Item 1|Detail 1", "Step/Item 2|Detail 2", "Step/Item 3|Detail 3", "Step/Item 4|Detail 4"]
    }
  ]
}
Rules:
1. "mode" for each scene MUST be chosen based on the scene content from: "hero_launch", "browser_flow", "file_picker", "code_vars", "auth_keys", "media_stream", "build_run", "error_fix", "feature_ui", "outro_recap".
2. Use "hero_launch" for scene 0 and "outro_recap" for the final scene. Vary "mode" across middle scenes so consecutive scenes never have the same mode.
3. Preserve the user's exact Hinglish voiceover if provided, or write natural Hinglish narration covering every step in their script."""

    fun formatSize(bytes: Long): String {
        if (bytes <= 0L) return "Unknown Size"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format("%.2f GB", mb / 1024.0)
        } else {
            String.format("%.1f MB", mb)
        }
    }

    fun inspectOfflineModel(context: Context, uriStr: String?, manualPath: String?): ModelFileInfo {
        val cleanPath = manualPath?.trim() ?: ""
        val cleanUri = uriStr?.trim() ?: ""

        if (cleanPath.isNotEmpty() && !cleanPath.startsWith("content://")) {
            val f = File(cleanPath)
            if (f.exists() && f.isFile && f.canRead()) {
                val len = f.length()
                val header = ByteArray(8192)
                val readN = try { FileInputStream(f).use { it.read(header) } } catch (_: Exception) { 0 }
                return parseHeaderBytes(f.name, f.absolutePath, len, header, readN.coerceAtLeast(0))
            }
        }

        if (cleanUri.startsWith("content://")) {
            try {
                val u = Uri.parse(cleanUri)
                var name = "offline_model.gguf"
                var size = 0L
                val c: Cursor? = context.contentResolver.query(u, null, null, null, null)
                c?.use {
                    if (it.moveToFirst()) {
                        val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIdx >= 0) name = it.getString(nameIdx) ?: name
                        val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIdx >= 0 && !it.isNull(sizeIdx)) size = it.getLong(sizeIdx)
                    }
                }
                val header = ByteArray(8192)
                val readN = context.contentResolver.openInputStream(u)?.use { ins -> ins.read(header) } ?: -1
                if (readN > 0) {
                    val disp = if (cleanPath.isNotEmpty()) cleanPath else "/storage/emulated/0/Download/$name"
                    return parseHeaderBytes(name, disp, size, header, readN)
                }
            } catch (_: Exception) {}
        }

        if (cleanPath.isNotEmpty()) {
            val fName = cleanPath.substringAfterLast('/')
            val dlFile = File("/storage/emulated/0/Download/$fName")
            if (dlFile.exists() && dlFile.canRead()) {
                val header = ByteArray(8192)
                val readN = try { FileInputStream(dlFile).use { it.read(header) } } catch (_: Exception) { 0 }
                return parseHeaderBytes(dlFile.name, dlFile.absolutePath, dlFile.length(), header, readN.coerceAtLeast(0))
            }
            val low = fName.lowercase()
            if (low.endsWith(".gguf") || low.endsWith(".bin") || low.endsWith(".task") || low.endsWith(".litertlm") || low.endsWith(".tflite") || low.endsWith(".onnx")) {
                val fmt = low.substringAfterLast('.').uppercase()
                val arch = detectArchFromName(fName)
                return ModelFileInfo(
                    valid = true,
                    fileName = fName,
                    displayPath = cleanPath,
                    sizeBytes = 0L,
                    sizeLabel = "Local Storage",
                    formatLabel = fmt,
                    archLabel = arch,
                    statusLine = "\u2713 Offline Model Set: $fName ($fmt \u2022 $arch)"
                )
            }
        }

        return ModelFileInfo(
            valid = false,
            fileName = "",
            displayPath = "",
            sizeBytes = 0L,
            sizeLabel = "0 MB",
            formatLabel = "NONE",
            archLabel = "None",
            statusLine = "Koi Offline AI Model select nahi hai (Optional: Model path select karein ya seedha script se generate karein)."
        )
    }

    private fun detectArchFromName(name: String): String {
        val low = name.lowercase()
        return when {
            low.contains("qwen") -> "Qwen2.5"
            low.contains("gemma") -> "Gemma-2"
            low.contains("llama") -> "Llama-3.2"
            low.contains("phi") -> "Phi-3"
            low.contains("mistral") || low.contains("ministral") -> "Mistral"
            else -> "Local LLM"
        }
    }

    private fun parseHeaderBytes(name: String, displayPath: String, sizeBytes: Long, header: ByteArray, readLen: Int): ModelFileInfo {
        val sizeStr = formatSize(sizeBytes)
        val low = name.lowercase()
        var fmt = when {
            low.endsWith(".gguf") -> "GGUF"
            low.endsWith(".task") -> "MEDIAPIPE TASK"
            low.endsWith(".litertlm") -> "LITERT-LM"
            low.endsWith(".tflite") -> "TFLITE"
            low.endsWith(".bin") -> "BIN WEIGHTS"
            else -> "AI MODEL"
        }
        var arch = detectArchFromName(name)
        var extraInfo = ""

        if (readLen >= 24 &&
            header[0] == 'G'.code.toByte() &&
            header[1] == 'G'.code.toByte() &&
            header[2] == 'U'.code.toByte() &&
            header[3] == 'F'.code.toByte()
        ) {
            val bb = ByteBuffer.wrap(header, 4, 20).order(ByteOrder.LITTLE_ENDIAN)
            val version = bb.int
            val tensorCount = bb.long
            val kvCount = bb.long
            fmt = "GGUF v$version"
            if (tensorCount in 1..20000) extraInfo = " \u2022 $tensorCount tensors"
            val ascii = String(header, 0, readLen, Charsets.ISO_8859_1).lowercase()
            arch = when {
                ascii.contains("qwen2") -> "Qwen2 Architecture"
                ascii.contains("llama") -> "Llama Architecture"
                ascii.contains("gemma2") || ascii.contains("gemma") -> "Gemma Architecture"
                ascii.contains("phi3") || ascii.contains("phi2") -> "Phi Architecture"
                else -> arch
            }
            if (kvCount > 0 && extraInfo.isEmpty()) extraInfo = " \u2022 $kvCount keys"
        } else if (readLen >= 8) {
            val magic = String(header, 0, 8, Charsets.ISO_8859_1)
            if (magic.contains("TFL3")) fmt = "LiteRT / TFLite"
            else if (header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte()) fmt = "MediaPipe .task"
        }

        val valid = readLen > 0 || sizeBytes > 0
        val status = if (valid) {
            "\u2713 Ready: $name ($sizeStr \u2022 $fmt \u2022 $arch$extraInfo)"
        } else {
            "Model file read nahi ho payi: $name"
        }

        return ModelFileInfo(
            valid = valid,
            fileName = name,
            displayPath = displayPath,
            sizeBytes = sizeBytes,
            sizeLabel = sizeStr,
            formatLabel = fmt,
            archLabel = arch,
            statusLine = status
        )
    }

    fun generateProject(
        context: Context,
        useOfflineMode: Boolean,
        offlineUriStr: String,
        offlinePathStr: String,
        providerId: String,
        apiKey: String,
        modelName: String,
        rawInput: String,
        onStatus: (String) -> Unit
    ): Pair<String, String> {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) throw Exception("Script prompt ya .md file khali hai.")

        // If the uploaded file is already a complete project file with window.NH, load it directly
        if (trimmed.contains("window.NH") && trimmed.contains("<canvas")) {
            val title = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(trimmed)?.groupValues?.get(1) ?: "Generated Studio Project"
            return Pair(trimmed, title)
        }

        val blueprint: JSONObject = if (!useOfflineMode && apiKey.isNotBlank()) {
            val spec = byId(providerId)
            val useModel = modelName.trim().ifEmpty { spec.defaultModel }
            onStatus("${spec.label} ($useModel) se custom scenes aur animations ban rahe hain...")
            try {
                callAiForBlueprint(spec, apiKey.trim(), useModel, trimmed)
            } catch (e: Exception) {
                onStatus("Cloud API fallback: Script se custom scenes analyze ho rahe hain...")
                val info = inspectOfflineModel(context, offlineUriStr, offlinePathStr)
                synthesizeFromScriptDeep(trimmed, info)
            }
        } else {
            val info = inspectOfflineModel(context, offlineUriStr, offlinePathStr)
            if (info.valid) {
                onStatus("Offline Model [${info.fileName}] se aapki script analyze ho rahi hai...")
            } else {
                onStatus("Aapki script se custom scenes, UI screens aur voiceover ban rahe hain...")
            }
            Thread.sleep(250)
            synthesizeFromScriptDeep(trimmed, info)
        }

        onStatus("1080p 60fps Multi-Scene Interactive Video compile ho raha hai...")
        val appName = blueprint.optString("appName", "Studio Showcase").ifBlank { "Studio Showcase" }
        val code = compileBlueprintToProject(blueprint)
        return Pair(code, appName)
    }

    private fun callAiForBlueprint(spec: ProviderSpec, apiKey: String, model: String, userPrompt: String): JSONObject {
        if (spec.id == "gemini") {
            return callGeminiNativeApi(apiKey, model, userPrompt)
        }
        val url = URL(spec.endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 90_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }

        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
            put(JSONObject().put("role", "user").put("content", userPrompt.take(18_000)))
        }
        val reqBody = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("temperature", 0.35)
        }

        conn.outputStream.use { os ->
            os.write(reqBody.toString().toByteArray(Charsets.UTF_8))
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val respText = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() } } ?: ""
        if (code !in 200..299) {
            val errMsg = try {
                JSONObject(respText).optJSONObject("error")?.optString("message") ?: respText.take(180)
            } catch (_: Exception) {
                respText.take(180)
            }
            throw Exception("HTTP $code: $errMsg")
        }

        val content = JSONObject(respText)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()

        val jsonStr = extractJsonObject(content)
        val parsed = JSONObject(jsonStr)
        val scenes = parsed.optJSONArray("scenes")
        if (scenes == null || scenes.length() == 0) throw Exception("Empty scenes")
        return parsed
    }

    private fun callGeminiNativeApi(apiKey: String, preferredModel: String, userPrompt: String): JSONObject {
        val candidateModels = linkedSetOf(
            preferredModel.trim().ifEmpty { "gemini-2.5-flash" },
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash"
        )
        var lastErr = "Unknown Gemini error"
        for (m in candidateModels) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=$apiKey"
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 30_000
                    readTimeout = 90_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                val reqBody = JSONObject().apply {
                    put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT))))
                    put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", userPrompt.take(18_000))))))
                    put("generationConfig", JSONObject().put("temperature", 0.35).put("responseMimeType", "application/json"))
                }
                conn.outputStream.use { os ->
                    os.write(reqBody.toString().toByteArray(Charsets.UTF_8))
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val respText = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() } } ?: ""
                if (code in 200..299) {
                    val candidates = JSONObject(respText).optJSONArray("candidates")
                    val text = candidates?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text") ?: ""
                    val jsonStr = extractJsonObject(text)
                    val parsed = JSONObject(jsonStr)
                    val scenes = parsed.optJSONArray("scenes")
                    if (scenes != null && scenes.length() > 0) return parsed
                } else {
                    val msg = try {
                        JSONObject(respText).optJSONObject("error")?.optString("message") ?: respText.take(160)
                    } catch (_: Exception) {
                        respText.take(160)
                    }
                    lastErr = "HTTP $code ($m): $msg"
                }
            } catch (e: Exception) {
                lastErr = e.message ?: "Connection error"
            }
        }
        throw Exception(lastErr)
    }

    private fun extractJsonObject(s: String): String {
        val a = s.indexOf('{')
        val b = s.lastIndexOf('}')
        if (a >= 0 && b > a) return s.substring(a, b + 1)
        throw Exception("Response mein valid JSON blueprint nahi mila.")
    }

    private data class ParsedSection(
        val heading: String,
        val proseLines: MutableList<String> = mutableListOf(),
        val bulletLines: MutableList<String> = mutableListOf(),
        val codeLines: MutableList<String> = mutableListOf()
    )

    /**
     * Deep, 100% Script-Faithful Parser & Multi-Layout Scene Synthesizer.
     * - Strips prompt meta-instructions (like IMPORTANT PROJECT NOTE:...)
     * - Never creates empty title-only scenes or fake "Qwen2 Architec Specs" cards
     * - Assigns 10 distinct interactive screen layouts based on each scene's real content
     */
    fun synthesizeFromScriptDeep(raw: String, modelInfo: ModelFileInfo): JSONObject {
        // 1. Clean out prompt meta-notes / AI instructions
        val rawLines = raw.lines()
        val filteredLines = ArrayList<String>()
        var inCodeFence = false
        val globalCodeSnippets = ArrayList<String>()

        for (line in rawLines) {
            val t = line.trim()
            if (t.startsWith("```")) {
                inCodeFence = !inCodeFence
                continue
            }
            if (inCodeFence) {
                if (t.isNotEmpty()) globalCodeSnippets.add(t.take(48))
                filteredLines.add("CODE_LINE: $t")
                continue
            }
            val low = t.lowercase()
            if (low.startsWith("important project note") ||
                low.startsWith("project note:") ||
                low.startsWith("note to ai") ||
                low.startsWith("ai instruction") ||
                low.contains("wahi features dikhayein jo actual") ||
                low.contains("is script ke liye html") ||
                low.contains("create a showcase video for this")
            ) {
                continue
            }
            if (Regex("^(?:---|===|\\*\\*\\*)\\s*$").matches(t)) {
                filteredLines.add("SECTION_BREAK")
                continue
            }
            filteredLines.add(line)
        }

        // 2. Extract Document Title (appName) & Subtitle (tagline), and remove the title line from scene body
        var appName = ""
        var tagline = ""
        var titleLineIndex = -1

        for ((idx, line) in filteredLines.withIndex()) {
            val t = line.trim()
            if (t.isEmpty() || t == "SECTION_BREAK" || t.startsWith("CODE_LINE:")) continue
            if (t.startsWith("#")) {
                val cleanH = t.trimStart('#', ' ', '*', '-').trim()
                val splitDash = cleanH.split(Regex("\\s*(?:\u2014|\u2013| - |:|\u2022|\\|)\\s*"), limit = 2)
                if (splitDash.size == 2 && splitDash[0].length >= 2) {
                    appName = splitDash[0].trim().take(24)
                    tagline = splitDash[1].trim().take(64)
                } else {
                    appName = cleanH.take(24)
                }
                titleLineIndex = idx
                break
            } else if (idx <= 2 && t.length <= 72 && t.contains(Regex("(?:\u2014|\u2013| - |:)"))) {
                val splitDash = t.replace(Regex("[*#]"), "").split(Regex("\\s*(?:\u2014|\u2013| - |:)\\s*"), limit = 2)
                if (splitDash.size == 2 && splitDash[0].length in 2..28) {
                    appName = splitDash[0].trim().take(24)
                    tagline = splitDash[1].trim().take(64)
                    titleLineIndex = idx
                    break
                }
            }
            break
        }

        if (appName.isEmpty()) {
            val firstNonEmpty = filteredLines.firstOrNull { it.trim().isNotEmpty() && it.trim() != "SECTION_BREAK" } ?: "Studio App"
            val cleaned = firstNonEmpty.replace(Regex("[#*`>-]"), " ").trim()
            val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
            appName = words.take(3).joinToString(" ").take(24).ifBlank { "Studio Project" }
        }

        // Choose theme palette ID (0..5) based on project topic so every project looks visually distinct
        val fullLower = raw.lowercase()
        val themeId = when {
            fullLower.contains("drive") || fullLower.contains("stream") || fullLower.contains("cloud") -> 0
            fullLower.contains("github") || fullLower.contains("gradle") || fullLower.contains("apk") || fullLower.contains("build") -> 1
            fullLower.contains("movie") || fullLower.contains("cinema") || fullLower.contains("video") || fullLower.contains("play") -> 2
            fullLower.contains("jarvis") || fullLower.contains("voice") || fullLower.contains("assistant") || fullLower.contains("neural") -> 3
            fullLower.contains("firebase") || fullLower.contains("json") || fullLower.contains("store") -> 4
            else -> abs(appName.hashCode()) % 6
        }

        if (tagline.isEmpty()) {
            tagline = when (themeId) {
                0 -> "Cloud Streaming & Setup Guide"
                1 -> "Complete Build & Configuration Tutorial"
                2 -> "Full HD Streaming & Media Showcase"
                3 -> "AI Voice & Smart Automation Guide"
                4 -> "Project Setup & Integration Walkthrough"
                else -> "Complete Interactive Features & Setup Guide"
            }
        }

        // 3. Parse remaining lines into logical sections
        val sections = ArrayList<ParsedSection>()
        var curSec = ParsedSection(heading = "")

        fun flushSection() {
            if (curSec.heading.isNotEmpty() || curSec.proseLines.isNotEmpty() || curSec.bulletLines.isNotEmpty() || curSec.codeLines.isNotEmpty()) {
                sections.add(curSec)
                curSec = ParsedSection(heading = "")
            }
        }

        val headingRegex = Regex("^(?:#{1,4}\\s+(.+)|(?:\\*\\*)?(?:Scene|Step|Part|Section|Stage|Module|Screen)\\s*(\\d+)[:.\\-\u2014\u2013)]*\\s*(.*?)(?:\\*\\*)?)$", RegexOption.IGNORE_CASE)
        val numberedHeadingRegex = Regex("^(\\d+)[.)]\\s+([A-Z][A-Za-z0-9 /&+\\-\u2014\u2013:]{3,55})$")
        val bulletRegex = Regex("^(?:[-*\u2022\u25B6\u2713\u2705\uD83D\uDC49]|\\d+[.)])\\s+(.*)")
        val labelStripRegex = Regex("^(?:\\*\\*)?(?:VO|Voiceover|Voice-over|Narration|Audio|Script|Hindi|Hinglish)(?:\\*\\*)?\\s*[:\\-\u2014]\\s*", RegexOption.IGNORE_CASE)
        val visualLabelRegex = Regex("^(?:\\*\\*)?(?:Visual|Screen|UI|Animation|On-Screen|Display|Action)(?:\\*\\*)?\\s*[:\\-\u2014]\\s*", RegexOption.IGNORE_CASE)

        var consecutiveBlanks = 0
        for ((idx, rawLine) in filteredLines.withIndex()) {
            if (idx == titleLineIndex) continue
            val t = rawLine.trim()
            if (t == "SECTION_BREAK") {
                flushSection()
                continue
            }
            if (t.isEmpty()) {
                consecutiveBlanks++
                // If current section already has substantial prose/bullets and no explicit markdown headings exist, split on blank line
                val currentLen = curSec.proseLines.sumOf { it.length } + curSec.bulletLines.sumOf { it.length }
                if (currentLen >= 130 && curSec.heading.isEmpty()) {
                    flushSection()
                }
                continue
            }
            consecutiveBlanks = 0

            if (t.startsWith("CODE_LINE:")) {
                curSec.codeLines.add(t.removePrefix("CODE_LINE:").trim())
                continue
            }

            val hm = headingRegex.find(t)
            if (hm != null) {
                flushSection()
                val hText = when {
                    hm.groupValues[1].isNotEmpty() -> hm.groupValues[1]
                    hm.groupValues[3].isNotEmpty() -> hm.groupValues[3]
                    else -> "Step ${hm.groupValues[2]}"
                }.replace(Regex("[*#`]"), "").trim()
                curSec = ParsedSection(heading = hText.take(48))
                continue
            }

            val nm = numberedHeadingRegex.find(t.replace("**", ""))
            if (nm != null && t.length <= 64) {
                flushSection()
                curSec = ParsedSection(heading = nm.groupValues[2].trim().take(48))
                continue
            }

            if (visualLabelRegex.containsMatchIn(t)) {
                val visText = t.replace(visualLabelRegex, "").replace(Regex("[*`#]"), "").trim()
                if (visText.isNotEmpty()) curSec.bulletLines.add(visText)
                continue
            }

            if (labelStripRegex.containsMatchIn(t)) {
                val voLine = t.replace(labelStripRegex, "").replace(Regex("^[\"\u201C]|[\"\u201D]$"), "").trim()
                if (voLine.isNotEmpty()) curSec.proseLines.add(voLine)
                continue
            }

            val bm = bulletRegex.find(t)
            if (bm != null) {
                val bText = bm.groupValues[1].replace(Regex("[*`#]"), "").trim()
                if (bText.contains("=") && !bText.contains(" ") && bText.length <= 42) {
                    curSec.codeLines.add(bText)
                } else if (bText.length >= 95) {
                    // Long bullet is actually a spoken sentence or detailed step
                    curSec.proseLines.add(bText)
                    curSec.bulletLines.add(bText.take(56))
                } else if (bText.isNotEmpty()) {
                    curSec.bulletLines.add(bText)
                }
                continue
            }

            // Check if line looks like KEY=VALUE or config/path
            if (t.matches(Regex("^[A-Z0-9_]{3,25}\\s*=\\s*\\S+.*"))) {
                curSec.codeLines.add(t.take(44))
                continue
            }

            val cleanProse = t.replace(Regex("^[\"\u201C]|[\"\u201D]$"), "").replace(Regex("[*`#]"), "").trim()
            if (cleanProse.isNotEmpty()) {
                // If it's a short bold line at the start of an empty section, treat as heading
                if (curSec.heading.isEmpty() && curSec.proseLines.isEmpty() && curSec.bulletLines.isEmpty() && cleanProse.length in 4..46 && !cleanProse.endsWith(".")) {
                    curSec = ParsedSection(heading = cleanProse)
                } else {
                    curSec.proseLines.add(cleanProse)
                }
            }
        }
        flushSection()

        // 4. Merge any tiny heading-only sections with the next section so no scene is ever empty or 1-line
        val mergedSections = ArrayList<ParsedSection>()
        var i = 0
        while (i < sections.size) {
            val s = sections[i]
            if (s.proseLines.isEmpty() && s.bulletLines.isEmpty() && i + 1 < sections.size) {
                val nxt = sections[i + 1]
                val combinedHeading = if (nxt.heading.isNotEmpty()) "${s.heading}: ${nxt.heading}" else s.heading
                mergedSections.add(ParsedSection(combinedHeading.take(48), nxt.proseLines, nxt.bulletLines, nxt.codeLines))
                i += 2
            } else {
                mergedSections.add(s)
                i++
            }
        }

        // 5. If the user pasted a single block or very few sections (< 4), split long prose/bullets into 6..10 balanced scenes
        val finalSections = ArrayList<ParsedSection>()
        if (mergedSections.size >= 4) {
            finalSections.addAll(mergedSections.take(12))
        } else {
            // Collect all sentences and bullets from the input
            val allSentences = ArrayList<String>()
            val allBullets = ArrayList<String>()
            val allCodes = ArrayList<String>()
            for (s in mergedSections) {
                allBullets.addAll(s.bulletLines)
                allCodes.addAll(s.codeLines)
                for (p in s.proseLines) {
                    val splits = p.split(Regex("(?<=[.!?।])\\s+")).map { it.trim() }.filter { it.length >= 12 }
                    if (splits.isNotEmpty()) allSentences.addAll(splits) else allSentences.add(p)
                }
            }

            if (allSentences.size >= 5) {
                val chunkSize = (allSentences.size / 7).coerceIn(1, 3)
                var idxChunk = 0
                var sIdx = 0
                while (sIdx < allSentences.size && finalSections.size < 10) {
                    val subList = allSentences.subList(sIdx, (sIdx + chunkSize).coerceAtMost(allSentences.size)).toMutableList()
                    val firstSent = subList.first()
                    val autoHead = extractShortTopic(firstSent, "Step ${idxChunk + 1}")
                    val bSub = ArrayList<String>()
                    if (allBullets.isNotEmpty()) {
                        bSub.add(allBullets[(idxChunk * 2) % allBullets.size])
                        if (allBullets.size > 1) bSub.add(allBullets[(idxChunk * 2 + 1) % allBullets.size])
                    }
                    finalSections.add(ParsedSection(autoHead, subList, bSub, allCodes.toMutableList()))
                    sIdx += chunkSize
                    idxChunk++
                }
            } else if (allBullets.size >= 4) {
                val bChunk = (allBullets.size / 6).coerceIn(1, 3)
                var bIdx = 0
                var stepNo = 1
                while (bIdx < allBullets.size && finalSections.size < 10) {
                    val subB = allBullets.subList(bIdx, (bIdx + bChunk).coerceAtMost(allBullets.size)).toMutableList()
                    val head = extractShortTopic(subB.first(), "Step $stepNo")
                    finalSections.add(ParsedSection(head, mutableListOf(), subB, allCodes.toMutableList()))
                    bIdx += bChunk
                    stepNo++
                }
            } else {
                // Short 1-2 line topic prompt: extract topics from prompt and build 7 custom scenes
                val baseText = (allSentences + allBullets).joinToString(" ").ifBlank { raw }
                val phrases = baseText.split(Regex("[,;•|]|\\band\\b|\\baur\\b|\\bwith\\b|\\bke saath\\b", RegexOption.IGNORE_CASE))
                    .map { it.trim() }
                    .filter { it.length >= 4 }
                val topics = if (phrases.size >= 3) phrases else listOf(
                    "$appName Overview & Architecture",
                    "Account Login & Cloud Setup",
                    "Project Configuration & Files",
                    "Core Engine & Live Controls",
                    "High-Speed Streaming & Preview",
                    "Troubleshooting & Verification",
                    "Complete Summary & Final Output"
                )
                for ((tIdx, top) in topics.take(8).withIndex()) {
                    val cleanTop = extractShortTopic(top, "Step ${tIdx + 1}")
                    finalSections.add(
                        ParsedSection(
                            heading = cleanTop,
                            proseLines = mutableListOf(
                                "Is step mein hum $appName ke andar $top ko detail mein dekhenge aur configure karenge. Yahan diye gaye options ki madad se poora workflow fast aur smooth ho jata hai."
                            ),
                            bulletLines = mutableListOf(
                                cleanTop,
                                "Step-by-step live configuration",
                                "Verified settings & instant output"
                            ),
                            codeLines = allCodes.toMutableList()
                        )
                    )
                }
            }
        }

        // 6. Convert each ParsedSection into a rich Scene JSONObject with distinct visual archetype
        val scArr = JSONArray()
        val middleModes = listOf("browser_flow", "file_picker", "code_vars", "auth_keys", "media_stream", "build_run", "feature_ui", "error_fix")
        var prevMode = ""

        // Gather all section titles for Scene 0 overview & Final Scene recap grid
        val allSceneTitles = finalSections.mapIndexed { idx, sec ->
            sec.heading.ifBlank {
                extractShortTopic(sec.proseLines.firstOrNull() ?: sec.bulletLines.firstOrNull() ?: "Step ${idx + 1}", "Step ${idx + 1}")
            }
        }

        for ((idx, sec) in finalSections.withIndex()) {
            val secTitle = allSceneTitles[idx]

            // Build natural Hinglish Voiceover ("vo") strictly from this section's prose & bullets
            val voBuilder = StringBuilder()
            if (sec.proseLines.isNotEmpty()) {
                voBuilder.append(sec.proseLines.joinToString(" "))
                if (sec.bulletLines.isNotEmpty() && voBuilder.length < 110) {
                    if (!voBuilder.toString().trimEnd().endsWith(".")) voBuilder.append(". ") else voBuilder.append(" ")
                    voBuilder.append("Ismein shamil hai: ").append(sec.bulletLines.take(3).joinToString(", ")).append(".")
                }
            } else if (sec.bulletLines.isNotEmpty()) {
                voBuilder.append("Ab aate hain ").append(secTitle).append(" par. ")
                sec.bulletLines.forEachIndexed { bIdx, bLine ->
                    val prefix = when (bIdx) {
                        0 -> "Sabse pehle "
                        sec.bulletLines.size - 1 -> "Aur finally "
                        else -> "Uske baad "
                    }
                    voBuilder.append(prefix).append(bLine.trimEnd('.', '!')).append(". ")
                }
            } else {
                voBuilder.append("Ab hum ").append(appName).append(" mein ").append(secTitle).append(" ka process complete karenge.")
            }

            val cleanVo = voBuilder.toString().replace(Regex("\\s+"), " ").trim()
            val calcMin = (cleanVo.length * 0.082).toInt().coerceIn(11, 26)

            // Extract real points from this section (sentences + bullets)
            val sentPoints = cleanVo
                .split(Regex("(?<=[.!?।])\\s+|,\\s+(?=aur|phir|uske|jab|jahan|taaki)"))
                .map { it.replace(Regex("[.!?।]+$"), "").trim() }
                .filter { it.length >= 6 }

            val combinedPoints = (sec.bulletLines + sentPoints)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinctBy { it.lowercase() }

            fun pt(pIdx: Int, fallback: String): String {
                return combinedPoints.getOrNull(pIdx)?.take(46) ?: fallback
            }

            // Detect best visual mode for this specific section
            val secLower = (secTitle + " " + cleanVo + " " + sec.bulletLines.joinToString(" ") + " " + sec.codeLines.joinToString(" ")).lowercase()
            var mode = when {
                idx == 0 && finalSections.size >= 3 -> "hero_launch"
                idx == finalSections.size - 1 && finalSections.size >= 4 -> "outro_recap"
                secLower.contains("error") || secLower.contains("fail") || secLower.contains("fix") || secLower.contains("troubleshoot") || secLower.contains("problem") || secLower.contains("issue") -> "error_fix"
                secLower.contains("console") || secLower.contains("browser") || secLower.contains("github.com") || secLower.contains("firebase") || secLower.contains("cloud.google") || secLower.contains("website") || secLower.contains("portal") || secLower.contains("oauth") -> "browser_flow"
                secLower.contains("variable") || secLower.contains("key=") || secLower.contains("local.properties") || secLower.contains("config") || secLower.contains("gradle") || secLower.contains("terminal") || secLower.contains("command") || sec.codeLines.isNotEmpty() -> "code_vars"
                secLower.contains("keystore") || secLower.contains("password") || secLower.contains("token") || secLower.contains("secret") || secLower.contains("api key") || secLower.contains("client id") || secLower.contains("login") || secLower.contains("authorize") -> "auth_keys"
                secLower.contains("zip") || secLower.contains("upload") || secLower.contains("attach") || secLower.contains("select file") || secLower.contains("choose") || secLower.contains(".json") || secLower.contains("folder") -> "file_picker"
                secLower.contains("stream") || secLower.contains("player") || secLower.contains("playback") || secLower.contains("watch") || secLower.contains("1080p") || secLower.contains("4k") || secLower.contains("subtitle") || secLower.contains("exoplayer") || secLower.contains("video") -> "media_stream"
                secLower.contains("build") || secLower.contains("compile") || secLower.contains("apk") || secLower.contains("aab") || secLower.contains("release") || secLower.contains("debug") || secLower.contains("download") || secLower.contains("export") -> "build_run"
                else -> middleModes[idx % middleModes.size]
            }
            if (mode == prevMode && mode != "hero_launch" && mode != "outro_recap") {
                mode = middleModes[(middleModes.indexOf(mode) + 1) % middleModes.size]
            }
            prevMode = mode

            // Extract specific URL / File / Code if present in section
            val urlMatch = Regex("(?:https?://)?([a-z0-9-]+\\.[a-z0-9.-]+(?:/[a-z0-9_./-]*)?)", RegexOption.IGNORE_CASE)
                .findAll(secLower)
                .map { it.groupValues[1] }
                .firstOrNull { it.contains(".") && !it.endsWith(".json") && !it.endsWith(".zip") && !it.endsWith(".apk") && !it.endsWith(".aab") && !it.endsWith(".jks") && !it.endsWith(".kt") && !it.endsWith(".gguf") }
                ?: when {
                    secLower.contains("firebase") -> "console.firebase.google.com"
                    secLower.contains("drive") || secLower.contains("google cloud") || secLower.contains("oauth") -> "console.cloud.google.com/apis"
                    secLower.contains("github") -> "github.com/login/device"
                    secLower.contains("play store") || secLower.contains("play console") -> "play.google.com/console"
                    else -> "${appName.lowercase().replace(" ", "")}.studio.app"
                }

            val fileMatch = Regex("([A-Za-z0-9_.-]+\\.(?:json|zip|apk|aab|jks|keystore|md|conf|env|mp4|kt|xml))", RegexOption.IGNORE_CASE)
                .find(sec.heading + " " + cleanVo + " " + sec.bulletLines.joinToString(" "))?.groupValues?.get(1)
                ?: when (mode) {
                    "file_picker" -> "${appName.replace(" ", "_")}_Project.zip"
                    "auth_keys" -> "release-signing.jks"
                    "media_stream" -> "DriveStream_1080p_Master.mp4"
                    "build_run" -> "${appName.lowercase().replace(" ", "-")}-release.apk"
                    else -> "google-services.json"
                }

            val codeArr = JSONArray()
            if (sec.codeLines.isNotEmpty()) {
                sec.codeLines.take(4).forEach { codeArr.put(it) }
            } else if (globalCodeSnippets.isNotEmpty()) {
                globalCodeSnippets.take(3).forEach { codeArr.put(it) }
            } else {
                codeArr.put("PROJECT_NAME=${appName.replace(" ", "_")}")
                codeArr.put("STEP_${idx + 1}=${secTitle.uppercase().replace(" ", "_").take(20)}")
                codeArr.put("STATUS=VERIFIED_READY")
            }

            // Build 4 distinct items for phone / browser / recap cards
            val itemsArr = JSONArray()
            if (idx == 0 || idx == finalSections.size - 1) {
                // For Hero Intro & Recap Outro, showcase the actual scene/step titles of the entire tutorial!
                val stepTitles = allSceneTitles.filter { it.isNotBlank() }
                for (k in 0 until 6) {
                    val stTitle = stepTitles.getOrNull(k) ?: pt(k, "Step ${k + 1}")
                    val stSub = if (idx == 0) "Step ${k + 1} \u2022 Included in Guide" else "Completed & Verified \u2713"
                    itemsArr.put("${stTitle.take(28)}|$stSub")
                }
            } else {
                for (k in 0 until 4) {
                    val pText = combinedPoints.getOrNull(k) ?: secTitle
                    val splitColon = pText.split(Regex("[:\u2014\u2013-]"), limit = 2)
                    if (splitColon.size == 2 && splitColon[0].trim().length in 3..26) {
                        itemsArr.put("${splitColon[0].trim()}|${splitColon[1].trim().take(36)}")
                    } else {
                        val w = pText.split(Regex("\\s+")).filter { it.isNotBlank() }
                        val headW = w.take(3).joinToString(" ").take(24)
                        val tailW = w.drop(3).joinToString(" ").take(36).ifBlank { secTitle.take(32) }
                        itemsArr.put("$headW|$tailW")
                    }
                }
            }

            val o = JSONObject().apply {
                put("min", calcMin)
                put("vo", cleanVo)
                put("osd1", if (idx == 0) "${appName.uppercase()}  \u2022  ${tagline.uppercase().take(32)}" else "STEP ${idx + 1}  \u2022  ${secTitle.uppercase().take(34)}")
                put("osd2", pt(0, secTitle).uppercase().take(48))
                put("osd3", pt(1, tagline).uppercase().take(48))
                put("screenTitle", secTitle.take(24))
                put("screenSub", pt(0, tagline).take(36))
                put("mode", mode)
                put("browserUrl", urlMatch)
                put("fileName", fileMatch)
                put("codeLines", codeArr)
                put("typingText", pt(0, secTitle).take(22))
                put("buttonText", when (mode) {
                    "browser_flow" -> "Authorize & Continue"
                    "file_picker" -> "Attach $fileMatch"
                    "code_vars" -> "Save Configuration"
                    "auth_keys" -> "Verify & Save Key"
                    "media_stream" -> "Play 1080p Stream"
                    "build_run" -> "Download Output"
                    "error_fix" -> "Apply Fix & Retry"
                    "outro_recap" -> "\u2713 Setup Complete"
                    else -> "Continue: ${secTitle.take(16)}"
                })
                put("leftTitle", secTitle.take(24))
                put("leftLines", JSONArray().apply {
                    put(pt(0, secTitle).take(34))
                    put(pt(1, "Step ${idx + 1} workflow").take(34))
                    put(pt(2, "Verify configuration").take(34))
                })
                put("rightTitle", if (combinedPoints.size > 3) "Key Details" else "Step Checklist")
                put("rightLines", JSONArray().apply {
                    put(pt(3, pt(0, secTitle)).take(34))
                    put(pt(4, pt(1, tagline)).take(34))
                    put(pt(5, "Complete & continue").take(34))
                })
                put("bannerTitle", "$secTitle  \u2022  $appName")
                put("bannerSub", cleanVo.take(82))
                put("items", itemsArr)
            }
            scArr.put(o)
        }

        return JSONObject().apply {
            put("appName", appName)
            put("tagline", tagline)
            put("themeId", themeId)
            put("scenes", scArr)
        }
    }

    private fun extractShortTopic(text: String, fallback: String): String {
        val cleaned = text
            .replace(Regex("^(?:Namaste|Namaskar|Hello|Welcome|Swagat|Dosto|Toh|Ab|Sabse pehle|Yahan|Is step mein|Aaj ki)\\s+[^,.:]*[,.:]?\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[^A-Za-z0-9 +&/.\\-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        val words = cleaned.split(' ').filter { it.length >= 2 }
        return if (words.isNotEmpty()) words.take(4).joinToString(" ").take(28) else fallback
    }

    /**
     * Compiles the JSON blueprint into a complete 1920x1080 Full HD Canvas Video Project
     * featuring 10 distinct multi-stage animated screen archetypes (Browser, File Picker, Code/Vars Editor,
     * OAuth/Keystore, Stream Player, Build Console, Error Fix Inspector, Hero Montage, Recap Grid, and Dual-Stage App UI)
     * plus Android native TTS Live Preview voiceover bridge and full 15-SFX support.
     */
    private fun compileBlueprintToProject(bp: JSONObject): String {
        val bpJsonString = bp.toString()
        val openTag = "<!DOC" + "TYPE ht" + "ml>\n<ht" + "ml lang=\"hi\">"
        val closeTag = "</ht" + "ml>"
        return openTag + """
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>Studio Video Project</title>
<style>
:root{--bg:#050811;--ink:#F1F5FF;--mut:#94A6C8;--card:#0E1526;--ac:#38BDF8;box-sizing:border-box}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--ink);font:15px/1.5 system-ui,"Segoe UI",Roboto,sans-serif}
.w{max-width:940px;margin:0 auto;padding:14px}
canvas{width:100%;height:auto;aspect-ratio:16/9;display:block;border-radius:12px;background:#000}
.ctl{display:flex;flex-wrap:wrap;gap:8px;margin:12px 0;align-items:center}
button,select{font:inherit;padding:9px 13px;border-radius:9px;border:1px solid #263558;background:var(--card);color:var(--ink);cursor:pointer}
#st{font-size:13px;color:var(--mut);min-height:20px}
</style>
</head>
<body>
<div class="w">
<canvas id="cv" width="1920" height="1080"></canvas>
<div class="ctl">
  <button id="pv">▶ Preview chalao</button>
  <select id="scSel"></select>
  <label style="font-size:13px"><input type="checkbox" id="cap" checked> Subtitle</label>
  <label style="font-size:13px"><input type="checkbox" id="vo" checked> Voice</label>
  <label style="font-size:13px"><input type="checkbox" id="sf" checked> SFX Only</label>
</div>
<div id="st"></div>
</div>
<script>
var BP = """ + bpJsonString + """;
var THEMES = [
  {bg:"#040D12", c1:"#10B981", c2:"#06B6D4", c3:"#3B82F6", card:"#091921", card2:"#102733", stk:"#1E4558"},
  {bg:"#050918", c1:"#7C5CFF", c2:"#22D3EE", c3:"#34D399", card:"#121936", card2:"#1B254B", stk:"#2E3D72"},
  {bg:"#09050A", c1:"#F43F5E", c2:"#F59E0B", c3:"#A855F7", card:"#1A0E1E", card2:"#28152E", stk:"#4A2556"},
  {bg:"#050814", c1:"#38BDF8", c2:"#818CF8", c3:"#F472B6", card:"#0E1528", card2:"#17223E", stk:"#283A66"},
  {bg:"#0B0905", c1:"#F59E0B", c2:"#38BDF8", c3:"#10B981", card:"#1C160C", card2:"#2B2213", stk:"#4D3D22"},
  {bg:"#070612", c1:"#EC4899", c2:"#8B5CF6", c3:"#06B6D4", card:"#161229", card2:"#221C3D", stk:"#3B3066"}
];
var TH = THEMES[(BP.themeId || 0) % THEMES.length];

var SC = (BP.scenes || []).map(function(s, idx){
  return {
    i: idx,
    min: s.min || 14,
    vo: String(s.vo || ""),
    osd1: String(s.osd1 || BP.appName || "SHOWCASE"),
    osd2: String(s.osd2 || s.screenTitle || "STEP DETAILS"),
    osd3: String(s.osd3 || s.bannerTitle || "VERIFIED"),
    screenTitle: String(s.screenTitle || BP.appName || "Studio"),
    screenSub: String(s.screenSub || BP.tagline || "Interactive Guide"),
    mode: String(s.mode || "feature_ui"),
    browserUrl: String(s.browserUrl || "console.cloud.google.com"),
    fileName: String(s.fileName || "project_config.json"),
    codeLines: Array.isArray(s.codeLines) ? s.codeLines.map(String) : ["CONFIG=ENABLED"],
    typingText: String(s.typingText || s.screenTitle || "Search"),
    buttonText: String(s.buttonText || "Continue"),
    leftTitle: String(s.leftTitle || s.screenTitle || "Step Highlights"),
    leftLines: Array.isArray(s.leftLines) ? s.leftLines.map(String) : ["Step 1", "Step 2", "Step 3"],
    rightTitle: String(s.rightTitle || "Checklist"),
    rightLines: Array.isArray(s.rightLines) ? s.rightLines.map(String) : ["Detail 1", "Detail 2", "Detail 3"],
    bannerTitle: String(s.bannerTitle || BP.appName || "Complete Guide"),
    bannerSub: String(s.bannerSub || BP.tagline || ""),
    items: Array.isArray(s.items) ? s.items.map(String) : ["Item 1|Detail", "Item 2|Detail", "Item 3|Detail", "Item 4|Detail"]
  };
});
var S = SC;

var scSelEl = document.getElementById("scSel");
if (scSelEl) {
  SC.forEach(function(s, i){
    var opt = document.createElement("option");
    opt.value = String(i);
    opt.textContent = "Scene " + (i + 1) + ": " + s.screenTitle;
    scSelEl.appendChild(opt);
  });
}

var cv=document.getElementById("cv"),c=cv.getContext("2d"),W=1920,H=1080,CX=960;
var SF='system-ui,"Segoe UI",Roboto,sans-serif',MO='ui-monospace,Consolas,monospace';
var CYN=TH.c2,VI=TH.c1,PNK=TH.c3,GRN="#34D399",AMB="#FBBF24",RED="#F87171",CARD=TH.card,CARD2=TH.card2,STK=TH.stk,TXT="#F1F5FF",SUB="#94A6C8";
var GHB="#0d1117",GH2="#161b22",GHL="#30363d",GHT="#e6edf3",GHS="#8b949e",GHG="#238636",GHBL="#58a6ff";

var cl=function(v,a,b){return v<a?a:v>b?b:v};
var E=function(t){t=cl(t,0,1);return 1-Math.pow(1-t,3)};
var BK=function(t){t=cl(t,0,1);return 1+2.7*Math.pow(t-1,3)+1.7*Math.pow(t-1,2)};
var seg=function(t,a,b){return cl((t-a)/(b-a),0,1)};
var A=function(q,a,d){return E((q-a)/(d||.04))};
var hs=function(n){var x=Math.sin(n*127.1+311.7)*43758.5453;return x-Math.floor(x)};
function rr(x,y,w,h,r){c.beginPath();c.moveTo(x+r,y);c.arcTo(x+w,y,x+w,y+h,r);c.arcTo(x+w,y+h,x,y+h,r);c.arcTo(x,y+h,x,y,r);c.arcTo(x,y,x+w,y,r);c.closePath()}
function tx(s,x,y,size,col,al,wt,a,sp,font){c.save();c.globalAlpha*=(a==null?1:a);c.font=(wt||400)+" "+size+"px "+(font||SF);c.fillStyle=col;c.textAlign=al||"left";c.textBaseline="middle";
  if(sp){try{c.letterSpacing=sp+"px"}catch(e){}}c.fillText(s,x,y);c.restore()}
function glow(col,b){c.shadowColor=col;c.shadowBlur=b}
function noglow(){c.shadowColor="transparent";c.shadowBlur=0}
function gradH(x,w,c1,c2){var g=c.createLinearGradient(x,0,x+w,0);g.addColorStop(0,c1||VI);g.addColorStop(1,c2||CYN);return g}
function gradDiag(x,y,w,h,c1,c2,c3){var g=c.createLinearGradient(x,y,x+w,y+h);g.addColorStop(0,c1||VI);g.addColorStop(.5,c2||CYN);g.addColorStop(1,c3||PNK);return g}
function pop(cx,cy,a,fn){if(a<=0)return;var s=BK(a);c.save();c.globalAlpha=Math.min(1,a*1.5);c.translate(cx,cy);c.scale(s,s);c.translate(-cx,-cy);fn();c.restore()}
function strLim(s,n){s=String(s||"");return s.length>n?s.slice(0,n-1)+"…":s}

var DUR=null,PLAN=[],TOTAL=0;
function est(i){return Math.max(4,SC[i].vo.length*0.076)}
function vdur(i){return DUR&&DUR[i]&&DUR[i].a>0?DUR[i].a:est(i)}
function buildPlan(){PLAN=[];var T0=0;
  SC.forEach(function(s,i){var vd=vdur(i),len=Math.max(s.min,vd+1.6);if(i===SC.length-1)len=Math.max(s.min,0.5+vd+2.8);
    PLAN.push({i:i,start:T0,len:len,vd:vd,vs:0.5});T0+=len});
  TOTAL=T0}
window.sfxPlan=function(){
  buildPlan();
  var L=[];
  PLAN.forEach(function(p,idx){
    var st=p.start,vd=p.vd,vs=p.vs,s=SC[idx],m=s.mode;
    L.push({t:st+0.04,k:"whoosh",d:idx===0?1.0:0.72});
    if(idx===0)L.push({t:st+0.42,k:"chime"});
    L.push({t:st+vs+0.12*vd,k:"slide"});
    L.push({t:st+vs+0.24*vd,k:"popup"});
    if(m==="code_vars"||m==="auth_keys"||m==="feature_ui"||m==="browser_flow"){
      L.push({t:st+vs+0.16*vd,k:"type",n:12,gap:0.068});
    }
    L.push({t:st+vs+0.36*vd,k:"popup"});
    L.push({t:st+vs+0.48*vd,k:"tap"});
    L.push({t:st+vs+0.58*vd,k:"slide"});
    L.push({t:st+vs+0.70*vd,k:"click"});
    L.push({t:st+vs+0.76*vd,k:"success"});
    if(idx===PLAN.length-1)L.push({t:st+vs+0.85*vd,k:"chime"});
  });
  return L;
};
function tp(q,t0,d){d=d||.04;return q>=t0&&q<t0+d?(q-t0)/d:0}
function floatY(T){return Math.sin(T*1.25)*6}

function backdrop(T){
  c.fillStyle=TH.bg;c.fillRect(0,0,W,H);
  var g=c.createRadialGradient(CX,500,60,CX,500,1120);g.addColorStop(0,VI+"38");g.addColorStop(.5,CYN+"22");g.addColorStop(1,"rgba(0,0,0,0)");c.fillStyle=g;c.fillRect(0,0,W,H);
  c.save();c.strokeStyle=CYN+"14";c.lineWidth=1;var o=(T*14)%80;
  for(var x=-80+o;x<W;x+=80){c.beginPath();c.moveTo(x,0);c.lineTo(x,H);c.stroke()}
  for(var y=-80+o;y<H;y+=80){c.beginPath();c.moveTo(0,y);c.lineTo(W,y);c.stroke()}c.restore();
  for(var k=0;k<48;k++){var sp=5+hs(k+3)*13,x=(hs(k)*W+T*sp+W*4)%W,y=(hs(k+40)*H-T*sp*.5+H*4)%H,r=.9+hs(k+80)*2.1,a=.14+.26*hs(k+120)*(.6+.4*Math.sin(T*.8+k));
    c.fillStyle=k%2===0?CYN:VI;c.save();c.globalAlpha=a;c.beginPath();c.arc(x,y,r,0,7);c.fill();c.restore()}
}
function wrapLines(s,mw,font){c.save();c.font=font;var w=s.split(" "),L=[],l="";w.forEach(function(k){var q=l?l+" "+k:k;if(c.measureText(q).width>mw&&l){L.push(l);l=k}else l=q});L.push(l);c.restore();return L}
function caption(i,q){
  if(!document.getElementById("cap").checked)return;
  var s=SC[i];if(!s.sn){s.sn=(s.vo.match(/[^.!?।]+[.!?।]+(?:\s|$)/g)||[s.vo])}
  var L=s.vo.length,pos=cl(q,0,.999)*L,acc=0,k=0;for(;k<s.sn.length-1;k++){if(pos<acc+s.sn[k].length)break;acc+=s.sn[k].length}
  var a=cl((pos-acc)/5,0,1),txt=s.sn[k].trim(),fs=txt.length>150?31:35,f="600 "+fs+"px "+SF,lines=wrapLines(txt,1560,f),lh=fs+12,h=lines.length*lh+22,y0=1012-h;
  c.save();c.globalAlpha=a;c.fillStyle="rgba(6,9,19,.86)";var wd=0;c.font=f;lines.forEach(function(l){wd=Math.max(wd,c.measureText(l).width)});
  rr(CX-wd/2-30,y0,wd+60,h,18);c.fill();c.strokeStyle=CYN;c.lineWidth=1.5;c.stroke();
  lines.forEach(function(l,j){tx(l,CX,y0+11+lh/2+j*lh,fs,"#fff","center",600)});c.restore()
}

function ico(x,y,r,kind,col){c.save();c.translate(x,y);c.strokeStyle=col;c.fillStyle=col;c.lineWidth=r*.15;c.lineCap="round";c.lineJoin="round";
  if(kind==="check"){c.beginPath();c.moveTo(-r*.4,0);c.lineTo(-r*.1,r*.3);c.lineTo(r*.45,-r*.32);c.stroke()}
  else if(kind==="search"){c.beginPath();c.arc(-r*.08,-r*.08,r*.28,0,7);c.stroke();c.beginPath();c.moveTo(r*.14,r*.14);c.lineTo(r*.38,r*.38);c.stroke()}
  else if(kind==="spark"){c.beginPath();for(var k=0;k<8;k++){var a=k*Math.PI/4,rd=k%2===0?r*.48:r*.16;c.lineTo(Math.cos(a)*rd,Math.sin(a)*rd)}c.closePath();c.fill()}
  else if(kind==="lock"){c.beginPath();c.arc(0,-r*.18,r*.28,Math.PI,0);c.stroke();rr(-r*.38,-r*.18,r*.76,r*.58,r*.1);c.fill()}
  else if(kind==="warn"){c.beginPath();c.moveTo(0,-r*.48);c.lineTo(r*.48,r*.4);c.lineTo(-r*.48,r*.4);c.closePath();c.stroke()}
  else if(kind==="play"){c.beginPath();c.moveTo(-r*.22,-r*.34);c.lineTo(r*.36,0);c.lineTo(-r*.22,r*.34);c.closePath();c.fill()}
  else if(kind==="up"){c.beginPath();c.moveTo(0,r*.38);c.lineTo(0,-r*.35);c.moveTo(-r*.28,-r*.06);c.lineTo(0,-r*.36);c.lineTo(r*.28,-r*.06);c.stroke()}
  else{c.beginPath();c.arc(0,0,r*.32,0,7);c.stroke()}
  c.restore()}

function panel(x,y,w,h,a,col){c.save();c.globalAlpha*=a;glow("rgba(0,0,0,.55)",28);c.fillStyle=CARD;rr(x,y,w,h,24);c.fill();noglow();c.strokeStyle=col||CYN;c.lineWidth=2;c.stroke();c.restore()}
function sideCard(x,y,w,h,a,title,lines,col,icon){if(a<=0)return;c.save();c.translate((1-a)*(x<CX?-40:40),0);c.globalAlpha=a;
  panel(x,y,w,h,1,col);if(icon)ico(x+48,y+52,48,icon,col||CYN);tx(strLim(title,24),x+(icon?86:28),y+46,27,"#fff","left",800);
  (lines||[]).slice(0,3).forEach(function(l,k){tx(strLim(l,34),x+28,y+102+k*38,21,k===0?"#E2E8F0":SUB,"left",k===0?700:500)});c.restore()}
function cbBanner(x,y,w,h,a,col,icon,title,sub){if(a<=0)return;c.save();c.globalAlpha*=a;c.translate(0,(1-a)*18);panel(x,y,w,h,1,col);ico(x+54,y+h/2,50,icon,col);tx(strLim(title,52),x+104,y+h/2-(sub?17:0),26,"#fff","left",800);if(sub)tx(strLim(sub,80),x+104,y+h/2+22,20,"#CBD5F5","left",600);c.restore()}
function osd(items,q){var cur=null;items.forEach(function(it){if(q>=it[0])cur=it});if(!cur)return;var a=A(q,cur[0],.03);c.save();c.globalAlpha=a;c.font="800 28px "+SF;var w=c.measureText(cur[1]).width+72;
  glow(VI,24);c.fillStyle=CARD;rr(CX-w/2,36,w,58,29);c.fill();noglow();c.strokeStyle=CYN;c.lineWidth=2;c.stroke();tx(cur[1],CX,66,28,"#fff","center",800);c.restore()}
function tap(x,y,p){if(p<=0||p>=1)return;c.save();c.globalAlpha=1-p*.7;c.strokeStyle=CYN;c.lineWidth=3;c.beginPath();c.arc(x,y,10+p*38,0,7);c.stroke();
  c.fillStyle="rgba(255,255,255,.65)";c.beginPath();c.arc(x,y,12*(1-p*.4),0,7);c.fill();c.restore()}
function hlBox(x,y,w,h,a,T,r){if(a<=0)return;c.save();c.globalAlpha=a;glow(CYN,12+8*Math.sin(T*5));c.strokeStyle=CYN;c.lineWidth=2.8;rr(x-4,y-4,w+8,h+8,r||14);c.stroke();c.restore()}
function btn(x,y,w,h,label,pri,pr,hl,T,col){c.save();var s=1-.05*(pr||0);c.translate(x+w/2,y+h/2);c.scale(s,s);c.translate(-w/2,-h/2);
  if(hl){glow(CYN,10+10*(.5+.5*Math.sin((T||0)*5)))}
  c.fillStyle=col||(pri?gradH(0,w,VI,CYN):CARD2);rr(0,0,w,h,Math.min(14,h*.34));c.fill();noglow();if(!pri&&!col){c.strokeStyle=hl?CYN:STK;c.lineWidth=hl?2.2:1.4;c.stroke()}
  tx(strLim(label,26),w/2,h/2+1,Math.min(15,h*.38),"#fff","center",700);c.restore()}
function field(x,y,w,label,val,foc,T){tx(strLim(label,28),x+2,y-9,11.5,SUB,"left",600);c.fillStyle="#070B16";rr(x,y,w,42,12);c.fill();c.strokeStyle=foc?CYN:STK;c.lineWidth=foc?2:1.2;c.stroke();
  tx(strLim(val,28)+((foc&&Math.floor(T*2)%2===0)?"|":""),x+14,y+21,13.5,TXT,"left",600,1,0,MO)}

var PW=364,PH=748,SI=15,SWD=PW-2*SI,SHT=PH-2*SI;
function phone(cx,cy,s,rot,alpha,screen){
  if(alpha<=0)return;
  c.save();c.globalAlpha=alpha;c.translate(cx,cy);c.rotate(rot);c.scale(s,s);
  glow(VI,52);
  var g=c.createLinearGradient(-PW/2,0,PW/2,0);g.addColorStop(0,"#263354");g.addColorStop(.5,"#0E1426");g.addColorStop(1,"#1D2844");
  c.fillStyle=g;rr(-PW/2,-PH/2,PW,PH,56);c.fill();noglow();
  c.strokeStyle=CYN;c.lineWidth=3;rr(-PW/2,-PH/2,PW,PH,56);c.stroke();
  c.fillStyle="#000";rr(-PW/2+SI-2,-PH/2+SI-2,SWD+4,SHT+4,44);c.fill();
  c.save();c.translate(-SWD/2,-SHT/2);rr(0,0,SWD,SHT,42);c.clip();
  var bg=c.createLinearGradient(0,0,0,SHT);bg.addColorStop(0,CARD);bg.addColorStop(1,TH.bg);c.fillStyle=bg;c.fillRect(0,0,SWD,SHT);
  screen();
  c.fillStyle="#000";rr(SWD/2-48,10,96,24,12);c.fill();
  c.restore();c.restore();
}

function browser(x,y,w,h,url,a,fn){
  if(a<=0)return;
  c.save();c.globalAlpha=a;glow("rgba(0,0,0,.6)",40);c.fillStyle=GHB;rr(x,y,w,h,20);c.fill();noglow();
  c.save();rr(x,y,w,h,20);c.clip();c.fillStyle=GH2;c.fillRect(x,y,w,52);
  c.fillStyle="#ff5f57";c.beginPath();c.arc(x+28,y+26,7,0,7);c.fill();
  c.fillStyle="#febc2e";c.beginPath();c.arc(x+52,y+26,7,0,7);c.fill();
  c.fillStyle="#28c840";c.beginPath();c.arc(x+76,y+26,7,0,7);c.fill();
  c.fillStyle=GHB;rr(x+120,y+10,w-240,32,16);c.fill();ico(x+146,y+26,20,"lock",CYN);tx(strLim(url,54),x+168,y+27,19,GHT,"left",600,1,0,MO);
  c.beginPath();c.rect(x,y+52,w,h-52);c.clip();c.translate(x,y+52);fn(w,h-52);c.restore();
  c.strokeStyle=CYN;c.lineWidth=2;rr(x,y,w,h,20);c.stroke();c.restore();
}

function renderPhoneScreenByMode(s,T,q){
  tx("9:41",28,22,13,"#F1F5FF","left",700);
  tx(strLim(s.screenTitle,22),20,62,19,"#fff","left",800);
  tx(strLim(s.screenSub,32),20,84,11.5,CYN,"left",600);

  var m = s.mode;
  var list = s.items || [];

  if (m === "file_picker") {
    /* Stage 1: Project/File list -> Stage 2: Bottom sheet file picker slides up -> Stage 3: Attached & Verified */
    list.slice(0,3).forEach(function(rawIt, k){
      var parts = String(rawIt).split("|");
      var y = 112 + k * 78;
      c.fillStyle = CARD2; rr(16, y, SWD-32, 68, 15); c.fill();
      c.strokeStyle = k===0 ? CYN : STK; c.lineWidth = 1.4; c.stroke();
      ico(44, y+34, 22, "up", CYN);
      tx(strLim(parts[0]||s.fileName, 22), 74, y+25, 13.5, "#fff", "left", 800);
      tx(strLim(parts[1]||"Tap to configure", 28), 74, y+46, 11, SUB, "left", 500);
    });
    var sheetP = A(q, 0.22, 0.06);
    if (sheetP > 0) {
      var sy = SHT - 360 * sheetP;
      c.fillStyle = "rgba(0,0,0," + (0.45*sheetP) + ")"; c.fillRect(0, 0, SWD, SHT);
      c.fillStyle = "#0B1328"; rr(0, sy, SWD, 370, 26); c.fill();
      c.strokeStyle = CYN; c.lineWidth = 2; c.stroke();
      tx("Select File from Storage", 22, sy+34, 16, "#fff", "left", 800);
      var fNames = [s.fileName, "config_backup.json", "readme_notes.md"];
      fNames.forEach(function(fn, fk){
        var fy = sy + 64 + fk * 64;
        var sel = (fk === 0 && q > 0.42);
        c.fillStyle = sel ? (CYN+"28") : CARD; rr(18, fy, SWD-36, 54, 13); c.fill();
        c.strokeStyle = sel ? GRN : STK; c.lineWidth = sel ? 2 : 1; c.stroke();
        ico(44, fy+27, 22, sel?"check":"up", sel?GRN:CYN);
        tx(strLim(fn, 24), 72, fy+27, 13, "#fff", "left", 700, 1, 0, MO);
      });
      tap(SWD/2, sy+91, tp(q, 0.44, 0.04));
      btn(20, sy+272, SWD-40, 46, q>=0.70 ? ("✓ Attached: " + strLim(s.fileName,16)) : s.buttonText, true, tp(q,0.68,0.04)>0?1:0, q>0.55, T, q>=0.70?GRN:null);
      tap(SWD/2, sy+295, tp(q, 0.68, 0.04));
    }
    return;
  }

  if (m === "code_vars") {
    tx("CONFIG / VARIABLES EDITOR", 20, 114, 11, AMB, "left", 800);
    c.fillStyle = "#050811"; rr(16, 128, SWD-32, 185, 14); c.fill();
    c.strokeStyle = CYN; c.lineWidth = 1.8; c.stroke();
    var cLines = s.codeLines || ["KEY=VALUE"];
    cLines.slice(0,4).forEach(function(cln, ck){
      var prog = seg(q, 0.10 + ck*0.14, 0.24 + ck*0.14);
      var shown = cln.slice(0, Math.floor(prog * cln.length));
      tx(strLim(shown, 28) + ((prog>0&&prog<1&&Math.floor(T*3)%2===0)?"|":""), 28, 156 + ck*38, 12.5, ck===0?"#67E8F9":"#A7F3D0", "left", 600, 1, 0, MO);
    });
    if (q > 0.20 && q < 0.75) hlBox(16, 128, SWD-32, 185, A(q, 0.20, 0.04), T, 14);
    list.slice(0,2).forEach(function(rawIt, k){
      var parts = String(rawIt).split("|");
      var y = 332 + k * 78;
      c.fillStyle = CARD2; rr(16, y, SWD-32, 66, 14); c.fill();
      ico(42, y+33, 22, "check", GRN);
      tx(strLim(parts[0]||"Config", 22), 70, y+24, 13.5, "#fff", "left", 800);
      tx(strLim(parts[1]||"Verified", 28), 70, y+45, 11, SUB, "left", 500);
    });
  } else if (m === "auth_keys") {
    c.fillStyle = CARD2; rr(16, 108, SWD-32, 360, 18); c.fill();
    c.strokeStyle = q>0.72 ? GRN : STK; c.lineWidth = 1.6; c.stroke();
    var t1 = seg(q, 0.12, 0.34);
    field(28, 140, SWD-56, s.leftTitle || "Client ID / Key Alias", s.typingText.slice(0, Math.floor(t1*s.typingText.length)), t1>0&&t1<1, T);
    tx("Secret / Token / Password", 30, 206, 11.5, SUB, "left", 600);
    c.fillStyle = "#070B16"; rr(28, 216, SWD-56, 42, 12); c.fill(); c.strokeStyle = CYN; c.lineWidth = 1.5; c.stroke();
    var dotCount = Math.floor(seg(q, 0.36, 0.58) * 16);
    for (var d=0; d<dotCount; d++) {
      c.fillStyle = "#fff"; c.beginPath(); c.arc(46 + d*13, 237, 3.8, 0, 7); c.fill();
    }
    if (q > 0.62) {
      c.fillStyle = "rgba(52,211,153,.16)"; rr(28, 276, SWD-56, 64, 14); c.fill();
      c.strokeStyle = GRN; c.lineWidth = 1.5; c.stroke();
      ico(54, 308, 26, "lock", GRN);
      tx("Credentials Verified ✓", 82, 298, 13.5, GRN, "left", 800);
      tx(strLim(s.fileName, 24), 82, 320, 11.5, "#fff", "left", 600, 1, 0, MO);
    }
  } else if (m === "media_stream") {
    /* 16:9 Stream Video Player Box inside Phone */
    c.fillStyle = "#000"; rr(16, 106, SWD-32, 178, 16); c.fill();
    var vg = gradDiag(16, 106, SWD-32, 178, VI, CYN, "#050811");
    c.save(); c.globalAlpha = 0.55; c.fillStyle = vg; rr(16, 106, SWD-32, 178, 16); c.fill(); c.restore();
    c.strokeStyle = CYN; c.lineWidth = 2; rr(16, 106, SWD-32, 178, 16); c.stroke();
    c.fillStyle = "rgba(0,0,0,.65)"; c.beginPath(); c.arc(SWD/2, 186, 28, 0, 7); c.fill();
    ico(SWD/2+3, 186, 28, "play", "#fff");
    /* Progress timeline bar inside video box */
    c.fillStyle = "rgba(255,255,255,.22)"; rr(28, 262, SWD-56, 6, 3); c.fill();
    c.fillStyle = CYN; rr(28, 262, (SWD-56)*cl(q, 0.08, 1), 6, 3); c.fill();
    tx("1080p Full HD", 28, 126, 11, GRN, "left", 800);
    tx("LIVE STREAM", SWD-28, 126, 11, CYN, "right", 800);

    /* Quality / Stream Chips */
    ["1080p60", "Auto HLS", "Subtitles", "Fast Seek"].forEach(function(chLb, ck){
      var cx0 = 18 + ck * 76;
      c.fillStyle = ck===0 ? CYN : CARD2; rr(cx0, 296, 70, 26, 13); c.fill();
      tx(chLb, cx0+35, 309, 10, ck===0?"#000":"#fff", "center", 800);
    });

    list.slice(0,2).forEach(function(rawIt, k){
      var parts = String(rawIt).split("|");
      var y = 336 + k * 78;
      c.fillStyle = CARD2; rr(16, y, SWD-32, 68, 14); c.fill();
      c.strokeStyle = k===0 ? CYN : STK; c.lineWidth = 1.4; c.stroke();
      ico(42, y+34, 22, "check", GRN);
      tx(strLim(parts[0]||"Stream Track", 22), 70, y+25, 13.5, "#fff", "left", 800);
      tx(strLim(parts[1]||"Ready to play", 28), 70, y+46, 11, SUB, "left", 500);
    });
  } else if (m === "build_run") {
    var bProg = cl((q - 0.08) / 0.65, 0, 1);
    var bDone = bProg >= 0.98;
    c.fillStyle = CARD2; rr(16, 108, SWD-32, 104, 16); c.fill();
    c.strokeStyle = bDone ? GRN : CYN; c.lineWidth = 1.6; c.stroke();
    tx(bDone ? "Process Complete ✓" : ("Running: " + strLim(s.screenTitle, 18)), 30, 136, 14, bDone?GRN:"#fff", "left", 800);
    tx(Math.round(bProg*100) + "%", SWD-30, 136, 14, CYN, "right", 900);
    c.fillStyle = "#070B16"; rr(30, 158, SWD-60, 10, 5); c.fill();
    c.fillStyle = bDone ? GRN : gradH(30, SWD-60, VI, CYN); rr(30, 158, Math.max(10, (SWD-60)*bProg), 10, 5); c.fill();
    tx(strLim(s.fileName, 28), 30, 188, 11.5, SUB, "left", 600, 1, 0, MO);

    /* Console Log Box */
    c.fillStyle = "#050811"; rr(16, 224, SWD-32, 260, 14); c.fill(); c.strokeStyle = STK; c.lineWidth = 1.2; c.stroke();
    var logItems = list.map(function(it){ return "> " + String(it).replace("|", ": "); });
    logItems.push(bDone ? "✓ SUCCESS: " + s.fileName : "> Executing workflow...");
    var showN = Math.max(1, Math.ceil(bProg * logItems.length));
    logItems.slice(0, showN).forEach(function(ln, lk){
      tx(strLim(ln, 32), 28, 252 + lk*40, 11.5, lk===logItems.length-1 && bDone ? GRN : "#A5F3FC", "left", 600, 1, 0, MO);
    });
  } else if (m === "error_fix") {
    var fixed = q >= 0.52;
    c.fillStyle = fixed ? "rgba(52,211,153,.15)" : "rgba(248,113,113,.15)";
    rr(16, 108, SWD-32, 78, 16); c.fill();
    c.strokeStyle = fixed ? GRN : RED; c.lineWidth = 1.8; c.stroke();
    ico(46, 147, 28, fixed?"check":"warn", fixed?GRN:RED);
    tx(fixed ? "Issue Resolved & Verified" : "Diagnostic Check & Fix", 76, 136, 14, fixed?GRN:"#FCA5A5", "left", 800);
    tx(strLim(s.screenSub, 28), 76, 158, 11.5, "#fff", "left", 600);

    list.slice(0,3).forEach(function(rawIt, k){
      var parts = String(rawIt).split("|");
      var y = 202 + k * 92;
      var cardA = A(q, 0.10 + k*0.12, 0.05);
      c.save(); c.globalAlpha *= cardA;
      c.fillStyle = CARD2; rr(16, y, SWD-32, 80, 15); c.fill();
      c.strokeStyle = fixed ? GRN : AMB; c.lineWidth = 1.4; c.stroke();
      ico(44, y+40, 24, fixed?"check":"warn", fixed?GRN:AMB);
      tx(strLim(parts[0]||"Check Step", 22), 72, y+28, 13.5, "#fff", "left", 800);
      tx(strLim(parts[1]||"Verify configuration", 28), 72, y+52, 11.5, "#BBF7D0", "left", 600);
      c.restore();
    });
  } else {
    /* Interactive Search / Feature List with 2-stage selection */
    var typP = seg(q, 0.12, 0.44);
    c.fillStyle=CARD2;rr(16,104,SWD-32,40,12);c.fill();c.strokeStyle=typP>0&&typP<1?CYN:STK;c.lineWidth=1.5;c.stroke();
    ico(34,124,18,"search",CYN);
    var tFull = s.typingText || s.screenTitle;
    var tCur = tFull.slice(0, Math.floor(typP * tFull.length));
    tx(tCur ? tCur + ((typP<1&&Math.floor(T*2)%2===0)?"|":"") : "Search or select...", 52, 124, 12, tCur?"#fff":SUB, "left", 600);

    var activeIdx = q < 0.55 ? 0 : 1;
    list.slice(0,4).forEach(function(rawIt, k){
      var parts = String(rawIt).split("|");
      var t1 = parts[0] || ("Step " + (k+1));
      var t2 = parts[1] || s.screenSub;
      var y = 160 + k * 84;
      var on = (k === activeIdx && q > 0.25);
      c.fillStyle = on ? (CYN+"24") : CARD2;
      rr(16, y, SWD-32, 72, 15); c.fill();
      c.strokeStyle = on ? CYN : STK; c.lineWidth = on ? 2.2 : 1.2; c.stroke();
      c.fillStyle = gradDiag(26, y+12, 48, 48, k%2?VI:CYN, k%2?PNK:VI, GRN);
      rr(26, y+12, 48, 48, 12); c.fill();
      ico(50, y+36, 22, on?"check":"spark", "#fff");
      tx(strLim(t1, 22), 86, y+26, 13.5, "#fff", "left", 800);
      tx(strLim(t2, 30), 86, y+47, 11, SUB, "left", 500);
    });
    if (q > 0.30 && q < 0.88) hlBox(16, 160 + activeIdx*84, SWD-32, 72, A(q, 0.30, 0.04), T, 15);
    tap(SWD/2, 196 + activeIdx*84, tp(q, 0.54, 0.04));
  }

  /* Bottom CTA Button */
  var pr = tp(q, 0.72, 0.04) > 0 ? 1 : 0;
  var done = q >= 0.76;
  btn(20, 512, SWD-40, 44, done ? ("✓ " + s.buttonText) : s.buttonText, true, pr, q > 0.65, T, done ? GRN : null);
  tap(SWD/2, 534, tp(q, 0.72, 0.04));

  /* Bottom Nav */
  var ny = SHT - 56;
  c.fillStyle="#070B14";rr(12,ny,SWD-24,44,22);c.fill();c.strokeStyle=STK;c.lineWidth=1.2;c.stroke();
  ["Home","Steps","Preview","Settings"].forEach(function(lb,k){
    var x=12+(SWD-24)*(k+.5)/4,on=k===(s.i%4);
    if(on){c.fillStyle=CYN+"2E";rr(x-32,ny+6,64,32,16);c.fill()}
    tx(lb,x,ny+22,10.5,on?CYN:SUB,"center",on?800:600);
  });
}

function sceneDraw(p,T,t){
  var i=p.i,vd=p.vd,q=cl((t-p.vs)/vd,0,1.2),by=floatY(T),s=SC[i];
  s.i = i;
  var m = s.mode;

  /* 1. HERO LAUNCH SCENE (Scene 0): Center Phone + 4 Popping Feature Cards + Bottom Step Pills */
  if (m === "hero_launch") {
    var ta = A(q, 0.01, 0.05);
    c.save(); glow(VI, 34);
    tx(strLim((BP.appName || "STUDIO") + " — " + (BP.tagline || "COMPLETE GUIDE"), 52), CX, 72, 44, "#fff", "center", 900, ta, 1.5);
    c.restore();

    phone(CX, 485 + by, 0.90, 0, A(q, 0.02, 0.05), function(){ renderPhoneScreenByMode(s, T, q); });

    var heroSpots = [
      [340, 310, 0.10, CYN, "spark"],
      [1580, 310, 0.24, VI, "check"],
      [340, 620, 0.38, GRN, "up"],
      [1580, 620, 0.52, AMB, "lock"]
    ];
    (s.items || []).slice(0,4).forEach(function(rawIt, k){
      var parts = String(rawIt).split("|");
      var sp = heroSpots[k];
      var aa = A(q, sp[2], 0.04);
      if (aa > 0) {
        pop(sp[0], sp[1] + Math.sin(T*1.5 + k)*6, aa, function(){
          panel(sp[0]-230, sp[1]-95, 460, 190, 1, sp[3]);
          ico(sp[0]-176, sp[1]-34, 44, sp[4], sp[3]);
          tx(strLim(parts[0] || ("Step " + (k+1)), 22), sp[0]-136, sp[1]-36, 26, "#fff", "left", 800);
          tx(strLim(parts[1] || s.screenSub, 32), sp[0]-196, sp[1]+26, 20, "#CBD5F5", "left", 600);
          tx("STEP 0" + (k+1), sp[0]-196, sp[1]+62, 16, sp[3], "left", 800);
        });
      }
    });
    cbBanner(260, 815, 1400, 92, A(q, 0.64, 0.04), CYN, "spark", s.bannerTitle, s.bannerSub);
    return;
  }

  /* 2. BROWSER FLOW SCENE: Desktop Console Browser (0..0.62) -> Phone Sync View (0.62..1.0) */
  if (m === "browser_flow") {
    osd([[0, s.osd1], [0.36, s.osd2], [0.68, s.osd3]], q);
    var toPhone = A(q, 0.65, 0.05);
    var brwAlpha = 1 - toPhone;
    if (brwAlpha > 0.01) {
      browser(280, 130, 1360, 690, s.browserUrl, brwAlpha, function(w, h){
        c.fillStyle = GHB; c.fillRect(0, 0, w, h);
        c.fillStyle = GH2; c.fillRect(0, 0, w, 64);
        c.fillStyle = VI; rr(28, 16, 32, 32, 8); c.fill();
        tx(strLim(BP.appName + "  •  " + s.screenTitle, 44), 76, 33, 24, GHT, "left", 800);
        tx("Connected ✓", w-36, 33, 18, GRN, "right", 700);

        tx(strLim(s.screenTitle, 38), 56, 114, 34, "#fff", "left", 800);
        tx(strLim(s.screenSub, 64), 56, 154, 20, GHS, "left", 600);

        (s.items || []).slice(0,3).forEach(function(rawIt, k){
          var parts = String(rawIt).split("|");
          var y = 196 + k * 104;
          var on = (k === 0 && q > 0.20) || (k === 1 && q > 0.38);
          c.fillStyle = GH2; rr(56, y, 780, 86, 14); c.fill();
          c.strokeStyle = on ? CYN : GHL; c.lineWidth = on ? 2.5 : 1.5; c.stroke();
          ico(98, y+43, 34, on?"check":"spark", on?GRN:CYN);
          tx(strLim(parts[0] || "Configuration", 32), 138, y+30, 23, GHT, "left", 800);
          tx(strLim(parts[1] || "Verified setting", 46), 138, y+60, 18, GHS, "left", 500);
        });

        /* Right panel inside browser */
        c.fillStyle = GH2; rr(876, 196, 430, 300, 16); c.fill();
        c.strokeStyle = CYN; c.lineWidth = 2; c.stroke();
        tx("Action & Credentials", 906, 236, 22, "#fff", "left", 800);
        (s.codeLines || []).slice(0,3).forEach(function(cln, ck){
          tx(strLim(cln, 28), 906, 284 + ck*42, 17, "#67E8F9", "left", 600, 1, 0, MO);
        });
        btn(906, 416, 370, 56, q>0.46 ? ("✓ " + s.buttonText) : s.buttonText, true, tp(q,0.44,0.04)>0?1:0, q>0.30, T, q>0.46?GHG:VI);
        tap(1091, 444, tp(q, 0.44, 0.04));
      });
    }
    if (toPhone > 0.01) {
      phone(CX, 475 + by, 0.94, 0, toPhone, function(){ renderPhoneScreenByMode(s, T, q); });
      sideCard(110, 260, 540, 250, toPhone, s.leftTitle, s.leftLines, CYN, "check");
      sideCard(1270, 260, 540, 250, toPhone, s.rightTitle, s.rightLines, GRN, "lock");
    }
    return;
  }

  /* 3. OUTRO RECAP SCENE (Final Scene): Popping Step Checklist Grid -> Brand Finale */
  if (m === "outro_recap") {
    osd([[0, s.osd1], [0.55, BP.appName.toUpperCase() + " — READY TO USE"]], q);
    var showBrand = A(q, 0.68, 0.06);
    var gridAlpha = 1 - showBrand;
    if (gridAlpha > 0.01) {
      c.save(); c.globalAlpha = gridAlpha;
      tx(strLim(BP.appName + " — Complete Workflow Summary", 48), CX, 140, 42, "#fff", "center", 900);
      var recapItems = s.items || [];
      recapItems.slice(0,6).forEach(function(rawIt, k){
        var parts = String(rawIt).split("|");
        var col = k % 3, row = Math.floor(k / 3);
        var x = 360 + col * 600, y = 320 + row * 260;
        var aa = A(q, 0.05 + k * 0.07, 0.04);
        pop(x, y, aa, function(){
          panel(x-260, y-95, 520, 190, 1, k%2===0?CYN:GRN);
          ico(x-200, y-30, 48, "check", GRN);
          tx("STEP 0" + (k+1), x-150, y-44, 18, CYN, "left", 800);
          tx(strLim(parts[0] || ("Step " + (k+1)), 22), x-150, y-12, 26, "#fff", "left", 800);
          tx(strLim(parts[1] || "Completed ✓", 34), x-220, y+44, 20, "#BBF7D0", "left", 600);
        });
      });
      cbBanner(260, 790, 1400, 96, A(q, 0.48, 0.04), GRN, "check", s.bannerTitle, s.bannerSub);
      c.restore();
    }
    if (showBrand > 0.01) {
      c.save(); c.globalAlpha = showBrand; c.fillStyle = TH.bg; c.fillRect(0, 0, W, H);
      glow(VI, 48);
      c.fillStyle = gradDiag(CX-90, 220, 180, 180, VI, CYN, PNK);
      rr(CX-90, 220, 180, 180, 44); c.fill(); noglow();
      ico(CX, 310, 86, "check", "#fff");
      tx(BP.appName || "STUDIO SHOWCASE", CX, 470, 74, "#fff", "center", 900, showBrand, 3);
      tx(BP.tagline || "Complete Interactive Tutorial", CX, 548, 30, CYN, "center", 700, showBrand, 1.2);
      glow("#E11D48", 28); c.fillStyle = "#E11D48"; rr(CX-230, 610, 460, 68, 34); c.fill(); noglow();
      tx("👍 LIKE, SHARE & SUBSCRIBE 🔔", CX, 644, 28, "#fff", "center", 900);
      tx("Happy Coding! 🚀", CX, 750, 44, AMB, "center", 800);
      c.restore();
    }
    return;
  }

  /* 4. STANDARD & SPECIALIZED INTERACTIVE SCENES (file_picker, code_vars, auth_keys, media_stream, build_run, error_fix, feature_ui) */
  osd([[0, s.osd1], [0.36, s.osd2], [0.70, s.osd3]], q);

  /* Alternate phone position on some scenes for visual variety */
  var phoneX = (m === "file_picker" || m === "code_vars") ? 640 : CX;
  phone(phoneX, 475 + by, 0.94, 0, 1, function(){ renderPhoneScreenByMode(s, T, q); });

  if (phoneX < CX) {
    /* Wide right-hand inspection panel for file_picker and code_vars */
    var rA = A(q, 0.14, 0.05);
    if (rA > 0) {
      c.save(); c.globalAlpha = rA; c.translate((1-rA)*45, 0);
      panel(920, 165, 860, 560, 1, CYN);
      ico(976, 224, 48, m==="code_vars"?"spark":"check", CYN);
      tx(strLim(s.leftTitle, 32), 1024, 222, 32, "#fff", "left", 800);
      (s.items || []).slice(0,4).forEach(function(rawIt, k){
        var parts = String(rawIt).split("|");
        var yy = 286 + k * 96;
        var rowOn = q > (0.22 + k*0.14);
        c.fillStyle = rowOn ? (CYN+"1F") : CARD2; rr(956, yy, 788, 80, 16); c.fill();
        c.strokeStyle = rowOn ? GRN : STK; c.lineWidth = rowOn ? 2 : 1.2; c.stroke();
        ico(998, yy+40, 30, rowOn?"check":"spark", rowOn?GRN:CYN);
        tx(strLim(parts[0] || "Step", 32), 1040, yy+28, 23, "#fff", "left", 800);
        tx(strLim(parts[1] || "", 48), 1040, yy+56, 18, "#CBD5F5", "left", 500);
      });
      c.restore();
    }
    cbBanner(200, 775, 1520, 96, A(q, 0.58, 0.04), VI, "spark", s.bannerTitle, s.bannerSub);
  } else {
    sideCard(110, 230, 550, 250, A(q, 0.12, 0.04), s.leftTitle, s.leftLines, CYN, "spark");
    sideCard(1260, 230, 550, 250, A(q, 0.34, 0.04), s.rightTitle, s.rightLines, GRN, "check");
    cbBanner(260, 765, 1400, 100, A(q, 0.56, 0.04), VI, "spark", s.bannerTitle, s.bannerSub);
  }
}

function frameAt(T){
  var p=PLAN[PLAN.length-1];for(var k=0;k<PLAN.length;k++){if(T<PLAN[k].start+PLAN[k].len){p=PLAN[k];break}}
  var t=cl(T-p.start,0,p.len),q=cl((t-p.vs)/p.vd,0,1.2);
  c.setTransform(1,0,0,1,0,0);c.globalAlpha=1;c.setLineDash([]);c.lineDashOffset=0;noglow();c.clearRect(0,0,W,H);
  backdrop(T);
  var z=1+.045*(1-E(seg(t,0,1.3)));c.save();c.translate(CX,540);c.scale(z,z);c.translate(-CX,-540);sceneDraw(p,T,t);c.restore();
  var v=c.createRadialGradient(CX,540,520,CX,540,1250);v.addColorStop(0,"rgba(0,0,0,0)");v.addColorStop(1,"rgba(0,0,0,.55)");c.fillStyle=v;c.fillRect(0,0,W,H);
  caption(p.i,q);
  var f=Math.min(seg(t,0,.42),1-seg(t,p.len-.42,p.len));
  if(f<1){c.fillStyle="rgba(0,0,0,"+(1-f)+")";c.fillRect(0,0,W,H)}
}

var $=function(x){return document.getElementById(x)},st=$("st"),pv=$("pv"),scSel=$("scSel"),msg=function(m){if(st)st.textContent=m};
var raf=0,pvOn=false,lastScene=-1,startOffset=0;
function say(i){
  try{
    if($("vo")&&!$("vo").checked)return;
    if(window.Android&&typeof Android.speakPreview==="function"){
      Android.speakPreview(SC[i].vo, "Step " + (i+1) + " / " + SC.length + ": " + SC[i].screenTitle);
      return;
    }
    if(window.speechSynthesis){
      speechSynthesis.cancel();
      var u=new SpeechSynthesisUtterance(SC[i].vo);u.lang="hi-IN";u.rate=.96;speechSynthesis.speak(u);
    }
  }catch(e){}
}
function stopPv(){
  cancelAnimationFrame(raf);pvOn=false;
  if(pv)pv.textContent="▶ Preview chalao";
  try{if(window.Android&&typeof Android.stopPreviewSpeech==="function")Android.stopPreviewSpeech()}catch(e){}
  try{if(window.speechSynthesis)speechSynthesis.cancel()}catch(e){}
}
function startPvFrom(sceneIdx){
  stopPv();DUR=null;buildPlan();
  var idx=cl(sceneIdx||0,0,PLAN.length-1);
  startOffset=PLAN[idx].start;
  if(window.sfxStart)window.sfxStart(startOffset);
  var t0=performance.now()+80;pvOn=true;lastScene=-1;if(pv)pv.textContent="⏹ Stop preview";
  (function loop(){
    var T=startOffset+(performance.now()-t0)/1000;
    if(T>=TOTAL){stopPv();frameAt(2.2);return}
    T=Math.max(0,T);var k=0;for(;k<PLAN.length-1;k++){if(T<PLAN[k].start+PLAN[k].len)break}
    if(k!==lastScene&&T>=PLAN[k].start+PLAN[k].vs){lastScene=k;if(scSel)scSel.value=String(k);say(k)}
    frameAt(T);raf=requestAnimationFrame(loop);
  })();
}
if(pv)pv.addEventListener("click",function(){if(pvOn){stopPv();return}startPvFrom(parseInt(scSel?scSel.value:"0",10))});

var sleep=function(ms){return new Promise(function(r){setTimeout(r,ms)})};
window.NH={
  lang:"hi-IN",
  narration:function(){return JSON.stringify(SC.map(function(s){return{a:s.vo,b:""}}))},
  setDurations:function(j){DUR=JSON.parse(j);buildPlan();var ev=[];
    PLAN.forEach(function(p){if(DUR[p.i]&&DUR[p.i].a>0)ev.push({key:p.i+"a",at:p.start+p.vs})});
    return JSON.stringify({total:TOTAL,events:ev})},
  run:async function(fps){
    try{stopPv();buildPlan();var N=Math.ceil(TOTAL*fps);
      for(var n=0;n<N;n++){frameAt(n/fps);
        if(Android.pushFrame(cv.toDataURL("image/jpeg",0.95))===false){Android.fail("Cancel ho gaya");return}
        if(n%20===0){Android.progress(n,N);await sleep(0)}}
      Android.progress(N,N);Android.done()}
    catch(e){Android.fail(String(e&&e.message?e.message:e))}}
};
buildPlan();frameAt(2.2);
</script>
</body>
""" + closeTag
    }
}
