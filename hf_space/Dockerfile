FROM python:3.10-slim
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg git && rm -rf /var/lib/apt/lists/*
RUN useradd -m -u 1000 user
USER user
ENV PATH="/home/user/.local/bin:$PATH"
WORKDIR /home/user/app
COPY --chown=user requirements.txt .
RUN pip install --no-cache-dir torch torchaudio --index-url https://download.pytorch.org/whl/cpu \
 && pip install --no-cache-dir -r requirements.txt \
 && pip install --no-cache-dir --no-deps git+https://github.com/myshell-ai/OpenVoice.git
COPY --chown=user . .
CMD ["python", "app.py"]
