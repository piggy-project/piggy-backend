FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# 1. 빌드된 jar 파일 복사
COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]