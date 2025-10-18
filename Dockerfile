FROM eclipse-temurin:21-jre AS base

# 1) 시스템 패키지 (ffmpeg + 파이썬 + 네이티브 런타임)
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg python3 python3-venv python3-pip ca-certificates curl \
    libsndfile1 libgomp1 \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 2) 파이썬 의존성 먼저 (레이어 캐시 활용)
COPY requirements.txt /app/requirements.txt

ENV VENV_PATH=/opt/venv
# pip 업그레이드 + requirements 설치 (CPU용 Torch 인덱스 그대로 사용)
RUN python3 -m venv $VENV_PATH \
 && . $VENV_PATH/bin/activate \
 && pip install --upgrade pip \
 && pip install --no-cache-dir -r /app/requirements.txt \
       --extra-index-url https://download.pytorch.org/whl/cpu \
 # 3) 빌드 타임 임포트 체킹(여기서 실패하면 이미지 빌드가 멈춰 원인 바로 파악 가능)
 && python - <<'PY'
mods = ["boto3","torch","ctranslate2","faster_whisper","soundfile","numpy"]
import importlib; [importlib.import_module(m) for m in mods]
print("== PY DEPS OK ==")
PY

# 4) 파이썬 워커 스크립트 포함
COPY --chown=10001:10001 stt_worker /app/stt_worker

# 5) 앱 JAR
COPY --chown=10001:10001 build/libs/app.jar /app/app.jar

# 6) 비루트 유저
RUN useradd -m -u 10001 appuser
USER appuser

# 7) venv PATH
ENV PATH="$VENV_PATH/bin:$PATH"
ENV PYTHONUNBUFFERED=1

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
