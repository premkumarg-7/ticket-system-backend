FROM eclipse-temurin:21-jdk
LABEL authors="Prem7"
WORKDIR /app
ADD build/libs/*.jar e2d-auth-server.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "e2d-auth-server.jar"]