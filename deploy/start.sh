#!/bin/bash

# 网上商城系统 - 快速启动脚本

set -e

echo "================================================"
echo "  网上商城系统 - 快速启动"
echo "================================================"

# Step 1: Start infrastructure (MySQL, Redis, Nacos)
echo ""
echo "[1/5] Starting infrastructure services..."
docker compose -f deploy/docker-compose.yml up -d nacos mysql redis
echo "Waiting for MySQL and Nacos to initialize..."
sleep 20

# Step 2: Build backend services
echo ""
echo "[2/5] Building backend services..."
cd backend
mvn clean package -DskipTests -q
cd ..

# Step 3: Start all services via Docker Compose
echo ""
echo "[3/5] Starting all services..."
docker compose -f deploy/docker-compose.yml up -d

# Step 4: Install frontend dependencies
echo ""
echo "[4/5] Installing frontend dependencies..."
cd frontend/client
npm install --silent
cd ../admin
npm install --silent
cd ../..

# Step 5: Start frontend dev servers
echo ""
echo "[5/5] Starting frontend dev servers..."
cd frontend/client
npm run dev &
cd ../admin
npm run dev &
cd ../..

echo ""
echo "================================================"
echo "  System is starting up!"
echo "  Client:  http://localhost:5173"
echo "  Admin:   http://localhost:5174"
echo "  Gateway: http://localhost:8080"
echo "  Nacos:   http://localhost:8848/nacos"
echo "================================================"
