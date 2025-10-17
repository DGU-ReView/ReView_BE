FROM eclipse-temurin:21-jre-alpine

# ffmpeg 설치
RUN apk add --no-cache ffmpeg

WORKDIR /app

# 앱 JAR 복사
COPY --chown=10001:10001 build/libs/app.jar /app/app.jar

# 비루트 유저 생성
RUN adduser -D -u 10001 appuser
USER appuser

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

