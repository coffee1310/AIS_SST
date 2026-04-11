#!/bin/bash

echo "🔥 ПОЛНАЯ ПЕРЕСБОРКА С НУЛЯ 🔥"

# 1. Остановить всё
echo "[1/7] Stopping everything..."
docker compose down -v
docker stop $(docker ps -aq) 2>/dev/null || true
docker rm $(docker ps -aq) 2>/dev/null || true

# 2. Удалить образы
echo "[2/7] Removing images..."
docker rmi coffee1310/ais-sst-app:latest 2>/dev/null || true
docker rmi $(docker images -q) 2>/dev/null || true

# 3. Полная очистка Docker
echo "[3/7] Cleaning Docker cache..."
docker system prune -a --volumes -f
docker builder prune -a -f

# 4. Удалить папку target (важно!)
echo "[4/7] Deleting target folder..."
rm -rf target/

# 5. Пересобрать Maven с нуля
echo "[5/7] Building JAR from scratch..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Maven build failed!"
    exit 1
fi

# 6. Собрать Docker без кэша
echo "[6/7] Building Docker image (no cache)..."
docker build --no-cache -t coffee1310/ais-sst-app:latest .

if [ $? -ne 0 ]; then
    echo "❌ Docker build failed!"
    exit 1
fi

# 7. Запустить
echo "[7/7] Starting containers..."
docker compose up -d

echo "✅ Complete! Checking logs..."
sleep 5
docker compose logs --tail=30 app