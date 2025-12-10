FROM eclipse-temurin:21-jdk
WORKDIR /app

COPY target/data-storage-1.0.0.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
