FROM eclipse-temurin:21-jre

# ffmpeg 설치 
RUN apt-get update \
 && apt-get install -y --no-install-recommends ffmpeg \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 앱 JAR 복사
COPY build/libs/app.jar app.jar

# 비루트 유저로 실행
RUN useradd -r -u 10001 appuser
USER appuser

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
