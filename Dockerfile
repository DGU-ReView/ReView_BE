FROM eclipse-temurin:21-jre-alpine

# ffmpeg + python + pip
RUN apk add --no-cache ffmpeg python3 py3-pip

WORKDIR /app

# 의존성 캐시 최적화를 위해 requirements만 먼저 복사
COPY requirements.txt /app/requirements.txt

# 가상환경에 설치 — 깔끔하고 PATH로 노출
RUN python3 -m venv /opt/venv \
 && . /opt/venv/bin/activate \
 && pip install --no-cache-dir -r /app/requirements.txt

# 앱 JAR 복사
COPY --chown=10001:10001 build/libs/app.jar /app/app.jar

# 비루트 유저
RUN adduser -D -u 10001 appuser
USER appuser

# 가상환경 PATH 등록
ENV PATH="/opt/venv/bin:${PATH}"

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

