# NH Video Edit (v4.4)

## v4.4: Cartesia.ai (`play.cartesia.ai/text-to-speech`) All Hindi Voices API Integration
- **Cartesia.ai Sonic Hindi Engine (`CartesiaTts.kt` + Settings Page):**
  - **Settings -> VOICE-OVER & API SETTINGS** mein **`🚀 Cartesia AI`** add kar diya gaya hai (`⚡ Edge Free Neural`, `🚀 Cartesia AI`, `🎙 ElevenLabs API`, `🇮🇳 Sarvam AI`, `📱 Phone Offline TTS`).
  - **14 Pre-Loaded Cartesia Native Hindi & Sonic-3.6 Voices:** `Hindi Narrator Woman`, `Hindi Reporter Man`, `Indian Man (Hinglish)`, `Indian Lady (Hinglish)`, `Skylar`, `Daniel`, `Jacqueline`, `Gemma`, `Archie`, `Sarah`, `Newslady`, `Newsman`, `Helpful Woman`, aur `Wise Guide Man`.
  - **Fetch All Hindi Voices (`🔄 Fetch All Hindi Voices`):** Apni Cartesia API Key (`sk_car_...`) daal kar tap karte hi `https://api.cartesia.ai/voices` se aapke account ki sabhi Native Hindi (`hi`), Indian-accented, Cloned aur Sonic Multilingual voices fetch hokar list mein aa jaati hain.
  - **Live Hindi Voice Preview (`▶ Play` / `⏹ Stop`) & One-Tap `Set`:** Har voice ke samne **`▶ Play`** button se live 24kHz WAV Hindi sample sunein aur **`Set`** button se use MP4 export ke liye active karein.
  - **Sonic Model Selector:** `Sonic-3 (Best Hindi)`, `Sonic-2 (Stable)`, aur `Sonic Turbo` teeno models supported hain.

---

# NH Video Edit (v4.3)

## v4.3: Microsoft Edge Neural TTS (100% Free & Unlimited, Zero API Key) Added
- **Microsoft Edge Neural TTS (`EdgeFreeTts.kt` + Settings Page):**
  - **Settings -> VOICE-OVER & API SETTINGS** mein **`⚡ Edge Free Neural`** default #1 option ke roop mein jod diya gaya hai (`⚡ Edge Free Neural`, `🎙 ElevenLabs API`, `🇮🇳 Sarvam AI`, `📱 Phone Offline`).
  - **100% Free & No API Key Needed:** Bina kisi API key ya signup ke 14+ Microsoft Neural Hindi (`hi-IN`) aur Hinglish (`en-IN`) studio voices (`Swara`, `Madhur`, `Aarav`, `Ananya`, `Kavya`, `Kunal`, `Rehaan`, `Aarti`, `Arjun`, `Neerja`, `Prabhat`, `Aashi`) bilkul free aur unlimited chalti hain.
  - **Live Play & 1-Tap Set:** Har voice ke samne **`▶ Play`** button hai jisse aap live Hindi voice sample sun sakte hain, aur **`Set`** button se use active voice bana sakte hain.

---

# NH Video Edit (v4.2)

## v4.2: ElevenLabs.io Hindi Voice API Integration (Fetch / Play / Set All Hindi Voices)
- **ElevenLabs.io Audio Engine (`ElevenLabsTts.kt` + `AudioUtils.kt`):**
  - **Settings -> Voice-Over & API Settings** mein **`ElevenLabs`** option add kar diya gaya hai (`ElevenLabs`, `Sarvam AI`, `Phone TTS`).
  - **Fetch All Hindi Voices (`🔄 Fetch All Hindi Voices`):** Apni `elevenlabs.io` API Key (`xi-api-key`) daal kar tap karte hi aapke ElevenLabs account ki sabhi Hindi, Indian-accented, Cloned aur Multilingual v2 voices fetch hokar list mein aa jaati hain (saath hi 14 top Hindi-ready voices pehle se pre-loaded hain).
  - **Live Hindi Voice Preview (`▶ Play` / `⏹ Stop`):** List mein har voice ke samne **`▶ Play`** button diya gaya hai jisse aap export se pehle hi us voice ki live Hindi awaaz sun sakte hain.
  - **One-Tap Voice Selection (`✓ Set`):** Kisi bhi voice ke samne **`Set`** button dabate hi woh voice active set ho jaati hai aur MP4 Export mein usi ElevenLabs Hindi voice ka use hota hai.
  - **Model Choice:** `Multilingual v2 (Best)` (`eleven_multilingual_v2`) aur `Flash v2.5 (Fast)` (`eleven_flash_v2_5`) dono models supported hain, aur MP3 decoding Android ke native `MediaExtractor` + `MediaCodec` se hoti hai taaki Free aur Paid dono ElevenLabs accounts par 100% bina error kaam kare.

