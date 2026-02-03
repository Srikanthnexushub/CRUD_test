#!/bin/bash

# CRUD Test Application - Automated Startup Script
# This script ensures all services start in the correct order

set -e  # Exit on any error

echo "========================================"
echo "  CRUD Test Application Startup"
echo "========================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_waiting() {
    echo -e "${YELLOW}⏳${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Step 1: Check if Colima is running
echo "Step 1: Checking Colima (Docker runtime)..."
if colima status > /dev/null 2>&1; then
    print_status "Colima is already running"
else
    print_waiting "Starting Colima..."
    colima start
    print_status "Colima started successfully"
fi
echo ""

# Step 2: Verify Docker is accessible
echo "Step 2: Verifying Docker daemon..."
if docker info > /dev/null 2>&1; then
    print_status "Docker daemon is accessible"
else
    print_error "Docker daemon is not accessible. Please check Colima."
    exit 1
fi
echo ""

# Step 3: Start PostgreSQL container
echo "Step 3: Starting PostgreSQL database..."
cd /Users/ainexusstudio/Documents/GitHub/CRUD_test

# Check if container exists and is running
if docker ps --filter "name=crud_test_postgres" --filter "status=running" | grep -q crud_test_postgres; then
    print_status "PostgreSQL is already running"
else
    print_waiting "Starting PostgreSQL container..."
    docker-compose up -d

    # Wait for PostgreSQL to be healthy
    print_waiting "Waiting for PostgreSQL to be healthy..."
    max_wait=30
    count=0
    while [ $count -lt $max_wait ]; do
        if docker ps --filter "name=crud_test_postgres" --filter "health=healthy" | grep -q crud_test_postgres; then
            print_status "PostgreSQL is healthy and ready"
            break
        fi
        sleep 1
        count=$((count + 1))
        echo -n "."
    done
    echo ""

    if [ $count -eq $max_wait ]; then
        print_error "PostgreSQL failed to become healthy in time"
        docker-compose logs postgres
        exit 1
    fi
fi
echo ""

# Step 4: Check if backend is already running
echo "Step 4: Checking if backend is already running..."
if lsof -ti:8080 > /dev/null 2>&1; then
    print_status "Backend is already running on port 8080"
    echo ""
    echo "========================================"
    echo "  All Services Are Ready!"
    echo "========================================"
    echo ""
    echo "Backend:  http://localhost:8080"
    echo "Database: PostgreSQL on port 5432"
    echo ""
    echo "To start frontend, run:"
    echo "  cd frontend && npm run dev"
    echo ""
    exit 0
fi
echo ""

# Step 5: Display next steps
echo "========================================"
echo "  Infrastructure Ready!"
echo "========================================"
echo ""
print_status "Colima: Running"
print_status "Docker: Running"
print_status "PostgreSQL: Running and healthy"
echo ""
echo "Next steps:"
echo "  1. Open IntelliJ IDEA"
echo "  2. Open: CrudTestApplication.java"
echo "  3. Click the green ▶️  or 🐛 icon to run/debug"
echo ""
echo "Or run from terminal:"
echo "  mvn spring-boot:run"
echo ""
echo "Then start frontend:"
echo "  cd frontend && npm run dev"
echo ""
