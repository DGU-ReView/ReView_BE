FROM eclipse-temurin:21-jre AS base

# 1) System deps: Python + ffmpeg + OpenMP(runtime for ctranslate2) 등
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg python3 python3-venv python3-pip ca-certificates curl libgomp1 \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 2) Python venv + requirements 설치
#    * torch 계열을 쓰면 아래 extra-index-url 유지, faster-whisper만 쓸 거면 제거해도 됨
COPY requirements.txt /app/requirements.txt
ENV VENV_PATH=/opt/venv
RUN python3 -m venv $VENV_PATH \
 && . $VENV_PATH/bin/activate \
 && pip install --upgrade pip \
 && pip install --no-cache-dir -r /app/requirements.txt \
      --extra-index-url https://download.pytorch.org/whl/cpu

# 3) HF/XDG 캐시 경로 고정 (+ ISA 호환을 위해 GENERIC 강제 권장)
ENV HF_HOME=/app/.cache/hf
ENV XDG_CACHE_HOME=/app/.cache
ENV CT2_FORCE_CPU_ISA=GENERIC

# 4) 빌드 타임에 faster-whisper large-v3 모델을 미리 다운로드(캐시)
#    * 최초 로드 시 CTranslate2 포맷 모델이 HF 캐시에 내려받아짐
RUN . $VENV_PATH/bin/activate && python - <<'PY'
from faster_whisper import WhisperModel
# 캐시 프리페치 (CPU + int8)
WhisperModel("large-v3", device="cpu", compute_type="int8")
print("== cached faster-whisper large-v3 ==")
PY

# 5) 애플리케이션 파일 복사
#    * 워커 스크립트와 JAR
COPY --chown=10001:10001 stt_worker /app/stt_worker
COPY --chown=10001:10001 build/libs/app.jar /app/app.jar

# 6) non-root 유저 생성 + 캐시/앱 경로 권한 정리
RUN useradd -m -u 10001 appuser \
 && mkdir -p /app/.cache \
 && chown -R 10001:10001 /app

USER appuser

# 7) venv PATH 노출 (자바에서 /opt/venv/bin/python 호출 권장)
ENV PATH="$VENV_PATH/bin:$PATH"
ENV PYTHONUNBUFFERED=1

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