---

# NH Video Edit (v4.1)

## v4.1: Deep Multi-Layout Script Compiler + Live Preview Voiceover + Zero Generic Templates
- **100% Script-Faithful Multi-Scene Engine (`AiScriptBuilder.kt` v4.1):**
  - Har `.md` / `.txt` script ya prompt se uske asli headings, steps, bullet points, filenames (`.json`, `.zip`, `.apk`, `.mp4`), URLs (`console.cloud.google.com`, `github.com`, `firebase.google.com`), aur `KEY=VALUE` variables extract karta hai.
  - Prompt ke meta-notes (`IMPORTANT PROJECT NOTE:...`) ko automatically filter kar deta hai taaki woh kabhi screen ya voiceover mein na aayein.
  - **10 Distinct Animated Layouts:** Har scene apne content ke hisaab se alag layout dikhata hai (`hero_launch` 4-card pop montage, `browser_flow` desktop console + phone transition, `file_picker` bottom-sheet file selector + right checklist panel, `code_vars` live typing code editor, `auth_keys` OAuth/Keystore masked dots screen, `media_stream` 16:9 video player screen, `build_run` live progress & console logs, `error_fix` diagnostic & fix cards, `feature_ui` 2-stage search & card highlight, aur `outro_recap` 6-step summary grid + brand finale).
  - **6 Unique Project Color Palettes:** Project ke naam/topic ke hisaab se alag color theme (`Emerald/Cyan`, `Violet/Cyan`, `Crimson/Amber`, `Electric Blue/Pink`, `Gold/Sky`, `Rose/Purple`) select hoti hai.
  - **Live Preview Voiceover (`TextToSpeech` Bridge):** Ab Player screen par **`▶ Preview`** dabane par Android WebView mein real-time Hindi/Hinglish voiceover + 15 Sound Effects dono saath chalte hain!

---

# NH Video Edit (v3.8)

## v3.8: Clean Player Page, NVE / NH VIDEO EDIT Header & Zero HTML References in UI
- **Top Header Fixed (`[NVE] NH VIDEO EDIT`):** Badge ab `NVE` hai aur title `NH VIDEO EDIT` (`singleLine = true`, `16sp` bold) + `v3.8 • Studio Pro` subtitle hai jo kisi bhi phone screen par do line mein toot kar kharab nahi dikhta.
- **Home / Player Screen Cleaned:** Player screen se `Active Project / Upload` card hata diya gaya hai. Ab Player page par sirf **Video Player**, **Export Size (`16:9` / `9:16`, `30 FPS` / `60 FPS`, `Preview`, `Export MP4`, `Cancel Export`)**, aur **Export Processing (`Status` + `Progress Bar`)** dikhta hai.
- **Project Upload Moved Exclusively to Settings Page:** Project file select karna (`Select Project File`), `Pichla project dobara kholo`, aur `Studio Templates` ab sirf **Settings** page mein hain.
- **Zero HTML Text Anywhere in UI:** App UI, buttons, status messages, ya file names mein kahin bhi `HTML` ya `.html` text show nahi hota (`cleanProjectName()` apne-aap `.html` extension hata deta hai).

---

# HTML2Video v3.7

