"""HTML2Video "My Voice" cloud server.
Edge TTS (Hinglish base voice) -> OpenVoice v2 tone-colour convert -> 48 kHz mono WAV.
Env: API_TOKEN (secret, recommended), BASE_VOICE (default en-IN-PrabhatNeural), STUB=1 (sirf test: sine-tone WAV deta hai).
"""
import asyncio, hashlib, io, math, os, struct, subprocess, tempfile, threading, wave

import uvicorn
from fastapi import FastAPI, File, Header, HTTPException, Request, UploadFile
from fastapi.responses import JSONResponse, Response

STUB = os.environ.get("STUB") == "1"
API_TOKEN = os.environ.get("API_TOKEN", "")
BASE_VOICE = os.environ.get("BASE_VOICE", "en-IN-PrabhatNeural")
DATA = os.environ.get("DATA_DIR", "voices")
os.makedirs(DATA, exist_ok=True)

app = FastAPI()
LOCK = threading.Lock()          # ek time par ek hi conversion (free CPU)
_ov = {}                         # lazily loaded OpenVoice objects
_voices = {}                     # voice_id -> target_se (memory cache)
_src_se = {}                     # base voice -> source_se


def check(tok: str):
    if API_TOKEN and tok != API_TOKEN:
        raise HTTPException(401, "bad token")


def ff(args):
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error"] + args, check=True)


def sine_wav(seconds: float) -> bytes:
    sr = 48000
    n = int(sr * seconds)
    buf = io.BytesIO()
    with wave.open(buf, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(sr)
        w.writeframes(b"".join(struct.pack("<h", int(9000 * math.sin(2 * math.pi * 220 * i / sr))) for i in range(n)))
    return buf.getvalue()


def init_ov():
    if _ov:
        return
    import torch
    from huggingface_hub import snapshot_download
    from openvoice import se_extractor
    from openvoice.api import ToneColorConverter
    ck = snapshot_download(repo_id="myshell-ai/OpenVoiceV2", local_dir="checkpoints_v2")
    conv = ToneColorConverter(os.path.join(ck, "converter", "config.json"), device="cpu", enable_watermark=False)
    conv.load_ckpt(os.path.join(ck, "converter", "checkpoint.pth"))
    _ov.update(torch=torch, se=se_extractor, conv=conv)


def load_voice(voice_id: str):
    if voice_id in _voices:
        return _voices[voice_id]
    p = os.path.join(DATA, voice_id + ".pth")
    if os.path.exists(p):
        init_ov()
        _voices[voice_id] = _ov["torch"].load(p, map_location="cpu")
        return _voices[voice_id]
    return None


async def edge_wav(text: str, voice: str, rate: str, out_wav: str):
    import edge_tts
    mp3 = out_wav + ".mp3"
    await edge_tts.Communicate(text, voice, rate=rate).save(mp3)
    ff(["-i", mp3, "-ac", "1", "-ar", "24000", out_wav])


async def source_se(voice: str):
    """Base voice ki apni tone (ek baar nikal kar cache)."""
    if voice in _src_se:
        return _src_se[voice]
    with tempfile.TemporaryDirectory() as d:
        ref = os.path.join(d, "base_ref.wav")
        txt = ("Namaste dosto, aaj hum ek naya video banane wale hain. Is video mein hum step by step seekhenge ki "
               "application kaise banti hai, kaise configure hoti hai, aur kaise test ki jaati hai. "
               "Agar aapko video pasand aaye to like karein aur channel ko subscribe zaroor karein.")
        await edge_wav(txt, voice, "+0%", ref)
        init_ov()
        with LOCK:
            se, _ = _ov["se"].get_se(ref, _ov["conv"], target_dir=os.path.join(d, "se"), vad=True)
    _src_se[voice] = se
    return se


@app.get("/")
def root():
    return {"ok": True, "name": "HTML2Video My Voice server"}


@app.get("/health")
def health(x_token: str = Header(default="")):
    check(x_token)          # Test button se token bhi verify ho jata hai
    return {"ok": True, "stub": STUB, "token_required": bool(API_TOKEN)}


@app.post("/register")
async def register(sample: UploadFile = File(...), x_token: str = Header(default="")):
    check(x_token)
    raw = await sample.read()
    if len(raw) < 20000:
        raise HTTPException(400, "sample bahut chhota hai (1-3 min ka sample do)")
    vid = hashlib.sha1(raw).hexdigest()[:16]
    if STUB:
        _voices[vid] = True
        return {"voice_id": vid}
    if load_voice(vid) is not None:
        return {"voice_id": vid}
    with tempfile.TemporaryDirectory() as d:
        src = os.path.join(d, "in.bin")
        open(src, "wb").write(raw)
        ref = os.path.join(d, "ref.wav")
        ff(["-i", src, "-ac", "1", "-ar", "22050", ref])
        init_ov()
        try:
            with LOCK:
                se, _ = _ov["se"].get_se(ref, _ov["conv"], target_dir=os.path.join(d, "se"), vad=True)
        except Exception as e:
            raise HTTPException(500, "voice sample process nahi hua: " + str(e)[:150])
    _ov["torch"].save(se, os.path.join(DATA, vid + ".pth"))
    _voices[vid] = se
    return {"voice_id": vid}


def convert_blocking(base_wav: str, src_se, tgt_se, out_wav: str):
    with LOCK:
        _ov["conv"].convert(audio_src_path=base_wav, src_se=src_se, tgt_se=tgt_se, output_path=out_wav, message="@MyVoice")


@app.post("/synth")
async def synth(req: Request, x_token: str = Header(default="")):
    check(x_token)
    body = await req.json()
    vid = str(body.get("voice_id", ""))
    text = str(body.get("text", "")).strip()
    if not text:
        raise HTTPException(400, "text khali hai")
    voice = str(body.get("base_voice") or BASE_VOICE)
    rate = str(body.get("rate") or "+0%")
    if STUB:
        if vid not in _voices:
            raise HTTPException(404, "unknown voice")
        return Response(sine_wav(min(20.0, 0.5 + len(text) * 0.05)), media_type="audio/wav")
    tgt = load_voice(vid)
    if tgt is None:
        raise HTTPException(404, "unknown voice")
    with tempfile.TemporaryDirectory() as d:
        base = os.path.join(d, "base.wav")
        out = os.path.join(d, "out.wav")
        fin = os.path.join(d, "final.wav")
        try:
            await edge_wav(text, voice, rate, base)
            src = await source_se(voice)
            await asyncio.to_thread(convert_blocking, base, src, tgt, out)
            ff(["-i", out, "-ac", "1", "-ar", "48000", "-sample_fmt", "s16", fin])
        except Exception as e:
            raise HTTPException(500, "synth fail: " + str(e)[:150])
        return Response(open(fin, "rb").read(), media_type="audio/wav")


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=int(os.environ.get("PORT", "7860")))
