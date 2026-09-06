# syntax=docker/dockerfile:1.6
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY . .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw \
 && ./mvnw -B clean package -DskipTests \
 && cp "target/$(ls target | grep '\.jar$' | grep -v 'original-' | head -n1)" /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar
EXPOSE 8088
ENTRYPOINT ["java","-jar","/app/app.jar"]