## v3.7: Monochrome Black & White Studio UI, Dedicated Settings Page, Sarvam AI + Phone Only & Premium App Icon
- **Editor Screen Removed & Dedicated Settings Page Added:** Top navigation bar mein ab `[ ▶ Player | ⚙ Settings ]` hai. Editor screen hata diya gaya hai.
- **Settings Page (Project Upload + API + Audio Mixer):**
  - **Project Upload (HTML Files):** Phone storage se `.html` file select karne ka button, pichla project kholne ka button, aur 4 built-in demo templates (`Firebase Tutorial`, `Build Tutorial`, `MovieBox Promo`, `UI/UX Explorer`). Player page par bhi quick `Upload HTML` aur `API & Settings` shortcut diya gaya hai.
  - **Voice-Over & API Configuration (Sarvam AI & Phone Only):** `Google Cloud` aur `My Voice` options hata diye gaye hain. Ab sirf **Sarvam AI (API)** (API key + speaker input + Save button ke saath) aur **Phone (Offline)** TTS dikhte hain.
  - **Audio & SFX Mixer:** Master, Voice-over, aur Sound Effects (SFX) volume sliders + Auto-duck + SFX ON/OFF toggle (Background Music 100% disabled).
- **Pure Black & White Monochrome UI & Layout Fix:** Poora interface sleek OLED Black (`#000000`), Matte Carbon Cards (`#111111`), Subtle Borders (`#2E2E2E`), aur Pure White (`#FFFFFF`) high-contrast buttons/chips/sliders mein design kiya gaya hai.
- **Premium Adaptive App Icon Added:** `mipmap-anydpi-v26/ic_launcher.xml` aur `ic_launcher_round.xml` ke saath vector foreground, background, aur monochrome app icon (`AndroidManifest.xml` mein wired) jod diya gaya hai.

---

# HTML2Video v3.6

## v3.6: All Sound Effects (Typing, Popup, Slide, Tap, Click, Whoosh) + Background Music Removed + Kotlin Build Fix
- **Fixed Kotlin Duplicate Declaration Error:** Cleaned up `btnUiUx` and `sfxEngineJs` single declarations in `MainActivity.kt`.
- **Background Music Completely Removed:** Music bed (`AutoSfx.music`, `bedSched`, `renderMusic`, `musicStart`) poori tarah hata diya gaya hai. Ab video aur preview mein sirf **Voice-over + Sound Effects (SFX)** rehte hain.
- **Universal Automatic SFX Extraction (`h2v_sfx.js` + `AutoSfx.kt`):** Is type ke har HTML code (jaise `UI/UX Name Explorer`, `Build Tutorial`, `Firebase Tutorial`, `Screenshot Studio`, ya koi bhi naya canvas/DOM HTML) se app apne-aap saare visual aur explicit sound effects detect karke voiceover timeline ke saath sync karta hai:
  - **Typing (`key` / `type`):** Search bar, license key (`NH-...`), package name, API keys, aur text fields mein har character type hone par realistic mechanical/tactile keystroke sound.
  - **Popup (`popup` / `pop`):** Cards, banners (`cbBanner`), top OSD pills (`osd`), cyan highlight boxes (`hlBox`), callout notes, aur modals pop hone par crisp UI bubble pop sound.
  - **Slide (`slide`):** Left/right `sideCard` slide-in, bottom sheets (`sheetBase`, `fixesSheet`, `pickScr`), browser views, aur theme/tab transitions par smooth air-slide + landing sound.
  - **Tap & Click (`tap`, `click`):** Mobile screen finger tap rings (`tap`, `ripple`) aur button presses (`btn`, `ghBtn`) par snappy touch/click sound.
  - **Special UI SFX (`copy`, `lock`, `notif`, `chime`, `success`, `tick`, `riser`, `hit`, `whoosh`):** Smart Lock, Copy Prompt, License Key Activation, 4K PNG Save, aur Screen In-Out (`whoosh`) transitions.

---

# HTML2Video v3.4

## v3.4: Sound effects fix
- **Built-in SFX + music (Auto SFX/Music):** volume card mein naya toggle. Jis HTML mein apna SFX/music nahi hota (jaise canvas wali Build tutorial),
  ya page ka SFX render fail/silent aaye, to app khud halka SFX (scene change par whoosh, line par tick, start/end par chime) aur music bed laga deta hai.
  Toggle OFF karke band kar sakte ho. (`AutoSfx.kt`)
