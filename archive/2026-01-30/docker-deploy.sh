#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project name
PROJECT_NAME="crud-test"

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}    CRUD Test Application - Docker Deployment${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# Step 1: Clean up existing containers
echo -e "${YELLOW}[1/6] Cleaning up existing containers...${NC}"
docker-compose down --remove-orphans 2>/dev/null || true
docker rm -f crud-test-app crud-test-postgres 2>/dev/null || true
echo -e "${GREEN}✓ Cleanup complete${NC}"
echo ""

# Step 2: Clean up volumes (optional, preserves data if commented out)
# echo -e "${YELLOW}[2/6] Cleaning up volumes...${NC}"
# docker volume rm crud-test-postgres-data 2>/dev/null || true
# echo -e "${GREEN}✓ Volumes cleaned${NC}"
# echo ""

# Step 3: Build application
echo -e "${YELLOW}[2/6] Building Docker images...${NC}"
docker-compose build --no-cache
echo -e "${GREEN}✓ Build complete${NC}"
echo ""

# Step 4: Start services
echo -e "${YELLOW}[3/6] Starting services...${NC}"
docker-compose up -d
echo -e "${GREEN}✓ Services started${NC}"
echo ""

# Step 5: Wait for database to be ready
echo -e "${YELLOW}[4/6] Waiting for PostgreSQL to be healthy...${NC}"
timeout=60
counter=0
while [ $counter -lt $timeout ]; do
    if docker exec crud-test-postgres pg_isready -U postgres -d crud_test_db > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PostgreSQL is ready${NC}"
        break
    fi
    echo -n "."
    sleep 2
    counter=$((counter + 2))
done

if [ $counter -ge $timeout ]; then
    echo -e "${RED}✗ PostgreSQL failed to start within ${timeout}s${NC}"
    docker-compose logs postgres
    exit 1
fi
echo ""

# Step 6: Wait for application to be ready
echo -e "${YELLOW}[5/6] Waiting for Application to be healthy...${NC}"
timeout=120
counter=0
while [ $counter -lt $timeout ]; do
    if docker exec crud-test-app wget --quiet --tries=1 --spider http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Application is ready${NC}"
        break
    fi
    echo -n "."
    sleep 3
    counter=$((counter + 3))
done

if [ $counter -ge $timeout ]; then
    echo -e "${RED}✗ Application failed to start within ${timeout}s${NC}"
    echo -e "${YELLOW}Application logs:${NC}"
    docker-compose logs app
    exit 1
fi
echo ""

# Step 7: Verify all services
echo -e "${YELLOW}[6/6] Verifying services...${NC}"
echo ""

# Display service status
echo -e "${BLUE}Service Status:${NC}"
echo "================================================"
docker-compose ps
echo ""

# Display health status
echo -e "${BLUE}Health Checks:${NC}"
echo "================================================"
echo -e "PostgreSQL Health:"
docker exec crud-test-postgres pg_isready -U postgres -d crud_test_db
echo ""
echo -e "Application Health:"
curl -s http://localhost:8080/actuator/health | python3 -m json.tool 2>/dev/null || curl -s http://localhost:8080/actuator/health
echo ""

# Display network info
echo -e "${BLUE}Network Configuration:${NC}"
echo "================================================"
echo "Application: http://localhost:8080"
echo "Health Check: http://localhost:8080/actuator/health"
echo "PostgreSQL: localhost:5432"
echo ""

# Display logs location
echo -e "${BLUE}Logs:${NC}"
echo "================================================"
echo "View application logs: docker-compose logs -f app"
echo "View database logs: docker-compose logs -f postgres"
echo "View all logs: docker-compose logs -f"
echo ""

echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}   ✓ Deployment Complete - All Services Ready${NC}"
echo -e "${GREEN}================================================${NC}"
