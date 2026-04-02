# 1단계: 빌드 환경 설정
FROM openjdk:17-jdk-slim AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar

# 2단계: 실행 환경 설정
FROM openjdk:17-jdk-slim
WORKDIR /app
# 빌드 단계에서 생성된 jar 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