- **SFX ab sunai dete hain:** SFX ka base boost (x1.5), voice ke time ducking 30% se 15%, aur SFX slider ka default 70% se 100% (purani saved 70% value bhi reset hoti hai).
- **Export ke baad audio report:** status mein likha aata hai, jaise `Audio: Music ✓, SFX ✓ (319 sounds)` ya `SFX ✗ -> built-in SFX lage`.
  Isse pata chal jata hai ki page ka SFX aaya ya nahi.
- Ek window fail ho jaye to poora export nahi rukta (us hisse mein SFX nahi, baaki bachta hai).

---

# HTML2Video v3.3

## v3.3 mein naya
- **Cloud AI voice (free):** My Voice card mein naya section. Server URL + API Token daalo, **Sample chuno** (1-3 min ki apni awaaz),
  **Server test**, phir **Cloud se awaaz banao**. App har baaki line server ko bhejta hai, aapki awaaz mein WAV leta hai aur My Voice clips mein save karta hai
  (record/import se pehle ban chuki lines skip hoti hain; dobara dabane par ruk jata hai). Phir normal Export.
- Server ki files `hf_space/` folder mein hain (Hugging Face Space, Docker, FastAPI). Setup: `hf_space/SETUP_HINDI.md`.
  Server: Edge TTS base awaaz -> OpenVoice v2 se aapki tone, `API_TOKEN` secret se protected.
- Test: app ka cloud client stub server ke saath JVM par check hua (health, register, synth, token, unknown voice, Unicode text).
  Asli OpenVoice server/Docker build yahan test nahi hua (GPU/HF access nahi).

---

# HTML2Video v3.2

## v3.2 mein naya
- **My Voice engine (apni awaaz).** Voice card mein 4th chip **My Voice**. Iske liye alag card khulta hai:
  - **Record lines:** har line ka text dikhta hai, **Record / Stop / Play / Delete**, Pichhli / Agli / Agli baaki line. Clip phone mein save hoti hai (48 kHz mono).
    Script same rahe to dobara record nahi karna padta (clip text ke hash se pehchani jati hai).
  - **Voice pack import:** zip ya audio files (wav/mp3/m4a/ogg...). File ke naam ka pehla number = line number (`001.wav` = pehli line).
  - **Script export:** `my_voice_script.json` + `.txt` Download/HTML2Video mein (AI/Colab ke liye).
  - **Clips delete:** saari My Voice clips hata do.
  - Export par agar kisi line ki awaaz nahi hai to app saaf bata deta hai kaun si lines baaki hain.
- **Colab notebook (free):** `colab/MyVoice_Colab.ipynb`. Script + aapki awaaz ka sample do, `my_voice_pack.zip` milti hai jo app mein import hoti hai.
  `convert` mode: Edge TTS base voice -> OpenVoice v2 se aapki tone. `xtts` mode: seedha clone (license non-commercial, khud check karein).
  Ye notebook yahan test nahi ho paya (GPU/Colab nahi tha). Error aaye to notebook ke end mein tips hain.
- **Lambi video ke liye audio fix:** music/SFX 60 sec ke windows mein render hota hai (pehle 12+ minute ki video par bahut slow tha; ab ~13 min ki video ka audio ~50 sec mein bana).
  Stems kam sample-rate par aate hain (music 12 kHz, SFX 24 kHz) aur mixer block-wise hai, isliye memory kam lagti hai.
- Voice volume slider My Voice par bhi lagta hai. Auto-duck bhi.

---

# HTML2Video v3.1

## v3.1 fix
- **Export video mein background ka zigzag light flicker theek kiya.** Page ka `#grain` (film grain) overlay har 0.1 second mein poori screen par
  zigzag jump karta tha; export mein har frame par noise ki position badalti thi. Ab export ke dauran adapter grain animation band rakhta hai
  (static grain rehta hai) aur export khatam hote hi wapas chalu ho jata hai. Preview/browser par page jaisa tha waisa hi rehta hai.

