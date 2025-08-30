FROM openjdk:21-ea-1-jdk-slim

WORKDIR /app

COPY build/libs/cloud-storage-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]