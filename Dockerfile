FROM eclipse-temurin:17-jre-alpine
LABEL authors="coffee13102006"

WORKDIR /app

# Копируем правильный JAR из target
COPY target/AIS_SST-0.0.1-SNAPSHOT.jar app.jar

# Копируем миграции
COPY src/main/resources/db /app/BOOT-INF/classes/db/

ENTRYPOINT ["java", "-jar", "app.jar"]