---

# HTML2Video v3 (Android Studio project)

## v3 mein naya kya hai
- **MovieBox jaisi scene-based HTML ab load hoti hai.** Pehle "NH API nahi mili" aata tha, kyunki woh HTML canvas nahi, DOM se bani hai.
  Ab app `assets/h2v_dom.js` adapter se us HTML ko `NH` API de deti hai (jin HTML mein `scenes`, `CAPTIONS`, `render()`, `TOTAL` hota hai).
  Canvas wali HTML (jaise Build tutorial) par adapter kuch nahi karta, woh pehle jaisi hi chalti hai.
- **Voice-over + music + sound effects ek hi MP4 mein.** HTML ka apna sound design (SFX aur background music) offline render hota hai
  aur Sarvam / Google / Phone voice-over ke saath mix hota hai.
- **Voice ke hisaab se timeline.** Har caption ki awaaz jitni lambi ho, utna us hisse ka visual slow ho jata hai, taaki awaaz aur visual sync rahein
  (isliye video 2:30 se lambi ho sakti hai).
- **Volume card:** Master, Voice-over, Music, Sound effects sliders (0 se 200%), **Auto-duck** (voice ke time music/SFX halke) aur Reset.
  Volume yaad rehti hai. Preview mein music + SFX ki volume turant badalti hai; final mix export mein lagta hai.
- Naye sample buttons: **Build tutorial** aur **MovieBox promo**.
- DOM HTML ka frame WebView se seedha 1920x1080 Bitmap mein capture hota hai (JPEG ke bina). Capture CPU par hota hai, isliye DOM HTML ke liye
  **30 fps** chuno (60 fps bahut slow hoga). Export ke dauran screen on rakho aur app ko foreground mein rakho.
- Purane sab features waise hi hain (Player/Editor, 9:16, Sarvam, Google, Phone TTS, Studio, Cancel, Share).

## DOM HTML ke liye rule (MovieBox style)
Page mein ye hona chahiye: `scenes` (har `section.scene` par `data-start`/`data-end`), `CAPTIONS` (scene id -> `[[time, text], ...]`),
`render()` (time se page ki state), `TOTAL`, aur `t`. Sound ke liye page ka `sfx`, `sfxReveal`, `CUES` use hota hai (agar ho).
Canvas wali HTML ke liye purana `window.NH` rule hi chalta hai (neeche).

---

# HTML2Video v2 (Android Studio project)

## v2 mein naya kya hai
- Premium dark UI: cards, gradient buttons, chips, progress bar aur ETA.
- **Player / Editor mode:** Player mein sirf 16:9 video canvas dikhta hai (pehle video player kat jata tha). Editor mein poora page full screen khulta hai, jahan marks aur text edit kar sakte ho.
- **Preview button:** video seedha app mein chalakar dekho.
- **Screenshot Studio** app ke andar bundled hai (zip ya screenshots load karo, marks lagao).
- Studio ke andar file chooser, mic record aur "Save HTML" ab kaam karte hain (downloads `Download/HTML2Video` mein jate hain).
- 30 ya 60 fps chuno. Pichla project ek tap mein dobara khulta hai. Export ke baad Share button.
- **9:16 Portrait (naya):** Export card mein "16:9 Landscape / 9:16 Portrait" chuno. 9:16 mein player screen bhi vertical ho jati hai aur MP4 **1080x1920** banti hai.
  - **Fit (blur background):** poora 16:9 frame beech mein, upar-neeche blur background (preview mein black bars dikhte hain, export mein blur lagta hai).
  - **Fill (crop):** poori vertical screen bharta hai, frame ke beech ka hissa rehta hai (side kat jati hai).
  - Choice yaad rehti hai. 16:9 export pehle jaisa hi hai.
- Purane sab features (Sarvam, Google, phone TTS, bina awaaz, Cancel, TTS settings) waise hi hain.


HTML animation ko **1920x1080, 60 fps MP4** video mein badalne wali Android app.
Awaaz phone ke apne Hindi TTS se seedha file mein banti hai, isliye "audio capture" wali koi dikkat nahi aati.

