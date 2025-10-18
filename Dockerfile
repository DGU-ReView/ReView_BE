FROM eclipse-temurin:21-jre AS base

RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg python3 python3-venv python3-pip ca-certificates curl \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 파이썬 의존성 먼저
COPY requirements.txt /app/requirements.txt

ENV VENV_PATH=/opt/venv
RUN python3 -m venv $VENV_PATH \
 && . $VENV_PATH/bin/activate \
 && pip install --upgrade pip \
 && pip install --no-cache-dir -r /app/requirements.txt \
    --extra-index-url https://download.pytorch.org/whl/cpu

# 파이썬 워커 스크립트 포함
COPY --chown=10001:10001 stt_worker /app/stt_worker

# 앱 JAR
COPY --chown=10001:10001 build/libs/app.jar /app/app.jar

# 비루트
RUN useradd -m -u 10001 appuser
USER appuser

# venv PATH
ENV PATH="$VENV_PATH/bin:$PATH"
ENV PYTHONUNBUFFERED=1

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
