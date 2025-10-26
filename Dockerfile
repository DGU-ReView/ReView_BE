FROM eclipse-temurin:21-jre

RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    libgomp1 \
    libsndfile1 \
    libjpeg-turbo8 \
    libpng16-16 \
    zlib1g \
    libglib2.0-0 \
    python3 python3-venv python3-pip ca-certificates curl \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Python venv + requirements (torch CPU 휠 인덱스 사용)
ENV VENV_PATH=/opt/venv
COPY requirements.txt /app/requirements.txt
RUN python3 -m venv $VENV_PATH \
 && . $VENV_PATH/bin/activate \
 && pip install --upgrade pip \
 && pip install --no-cache-dir -r /app/requirements.txt \
      --extra-index-url https://download.pytorch.org/whl/cpu

# 캐시 경로
ENV HF_HOME=/app/.cache/hf
ENV XDG_CACHE_HOME=/app/.cache

# 애플리케이션 파일
COPY stt_worker /app/stt_worker
COPY build/libs/app.jar /app/app.jar

# 권한/유저
RUN useradd -m -u 10001 appuser \
 && mkdir -p /app/.cache \
 && chown -R 10001:10001 /app
USER appuser

# 런타임 ENV
ENV PATH="$VENV_PATH/bin:$PATH" \
    PYTHONUNBUFFERED=1 \
    OMP_NUM_THREADS=2 \
    MKL_NUM_THREADS=2 \
    NUMEXPR_NUM_THREADS=2

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