## Kaise kholein
1. Android Studio mein **File > Open** karke ye `HTML2Video` folder chuno.
2. Gradle sync hone do (internet chahiye). JDK 17 chahiye (Android Studio ke saath aata hai).
3. Phone (Android 10 ya upar) par Run karo. Code On the Go mein bhi build ho sakta hai.

## Kaise use karein
1. App kholo. **Sample (Firebase tutorial) kholo** dabao ya **HTML file chuno** se apni HTML do.
2. "Hindi AI awaaz" ON rakho.
3. **Export 1080p 60fps MP4** dabao. Progress aur baaki time dikhega.
4. Ho jane par video **Movies/HTML2Video** folder mein save hota hai. "Video kholo" se dekh sakte ho.

Export ke dauran screen on rakho aur app ko foreground mein rakho.
60 fps ke 7000+ frames banane mein kuch minute lagte hain (phone par depend karta hai).

## Free natural awaaz: Sarvam AI (recommended)
1. dashboard.sarvam.ai par sign up karo aur **API key** lo (signup par free credits milte hain; amount dashboard par dikhta hai).
2. App mein **"Sarvam AI: natural Hindi"** chuno, key daalo. Speaker (chhote letters mein): `ritu`, `shubh`, `priya`, `kavya`, `neha`, `rahul` ...
3. Export karo. Ek tutorial ke liye kuch hazaar characters lagte hain, to free credits mein kaafi videos ban jate hain.
Key sirf aapke phone mein save hoti hai. Kisi ko share mat karo.

## Google Cloud (optional, internet + API key)
Phone ki TTS awaaz thodi robotic hoti hai. Zyada natural awaaz ke liye app mein
**"Natural insaani awaaz (Google Cloud)"** chuno:
1. console.cloud.google.com par project banao, **Cloud Text-to-Speech API** enable karo, aur ek **API key** banao
   (billing account jodna padta hai; free monthly quota aam tutorial ke liye kaafi hota hai, limits Google ki pricing page par dekho).
2. API key app ke "Google Cloud API key" box mein daalo. Voice name: `hi-IN-Neural2-A` (female), `hi-IN-Neural2-B` (male),
   `hi-IN-Neural2-C` (male), `hi-IN-Neural2-D` (female). Naye Chirp3-HD voices Google ki voice list mein dekh sakte ho.
3. Export karo. Key sirf aapke phone mein save hoti hai. Apni key kisi ko share mat karo aur key ko sirf Text-to-Speech API tak restrict karo.

## Hindi voice nahi mili to (phone TTS)
Phone Settings > System > Language > Text-to-speech output > Google > Install voice data > Hindi.
Ya app ka "TTS settings kholo" button dabao.

## HTML ke liye zaroori rule
Aapki HTML mein ye `window.NH` object hona chahiye (master prompt ka template ise already deta hai):
- `NH.lang` (jaise "hi-IN")
- `NH.narration()` : JSON string, har step ka `{a, b}` text
- `NH.setDurations(json)` : awaaz ki length lekar timeline banata hai, `{total, events}` lauta ta hai
- `NH.run(fps)` : frames banata hai aur `Android.pushFrame(dataUrl)` ko bhejta hai
  (aakhir mein `Android.done()`, error par `Android.fail(msg)`)

## Files
- `app/src/main/java/com/nhbox/htmltovideo/MainActivity.kt` : screen, WebView, JS bridge, save
- `TtsRenderer.kt` : Hindi awaaz ko WAV mein banata hai
- `AudioUtils.kt` : WAV padhna, 48 kHz mono, mixing
- `VideoExporter.kt` : H.264 (1080p60) + AAC ko MP4 mein likhta hai
- `app/src/main/assets/sample_firebase.html` : test ke liye sample
- `app/src/main/assets/h2v_dom.js` : (v3) DOM HTML ke liye NH adapter (narration, timeline, music + SFX render, frame stepping)
- `app/src/main/assets/moviebox_promo.html`, `build_tutorial_video.html` : (v3) sample HTML
