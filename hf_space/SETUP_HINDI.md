# Hugging Face Space setup (free) — mobile se

1. **huggingface.co** par account banao / login karo (mobile Chrome, Desktop site on).
2. Upar profile ke paas **+ New → Space**:
   - Space name: `my-voice` (kuch bhi)
   - SDK: **Docker** (Blank)
   - Hardware: **CPU basic (free)**
   - Visibility: Public
   - **Create Space**.
3. Space ke **Files** tab mein **Add file → Upload files** se ye 4 files upload karo (README.md purani ko replace kar degi) aur **Commit**:
   `README.md`, `Dockerfile`, `requirements.txt`, `app.py`
4. **Settings → Variables and secrets → New secret**: Name `API_TOKEN`, Value koi lambi random string (ye app mein token ki jagah jayegi).
5. Space upar **Building...** dikhayega. Pehli build 10-20 minute le sakti hai. **Running** hone tak ruko. Error aaye to **Logs** tab ka text mujhe bhejo.
6. Space ka URL: Space page par **⋮ → Embed this Space → Direct URL**, jaisa `https://USERNAME-my-voice.hf.space`.
7. HTML2Video app: engine **My Voice** → **Server URL** aur **API Token** daalo → **Server test** → **Sample chuno** (1-3 min ki apni awaaz) → **Cloud se awaaz banao**.

Dhyan rakho:
- Free Space kuch der idle rehne par so jata hai; pehli request mein 1-2 minute lagte hain (app khud wait karta hai).
- Free CPU par har line 10-40 second le sakti hai. Pehle 5-10 line ki chhoti HTML se try karo.
- Pehli request par OpenVoice ke model download hote hain (extra time).
- Sirf apni awaaz (ya jiski permission ho) use karo.
- Ye server maine chalakar test nahi kiya (sirf stub mode + app client). Build ya conversion error aaye to Logs bhejo.
