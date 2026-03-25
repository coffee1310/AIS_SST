# Остановить контейнер
docker stop java-ais-sst

# Удалить контейнер
docker rm java-ais-sst

# 1. Собрать JAR (пропуская тесты)
mvn clean package -DskipTests

# 2. Очистить старые контейнеры
docker compose down

# 3. Пересобрать образ и запустить
docker compose up -d --build

Для выгрузки в докер хаб
# Собрать образ с вашим username
docker build -t coffee1310/ais-sst-app:latest .

# 4. Посмотреть логи
docker compose logs -f app