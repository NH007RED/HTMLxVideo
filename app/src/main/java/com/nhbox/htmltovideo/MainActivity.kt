package com.nhbox.htmltovideo

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.text.TextUtils
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {

    // ---------- Black & White Monochrome Studio Palette ----------
    private val cBg0 = Color.parseColor("#000000")
    private val cBg1 = Color.parseColor("#080808")
    private val cCard = Color.parseColor("#111111")
    private val cCard2 = Color.parseColor("#1C1C1C")
    private val cStroke = Color.parseColor("#2E2E2E")
    private val cTxt = Color.parseColor("#FFFFFF")
    private val cSub = Color.parseColor("#9E9E9E")
    private val cWhite = Color.parseColor("#FFFFFF")
    private val cSilver = Color.parseColor("#E4E4E4")
    private val cOk = Color.parseColor("#FFFFFF")
    private val cErr = Color.parseColor("#FF6B6B")

    // ---------- Views ----------
    private lateinit var web: WebView
    private lateinit var playerBox: FrameLayout
    private lateinit var playerScroll: ScrollView
    private lateinit var settingsScroll: ScrollView
    private lateinit var tabPlayer: TextView
    private lateinit var tabSettings: TextView

    // AI Script / .MD Auto-Generator & Offline Model views (Settings page)
    private lateinit var chipModeOffline: TextView
    private lateinit var chipModeCloud: TextView
    private lateinit var boxOfflineAi: LinearLayout
    private lateinit var boxCloudAi: LinearLayout
    private var useOfflineAi = true
    private var offlineModelUriStr = ""
    private lateinit var etOfflineModelPath: EditText
    private lateinit var tvOfflineModelStatus: TextView
    private lateinit var btnPickOfflineModel: TextView
    private val dlModelButtons = ArrayList<TextView>()
    private val aiProvChips = ArrayList<TextView>()
    private var curAiProv = "gemini"
    private lateinit var etAiKey: EditText
    private lateinit var etAiModel: EditText
    private lateinit var etScriptPrompt: EditText
    private lateinit var btnSaveAiKey: TextView
    private lateinit var btnPickMdScript: TextView
    private lateinit var btnGenFromScript: TextView
    private lateinit var aiGenStatusTv: TextView
    private var aiKeyVisible = false
    @Volatile private var aiGenerating = false
    private var previewTts: TextToSpeech? = null
    @Volatile private var previewTtsReady = false

    // Project views (Settings page only - removed from Home/Player screen)
    private lateinit var settingsFileLabel: TextView
    private lateinit var btnPick: TextView
    private lateinit var btnSample: TextView
    private lateinit var btnTutorial: TextView
    private lateinit var btnMovie: TextView
    private lateinit var btnUiUx: TextView
    private lateinit var btnLast: TextView

    // Export Size & Export Processing views (Player / Home page)
    private lateinit var btnPreview: TextView
    private lateinit var btnExport: TextView
    private lateinit var btnCancel: TextView
    private lateinit var btnOpen: TextView
    private lateinit var btnShare: TextView
    private lateinit var btnTts: TextView
    private lateinit var resultRow: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var statsText: TextView
    private lateinit var bar: ProgressBar
    private val fpsChips = ArrayList<TextView>()
    private val aspectChips = ArrayList<TextView>()
    private val fitChips = ArrayList<TextView>()
    private lateinit var fitRow: LinearLayout
    private lateinit var exportTitle: TextView

    // Voice & API views (Settings page - Only Sarvam AI & Phone)
    private val engChips = ArrayList<TextView>()
    private lateinit var engInfo: TextView
    private lateinit var edgeFreeBox: LinearLayout
    private lateinit var etEdgeVoice: EditText
    private lateinit var btnFetchEdgeVoices: TextView
    private lateinit var btnTestEdgeVoice: TextView
    private lateinit var tvEdgeVoicesStatus: TextView
    private lateinit var edgeVoicesListCol: LinearLayout
    private val edgeVoiceItems = ArrayList<EdgeFreeTts.EdgeVoiceItem>()
    @Volatile private var fetchingEdgeVoices = false
    private lateinit var apiSectionBox: LinearLayout
    private lateinit var keyTitleTv: TextView
    private lateinit var spkTitleTv: TextView
    private lateinit var cartesiaBox: LinearLayout
    private lateinit var chipCarSonic3: TextView
    private lateinit var chipCarSonic2: TextView
    private lateinit var chipCarTurbo: TextView
    private var curCartesiaModel = CartesiaTts.MODEL_SONIC_3
    private lateinit var btnOpenCartesiaPlay: TextView
    private lateinit var btnFetchCartesiaVoices: TextView
    private lateinit var btnPlayActiveCartesiaVoice: TextView
    private lateinit var tvCartesiaVoicesStatus: TextView
    private lateinit var cartesiaVoicesListCol: LinearLayout
    private val cartesiaVoiceItems = ArrayList<CartesiaTts.CartesiaVoiceItem>()
    @Volatile private var fetchingCartesiaVoices = false
    private lateinit var elevenBox: LinearLayout
    private lateinit var chipElMultiV2: TextView
    private lateinit var chipElFlashV25: TextView
    private var curElevenModel = ElevenLabsTts.MODEL_MULTILINGUAL_V2
    private lateinit var btnFetchElevenVoices: TextView
    private lateinit var btnPlayActiveElevenVoice: TextView
    private lateinit var tvElevenVoicesStatus: TextView
    private lateinit var elevenVoicesListCol: LinearLayout
    private val elevenVoiceItems = ArrayList<ElevenLabsTts.VoiceItem>()
    @Volatile private var fetchingElevenVoices = false
    private var voiceSamplePlayer: MediaPlayer? = null
    @Volatile private var playingVoiceId: String = ""
    private lateinit var phoneTtsBox: LinearLayout
    private lateinit var etKey: EditText
    private lateinit var etVoice: EditText
    private lateinit var keyRow: LinearLayout
    private lateinit var btnSaveApi: TextView
    private lateinit var btnPhoneTtsSettings: TextView
    private lateinit var btnBackToPlayer: TextView

    // Volume & SFX views (Settings page)
    private val volBars = ArrayList<SeekBar>()
    private lateinit var btnDuck: TextView
    private lateinit var btnAuto: TextView
    private lateinit var btnVolReset: TextView

    // ---------- State (Edge Free Neural, Cartesia AI, ElevenLabs, Sarvam AI & Phone) ----------
    private val engines = listOf("edge", "cartesia", "elevenlabs", "sarvam", "phone")
    private val engLabels = listOf("\u26A1 Edge Free Neural", "\uD83D\uDE80 Cartesia AI", "\uD83C\uDF99 ElevenLabs API", "\uD83C\uDDEE\uD83C\uDDF3 Sarvam AI", "\uD83D\uDCF1 Phone Offline TTS")
    private var curEng = "cartesia"
    private var fps = 60
    private var portrait = false   // false = 16:9, true = 9:16
    private var fillMode = false   // sirf 9:16: false = Fit (blur background), true = Fill (crop)
    private var settingsMode = false
    private var projectReady = false
    private var keyVisible = false
    @Volatile private var exporting = false
    @Volatile private var cancel = false
    @Volatile private var exporter: VideoExporter? = null
    private var savedUri: Uri? = null
    private var startMs = 0L
    private var fileCb: ValueCallback<Array<Uri>>? = null
    private var pendingPerm: PermissionRequest? = null

    // ---------- Audio & Stem State ----------
    private var domMode = false
    private var exportScaled = false
    private val vols = intArrayOf(100, 100, 0, 100) // master, voice, music(0=off), sfx (percent)
    private var duck = true
    private var autoSfx = true
    private val audioInfo = HashMap<String, Double>()
    @Volatile private var audioNote = ""
    private class Stem(val data: ShortArray, val rate: Int) { var pos = 0 }
    private val stemBufs = HashMap<String, Stem>()
    @Volatile private var audioLatch: CountDownLatch? = null
    @Volatile private var audioErr: String? = null
    private var capBmp: Bitmap? = null
    private var blankFrames = 0
    private var contentSeen = false
    private val domAdapterJs: String by lazy { assets.open("h2v_dom.js").bufferedReader().use { it.readText() } }
    private val sfxEngineJs: String by lazy { assets.open("h2v_sfx.js").bufferedReader().use { it.readText() } }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dpf(v: Int) = v * resources.displayMetrics.density

    // ---------- Monochrome Drawable Helpers ----------
    private fun rounded(color: Int, r: Float, stroke: Int = 0, sw: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = r
            setColor(color)
            if (sw > 0) setStroke(sw, stroke)
        }

    private fun gradMono(c1: Int, c2: Int, r: Float, stroke: Int = 0, sw: Int = 0): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(c1, c2)).apply {
            cornerRadius = r
            if (sw > 0) setStroke(sw, stroke)
        }

    private fun ripple(bg: Drawable, darkRipple: Boolean = false): Drawable =
        RippleDrawable(
            ColorStateList.valueOf(Color.parseColor(if (darkRipple) "#26000000" else "#26FFFFFF")),
            bg,
            null
        )

    private fun enable(v: View, on: Boolean) {
        v.isEnabled = on
        v.alpha = if (on) 1f else 0.38f
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun button(text: String, primary: Boolean, onClick: () -> Unit): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 14.5f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (primary) Color.BLACK else cTxt)
            setPadding(dp(14), dp(13), dp(14), dp(13))
            background = if (primary) {
                ripple(gradMono(cWhite, cSilver, dpf(14)), true)
            } else {
                ripple(rounded(cCard2, dpf(14), cStroke, dp(1)), false)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { if (isEnabled) onClick() }
        }

    private fun card(title: String): LinearLayout {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(16))
            background = rounded(cCard, dpf(18), cStroke, dp(1))
        }
        c.addView(label(title, 11f, cSub, true).apply { letterSpacing = 0.14f })
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(12)
        c.layoutParams = lp
        return c
    }

    private fun rowOf(vararg views: View): LinearLayout {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((i, v) in views.withIndex()) {
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            if (i > 0) lp.leftMargin = dp(10)
            r.addView(v, lp)
        }
        val rlp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        rlp.topMargin = dp(10)
        r.layoutParams = rlp
        return r
    }

    private fun chip(text: String, onClick: () -> Unit): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 13f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(10), dp(9), dp(10), dp(9))
            isSingleLine = true
            isClickable = true
            isFocusable = true
            setOnClickListener { if (isEnabled) onClick() }
        }

    private fun styleChip(c: TextView, selected: Boolean) {
        c.setTextColor(if (selected) Color.BLACK else cSub)
        c.background = if (selected) {
            ripple(gradMono(cWhite, cSilver, dpf(12)), true)
        } else {
            ripple(rounded(cCard2, dpf(12), cStroke, dp(1)), false)
        }
    }

    private fun input(hint: String): EditText =
        EditText(this).apply {
            this.hint = hint
            setHintTextColor(Color.parseColor("#6E6E6E"))
            setTextColor(cTxt)
            textSize = 14f
            isSingleLine = true
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.parseColor("#080808"), dpf(12), cStroke, dp(1))
        }

    // ---------- Lifecycle ----------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = cBg0
        window.navigationBarColor = cBg0
        getSharedPreferences("h2v", MODE_PRIVATE).let {
            portrait = it.getBoolean("portrait", false)
            fillMode = it.getBoolean("fill", false)
            useOfflineAi = it.getBoolean("use_offline_ai", true)
            offlineModelUriStr = it.getString("offline_model_uri", "") ?: ""
            curAiProv = it.getString("ai_prov", "gemini") ?: "gemini"
            curCartesiaModel = it.getString("cartesia_model", CartesiaTts.MODEL_SONIC_3) ?: CartesiaTts.MODEL_SONIC_3
            curElevenModel = it.getString("eleven_model", ElevenLabsTts.MODEL_MULTILINGUAL_V2) ?: ElevenLabsTts.MODEL_MULTILINGUAL_V2
            val savedEng = it.getString("cur_eng", "edge") ?: "edge"
            curEng = if (savedEng in engines) savedEng else "edge"
            val d = intArrayOf(100, 100, 0, 100)
            for (i in 0 until 4) vols[i] = it.getInt("vol_$i", d[i]).coerceIn(0, 200)
            vols[2] = 0
            duck = it.getBoolean("duck", true)
            autoSfx = it.getBoolean("autosfx", true)
        }
        buildUi()
        edgeVoiceItems.clear()
        edgeVoiceItems.addAll(EdgeFreeTts.defaultHindiVoices)
        cartesiaVoiceItems.clear()
        cartesiaVoiceItems.addAll(CartesiaTts.defaultHindiVoices)
        elevenVoiceItems.clear()
        elevenVoiceItems.addAll(ElevenLabsTts.defaultHindiVoices)
        loadEngineFields(curEng)
        refreshEngineUi()
        refreshElevenModelChips()
        refreshCartesiaModelChips()
        rebuildCartesiaVoicesListUi()
        rebuildElevenVoicesListUi()
        rebuildEdgeVoicesListUi()
        loadAiFields(curAiProv)
        refreshAiProvUi()
        refreshOfflineModelUi()
        try {
            previewTts = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    previewTts?.language = Locale("hi", "IN")
                    previewTts?.setSpeechRate(0.96f)
                    previewTtsReady = true
                }
            }
        } catch (_: Exception) {}
        refreshFpsUi()
        refreshAspectUi()
        applyPlayerLayout()
        refreshLastButton()
        setStatus("Settings mein jaakar project select karein ya template kholein.", 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVoiceSamplePlayback()
        try { previewTts?.stop(); previewTts?.shutdown() } catch (_: Exception) {}
        exporter?.abort()
        try { web.destroy() } catch (_: Exception) {}
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (settingsMode) setPage(false) else super.onBackPressed()
    }

    // ---------- Build Monochrome UI (Player Page + Settings Page) ----------
    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(cBg0, cBg1))
            setPadding(dp(14), dp(12), dp(14), 0)
        }

        // Top App Header: [NVE]  NH VIDEO EDIT  (Single-line, clean layout)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }

        val brandBadge = TextView(this).apply {
            text = "NVE"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            background = rounded(cWhite, dpf(10))
            setPadding(dp(10), dp(8), dp(10), dp(8))
            isSingleLine = true
        }
        header.addView(brandBadge, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            rightMargin = dp(10)
        })

        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, dp(8), 0)
        }
        titles.addView(label("NH VIDEO EDIT", 16f, cTxt, true).apply {
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            letterSpacing = 0.04f
        })
        titles.addView(label("v4.4  \u2022  Studio Pro", 11f, cSub).apply {
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
        })
        header.addView(titles, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        // Top Navigation Segmented Bar: [ Player | Settings ]
        val seg = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(cCard, dpf(14), cStroke, dp(1))
            setPadding(dp(3), dp(3), dp(3), dp(3))
        }
        tabPlayer = chip("\u25B6 Player") { if (!exporting) setPage(false) }
        tabSettings = chip("\u2699 Settings") { if (!exporting) setPage(true) }
        seg.addView(tabPlayer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        seg.addView(tabSettings, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(4) })
        header.addView(seg)
        root.addView(header)

        // Video Player Box (Shown on Player/Home Page)
        playerBox = FrameLayout(this).apply {
            background = rounded(Color.BLACK, dpf(16), cStroke, dp(1))
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
        }
        web = WebView(this)
        setupWebView()
        playerBox.addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        playerBox.addOnLayoutChangeListener { _, l, tp, r, b, ol, ot, orr, ob ->
            if (exportScaled && (r - l != orr - ol || b - tp != ob - ot)) layoutWeb()
        }
        val pw = resources.displayMetrics.widthPixels - dp(28)
        val plp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, pw * 9 / 16)
        plp.topMargin = dp(4)
        root.addView(playerBox, plp)

        // ==================== PAGE 1: PLAYER / HOME PAGE (ONLY PLAYER + EXPORT SIZE + EXPORT PROCESSING) ====================
        val playerCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(24))
        }

        // 1. Export Size & Controls Card
        val cExport = card("EXPORT SIZE  \u2022  1920x1080 (16:9)")
        exportTitle = cExport.getChildAt(0) as TextView
        val aspectRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((i, nm) in listOf("16:9  Landscape", "9:16  Portrait").withIndex()) {
            val ch = chip(nm) {
                if (!exporting) {
                    portrait = (i == 1)
                    getSharedPreferences("h2v", MODE_PRIVATE).edit().putBoolean("portrait", portrait).apply()
                    refreshAspectUi()
                    applyPlayerLayout()
                }
            }
            aspectChips.add(ch)
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            if (i > 0) lp.leftMargin = dp(8)
            aspectRow.addView(ch, lp)
        }
        val arlp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        arlp.topMargin = dp(10)
        cExport.addView(aspectRow, arlp)

        fitRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((i, nm) in listOf("Fit (Blur BG)", "Fill (Crop)").withIndex()) {
            val ch = chip(nm) {
                if (!exporting) {
                    fillMode = (i == 1)
                    getSharedPreferences("h2v", MODE_PRIVATE).edit().putBoolean("fill", fillMode).apply()
                    refreshAspectUi()
                    applyPlayerLayout()
                }
            }
            fitChips.add(ch)
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            if (i > 0) lp.leftMargin = dp(8)
            fitRow.addView(ch, lp)
        }
        val frlp2 = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        frlp2.topMargin = dp(8)
        cExport.addView(fitRow, frlp2)

        val fpsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((i, f) in listOf(30, 60).withIndex()) {
            val ch = chip("$f FPS") {
                if (!exporting) {
                    fps = f
                    refreshFpsUi()
                    getSharedPreferences("h2v", MODE_PRIVATE).edit().putInt("fps", fps).apply()
                }
            }
            fpsChips.add(ch)
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            if (i > 0) lp.leftMargin = dp(8)
            fpsRow.addView(ch, lp)
        }
        val frlp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        frlp.topMargin = dp(10)
        cExport.addView(fpsRow, frlp)

        btnPreview = button("\u25B6  Preview", false) { togglePreview() }
        btnExport = button("\u2B07  Export MP4", true) { startExport() }
        cExport.addView(rowOf(btnPreview, btnExport))
        btnCancel = button("Cancel Export", false) { cancel = true }
        cExport.addView(rowOf(btnCancel))
        playerCol.addView(cExport)

        // 2. Export Processing & Status Card
        val cStatus = card("EXPORT PROCESSING  \u2022  STATUS")
        statusText = label("", 13.5f, cTxt)
        statusText.setPadding(0, dp(10), 0, dp(10))
        cStatus.addView(statusText)
        bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            val ld = LayerDrawable(arrayOf<Drawable>(
                rounded(cCard2, dpf(5)),
                ClipDrawable(rounded(cWhite, dpf(5)), Gravity.START, ClipDrawable.HORIZONTAL)
            ))
            ld.setId(0, android.R.id.background)
            ld.setId(1, android.R.id.progress)
            progressDrawable = ld
        }
        cStatus.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)))
        statsText = label("", 12f, cSub)
        statsText.setPadding(0, dp(8), 0, 0)
        cStatus.addView(statsText)
        btnOpen = button("\u25B6  Video kholo", true) { openVideo() }
        btnShare = button("\u2934  Share", false) { shareVideo() }
        resultRow = rowOf(btnOpen, btnShare)
        resultRow.visibility = View.GONE
        cStatus.addView(resultRow)
        btnTts = button("Phone TTS Settings kholo (Hindi voice)", false) { openTtsSettings() }
        val ttsRow = rowOf(btnTts)
        ttsRow.visibility = View.GONE
        btnTts.tag = ttsRow
        cStatus.addView(ttsRow)
        playerCol.addView(cStatus)

        playerScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(playerCol)
        }
        root.addView(playerScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // ==================== PAGE 2: SETTINGS PAGE (PROJECT UPLOAD + API + AUDIO) ====================
        val settingsCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(28))
        }

        // 0. AI Script / .MD File to Video Auto-Generator (Offline AI Model + Cloud API)
        val cAiGen = card("AI SCRIPT TO VIDEO  \u2022  OFFLINE & AUTO GENERATOR")
        val aiSub = label("Apni koi bhi .md / .txt script file upload karein ya prompt likhein — app aapki script ke har step, screen aur voiceover se custom multi-scene video banakar Player mein load kar dega.", 12f, cSub)
        aiSub.setPadding(0, dp(6), 0, dp(6))
        cAiGen.addView(aiSub)

        // Mode Toggle: Offline Model vs Cloud API (short labels so they never truncate)
        chipModeOffline = chip("\uD83D\uDCF1 Offline Model") {
            if (!exporting && !aiGenerating) {
                useOfflineAi = true
                getSharedPreferences("h2v", MODE_PRIVATE).edit().putBoolean("use_offline_ai", true).apply()
                refreshOfflineModelUi()
            }
        }
        chipModeCloud = chip("\u2601 Cloud API") {
            if (!exporting && !aiGenerating) {
                useOfflineAi = false
                getSharedPreferences("h2v", MODE_PRIVATE).edit().putBoolean("use_offline_ai", false).apply()
                refreshOfflineModelUi()
            }
        }
        cAiGen.addView(rowOf(chipModeOffline, chipModeCloud))

        // ---------- OFFLINE AI MODEL CONTAINER ----------
        boxOfflineAi = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(4))
        }

        val dlTitle = label("1. DOWNLOAD OFFLINE AI MODEL (HUGGING FACE DIRECT LINK)", 11f, cSub, true).apply {
            letterSpacing = 0.08f
            setPadding(0, dp(4), 0, dp(4))
        }
        boxOfflineAi.addView(dlTitle)
        val dlHint = label("Kisi bhi model par tap karein — browser mein direct download start ho jayega:", 11.5f, cSub)
        dlHint.setPadding(0, 0, 0, dp(6))
        boxOfflineAi.addView(dlHint)

        val dlRow1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val dlRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((idx, mSpec) in AiScriptBuilder.offlineModels.withIndex()) {
            val b = button(mSpec.title, false) {
                openDownloadUrl(mSpec.url, mSpec.fileName)
            }
            dlModelButtons.add(b)
            val targetRow = if (idx < 2) dlRow1 else dlRow2
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            if (idx % 2 == 1) lp.leftMargin = dp(8)
            targetRow.addView(b, lp)
        }
        boxOfflineAi.addView(dlRow1, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })
        boxOfflineAi.addView(dlRow2, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })

        val pathTitle = label("2. SELECT DOWNLOADED MODEL PATH (MOBILE STORAGE)", 11f, cSub, true).apply {
            letterSpacing = 0.08f
            setPadding(0, dp(14), 0, dp(4))
        }
        boxOfflineAi.addView(pathTitle)

        etOfflineModelPath = input("/storage/emulated/0/Download/qwen2.5-1.5b-instruct-q4_k_m.gguf")
        val savedPath = getSharedPreferences("h2v", MODE_PRIVATE).getString("offline_model_path", "") ?: ""
        etOfflineModelPath.setText(savedPath)
        boxOfflineAi.addView(etOfflineModelPath, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        btnPickOfflineModel = button("\uD83D\uDCC2  Select Offline Model Path (.gguf / .bin / .task)", true) {
            pickOfflineModelFile()
        }
        boxOfflineAi.addView(rowOf(btnPickOfflineModel))

        tvOfflineModelStatus = label("", 12f, cOk)
        tvOfflineModelStatus.setPadding(0, dp(8), 0, dp(4))
        boxOfflineAi.addView(tvOfflineModelStatus)

        cAiGen.addView(boxOfflineAi)

        // ---------- CLOUD AI API CONTAINER (OPTIONAL) ----------
        boxCloudAi = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(4))
            visibility = View.GONE
        }

        val aiChipRow1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val aiChipRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((i, spec) in AiScriptBuilder.providers.withIndex()) {
            val ch = chip(spec.label) {
                if (!exporting && !aiGenerating) {
                    saveAiFields(curAiProv)
                    curAiProv = spec.id
                    getSharedPreferences("h2v", MODE_PRIVATE).edit().putString("ai_prov", curAiProv).apply()
                    loadAiFields(curAiProv)
                    refreshAiProvUi()
                }
            }
            aiProvChips.add(ch)
            val row = if (i < 2) aiChipRow1 else aiChipRow2
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            if (i % 2 == 1) lp.leftMargin = dp(8)
            row.addView(ch, lp)
        }
        boxCloudAi.addView(aiChipRow1, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })
        boxCloudAi.addView(aiChipRow2, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })

        val aiKeyTitle = label("AI PROVIDER API KEY", 11f, cSub, true).apply {
            letterSpacing = 0.1f
            setPadding(0, dp(12), 0, dp(4))
        }
        boxCloudAi.addView(aiKeyTitle)
        etAiKey = input("Enter Gemini / Groq / OpenAI API Key")
        etAiKey.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val aiEye = label("SHOW", 11f, cTxt, true).apply {
            gravity = Gravity.CENTER
            background = rounded(cCard2, dpf(10), cStroke, dp(1))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            isClickable = true
            setOnClickListener {
                aiKeyVisible = !aiKeyVisible
                text = if (aiKeyVisible) "HIDE" else "SHOW"
                etAiKey.inputType = InputType.TYPE_CLASS_TEXT or
                    (if (aiKeyVisible) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_TEXT_VARIATION_PASSWORD)
                etAiKey.setSelection(etAiKey.text.length)
            }
        }
        val aiKeyRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        aiKeyRow.addView(etAiKey, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        aiKeyRow.addView(aiEye, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(8) })
        boxCloudAi.addView(aiKeyRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val aiModelTitle = label("AI MODEL NAME", 11f, cSub, true).apply {
            letterSpacing = 0.1f
            setPadding(0, dp(10), 0, dp(4))
        }
        boxCloudAi.addView(aiModelTitle)
        etAiModel = input("Model (e.g. gemini-2.5-flash, llama-3.3-70b-versatile)")
        boxCloudAi.addView(etAiModel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        btnSaveAiKey = button("\u2713  Save AI API Key", false) {
            saveAiFields(curAiProv)
            Toast.makeText(this, "AI API Key saved!", Toast.LENGTH_SHORT).show()
        }
        boxCloudAi.addView(rowOf(btnSaveAiKey))
        cAiGen.addView(boxCloudAi)

        // ---------- 3. SCRIPT PROMPT OR .MD FILE UPLOAD ----------
        val scriptTitle = label("3. SCRIPT PROMPT OR .MD FILE (AUTO GENERATE & LOAD)", 11f, cSub, true).apply {
            letterSpacing = 0.08f
            setPadding(0, dp(14), 0, dp(4))
        }
        cAiGen.addView(scriptTitle)
        etScriptPrompt = EditText(this).apply {
            hint = "Yahan apni video script, setup tutorial (.md), ya features paste karein... (Ya neeche button se .md / .txt file upload karein)"
            setHintTextColor(Color.parseColor("#6E6E6E"))
            setTextColor(cTxt)
            textSize = 13.5f
            minLines = 4
            maxLines = 8
            gravity = Gravity.TOP or Gravity.START
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.parseColor("#080808"), dpf(12), cStroke, dp(1))
        }
        cAiGen.addView(etScriptPrompt, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        btnPickMdScript = button("\uD83D\uDCC4  Upload .MD / Script (Auto Load)", true) { pickMdScriptFile() }
        btnGenFromScript = button("\u2728  Generate & Load Video", true) { startAiScriptGeneration(etScriptPrompt.text.toString()) }
        cAiGen.addView(rowOf(btnPickMdScript, btnGenFromScript))

        aiGenStatusTv = label("", 12.5f, cSub)
        aiGenStatusTv.setPadding(0, dp(8), 0, 0)
        aiGenStatusTv.visibility = View.GONE
        cAiGen.addView(aiGenStatusTv)
        settingsCol.addView(cAiGen)

        // 1. Project Source / Upload Section in Settings
        val cProject = card("PROJECT SOURCE  \u2022  STUDIO FILES")
        settingsFileLabel = label("Active Project: None", 13.5f, cTxt, true)
        settingsFileLabel.setPadding(0, dp(8), 0, dp(4))
        cProject.addView(settingsFileLabel)
        val projSub = label("Apni video project file phone storage se select karein ya neeche diye gaye studio templates kholein.", 12f, cSub)
        projSub.setPadding(0, 0, 0, dp(4))
        cProject.addView(projSub)
        btnPick = button("\uD83D\uDCC2  Select Project File", true) { pickProjectFile() }
        cProject.addView(rowOf(btnPick))
        btnLast = button("\u21BA  Pichla project dobara kholo", false) { loadLast() }
        cProject.addView(rowOf(btnLast))

        val demoHead = label("STUDIO TEMPLATES & SAMPLES", 11f, cSub, true).apply {
            letterSpacing = 0.12f
            setPadding(0, dp(14), 0, dp(2))
        }
        cProject.addView(demoHead)
        btnSample = button("Firebase Tutorial", false) { loadAsset("sample_firebase.html", "Sample: Firebase Tutorial") }
        btnTutorial = button("Build Tutorial", false) { loadAsset("build_tutorial_video.html", "Sample: Build Tutorial") }
        cProject.addView(rowOf(btnSample, btnTutorial))
        btnMovie = button("MovieBox Promo", false) { loadAsset("moviebox_promo.html", "Sample: MovieBox Promo") }
        btnUiUx = button("UI/UX Explorer", false) { loadAsset("uiux_showcase_video.html", "Sample: UI/UX Name Explorer") }
        cProject.addView(rowOf(btnMovie, btnUiUx))
        settingsCol.addView(cProject)

        // 2. Voice-Over & API Configuration Section in Settings (Only Sarvam AI & Phone)
        val cVoice = card("VOICE-OVER & API SETTINGS")
        val chipRow1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val chipRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val chipRow3 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((i, name) in engLabels.withIndex()) {
            val ch = chip(name) {
                if (!exporting) {
                    saveEngineFields(curEng)
                    stopVoiceSamplePlayback()
                    curEng = engines[i]
                    getSharedPreferences("h2v", MODE_PRIVATE).edit().putString("cur_eng", curEng).apply()
                    loadEngineFields(curEng)
                    refreshEngineUi()
                }
            }
            engChips.add(ch)
            val targetRow = when {
                i < 2 -> chipRow1
                i < 4 -> chipRow2
                else -> chipRow3
            }
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            if (i % 2 == 1) lp.leftMargin = dp(8)
            targetRow.addView(ch, lp)
        }
        cVoice.addView(chipRow1, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
        cVoice.addView(chipRow2, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
        cVoice.addView(chipRow3, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })

        engInfo = label("", 12f, cSub)
        engInfo.setPadding(0, dp(10), 0, 0)
        cVoice.addView(engInfo)

        // ---------- 1. Microsoft Edge Neural TTS Box (100% Free & Unlimited, No API Key) ----------
        edgeFreeBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(4))
        }
        val freeBadge = label("\u2713 100% FREE & UNLIMITED — Kisi API Key ya Signup ki zaroorat nahi hai!", 12f, cOk, true)
        freeBadge.setPadding(0, dp(4), 0, dp(6))
        edgeFreeBox.addView(freeBadge)

        val edgeSelTitle = label("SELECTED MICROSOFT EDGE HINDI VOICE", 11f, cSub, true).apply {
            letterSpacing = 0.08f
            setPadding(0, dp(6), 0, dp(4))
        }
        edgeFreeBox.addView(edgeSelTitle)
        etEdgeVoice = input("Swara (Hindi) | hi-IN-SwaraNeural")
        edgeFreeBox.addView(etEdgeVoice, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        btnFetchEdgeVoices = button("\uD83D\uDD04  Refresh Free Hindi Voices", false) {
            fetchEdgeFreeVoicesNow()
        }
        btnTestEdgeVoice = button("\uD83D\uDD0A  Play Selected Voice", true) {
            val curSpec = etEdgeVoice.text.toString().trim().ifEmpty { defaultVoice("edge") }
            val shortId = EdgeFreeTts.resolveShortName(curSpec)
            val dispName = curSpec.substringBefore("|").trim().ifEmpty { shortId }
            val tempItem = EdgeFreeTts.EdgeVoiceItem(shortId, dispName, "Hindi", "hi-IN", "100% Free Neural Voice", "Testing Hindi speech")
            playOrStopEdgeVoiceSample(tempItem)
        }
        edgeFreeBox.addView(rowOf(btnFetchEdgeVoices, btnTestEdgeVoice))

        val edgeListHdr = label("ALL FREE HINDI & HINGLISH NEURAL VOICES (PLAY & SET)", 11f, cSub, true).apply {
            letterSpacing = 0.08f
            setPadding(0, dp(14), 0, dp(4))
        }
        edgeFreeBox.addView(edgeListHdr)

        tvEdgeVoicesStatus = label("14 Free Microsoft Neural Hindi & Hinglish voices ready! Kisi bhi voice ko Play (\u25B6) karke sunein ya Set (\u2713) dabayein.", 11.5f, cSub)
        tvEdgeVoicesStatus.setPadding(0, 0, 0, dp(8))
        edgeFreeBox.addView(tvEdgeVoicesStatus)

        edgeVoicesListCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        edgeFreeBox.addView(edgeVoicesListCol)
        cVoice.addView(edgeFreeBox)

        // Cloud Voice API Key & Speaker Box (ElevenLabs.io & Sarvam AI)
        apiSectionBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        keyTitleTv = label("ELEVENLABS.IO API KEY (XI-API-KEY)", 11f, cSub, true).apply {
            letterSpacing = 0.1f
            setPadding(0, dp(12), 0, dp(4))
        }
        apiSectionBox.addView(keyTitleTv)
        etKey = input("Enter ElevenLabs API key (elevenlabs.io)")
        etKey.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val eye = label("SHOW", 11f, cTxt, true).apply {
            gravity = Gravity.CENTER
            background = rounded(cCard2, dpf(10), cStroke, dp(1))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            isClickable = true
            setOnClickListener {
                keyVisible = !keyVisible
                text = if (keyVisible) "HIDE" else "SHOW"
                etKey.inputType = InputType.TYPE_CLASS_TEXT or
                    (if (keyVisible) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_TEXT_VARIATION_PASSWORD)
                etKey.setSelection(etKey.text.length)
            }
        }
        keyRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        keyRow.addView(etKey, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        keyRow.addView(eye, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(8) })
        apiSectionBox.addView(keyRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        spkTitleTv = label("SELECTED HINDI VOICE (NAME | VOICE_ID)", 11f, cSub, true).apply {
            letterSpacing = 0.1f
            setPadding(0, dp(12), 0, dp(4))
        }
        apiSectionBox.addView(spkTitleTv)
        etVoice = input("Aria | 9BWtsMINqrJLrRacOk9x")
        apiSectionBox.addView(etVoice, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        btnSaveApi = button("\u2713  Save API Key & Voice", true) {
            saveEngineFields(curEng)
            val labelName = when (curEng) {
                "cartesia" -> "Cartesia.ai"
                "elevenlabs" -> "ElevenLabs"
                else -> "Sarvam AI"
            }
            Toast.makeText(this, "$labelName API Key & Voice saved!", Toast.LENGTH_SHORT).show()
            if (curEng == "cartesia") rebuildCartesiaVoicesListUi()
            if (curEng == "elevenlabs") rebuildElevenVoicesListUi()
        }
        apiSectionBox.addView(rowOf(btnSaveApi))

        // ---------- Cartesia.ai Dedicated Hindi Voices Explorer (Fetch / Play / Set) ----------
        cartesiaBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(4))
        }

        btnOpenCartesiaPlay = button("\uD83D\uDD11  Open play.cartesia.ai (Get API Key / Voices)", false) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.cartesia.ai/text-to-speech")))
            } catch (e: Exception) {
                Toast.makeText(this, "Browser khul nahi paya: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        cartesiaBox.addView(rowOf(btnOpenCartesiaPlay))

        val carModelTitle = label("CARTESIA SONIC HINDI MODEL", 11f, cSub, true).apply {
            letterSpacing = 0.08f
            setPadding(0, dp(10), 0, dp(4))
        }
        cartesiaBox.addView(carModelTitle)

        chipCarSonic3 = chip("Sonic-3 (Best Hindi)") {
            curCartesiaModel = CartesiaTts.MODEL_SONIC_3
            getSharedPreferences("h2v", MODE_PRIVATE).edit().putString("cartesia_model", curCartesiaModel).apply()
            refreshCartesiaModelChips()
        }
        chipCarSonic2 = chip("Sonic-2 (Stable)") {
            curCartesiaModel = CartesiaTts.MODEL_SONIC_2
            getSharedPreferences("h2v", MODE_PRIVATE).edit().putString("cartesia_model", curCartesiaModel).apply()
            refreshCartesiaModelChips()
        }
        chipCarTurbo = chip("Sonic Turbo") {
            curCartesiaModel = CartesiaTts.MODEL_SONIC_TURBO
            getSharedPreferences("h2v", MODE_PRIVATE).edit().putString("cartesia_model", curCartesiaModel).apply()
            refreshCartesiaModelChips()
        }
        cartesiaBox.addView(rowOf(chipCarSonic3, chipCarSonic2, chipCarTurbo))

        btnFetchCartesiaVoices = button("\uD83D\uDD04  Fetch All Hindi Voices", true) {
            fetchCartesiaVoicesNow()
        }
        btnPlayActiveCartesiaVoice = button("\uD83D\uDD0A  Test Selected Voice", false) {
            val activeSpec = etVoice.text.toString().trim().ifEmpty { defaultVoice("cartesia") }
            val activeId = CartesiaTts.resolveVoiceId(activeSpec)
            val activeName = activeSpec.substringBefore("|").trim().ifEmpty { "Selected Voice" }
            val tempItem = CartesiaTts.CartesiaVoiceItem(activeId, activeName, "Hindi", "Active Cartesia Voice", "Testing Hindi narration")
            playOrStopCartesiaVoiceSample(tempItem)
        }
        cartesiaBox.addView(rowOf(btnFetchCartesiaVoices, btnPlayActiveCartesiaVoice))

        val carListHdr = label("CARTESIA HINDI & MULTILINGUAL VOICES (PLAY & SET)", 11f, cSub, true).apply {
            letterSpacing = 0.08f
            setPadding(0, dp(14), 0, dp(4))
        }
        cartesiaBox.addView(carListHdr)

        tvCartesiaVoicesStatus = label("14 Cartesia Hindi & Sonic-3.6 voices ready! Apni Cartesia API key daal kar 'Fetch All Hindi Voices' dabayein ya neeche Play (\u25B6) & Set (\u2713) karein.", 11.5f, cSub)
        tvCartesiaVoicesStatus.setPadding(0, 0, 0, dp(8))
        cartesiaBox.addView(tvCartesiaVoicesStatus)

        cartesiaVoicesListCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        cartesiaBox.addView(cartesiaVoicesListCol)
        apiSectionBox.addView(cartesiaBox)

        // ---------- ElevenLabs Dedicated Hindi Voices Explorer (Fetch / Play / Set) ----------
        elevenBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(4))
        }

        val elModelTitle = label("ELEVENLABS HINDI MODEL", 11f, cSub, true).apply {
            letterSpacing = 0.08f
            setPadding(0, dp(4), 0, dp(4))
        }
        elevenBox.addView(elModelTitle)

        chipElMultiV2 = chip("Multilingual v2 (Best)") {
            curElevenModel = ElevenLabsTts.MODEL_MULTILINGUAL_V2
            getSharedPreferences("h2v", MODE_PRIVATE).edit().putString("eleven_model", curElevenModel).apply()
            refreshElevenModelChips()
        }
        chipElFlashV25 = chip("Flash v2.5 (Fast)") {
            curElevenModel = ElevenLabsTts.MODEL_FLASH_V2_5
            getSharedPreferences("h2v", MODE_PRIVATE).edit().putString("eleven_model", curElevenModel).apply()
            refreshElevenModelChips()
        }
        elevenBox.addView(rowOf(chipElMultiV2, chipElFlashV25))

        btnFetchElevenVoices = button("\uD83D\uDD04  Fetch All Hindi Voices", true) {
            fetchElevenLabsVoicesNow()
        }
        btnPlayActiveElevenVoice = button("\uD83D\uDD0A  Test Selected Voice", false) {
            val activeSpec = etVoice.text.toString().trim().ifEmpty { defaultVoice("elevenlabs") }
            val activeId = ElevenLabsTts.resolveVoiceId(activeSpec)
            val activeName = activeSpec.substringBefore("|").trim().ifEmpty { "Selected Voice" }
            val tempItem = ElevenLabsTts.VoiceItem(activeId, activeName, "Hindi", "Active Voice", "Testing Hindi narration")
            playOrStopElevenVoiceSample(tempItem)
        }
        elevenBox.addView(rowOf(btnFetchElevenVoices, btnPlayActiveElevenVoice))

        val listHdr = label("ALL HINDI & MULTILINGUAL VOICES (PLAY & SET)", 11f, cSub, true).apply {
            letterSpacing = 0.08f
            setPadding(0, dp(14), 0, dp(4))
        }
        elevenBox.addView(listHdr)

        tvElevenVoicesStatus = label("14 Hindi-ready voices loaded. Apni ElevenLabs API key daal kar 'Fetch All Hindi Voices' dabayein apne account ki sabhi voices laane ke liye.", 11.5f, cSub)
        tvElevenVoicesStatus.setPadding(0, 0, 0, dp(8))
        elevenBox.addView(tvElevenVoicesStatus)

        elevenVoicesListCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        elevenBox.addView(elevenVoicesListCol)

        apiSectionBox.addView(elevenBox)
        cVoice.addView(apiSectionBox)

        // Phone Offline TTS Box
        phoneTtsBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        btnPhoneTtsSettings = button("\u2699  Open Android TTS Voice Settings", false) { openTtsSettings() }
        phoneTtsBox.addView(rowOf(btnPhoneTtsSettings))
        cVoice.addView(phoneTtsBox)
        settingsCol.addView(cVoice)

        // 3. Audio & Sound Effects Mixer Card in Settings
        val cVol = card("AUDIO & SOUND EFFECTS  \u2022  NO MUSIC")
        val volNames = listOf("Master Volume", "Voice-Over Volume", "Music (Disabled)", "Sound Effects (SFX)")
        for (i in 0 until 4) {
            val nameTv = label(volNames[i], 13f, cTxt, true)
            val pctTv = label("${vols[i]}%", 13f, cSub, true)
            val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            head.addView(nameTv, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            head.addView(pctTv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            val hlp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            hlp.topMargin = dp(12)
            cVol.addView(head, hlp)
            val sb = SeekBar(this).apply {
                max = 200
                progress = vols[i]
                progressTintList = ColorStateList.valueOf(cWhite)
                thumbTintList = ColorStateList.valueOf(cWhite)
                progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#2E2E2E"))
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                        vols[i] = if (i == 2) 0 else p
                        pctTv.text = "${vols[i]}%"
                        if (fromUser) pushVolumes()
                    }
                    override fun onStartTrackingTouch(sb: SeekBar?) {}
                    override fun onStopTrackingTouch(sb: SeekBar?) { saveVolumes() }
                })
            }
            if (i == 2) {
                head.visibility = View.GONE
                sb.visibility = View.GONE
            }
            volBars.add(sb)
            cVol.addView(sb, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        btnDuck = button("", false) { duck = !duck; refreshDuckUi(); saveVolumes() }
        btnVolReset = button("\u21BA  Reset Volume", false) { resetVolumes() }
        refreshDuckUi()
        cVol.addView(rowOf(btnDuck, btnVolReset))
        btnAuto = button("", false) { autoSfx = !autoSfx; refreshAutoUi(); saveVolumes() }
        refreshAutoUi()
        cVol.addView(rowOf(btnAuto))
        val volInfo = label("Background music poori tarah band hai. Typing, popup, slide, tap, click, chime, aur screen in-out whoosh sound effects har video project mein apne-aap voiceover ke saath sync hote hain.", 12f, cSub)
        volInfo.setPadding(0, dp(10), 0, 0)
        cVol.addView(volInfo)
        settingsCol.addView(cVol)

        btnBackToPlayer = button("\u2190  Back to Video Player", true) { setPage(false) }
        val backLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        backLp.topMargin = dp(14)
        settingsCol.addView(btnBackToPlayer, backLp)

        settingsScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            visibility = View.GONE
            addView(settingsCol)
        }
        root.addView(settingsScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        enable(btnPreview, false)
        enable(btnExport, false)
        enable(btnCancel, false)
        styleTabs()
    }

    private fun styleTabs() {
        styleChip(tabPlayer, !settingsMode)
        styleChip(tabSettings, settingsMode)
    }

    private fun refreshEngineUi() {
        for ((i, ch) in engChips.withIndex()) styleChip(ch, engines[i] == curEng)
        val isEdge = (curEng == "edge")
        val isCartesia = (curEng == "cartesia")
        val isEleven = (curEng == "elevenlabs")
        val isSarvam = (curEng == "sarvam")
        val isApiCloud = isCartesia || isEleven || isSarvam

        engInfo.text = when (curEng) {
            "edge" -> "Microsoft Edge Neural TTS (100% Free): Bina kisi API key ke Swara, Madhur, Aarav, Ananya, Kavya, Rehaan, Neerja aur Prabhat jaisi studio Hindi & Hinglish voices use karein!"
            "cartesia" -> "Cartesia.ai (Sonic-3.6 Hindi & Hinglish): Ultra-fast sub-90ms native Hindi & Hinglish voices (play.cartesia.ai/text-to-speech). Free 20,000 monthly credits!"
            "elevenlabs" -> "ElevenLabs.io: Ultra-realistic Hindi & Hinglish AI voices. Apni API key daal kar Hindi voices Fetch, Play aur Set karein."
            "sarvam" -> "Sarvam AI (Bulbul v3): Natural Indian Hindi & Hinglish studio voice. Internet aur Sarvam API key chahiye."
            else -> "Phone TTS: Offline Android voice engine. Bina internet aur bina API key ke kaam karta hai."
        }
        if (::edgeFreeBox.isInitialized) edgeFreeBox.visibility = if (isEdge) View.VISIBLE else View.GONE
        if (::keyTitleTv.isInitialized) {
            keyTitleTv.text = when (curEng) {
                "cartesia" -> "CARTESIA.AI API KEY (PLAY.CARTESIA.AI)"
                "elevenlabs" -> "ELEVENLABS.IO API KEY (XI-API-KEY)"
                else -> "SARVAM AI API KEY"
            }
        }
        if (::etKey.isInitialized) {
            etKey.hint = when (curEng) {
                "cartesia" -> "Enter Cartesia API Key (sk_car_... from play.cartesia.ai)"
                "elevenlabs" -> "Enter ElevenLabs API key (elevenlabs.io)"
                else -> "Enter Sarvam API key (dashboard.sarvam.ai)"
            }
        }
        if (::spkTitleTv.isInitialized) {
            spkTitleTv.text = when (curEng) {
                "cartesia" -> "SELECTED CARTESIA HINDI VOICE (NAME | VOICE_ID)"
                "elevenlabs" -> "SELECTED ELEVENLABS HINDI VOICE (NAME | ID)"
                else -> "SARVAM SPEAKER VOICE"
            }
        }
        if (::etVoice.isInitialized) {
            etVoice.hint = when (curEng) {
                "cartesia" -> "Hindi Narrator Woman | c1abd502-9231-4558-a054-10ac950c356d"
                "elevenlabs" -> "Aria | 9BWtsMINqrJLrRacOk9x"
                else -> "Speaker name (ritu, shubh, priya, kavya, rahul...)"
            }
        }
        if (::apiSectionBox.isInitialized) apiSectionBox.visibility = if (isApiCloud) View.VISIBLE else View.GONE
        if (::cartesiaBox.isInitialized) cartesiaBox.visibility = if (isCartesia) View.VISIBLE else View.GONE
        if (::elevenBox.isInitialized) elevenBox.visibility = if (isEleven) View.VISIBLE else View.GONE
        if (::phoneTtsBox.isInitialized) phoneTtsBox.visibility = if (curEng == "phone") View.VISIBLE else View.GONE
    }

    private fun refreshFpsUi() {
        for ((i, ch) in fpsChips.withIndex()) styleChip(ch, listOf(30, 60)[i] == fps)
    }

    private fun refreshAspectUi() {
        for ((i, ch) in aspectChips.withIndex()) styleChip(ch, (i == 1) == portrait)
        for ((i, ch) in fitChips.withIndex()) styleChip(ch, (i == 1) == fillMode)
        fitRow.visibility = if (portrait) View.VISIBLE else View.GONE
        exportTitle.text = if (portrait) "EXPORT SIZE  \u2022  1080x1920 (9:16)" else "EXPORT SIZE  \u2022  1920x1080 (16:9)"
    }

    private fun setStatus(msg: String, kind: Int) {
        statusText.text = msg
        statusText.setTextColor(if (kind == 2) cErr else if (kind == 1) cOk else cTxt)
    }

    private fun ui(msg: String) = runOnUiThread { setStatus(msg, 0) }

    // ---------- Renderer Setup ----------
    private fun setupWebView() {
        web.setBackgroundColor(Color.BLACK)
        val s = web.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.mediaPlaybackRequiresUserGesture = false
        s.allowFileAccess = false
        web.addJavascriptInterface(Bridge(), "Android")
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: WebChromeClient.FileChooserParams?
            ): Boolean {
                fileCb?.onReceiveValue(null)
                fileCb = filePathCallback
                val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    if (fileChooserParams?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                }
                return try {
                    startActivityForResult(i, 12)
                    true
                } catch (e: Exception) {
                    fileCb?.onReceiveValue(null)
                    fileCb = null
                    false
                }
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                val r = request ?: return
                runOnUiThread {
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        r.grant(r.resources)
                    } else {
                        pendingPerm = r
                        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 13)
                    }
                }
            }
        }
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                view.evaluateJavascript(H2V_JS) { _ ->
                    view.evaluateJavascript(domAdapterJs) { dr ->
                        val d = (dr ?: "").trim('"')
                        domMode = (d == "dom" || d == "dom-exists")
                        view.evaluateJavascript(sfxEngineJs) { _ ->
                            layoutWeb()
                            view.evaluateJavascript("(typeof NH!=='undefined'&&typeof NH.run==='function')") { r ->
                                projectReady = (r == "true")
                                refreshButtons()
                                if (projectReady) {
                                    if (domMode) {
                                        setStatus("Scene project taiyar hai. Voice-over + All SFX (No Music) export mein judenge.", 1)
                                    } else {
                                        setStatus("Project taiyar hai (Voice + All SFX • No Music). Preview dekho ya Export MP4 dabao.", 1)
                                    }
                                    pushVolumes()
                                } else if (d.startsWith("err")) {
                                    setStatus("Project adapter error: $d", 2)
                                } else {
                                    setStatus("Ye valid video project file nahi hai. Sahi studio project file chunein.", 2)
                                }
                                applyMode()
                                web.postDelayed({ applyMode() }, 750)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 13) {
            val r = pendingPerm
            pendingPerm = null
            if (r != null) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) r.grant(r.resources) else r.deny()
            }
        }
    }

    private fun refreshButtons() {
        enable(btnExport, projectReady && !exporting)
        enable(btnPreview, projectReady && !exporting)
        enable(btnCancel, exporting)
        val idle = !exporting && !aiGenerating
        if (::chipModeOffline.isInitialized) enable(chipModeOffline, idle)
        if (::chipModeCloud.isInitialized) enable(chipModeCloud, idle)
        if (::etOfflineModelPath.isInitialized) enable(etOfflineModelPath, idle)
        if (::btnPickOfflineModel.isInitialized) enable(btnPickOfflineModel, idle)
        for (b in dlModelButtons) enable(b, idle)
        for (c in aiProvChips) enable(c, idle)
        if (::etAiKey.isInitialized) enable(etAiKey, idle)
        if (::etAiModel.isInitialized) enable(etAiModel, idle)
        if (::etScriptPrompt.isInitialized) enable(etScriptPrompt, idle)
        if (::btnSaveAiKey.isInitialized) enable(btnSaveAiKey, idle)
        if (::btnPickMdScript.isInitialized) enable(btnPickMdScript, idle)
        if (::btnGenFromScript.isInitialized) enable(btnGenFromScript, idle)
        enable(btnPick, idle)
        enable(btnSample, !exporting)
        enable(btnLast, !exporting)
        enable(btnTutorial, !exporting)
        enable(btnMovie, !exporting)
        enable(btnUiUx, !exporting)
        enable(btnSaveApi, !exporting)
        if (::etEdgeVoice.isInitialized) enable(etEdgeVoice, !exporting)
        if (::btnFetchEdgeVoices.isInitialized) enable(btnFetchEdgeVoices, !exporting && !fetchingEdgeVoices)
        if (::btnTestEdgeVoice.isInitialized) enable(btnTestEdgeVoice, !exporting)
        if (::btnOpenCartesiaPlay.isInitialized) enable(btnOpenCartesiaPlay, !exporting)
        if (::btnFetchCartesiaVoices.isInitialized) enable(btnFetchCartesiaVoices, !exporting && !fetchingCartesiaVoices)
        if (::btnPlayActiveCartesiaVoice.isInitialized) enable(btnPlayActiveCartesiaVoice, !exporting)
        if (::chipCarSonic3.isInitialized) enable(chipCarSonic3, !exporting)
        if (::chipCarSonic2.isInitialized) enable(chipCarSonic2, !exporting)
        if (::chipCarTurbo.isInitialized) enable(chipCarTurbo, !exporting)
        if (::btnFetchElevenVoices.isInitialized) enable(btnFetchElevenVoices, !exporting && !fetchingElevenVoices)
        if (::btnPlayActiveElevenVoice.isInitialized) enable(btnPlayActiveElevenVoice, !exporting)
        if (::chipElMultiV2.isInitialized) enable(chipElMultiV2, !exporting)
        if (::chipElFlashV25.isInitialized) enable(chipElFlashV25, !exporting)
        enable(btnPhoneTtsSettings, !exporting)
        enable(btnBackToPlayer, !exporting)
        for (b in volBars) enable(b, !exporting)
        enable(btnDuck, !exporting)
        enable(btnAuto, !exporting)
        enable(btnVolReset, !exporting)
        for (c in engChips) enable(c, !exporting)
        for (c in fpsChips) enable(c, !exporting)
        for (c in aspectChips) enable(c, !exporting)
        for (c in fitChips) enable(c, !exporting)
        enable(etKey, !exporting)
        enable(etVoice, !exporting)
        enable(tabPlayer, !exporting)
        enable(tabSettings, !exporting)
    }

    // ---------- Player Page vs Settings Page Layout ----------
    private fun setPage(settings: Boolean) {
        if (settingsMode && !settings) {
            saveEngineFields(curEng)
            stopVoiceSamplePlayback()
        }
        settingsMode = settings
        if (settings) {
            playerBox.visibility = View.GONE
            playerScroll.visibility = View.GONE
            settingsScroll.visibility = View.VISIBLE
        } else {
            playerBox.visibility = View.VISIBLE
            playerScroll.visibility = View.VISIBLE
            settingsScroll.visibility = View.GONE
            val lp = playerBox.layoutParams as LinearLayout.LayoutParams
            val fullW = resources.displayMetrics.widthPixels - dp(28)
            if (portrait) {
                val maxH = resources.displayMetrics.heightPixels * 48 / 100
                var h = fullW * 16 / 9
                if (h > maxH) h = maxH
                lp.width = h * 9 / 16
                lp.height = h
                lp.gravity = Gravity.CENTER_HORIZONTAL
            } else {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                lp.gravity = Gravity.NO_GRAVITY
                lp.height = fullW * 9 / 16
            }
            lp.weight = 0f
            playerBox.layoutParams = lp
        }
        styleTabs()
        applyMode()
    }

    private fun applyPlayerLayout() {
        setPage(settingsMode)
    }

    private fun applyMode() {
        val js = if (domMode) "(window.__h2v?__h2v.off():'nohelper')"
        else "(window.__h2v?__h2v.on($portrait,$fillMode):'nohelper')"
        web.evaluateJavascript(js, null)
    }

    private fun togglePreview() {
        if (!projectReady || exporting) return
        if (settingsMode) setPage(false)
        pushVolumes()
        if (domMode) {
            web.evaluateJavascript("(window.NH&&NH.preview?NH.preview():'x')") { r ->
                setStatus(if (r == "\"playing\"") "Preview chal raha hai (SFX only, No Music). Dobara dabao to ruk jayega." else "Preview ruk gaya.", 0)
            }
            return
        }
        web.evaluateJavascript("(function(){var b=document.getElementById('pv');if(b){b.click();return b.textContent||''}return '0'})()") { r ->
            val clean = (r ?: "").trim('"')
            if (clean == "0") {
                setStatus("Is project mein preview button nahi mila.", 2)
            } else if (clean.contains("Stop", ignoreCase = true)) {
                setStatus("Preview chal raha hai (Voice + All SFX • No Music). Rukne ke liye dobara Preview dabayein.", 0)
            } else {
                try { previewTts?.stop() } catch (_: Exception) {}
                setStatus("Preview ruk gaya.", 0)
            }
        }
    }

    // ---------- Project Load (Strips any .html extension from UI display) ----------
    private fun pickProjectFile() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(i, 11)
    }

    private fun cleanProjectName(raw: String): String {
        return raw.replace(Regex("\\.[hH][tT][mM][lL]?$"), "")
            .replace(Regex("[hH][tT][mM][lL]2[vV][iI][dD][eE][oO]"), "NVE")
            .replace(Regex("[hH][tT][mM][lL]"), "Project")
            .trim()
            .ifEmpty { "Studio Project" }
    }

    private fun nameOf(u: Uri): String {
        var n = "Studio Project"
        try {
            contentResolver.query(u, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) n = c.getString(idx) ?: n
                }
            }
        } catch (_: Exception) {}
        return cleanProjectName(n)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 11 && resultCode == RESULT_OK) {
            val u = data?.data ?: return
            try {
                val content = contentResolver.openInputStream(u)!!.use { it.readBytes() }.toString(Charsets.UTF_8)
                loadProject(content, nameOf(u))
            } catch (e: Exception) {
                setStatus("Project file padh nahi paya: ${e.message}", 2)
            }
        } else if (requestCode == 16 && resultCode == RESULT_OK) {
            val u = data?.data ?: return
            try {
                val takeFlags = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
                try {
                    contentResolver.takePersistableUriPermission(u, takeFlags)
                } catch (_: Exception) {}
                offlineModelUriStr = u.toString()
                val fName = nameOf(u)
                val dispPath = "/storage/emulated/0/Download/$fName"
                etOfflineModelPath.setText(dispPath)
                getSharedPreferences("h2v", MODE_PRIVATE).edit()
                    .putString("offline_model_uri", offlineModelUriStr)
                    .putString("offline_model_path", dispPath)
                    .apply()
                refreshOfflineModelUi()
                Toast.makeText(this, "Offline AI Model selected: $fName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Model path select nahi ho paya: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == 15 && resultCode == RESULT_OK) {
            val u = data?.data ?: return
            try {
                val scriptText = contentResolver.openInputStream(u)!!.use { it.readBytes() }.toString(Charsets.UTF_8)
                etScriptPrompt.setText(scriptText)
                startAiScriptGeneration(scriptText, nameOf(u))
            } catch (e: Exception) {
                Toast.makeText(this, "Script file padh nahi paya: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == 12) {
            val res = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            fileCb?.onReceiveValue(res)
            fileCb = null
        }
    }

    private fun loadAsset(name: String, title: String) {
        try {
            val content = assets.open(name).bufferedReader().use { it.readText() }
            loadProject(content, cleanProjectName(title))
        } catch (e: Exception) {
            setStatus("Template nahi khula: ${e.message}", 2)
        }
    }

    private fun lastFile() = File(filesDir, "last_project.dat")
    private fun lastNameFile() = File(filesDir, "last_name.txt")

    private fun refreshLastButton() {
        val has = lastFile().exists()
        (btnLast.parent as View).visibility = if (has) View.VISIBLE else View.GONE
    }

    private fun loadLast() {
        try {
            val content = lastFile().readText(Charsets.UTF_8)
            val n = if (lastNameFile().exists()) cleanProjectName(lastNameFile().readText()) else "Pichla project"
            loadProject(content, n, false)
        } catch (e: Exception) {
            setStatus("Pichla project nahi khula: ${e.message}", 2)
        }
    }

    private fun loadProject(content: String, name: String, remember: Boolean = true) {
        if (exporting) return
        if (settingsMode) setPage(false)
        projectReady = false
        domMode = false
        exportScaled = false
        layoutWeb()
        refreshButtons()
        resultRow.visibility = View.GONE
        val safeTitle = cleanProjectName(name)
        settingsFileLabel.text = "Active Project: $safeTitle"
        setStatus("Project load ho raha hai...", 0)
        bar.progress = 0
        statsText.text = ""
        if (remember) {
            Thread {
                try {
                    lastFile().writeText(content, Charsets.UTF_8)
                    lastNameFile().writeText(safeTitle, Charsets.UTF_8)
                    runOnUiThread { refreshLastButton() }
                } catch (_: Exception) {}
            }.start()
        }
        val mimeType = "text/" + "ht" + "ml"
        web.loadDataWithBaseURL("https://nve.studio.local/", content, mimeType, "UTF-8", null)
    }

    // ---------- Offline AI Model & Script Auto-Generator ----------
    private fun openDownloadUrl(url: String, fileName: String) {
        try {
            Toast.makeText(this, "Downloading $fileName in Browser... Download ke baad 'Select Offline Model Path' se select karein.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, "Browser khul nahi paya: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun pickOfflineModelFile() {
        if (exporting || aiGenerating) return
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(i, 16)
    }

    private fun refreshOfflineModelUi() {
        if (!::chipModeOffline.isInitialized) return
        styleChip(chipModeOffline, useOfflineAi)
        styleChip(chipModeCloud, !useOfflineAi)
        boxOfflineAi.visibility = if (useOfflineAi) View.VISIBLE else View.GONE
        boxCloudAi.visibility = if (useOfflineAi) View.GONE else View.VISIBLE

        val manualPath = etOfflineModelPath.text.toString().trim()
        val info = AiScriptBuilder.inspectOfflineModel(this, offlineModelUriStr, manualPath)
        tvOfflineModelStatus.text = info.statusLine
        tvOfflineModelStatus.setTextColor(if (info.valid) cOk else cSub)
    }

    private fun saveAiFields(prov: String) {
        if (!::etAiKey.isInitialized) return
        val spec = AiScriptBuilder.byId(prov)
        val manualPath = if (::etOfflineModelPath.isInitialized) etOfflineModelPath.text.toString().trim() else ""
        getSharedPreferences("h2v", MODE_PRIVATE).edit()
            .putString("aikey_$prov", etAiKey.text.toString().trim())
            .putString("aimodel_$prov", etAiModel.text.toString().trim().ifEmpty { spec.defaultModel })
            .putString("offline_model_path", manualPath)
            .apply()
    }

    private fun loadAiFields(prov: String) {
        if (!::etAiKey.isInitialized) return
        val p = getSharedPreferences("h2v", MODE_PRIVATE)
        val spec = AiScriptBuilder.byId(prov)
        etAiKey.hint = "Enter ${spec.label} API Key"
        etAiKey.setText(p.getString("aikey_$prov", "") ?: "")
        etAiModel.setText(p.getString("aimodel_$prov", spec.defaultModel) ?: spec.defaultModel)
    }

    private fun refreshAiProvUi() {
        for ((i, ch) in aiProvChips.withIndex()) {
            styleChip(ch, AiScriptBuilder.providers[i].id == curAiProv)
        }
    }

    private fun pickMdScriptFile() {
        if (exporting || aiGenerating) return
        saveAiFields(curAiProv)
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(i, 15)
    }

    private fun startAiScriptGeneration(rawScript: String, fileTitle: String? = null) {
        if (exporting || aiGenerating) return
        val trimmed = rawScript.trim()
        if (trimmed.isEmpty()) {
            Toast.makeText(this, "Pehle script prompt likhein ya .md / .txt file upload karein.", Toast.LENGTH_SHORT).show()
            return
        }
        saveAiFields(curAiProv)
        refreshOfflineModelUi()
        val prov = curAiProv
        val apiKey = etAiKey.text.toString().trim()
        val model = etAiModel.text.toString().trim()
        val manualPath = etOfflineModelPath.text.toString().trim()
        val offlineMode = useOfflineAi
        val savedUri = offlineModelUriStr

        aiGenerating = true
        refreshButtons()
        aiGenStatusTv.visibility = View.VISIBLE
        aiGenStatusTv.setTextColor(cTxt)
        aiGenStatusTv.text = "Aapki script se custom scenes aur animations ban rahe hain..."

        Thread {
            try {
                val (projectCode, genTitle) = AiScriptBuilder.generateProject(
                    this,
                    offlineMode,
                    savedUri,
                    manualPath,
                    prov,
                    apiKey,
                    model,
                    trimmed
                ) { stepMsg ->
                    runOnUiThread {
                        aiGenStatusTv.text = stepMsg
                    }
                }
                val finalTitle = fileTitle?.ifBlank { genTitle } ?: genTitle
                runOnUiThread {
                    aiGenerating = false
                    aiGenStatusTv.visibility = View.GONE
                    refreshButtons()
                    Toast.makeText(this, "Custom Video Ready: $finalTitle", Toast.LENGTH_SHORT).show()
                    loadProject(projectCode, finalTitle, true)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    aiGenerating = false
                    refreshButtons()
                    aiGenStatusTv.setTextColor(cErr)
                    aiGenStatusTv.text = "Error: ${e.message}"
                }
            }
        }.start()
    }

    // ---------- Voice Fields (Edge Free Neural, Cartesia.ai, ElevenLabs.io, Sarvam AI & Phone) ----------
    private fun defaultVoice(eng: String): String = when (eng) {
        "edge" -> "Swara (Hindi) | hi-IN-SwaraNeural"
        "cartesia" -> "Hindi Narrator Woman | c1abd502-9231-4558-a054-10ac950c356d"
        "elevenlabs" -> "Aria | 9BWtsMINqrJLrRacOk9x"
        "sarvam" -> "ritu"
        else -> ""
    }

    private fun saveEngineFields(eng: String) {
        val p = getSharedPreferences("h2v", MODE_PRIVATE).edit()
        if (eng == "edge" && ::etEdgeVoice.isInitialized) {
            p.putString("voice_edge", etEdgeVoice.text.toString().trim().ifEmpty { defaultVoice("edge") })
        } else if (eng == "cartesia" && ::etKey.isInitialized && ::etVoice.isInitialized) {
            p.putString("key_cartesia", etKey.text.toString().trim())
            p.putString("voice_cartesia", etVoice.text.toString().trim().ifEmpty { defaultVoice("cartesia") })
            p.putString("cartesia_model", curCartesiaModel)
        } else if (eng == "elevenlabs" && ::etKey.isInitialized && ::etVoice.isInitialized) {
            p.putString("key_elevenlabs", etKey.text.toString().trim())
            p.putString("voice_elevenlabs", etVoice.text.toString().trim().ifEmpty { defaultVoice("elevenlabs") })
            p.putString("eleven_model", curElevenModel)
        } else if (eng == "sarvam" && ::etKey.isInitialized && ::etVoice.isInitialized) {
            p.putString("key_sarvam", etKey.text.toString().trim())
            p.putString("voice_sarvam", etVoice.text.toString().trim().ifEmpty { defaultVoice("sarvam") })
        }
        p.apply()
    }

    private fun loadEngineFields(eng: String) {
        val p = getSharedPreferences("h2v", MODE_PRIVATE)
        fps = p.getInt("fps", 60)
        if (::etEdgeVoice.isInitialized) {
            etEdgeVoice.setText(p.getString("voice_edge", defaultVoice("edge")) ?: defaultVoice("edge"))
        }
        if (eng == "cartesia" && ::etKey.isInitialized && ::etVoice.isInitialized) {
            etKey.setText(p.getString("key_cartesia", "") ?: "")
            etVoice.setText(p.getString("voice_cartesia", defaultVoice("cartesia")) ?: defaultVoice("cartesia"))
        } else if (eng == "elevenlabs" && ::etKey.isInitialized && ::etVoice.isInitialized) {
            etKey.setText(p.getString("key_elevenlabs", "") ?: "")
            etVoice.setText(p.getString("voice_elevenlabs", defaultVoice("elevenlabs")) ?: defaultVoice("elevenlabs"))
        } else if (eng == "sarvam" && ::etKey.isInitialized && ::etVoice.isInitialized) {
            etKey.setText(p.getString("key_sarvam", "") ?: "")
            etVoice.setText(p.getString("voice_sarvam", defaultVoice("sarvam")) ?: defaultVoice("sarvam"))
        }
    }

    private fun refreshCartesiaModelChips() {
        if (!::chipCarSonic3.isInitialized) return
        styleChip(chipCarSonic3, curCartesiaModel == CartesiaTts.MODEL_SONIC_3)
        styleChip(chipCarSonic2, curCartesiaModel == CartesiaTts.MODEL_SONIC_2)
        styleChip(chipCarTurbo, curCartesiaModel == CartesiaTts.MODEL_SONIC_TURBO)
    }

    private fun fetchCartesiaVoicesNow() {
        if (exporting || fetchingCartesiaVoices) return
        saveEngineFields("cartesia")
        val apiKey = etKey.text.toString().trim()
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Pehle upar Cartesia API Key (play.cartesia.ai) enter karein!", Toast.LENGTH_LONG).show()
            return
        }
        fetchingCartesiaVoices = true
        refreshButtons()
        tvCartesiaVoicesStatus.setTextColor(cTxt)
        tvCartesiaVoicesStatus.text = "Cartesia.ai se sabhi Hindi aur Sonic Multilingual voices fetch ho rahi hain..."

        Thread {
            try {
                val list = CartesiaTts.fetchVoicesFromApi(apiKey)
                runOnUiThread {
                    fetchingCartesiaVoices = false
                    cartesiaVoiceItems.clear()
                    cartesiaVoiceItems.addAll(list)
                    tvCartesiaVoicesStatus.setTextColor(cOk)
                    tvCartesiaVoicesStatus.text = "\u2713 ${list.size} Cartesia Hindi & Sonic voices fetched! Play (\u25B6) ya Set (\u2713) dabayein."
                    refreshButtons()
                    rebuildCartesiaVoicesListUi()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    fetchingCartesiaVoices = false
                    tvCartesiaVoicesStatus.setTextColor(cErr)
                    tvCartesiaVoicesStatus.text = "Fetch Error: ${e.message}"
                    refreshButtons()
                }
            }
        }.start()
    }

    private fun playOrStopCartesiaVoiceSample(item: CartesiaTts.CartesiaVoiceItem) {
        if (playingVoiceId == item.voiceId) {
            stopVoiceSamplePlayback()
            rebuildCartesiaVoicesListUi()
            return
        }
        stopVoiceSamplePlayback()
        saveEngineFields("cartesia")
        val apiKey = etKey.text.toString().trim()
        val modelToUse = curCartesiaModel

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Cartesia Hindi voice sample sunne ke liye pehle Cartesia API key enter karein!", Toast.LENGTH_LONG).show()
            return
        }

        playingVoiceId = item.voiceId
        rebuildCartesiaVoicesListUi()
        tvCartesiaVoicesStatus.setTextColor(cTxt)
        tvCartesiaVoicesStatus.text = "\uD83D\uDD0A Generating Cartesia Sonic Hindi sample for ${item.name}..."

        Thread {
            try {
                val sampleFile = File(cacheDir, "car_sample_${item.voiceId}_${modelToUse}.wav")
                if (!sampleFile.exists() || sampleFile.length() < 256) {
                    val sampleText = "Namaste dosto! Aap sun rahe hain Cartesia Sonic ki ultra-fast aur natural Hindi AI voice."
                    val wavBytes = CartesiaTts.synthWavBytes(apiKey, sampleText, item.voiceId, modelToUse)
                    sampleFile.writeBytes(wavBytes)
                }
                runOnUiThread {
                    if (playingVoiceId != item.voiceId) return@runOnUiThread
                    try {
                        val mp = MediaPlayer()
                        voiceSamplePlayer = mp
                        mp.setDataSource(sampleFile.absolutePath)
                        mp.setOnCompletionListener {
                            stopVoiceSamplePlayback()
                            tvCartesiaVoicesStatus.text = "\u2713 Finished playing: ${item.name}"
                            rebuildCartesiaVoicesListUi()
                        }
                        mp.setOnErrorListener { _, _, _ ->
                            stopVoiceSamplePlayback()
                            rebuildCartesiaVoicesListUi()
                            true
                        }
                        mp.prepare()
                        mp.start()
                        tvCartesiaVoicesStatus.setTextColor(cOk)
                        tvCartesiaVoicesStatus.text = "\u25B6 Playing Cartesia Hindi Voice: ${item.name} (${item.badge})"
                        rebuildCartesiaVoicesListUi()
                    } catch (ex: Exception) {
                        stopVoiceSamplePlayback()
                        tvCartesiaVoicesStatus.setTextColor(cErr)
                        tvCartesiaVoicesStatus.text = "Play error: ${ex.message}"
                        rebuildCartesiaVoicesListUi()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    stopVoiceSamplePlayback()
                    tvCartesiaVoicesStatus.setTextColor(cErr)
                    tvCartesiaVoicesStatus.text = "Sample error (${item.name}): ${e.message}"
                    rebuildCartesiaVoicesListUi()
                }
            }
        }.start()
    }

    private fun rebuildCartesiaVoicesListUi() {
        if (!::cartesiaVoicesListCol.isInitialized) return
        cartesiaVoicesListCol.removeAllViews()
        val curSpec = if (::etVoice.isInitialized && curEng == "cartesia") {
            etVoice.text.toString().trim()
        } else {
            getSharedPreferences("h2v", MODE_PRIVATE).getString("voice_cartesia", defaultVoice("cartesia")) ?: defaultVoice("cartesia")
        }
        val activeId = CartesiaTts.resolveVoiceId(curSpec)

        for (item in cartesiaVoiceItems) {
            val isSelected = item.voiceId.equals(activeId, ignoreCase = true)
            val isPlaying = item.voiceId == playingVoiceId

            val rowCard = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = rounded(
                    if (isSelected) Color.parseColor("#1E1E1E") else Color.parseColor("#0A0A0A"),
                    dpf(12),
                    if (isSelected) cWhite else cStroke,
                    dp(if (isSelected) 2 else 1)
                )
            }

            val infoCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            val titleLine = if (isSelected) "\u2713 ${item.name}  (ACTIVE)" else item.name
            infoCol.addView(label(titleLine, 13.5f, cTxt, true))
            infoCol.addView(label(item.badge, 11f, cSilver, false).apply { setPadding(0, dp(2), 0, 0) })
            infoCol.addView(label("${item.description}  \u2022  ID: ${item.voiceId.take(12)}...", 10.5f, cSub, false).apply { setPadding(0, dp(2), 0, 0) })

            rowCard.addView(infoCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            val btnPlay = TextView(this).apply {
                text = if (isPlaying) "\u23F9 Stop" else "\u25B6 Play"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(if (isPlaying) Color.BLACK else cTxt)
                setPadding(dp(11), dp(8), dp(11), dp(8))
                background = if (isPlaying) {
                    ripple(gradMono(cWhite, cSilver, dpf(10)), true)
                } else {
                    ripple(rounded(cCard2, dpf(10), cStroke, dp(1)), false)
                }
                isClickable = true
                setOnClickListener {
                    playOrStopCartesiaVoiceSample(item)
                }
            }

            val btnSet = TextView(this).apply {
                text = if (isSelected) "\u2713 Set" else "Set"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(if (isSelected) Color.BLACK else cTxt)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = if (isSelected) {
                    ripple(gradMono(cWhite, cSilver, dpf(10)), true)
                } else {
                    ripple(rounded(cCard2, dpf(10), cStroke, dp(1)), false)
                }
                isClickable = true
                setOnClickListener {
                    curEng = "cartesia"
                    etVoice.setText(item.toSpecString())
                    saveEngineFields("cartesia")
                    getSharedPreferences("h2v", MODE_PRIVATE).edit().putString("cur_eng", "cartesia").apply()
                    refreshEngineUi()
                    rebuildCartesiaVoicesListUi()
                    Toast.makeText(this@MainActivity, "Cartesia Hindi Voice Set: ${item.name}", Toast.LENGTH_SHORT).show()
                }
            }

            rowCard.addView(btnPlay, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(6) })
            rowCard.addView(btnSet, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(6) })

            val rlp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            rlp.topMargin = dp(8)
            cartesiaVoicesListCol.addView(rowCard, rlp)
        }
    }

    private fun fetchEdgeFreeVoicesNow() {
        if (exporting || fetchingEdgeVoices) return
        saveEngineFields("edge")
        fetchingEdgeVoices = true
        refreshButtons()
        tvEdgeVoicesStatus.setTextColor(cTxt)
        tvEdgeVoicesStatus.text = "Microsoft Edge server se sabhi Free Hindi & Hinglish Neural voices fetch ho rahi hain..."

        Thread {
            try {
                val list = EdgeFreeTts.fetchHindiVoicesList()
                runOnUiThread {
                    fetchingEdgeVoices = false
                    edgeVoiceItems.clear()
                    edgeVoiceItems.addAll(list)
                    tvEdgeVoicesStatus.setTextColor(cOk)
                    tvEdgeVoicesStatus.text = "\u2713 ${list.size} Free Microsoft Neural Hindi & Hinglish voices ready! Play (\u25B6) ya Set (\u2713) dabayein."
                    refreshButtons()
                    rebuildEdgeVoicesListUi()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    fetchingEdgeVoices = false
                    tvEdgeVoicesStatus.setTextColor(cErr)
                    tvEdgeVoicesStatus.text = "Refresh note: ${e.message}"
                    refreshButtons()
                }
            }
        }.start()
    }

    private fun playOrStopEdgeVoiceSample(item: EdgeFreeTts.EdgeVoiceItem) {
        if (playingVoiceId == item.shortName) {
            stopVoiceSamplePlayback()
            rebuildEdgeVoicesListUi()
            return
        }
        stopVoiceSamplePlayback()
        saveEngineFields("edge")
        playingVoiceId = item.shortName
        rebuildEdgeVoicesListUi()
        tvEdgeVoicesStatus.setTextColor(cTxt)
        tvEdgeVoicesStatus.text = "\uD83D\uDD0A Generating Free Hindi Neural voice sample for ${item.displayName}..."

        Thread {
            try {
                val sampleFile = File(cacheDir, "edge_sample_${item.shortName}.mp3")
                if (!sampleFile.exists() || sampleFile.length() < 256) {
                    val cleanPerson = item.displayName.substringBefore("(").trim()
                    val sampleText = "Namaste dosto! Main $cleanPerson hoon, aur aap sun rahe hain Microsoft Edge ki bilkul free aur natural Hindi Neural AI awaaz."
                    val mp3Bytes = EdgeFreeTts.synthMp3Bytes(sampleText, item.shortName, 0)
                    sampleFile.writeBytes(mp3Bytes)
                }
                runOnUiThread {
                    if (playingVoiceId != item.shortName) return@runOnUiThread
                    try {
                        val mp = MediaPlayer()
                        voiceSamplePlayer = mp
                        mp.setDataSource(sampleFile.absolutePath)
                        mp.setOnCompletionListener {
                            stopVoiceSamplePlayback()
                            tvEdgeVoicesStatus.text = "\u2713 Finished playing: ${item.displayName}"
                            rebuildEdgeVoicesListUi()
                        }
                        mp.setOnErrorListener { _, _, _ ->
                            stopVoiceSamplePlayback()
                            rebuildEdgeVoicesListUi()
                            true
                        }
                        mp.prepare()
                        mp.start()
                        tvEdgeVoicesStatus.setTextColor(cOk)
                        tvEdgeVoicesStatus.text = "\u25B6 Playing Free Neural Voice: ${item.displayName} (${item.shortName})"
                        rebuildEdgeVoicesListUi()
                    } catch (ex: Exception) {
                        stopVoiceSamplePlayback()
                        tvEdgeVoicesStatus.setTextColor(cErr)
                        tvEdgeVoicesStatus.text = "Play error: ${ex.message}"
                        rebuildEdgeVoicesListUi()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    stopVoiceSamplePlayback()
                    tvEdgeVoicesStatus.setTextColor(cErr)
                    tvEdgeVoicesStatus.text = "Sample error (${item.displayName}): ${e.message}"
                    rebuildEdgeVoicesListUi()
                }
            }
        }.start()
    }

    private fun rebuildEdgeVoicesListUi() {
        if (!::edgeVoicesListCol.isInitialized) return
        edgeVoicesListCol.removeAllViews()
        val curSpec = if (::etEdgeVoice.isInitialized) etEdgeVoice.text.toString().trim() else defaultVoice("edge")
        val activeShort = EdgeFreeTts.resolveShortName(curSpec)

        for (item in edgeVoiceItems) {
            val isSelected = item.shortName.equals(activeShort, ignoreCase = true)
            val isPlaying = item.shortName == playingVoiceId

            val rowCard = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = rounded(
                    if (isSelected) Color.parseColor("#1E1E1E") else Color.parseColor("#0A0A0A"),
                    dpf(12),
                    if (isSelected) cWhite else cStroke,
                    dp(if (isSelected) 2 else 1)
                )
            }

            val infoCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            val titleLine = if (isSelected) "\u2713 ${item.displayName}  (ACTIVE)" else item.displayName
            infoCol.addView(label(titleLine, 13.5f, cTxt, true))
            infoCol.addView(label(item.badge, 11f, cSilver, false).apply { setPadding(0, dp(2), 0, 0) })
            infoCol.addView(label("${item.description}  \u2022  ${item.shortName}", 10.5f, cSub, false).apply { setPadding(0, dp(2), 0, 0) })

            rowCard.addView(infoCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            val btnPlay = TextView(this).apply {
                text = if (isPlaying) "\u23F9 Stop" else "\u25B6 Play"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(if (isPlaying) Color.BLACK else cTxt)
                setPadding(dp(11), dp(8), dp(11), dp(8))
                background = if (isPlaying) {
                    ripple(gradMono(cWhite, cSilver, dpf(10)), true)
                } else {
                    ripple(rounded(cCard2, dpf(10), cStroke, dp(1)), false)
                }
                isClickable = true
                setOnClickListener {
                    playOrStopEdgeVoiceSample(item)
                }
            }

            val btnSet = TextView(this).apply {
                text = if (isSelected) "\u2713 Set" else "Set"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(if (isSelected) Color.BLACK else cTxt)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = if (isSelected) {
                    ripple(gradMono(cWhite, cSilver, dpf(10)), true)
                } else {
                    ripple(rounded(cCard2, dpf(10), cStroke, dp(1)), false)
                }
                isClickable = true
                setOnClickListener {
                    curEng = "edge"
                    etEdgeVoice.setText(item.toSpecString())
                    saveEngineFields("edge")
                    getSharedPreferences("h2v", MODE_PRIVATE).edit().putString("cur_eng", "edge").apply()
                    refreshEngineUi()
                    rebuildEdgeVoicesListUi()
                    Toast.makeText(this@MainActivity, "Free Edge Hindi Voice Set: ${item.displayName}", Toast.LENGTH_SHORT).show()
                }
            }

            rowCard.addView(btnPlay, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(6) })
            rowCard.addView(btnSet, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(6) })

            val rlp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            rlp.topMargin = dp(8)
            edgeVoicesListCol.addView(rowCard, rlp)
        }
    }

    private fun refreshElevenModelChips() {
        if (!::chipElMultiV2.isInitialized) return
        styleChip(chipElMultiV2, curElevenModel == ElevenLabsTts.MODEL_MULTILINGUAL_V2)
        styleChip(chipElFlashV25, curElevenModel == ElevenLabsTts.MODEL_FLASH_V2_5)
    }

    private fun fetchElevenLabsVoicesNow() {
        if (exporting || fetchingElevenVoices) return
        saveEngineFields("elevenlabs")
        val apiKey = etKey.text.toString().trim()
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Pehle upar ElevenLabs API Key (xi-api-key) enter karein!", Toast.LENGTH_LONG).show()
            return
        }
        fetchingElevenVoices = true
        refreshButtons()
        tvElevenVoicesStatus.setTextColor(cTxt)
        tvElevenVoicesStatus.text = "ElevenLabs.io se sabhi Hindi aur Multilingual voices fetch ho rahi hain..."

        Thread {
            try {
                val list = ElevenLabsTts.fetchVoicesFromApi(apiKey)
                runOnUiThread {
                    fetchingElevenVoices = false
                    elevenVoiceItems.clear()
                    elevenVoiceItems.addAll(list)
                    tvElevenVoicesStatus.setTextColor(cOk)
                    tvElevenVoicesStatus.text = "\u2713 ${list.size} Hindi & Multilingual voices fetched! Kisi bhi voice ko Play (\u25B6) karein ya Set (\u2713) dabayein."
                    refreshButtons()
                    rebuildElevenVoicesListUi()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    fetchingElevenVoices = false
                    tvElevenVoicesStatus.setTextColor(cErr)
                    tvElevenVoicesStatus.text = "Fetch Error: ${e.message}"
                    refreshButtons()
                }
            }
        }.start()
    }

    private fun stopVoiceSamplePlayback() {
        playingVoiceId = ""
        try {
            voiceSamplePlayer?.stop()
        } catch (_: Exception) {}
        try {
            voiceSamplePlayer?.release()
        } catch (_: Exception) {}
        voiceSamplePlayer = null
    }

    private fun playOrStopElevenVoiceSample(item: ElevenLabsTts.VoiceItem) {
        if (playingVoiceId == item.voiceId) {
            stopVoiceSamplePlayback()
            rebuildElevenVoicesListUi()
            return
        }
        stopVoiceSamplePlayback()
        saveEngineFields("elevenlabs")
        val apiKey = etKey.text.toString().trim()
        val modelToUse = curElevenModel

        if (apiKey.isEmpty() && item.previewUrl.isBlank()) {
            Toast.makeText(this, "Hindi voice sample sunne ke liye pehle ElevenLabs API key enter karein!", Toast.LENGTH_LONG).show()
            return
        }

        playingVoiceId = item.voiceId
        rebuildElevenVoicesListUi()
        tvElevenVoicesStatus.setTextColor(cTxt)
        tvElevenVoicesStatus.text = "\uD83D\uDD0A Loading Hindi sample for ${item.name}..."

        Thread {
            try {
                val sampleFile = File(cacheDir, "el_sample_${item.voiceId}_${modelToUse}.mp3")
                if (apiKey.isNotEmpty()) {
                    if (!sampleFile.exists() || sampleFile.length() < 256) {
                        val sampleText = "Namaste dosto! Main ${item.name} hoon, aur aap sun rahe hain ElevenLabs ki natural Hindi AI voice."
                        val mp3Bytes = ElevenLabsTts.synthMp3Bytes(apiKey, sampleText, item.voiceId, modelToUse)
                        sampleFile.writeBytes(mp3Bytes)
                    }
                    runOnUiThread {
                        if (playingVoiceId != item.voiceId) return@runOnUiThread
                        startMediaPlayerFromFile(sampleFile.absolutePath, item)
                    }
                } else if (item.previewUrl.isNotBlank()) {
                    runOnUiThread {
                        if (playingVoiceId != item.voiceId) return@runOnUiThread
                        startMediaPlayerFromUrl(item.previewUrl, item)
                    }
                }
            } catch (e: Exception) {
                // If live TTS fails and previewUrl exists, fallback to previewUrl
                if (item.previewUrl.isNotBlank()) {
                    runOnUiThread {
                        if (playingVoiceId != item.voiceId) return@runOnUiThread
                        startMediaPlayerFromUrl(item.previewUrl, item)
                    }
                } else {
                    runOnUiThread {
                        stopVoiceSamplePlayback()
                        tvElevenVoicesStatus.setTextColor(cErr)
                        tvElevenVoicesStatus.text = "Sample play error (${item.name}): ${e.message}"
                        rebuildElevenVoicesListUi()
                    }
                }
            }
        }.start()
    }

    private fun startMediaPlayerFromFile(path: String, item: ElevenLabsTts.VoiceItem) {
        try {
            val mp = MediaPlayer()
            voiceSamplePlayer = mp
            mp.setDataSource(path)
            mp.setOnCompletionListener {
                stopVoiceSamplePlayback()
                tvElevenVoicesStatus.text = "\u2713 Finished playing: ${item.name}"
                rebuildElevenVoicesListUi()
            }
            mp.setOnErrorListener { _, _, _ ->
                stopVoiceSamplePlayback()
                rebuildElevenVoicesListUi()
                true
            }
            mp.prepare()
            mp.start()
            tvElevenVoicesStatus.setTextColor(cOk)
            tvElevenVoicesStatus.text = "\u25B6 Playing Hindi Voice: ${item.name} (${item.badge})"
            rebuildElevenVoicesListUi()
        } catch (e: Exception) {
            stopVoiceSamplePlayback()
            tvElevenVoicesStatus.setTextColor(cErr)
            tvElevenVoicesStatus.text = "Audio play error: ${e.message}"
            rebuildElevenVoicesListUi()
        }
    }

    private fun startMediaPlayerFromUrl(url: String, item: ElevenLabsTts.VoiceItem) {
        try {
            val mp = MediaPlayer()
            voiceSamplePlayer = mp
            mp.setDataSource(url)
            mp.setOnPreparedListener {
                if (playingVoiceId == item.voiceId) {
                    it.start()
                    tvElevenVoicesStatus.setTextColor(cOk)
                    tvElevenVoicesStatus.text = "\u25B6 Playing Preview: ${item.name}"
                    rebuildElevenVoicesListUi()
                } else {
                    stopVoiceSamplePlayback()
                }
            }
            mp.setOnCompletionListener {
                stopVoiceSamplePlayback()
                tvElevenVoicesStatus.text = "\u2713 Finished playing: ${item.name}"
                rebuildElevenVoicesListUi()
            }
            mp.setOnErrorListener { _, _, _ ->
                stopVoiceSamplePlayback()
                rebuildElevenVoicesListUi()
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            stopVoiceSamplePlayback()
            tvElevenVoicesStatus.setTextColor(cErr)
            tvElevenVoicesStatus.text = "Preview stream error: ${e.message}"
            rebuildElevenVoicesListUi()
        }
    }

    private fun rebuildElevenVoicesListUi() {
        if (!::elevenVoicesListCol.isInitialized) return
        elevenVoicesListCol.removeAllViews()
        val curSpec = if (::etVoice.isInitialized) etVoice.text.toString().trim() else defaultVoice("elevenlabs")
        val activeId = ElevenLabsTts.resolveVoiceId(curSpec)

        for (item in elevenVoiceItems) {
            val isSelected = item.voiceId.equals(activeId, ignoreCase = true)
            val isPlaying = item.voiceId == playingVoiceId

            val rowCard = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = rounded(
                    if (isSelected) Color.parseColor("#1E1E1E") else Color.parseColor("#0A0A0A"),
                    dpf(12),
                    if (isSelected) cWhite else cStroke,
                    dp(if (isSelected) 2 else 1)
                )
            }

            val infoCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            val titleLine = if (isSelected) "\u2713 ${item.name}  (ACTIVE)" else item.name
            infoCol.addView(label(titleLine, 13.5f, cTxt, true))
            infoCol.addView(label(item.badge, 11f, cSilver, false).apply { setPadding(0, dp(2), 0, 0) })
            infoCol.addView(label("${item.description}  \u2022  ID: ${item.voiceId.take(10)}...", 10.5f, cSub, false).apply { setPadding(0, dp(2), 0, 0) })

            rowCard.addView(infoCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            val btnPlay = TextView(this).apply {
                text = if (isPlaying) "\u23F9 Stop" else "\u25B6 Play"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(if (isPlaying) Color.BLACK else cTxt)
                setPadding(dp(11), dp(8), dp(11), dp(8))
                background = if (isPlaying) {
                    ripple(gradMono(cWhite, cSilver, dpf(10)), true)
                } else {
                    ripple(rounded(cCard2, dpf(10), cStroke, dp(1)), false)
                }
                isClickable = true
                setOnClickListener {
                    playOrStopElevenVoiceSample(item)
                }
            }

            val btnSet = TextView(this).apply {
                text = if (isSelected) "\u2713 Set" else "Set"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(if (isSelected) Color.BLACK else cTxt)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = if (isSelected) {
                    ripple(gradMono(cWhite, cSilver, dpf(10)), true)
                } else {
                    ripple(rounded(cCard2, dpf(10), cStroke, dp(1)), false)
                }
                isClickable = true
                setOnClickListener {
                    curEng = "elevenlabs"
                    etVoice.setText(item.toSpecString())
                    saveEngineFields("elevenlabs")
                    getSharedPreferences("h2v", MODE_PRIVATE).edit().putString("cur_eng", "elevenlabs").apply()
                    refreshEngineUi()
                    rebuildElevenVoicesListUi()
                    Toast.makeText(this@MainActivity, "ElevenLabs Hindi Voice Set: ${item.name}", Toast.LENGTH_SHORT).show()
                }
            }

            rowCard.addView(btnPlay, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(6) })
            rowCard.addView(btnSet, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(6) })

            val rlp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            rlp.topMargin = dp(8)
            elevenVoicesListCol.addView(rowCard, rlp)
        }
    }

    // ---------- Export MP4 ----------
    private fun setBusy(b: Boolean) {
        exporting = b
        refreshButtons()
    }

    private fun startExport() {
        if (!projectReady || exporting) return
        if (settingsMode) setPage(false)
        cancel = false
        blankFrames = 0
        contentSeen = false
        audioNote = ""
        savedUri = null
        resultRow.visibility = View.GONE
        (btnTts.tag as View).visibility = View.GONE
        bar.progress = 0
        statsText.text = ""
        stopVoiceSamplePlayback()
        try { previewTts?.stop() } catch (_: Exception) {}
        saveEngineFields(curEng)
        val mode = curEng
        val key = etKey.text.toString().trim()
        val voiceName = if (mode == "edge") {
            etEdgeVoice.text.toString().trim().ifEmpty { defaultVoice("edge") }
        } else {
            etVoice.text.toString().trim().ifEmpty { defaultVoice(mode) }
        }
        val useFps = fps
        val usePortrait = portrait
        val useFill = fillMode
        setBusy(true)
        Thread { runExport(mode, key, voiceName, useFps, usePortrait, useFill) }.start()
    }

    private fun evalSync(js: String, timeoutS: Long = 60): String? {
        val l = CountDownLatch(1)
        var res: String? = null
        runOnUiThread {
            web.evaluateJavascript(js) { r ->
                res = r
                l.countDown()
            }
        }
        l.await(timeoutS, TimeUnit.SECONDS)
        return res
    }

    private fun unquote(s: String?): String? {
        if (s == null || s == "null") return null
        return try {
            JSONTokener(s).nextValue() as? String
        } catch (e: Exception) {
            null
        }
    }

    private fun runExport(mode: String, key: String, voiceName: String, useFps: Int, usePortrait: Boolean = false, useFill: Boolean = false) {
        var tts: TtsRenderer? = null
        try {
            ui("Script padh raha hai...")
            val narr = unquote(evalSync("NH.narration()")) ?: throw Exception("Project script nahi mila")
            val steps = JSONArray(narr)
            val lang = unquote(evalSync("NH.lang")) ?: "hi-IN"
            val clips = HashMap<String, ShortArray>()
            val durs = JSONArray()

            var online: ((String) -> ShortArray)? = null
            if (mode == "edge") {
                val ed = EdgeFreeTts(cacheDir, voiceName, 0)
                online = { txt -> ed.synth(txt) }
            } else if (mode == "cartesia") {
                if (key.isBlank()) throw Exception("Cartesia API key khali hai. Settings tab mein jaakar Cartesia API key daalein ya 'Edge Free Neural' chunein.")
                val car = CartesiaTts(cacheDir, key, voiceName, curCartesiaModel)
                online = { txt -> car.synth(txt) }
            } else if (mode == "elevenlabs") {
                if (key.isBlank()) throw Exception("ElevenLabs API key khali hai. Settings tab mein jaakar ElevenLabs API key daalein ya 'Edge Free Neural' chunein.")
                val el = ElevenLabsTts(cacheDir, key, voiceName, curElevenModel)
                online = { txt -> el.synth(txt) }
            } else if (mode == "sarvam") {
                if (key.isBlank()) throw Exception("Sarvam API key khali hai. Settings tab mein jaakar Sarvam API key daalein ya 'Phone TTS' chunein.")
                val sv = SarvamTts(key, lang, voiceName, 1.0)
                online = { txt -> sv.synth(txt) }
            } else {
                val t = TtsRenderer(applicationContext)
                tts = t
                ui("Phone Hindi TTS engine shuru ho raha hai...")
                t.init(lang, 0.95f)
            }

            for (i in 0 until steps.length()) {
                val o = steps.getJSONObject(i)
                val d = JSONObject()
                for (k in listOf("a", "b")) {
                    if (cancel) throw Exception("Cancel ho gaya")
                    val text = o.optString(k)
                    var sec = 0.0
                    if (text.isNotBlank()) {
                        ui("Awaaz ban rahi hai: step ${i + 1}/${steps.length()} ($k)")
                        val pcm = if (online != null) online(text)
                        else tts!!.synth("$i$k", text)
                            ?: throw TtsMissingException("Step ${i + 1} ki awaaz nahi bani. Phone TTS settings mein Hindi voice check karein.")
                        clips["$i$k"] = pcm
                        sec = pcm.size / AudioUtils.SR.toDouble()
                    }
                    d.put(k, sec)
                }
                durs.put(d)
            }

            ui("Timeline aur Sound Effects set ho rahe hain...")
            val planJson = unquote(evalSync("NH.setDurations(" + JSONObject.quote(durs.toString()) + ")"))
                ?: throw Exception("Timeline setup fail hua")
            val plan = JSONObject(planJson)
            val total = plan.getDouble("total")

            var sfxPcm: Stem? = null
            val evAll = plan.getJSONArray("events")
            val cuesJson = unquote(evalSync("(window.__h2vExtractSfxCues?window.__h2vExtractSfxCues():(window.__h2vExtractDomCues?window.__h2vExtractDomCues():'[]'))", 90))
            val cuesArr = try { JSONArray(cuesJson ?: "[]") } catch (_: Exception) { JSONArray() }

            if (plan.optBoolean("stems", false)) {
                ui("Sound effects ban rahe hain (No Music)...")
                val st = renderStems()
                sfxPcm = st.second
                val err = audioErr
                val sfxPeak = synchronized(audioInfo) { audioInfo["sfx"] ?: 0.0 }
                val sfxNodes = synchronized(audioInfo) { (audioInfo["sfx_n"] ?: 0.0).toInt() }
                val sfxOk = sfxPcm != null && sfxPeak >= 0.02
                var note = "SFX " + (if (sfxOk) "\u2713 ($sfxNodes sounds)" else "\u2717")
                if (!sfxOk && autoSfx) {
                    sfxPcm = Stem(AutoSfx.sfxFromCues(total, cuesArr, evAll, false), AutoSfx.SFX_RATE)
                    note = "SFX \u2713 (${cuesArr.length()} sounds)"
                }
                if (err != null) note += " [note: $err]"
                audioNote = "$note \u2022 No Music"
            } else if (autoSfx) {
                ui("Sound effects (typing, popup, slide, tap, whoosh...) ban rahe hain...")
                sfxPcm = Stem(AutoSfx.sfxFromCues(total, cuesArr, evAll, true), AutoSfx.SFX_RATE)
                audioNote = "SFX \u2713 (${cuesArr.length()} sounds: typing, popup, slide, tap, whoosh) \u2022 No Music"
            }

            val audio = if (clips.isNotEmpty() || sfxPcm != null) {
                AudioUtils.mixStems(
                    total, plan.getJSONArray("events"), clips,
                    null, 12000, sfxPcm?.data, sfxPcm?.rate ?: 48000,
                    vols[0] / 100f, vols[1] / 100f, 0f, vols[3] / 100f, duck
                )
            } else null

            val out = File(cacheDir, "export_${System.currentTimeMillis()}.mp4")
            val e = VideoExporter(out, useFps, audio, usePortrait, useFill)
            ui("Encoder taiyar ho raha hai...")
            e.prepare()
            exporter = e
            if (domMode) {
                runOnUiThread { exportScaled = true; layoutWeb() }
                var waited = 0
                while ((web.width != 1920 || web.height != 1080) && waited < 6000) { Thread.sleep(100); waited += 100 }
                if (web.width != 1920 || web.height != 1080) {
                    throw Exception("Renderer 1920x1080 size set nahi kar paya (${web.width}x${web.height}).")
                }
                Thread.sleep(800)
            }
            startMs = SystemClock.elapsedRealtime()
            ui("Video ban raha hai (" + (if (usePortrait) "9:16 1080x1920" else "1080p") + ", $useFps fps)...")
            runOnUiThread { web.evaluateJavascript("NH.run($useFps)", null) }
        } catch (ex: Exception) {
            fail(ex.message ?: ex.toString(), ex is TtsMissingException)
        } finally {
            tts?.shutdown()
        }
    }

    private fun fail(msg: String, showTts: Boolean = false) {
        try { exporter?.abort() } catch (_: Exception) {}
        exporter = null
        runOnUiThread {
            if (exportScaled) { exportScaled = false; layoutWeb() }
            if (domMode) web.evaluateJavascript("(window.NH&&NH.endExport?NH.endExport():0)", null)
            setStatus("Error: $msg", 2)
            setBusy(false)
            if (showTts) (btnTts.tag as View).visibility = View.VISIBLE
        }
    }

    // ---------- Volume & SFX Controls ----------
    private fun volStr(i: Int): String = String.format(Locale.US, "%.2f", vols[i] / 100.0)

    private fun pushVolumes() {
        if (!::web.isInitialized || !projectReady) return
        web.evaluateJavascript("(window.NH&&NH.setVolumes?NH.setVolumes(${volStr(0)},${volStr(1)},0.0,${volStr(3)}):0)", null)
    }

    private fun saveVolumes() {
        val ed = getSharedPreferences("h2v", MODE_PRIVATE).edit()
        for (i in 0 until 4) ed.putInt("vol_$i", vols[i])
        ed.putBoolean("duck", duck)
        ed.putBoolean("autosfx", autoSfx)
        ed.apply()
    }

    private fun refreshAutoUi() {
        btnAuto.text = if (autoSfx) "Sound Effects (SFX): ON" else "Sound Effects (SFX): OFF"
    }

    private fun refreshDuckUi() {
        btnDuck.text = if (duck) "Auto-duck: ON" else "Auto-duck: OFF"
    }

    private fun resetVolumes() {
        val d = intArrayOf(100, 100, 0, 100)
        for (i in 0 until 4) volBars[i].progress = d[i]
        duck = true
        autoSfx = true
        refreshDuckUi()
        refreshAutoUi()
        saveVolumes()
        pushVolumes()
    }

    // ---------- Frame Capture & Audio Stems ----------
    private fun captureWeb(): Bitmap? {
        val latch = CountDownLatch(1)
        var okDraw = false
        runOnUiThread {
            try {
                val w = web.width
                val h = web.height
                if (w > 0 && h > 0) {
                    var b = capBmp
                    if (b == null || b.width != w || b.height != h) {
                        b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        capBmp = b
                    }
                    val c = Canvas(b!!)
                    c.drawColor(Color.BLACK)
                    web.draw(c)
                    okDraw = true
                }
            } catch (_: Exception) {
            }
            latch.countDown()
        }
        latch.await(15, TimeUnit.SECONDS)
        return if (okDraw) capBmp else null
    }

    private fun looksUniform(b: Bitmap): Boolean {
        val first = b.getPixel(b.width / 2, b.height / 2)
        var y = 40
        while (y < b.height) {
            var x = 40
            while (x < b.width) {
                if (b.getPixel(x, y) != first) return false
                x += 160
            }
            y += 120
        }
        return true
    }

    private fun renderStems(): Pair<Stem?, Stem?> {
        synchronized(stemBufs) { stemBufs.clear() }
        synchronized(audioInfo) { audioInfo.clear() }
        audioErr = null
        val latch = CountDownLatch(1)
        audioLatch = latch
        runOnUiThread { web.evaluateJavascript("NH.renderAudio()", null) }
        var waitedMs = 0L
        while (!latch.await(500, TimeUnit.MILLISECONDS)) {
            if (cancel) throw Exception("Cancel ho gaya")
            waitedMs += 500
            if (waitedMs > 20 * 60 * 1000L) throw Exception("SFX render timeout (20 min)")
        }
        audioLatch = null
        val m = synchronized(stemBufs) { stemBufs["music"] }
        val s = synchronized(stemBufs) { stemBufs["sfx"] }
        synchronized(stemBufs) { stemBufs.clear() }
        return Pair(m, s)
    }

    private fun layoutWeb() {
        if (!::web.isInitialized || !::playerBox.isInitialized) return
        val lp = web.layoutParams as? FrameLayout.LayoutParams ?: return
        val big = domMode && exportScaled
        val wantW = if (big) 1920 else ViewGroup.LayoutParams.MATCH_PARENT
        val wantH = if (big) 1080 else ViewGroup.LayoutParams.MATCH_PARENT
        if (lp.width != wantW || lp.height != wantH) {
            lp.width = wantW
            lp.height = wantH
            web.layoutParams = lp
        }
        if (big) {
            val bw = playerBox.width
            val bh = playerBox.height
            if (bw > 0 && bh > 0) {
                val sc = minOf(bw / 1920f, bh / 1080f)
                web.pivotX = 0f
                web.pivotY = 0f
                web.scaleX = sc
                web.scaleY = sc
                web.translationX = (bw - 1920f * sc) / 2f
                web.translationY = (bh - 1080f * sc) / 2f
            }
        } else {
            web.scaleX = 1f
            web.scaleY = 1f
            web.translationX = 0f
            web.translationY = 0f
        }
    }

    // ---------- Save / Share ----------
    private fun saveToGallery(f: File): Uri {
        val name = "NH_Video_Edit_${System.currentTimeMillis()}.mp4"
        val v = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/NHVideoEdit")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, v)
            ?: throw Exception("Gallery mein file nahi bana paya")
        contentResolver.openOutputStream(uri)!!.use { os -> f.inputStream().use { it.copyTo(os) } }
        v.clear()
        v.put(MediaStore.Video.Media.IS_PENDING, 0)
        contentResolver.update(uri, v, null, null)
        return uri
    }

    private fun saveDownload(name: String, mime: String, bytes: ByteArray): String {
        val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifEmpty { "file" }
        val v = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, safe)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/NHVideoEdit")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)
            ?: throw Exception("Downloads mein file nahi bana paya")
        contentResolver.openOutputStream(uri)!!.use { it.write(bytes) }
        v.clear()
        v.put(MediaStore.Downloads.IS_PENDING, 0)
        contentResolver.update(uri, v, null, null)
        return "Download/NHVideoEdit/$safe"
    }

    private fun openVideo() {
        val u = savedUri ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(u, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (e: Exception) {
            setStatus("Video player nahi mila: ${e.message}", 2)
        }
    }

    private fun shareVideo() {
        val u = savedUri ?: return
        try {
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, u)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(i, "Video share karo"))
        } catch (e: Exception) {
            setStatus("Share nahi ho paya: ${e.message}", 2)
        }
    }

    private fun openTtsSettings() {
        try {
            startActivity(Intent("com.android.settings.TTS_SETTINGS"))
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    // ---------- JS Bridge ----------
    inner class Bridge {
        @JavascriptInterface
        fun pushFrame(dataUrl: String): Boolean {
            if (cancel) return false
            val e = exporter ?: return false
            return try {
                e.addFrame(dataUrl)
                true
            } catch (ex: Exception) {
                fail("Frame encode fail: ${ex.message}")
                false
            }
        }

        @JavascriptInterface
        fun progress(n: Int, total: Int) {
            val sec = (SystemClock.elapsedRealtime() - startMs) / 1000
            val eta = if (n > 0) sec * (total - n) / n else 0
            runOnUiThread {
                bar.progress = if (total > 0) (n.toLong() * 1000 / total).toInt() else 0
                setStatus("Video ban raha hai... ${n * 100 / maxOf(total, 1)}%", 0)
                statsText.text = "Frame $n / $total   \u2022   ${sec}s gaye   \u2022   lagbhag ${eta}s baaki"
            }
        }

        @JavascriptInterface
        fun done() {
            Thread {
                try {
                    val e = exporter ?: return@Thread
                    ui("MP4 band ho raha hai...")
                    val f = e.finish()
                    exporter = null
                    val uri = saveToGallery(f)
                    f.delete()
                    savedUri = uri
                    runOnUiThread {
                        if (exportScaled) { exportScaled = false; layoutWeb() }
                        bar.progress = 1000
                        setStatus("Ho gaya! Video Movies/NHVideoEdit mein save hua (" + (if (portrait) "9:16 1080x1920" else "1080p") + ", $fps fps)." +
                            (if (audioNote.isNotEmpty()) " Audio: $audioNote" else ""), 1)
                        resultRow.visibility = View.VISIBLE
                        setBusy(false)
                    }
                } catch (ex: Exception) {
                    fail("MP4 save fail: ${ex.message}")
                }
            }.start()
        }

        @JavascriptInterface
        fun fail(msg: String) {
            this@MainActivity.fail(msg)
        }

        @JavascriptInterface
        fun captureFrame(n: Int): Boolean {
            if (cancel) return false
            val e = exporter ?: return false
            val bmp = captureWeb()
            if (bmp == null) {
                fail("Frame capture nahi ho paya")
                return false
            }
            if (!contentSeen) {
                if (looksUniform(bmp)) {
                    blankFrames++
                    if (blankFrames >= 90) {
                        fail("Capture ke frame khali aa rahe hain.")
                        return false
                    }
                } else {
                    contentSeen = true
                }
            }
            return try {
                e.addBitmap(bmp)
                true
            } catch (ex: Exception) {
                fail("Frame encode fail: ${ex.message}")
                false
            }
        }

        @JavascriptInterface
        fun audioBegin(stem: String, samples: Int, rate: Int) {
            synchronized(stemBufs) { stemBufs[stem] = Stem(ShortArray(samples), rate) }
        }

        @JavascriptInterface
        fun audioChunk(stem: String, b64: String) {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val st = synchronized(stemBufs) { stemBufs[stem] } ?: return
            val n = bytes.size / 2
            for (i in 0 until n) {
                if (st.pos >= st.data.size) break
                st.data[st.pos++] = ((bytes[2 * i].toInt() and 0xFF) or (bytes[2 * i + 1].toInt() shl 8)).toShort()
            }
        }

        @JavascriptInterface
        fun audioInfo(stem: String, v: Double, count: Int) {
            synchronized(this@MainActivity.audioInfo) {
                this@MainActivity.audioInfo[stem] = v
                this@MainActivity.audioInfo[stem + "_n"] = count.toDouble()
            }
        }

        @JavascriptInterface
        fun audioDone() {
            audioLatch?.countDown()
        }

        @JavascriptInterface
        fun audioFail(msg: String) {
            audioErr = msg
            audioLatch?.countDown()
        }

        @JavascriptInterface
        fun speakPreview(text: String, sceneLabel: String) {
            runOnUiThread {
                if (!exporting && sceneLabel.isNotBlank()) {
                    setStatus("▶ Preview: $sceneLabel", 0)
                }
            }
            try {
                if (previewTtsReady && !exporting && vols[1] > 0) {
                    previewTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "pv_utt")
                }
            } catch (_: Exception) {}
        }

        @JavascriptInterface
        fun stopPreviewSpeech() {
            try {
                previewTts?.stop()
            } catch (_: Exception) {}
        }

        @JavascriptInterface
        fun saveFile(name: String, mime: String, b64: String) {
            Thread {
                try {
                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                    val where = saveDownload(name, if (mime.isBlank()) "application/octet-stream" else mime, bytes)
                    runOnUiThread { Toast.makeText(this@MainActivity, "Saved: $where", Toast.LENGTH_LONG).show() }
                } catch (ex: Exception) {
                    runOnUiThread { Toast.makeText(this@MainActivity, "Save fail: ${ex.message}", Toast.LENGTH_LONG).show() }
                }
            }.start()
        }
    }

    companion object {
        private val H2V_JS = """(function(){
if(window.__h2v)return 'exists';
window.__h2v={
 on:function(pt,fl){
  try{
   var st=document.getElementById('h2v-style');
   if(!st){st=document.createElement('style');st.id='h2v-style';document.head.appendChild(st)}
   st.textContent='*,body{margin:0!important;padding:0!important;background:#000!important;overflow:hidden!important;height:100%!important}.h2v-hide{display:none!important}.h2v-path{display:block!important;margin:0!important;padding:0!important;max-width:none!important;width:100%!important;border:0!important;background:transparent!important}.h2v-cv{display:block!important;width:100vw!important;height:56.25vw!important;max-height:100vh!important;border-radius:0!important;margin:0!important;aspect-ratio:auto!important}';
   if(pt){st.textContent+='.h2v-cv{width:100vw!important;height:100vh!important;object-fit:'+(fl?'cover':'contain')+'!important}'}
   var c=document.getElementById('cv')||document.querySelector('canvas');
   if(!c)return 'nocanvas';
   var p=c;
   while(p&&p!==document.body){
    if(p.dataset&&p.dataset.h2vh===undefined)p.dataset.h2vh=p.hidden?'1':'0';
    p.hidden=false;p.classList.add('h2v-path');
    var par=p.parentElement;
    if(par){for(var i=0;i<par.children.length;i++){var s=par.children[i];if(s!==p&&!/^(SCRIPT|STYLE|LINK|META)${'$'}/.test(s.tagName))s.classList.add('h2v-hide')}}
    p=par;
   }
   c.classList.add('h2v-cv');
   window.scrollTo(0,0);
   if(typeof buildPlan==='function'&&typeof frameAt==='function'&&window.S&&window.S.length&&typeof TOTAL!=='undefined'){buildPlan();frameAt(Math.min(2.5,Math.max(0,TOTAL-0.05)))}
   return 'ok';
  }catch(e){return 'err:'+e.message}
 },
 off:function(){
  try{
   var st=document.getElementById('h2v-style');if(st)st.textContent='';
   document.querySelectorAll('.h2v-hide').forEach(function(e){e.classList.remove('h2v-hide')});
   document.querySelectorAll('.h2v-path').forEach(function(e){e.classList.remove('h2v-path');if(e.dataset&&e.dataset.h2vh!==undefined){e.hidden=(e.dataset.h2vh==='1');delete e.dataset.h2vh}});
   var c=document.getElementById('cv')||document.querySelector('canvas');if(c)c.classList.remove('h2v-cv');
   return 'ok';
  }catch(e){return 'err:'+e.message}
 }
};
if(!window.__h2vDl){window.__h2vDl=1;
 var oc=HTMLAnchorElement.prototype.click;
 HTMLAnchorElement.prototype.click=function(){
  var a=this;
  if(a.download&&a.href&&a.href.indexOf('blob:')===0&&window.Android&&Android.saveFile){
   fetch(a.href).then(function(r){return r.blob()}).then(function(b){
    var fr=new FileReader();
    fr.onload=function(){Android.saveFile(a.download,b.type||'application/octet-stream',String(fr.result).split(',')[1])};
    fr.readAsDataURL(b);
   });
   return;
  }
  return oc.apply(a,arguments);
 };
}
return 'defined';
})()"""
    }
}
