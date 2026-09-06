# syntax=docker/dockerfile:1.6
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY . .
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
 && ./gradlew bootJar --no-daemon -x test \
 && cp "build/libs/$(ls build/libs | grep -v plain | head -n1)" /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
