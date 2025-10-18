FROM eclipse-temurin:21-jre AS base  

# ffmpeg + python + pip + venv
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg python3 python3-venv python3-pip ca-certificates curl \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# requirements 먼저 복사 (캐시 최적화)
COPY requirements.txt /app/requirements.txt

# venv 생성 + torch CPU 인덱스 지정 설치
ENV VENV_PATH=/opt/venv
RUN python3 -m venv $VENV_PATH \
 && . $VENV_PATH/bin/activate \
 && pip install --upgrade pip \
 && pip install --no-cache-dir -r /app/requirements.txt \
    --extra-index-url https://download.pytorch.org/whl/cpu

# 앱 JAR 복사
COPY --chown=10001:10001 build/libs/app.jar /app/app.jar

# 비루트 유저
RUN useradd -m -u 10001 appuser
USER appuser

# venv PATH 등록
ENV PATH="$VENV_PATH/bin:$PATH"

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]


