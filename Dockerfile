FROM eclipse-temurin:17-jre-alpine
LABEL authors="coffee13102006"

WORKDIR /app

# Копируем JAR
COPY target/AIS_SST-0.0.1-SNAPSHOT.jar app.jar

# Создаем пользователя не-root для безопасности
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Порт для Spring (из application.properties)
EXPOSE 8080

# Запуск с профилем docker (может быть переопределен)
ENTRYPOINT ["java", "-jar", "app.jar"]