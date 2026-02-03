#!/bin/bash

# CRUD Test Application - Shutdown Script
# This script stops all services cleanly

echo "========================================"
echo "  CRUD Test Application Shutdown"
echo "========================================"
echo ""

GREEN='\033[0;32m'
NC='\033[0m'

print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

# Stop backend if running
if lsof -ti:8080 > /dev/null 2>&1; then
    echo "Stopping backend (port 8080)..."
    kill -9 $(lsof -ti:8080) 2>/dev/null || true
    print_status "Backend stopped"
fi

# Stop frontend if running
if lsof -ti:5173 > /dev/null 2>&1; then
    echo "Stopping frontend (port 5173)..."
    kill -9 $(lsof -ti:5173) 2>/dev/null || true
    print_status "Frontend stopped"
fi

# Stop PostgreSQL container
echo "Stopping PostgreSQL..."
cd /Users/ainexusstudio/Documents/GitHub/CRUD_test
docker-compose down > /dev/null 2>&1 || true
print_status "PostgreSQL stopped"

# Optionally stop Colima (uncomment if you want to stop it)
# echo "Stopping Colima..."
# colima stop
# print_status "Colima stopped"

echo ""
echo "All services stopped!"
echo ""
