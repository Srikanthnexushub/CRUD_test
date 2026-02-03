#!/bin/bash

# System Health Check Script

echo "========================================"
echo "  System Status Check"
echo "========================================"
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

check_service() {
    if $2; then
        echo -e "${GREEN}✓${NC} $1: Running"
        return 0
    else
        echo -e "${RED}✗${NC} $1: Not Running"
        return 1
    fi
}

check_warning() {
    if $2; then
        echo -e "${YELLOW}⚠${NC} $1"
    fi
}

all_good=true

# Check Colima
if colima status > /dev/null 2>&1; then
    check_service "Colima (Docker Runtime)" true
else
    check_service "Colima (Docker Runtime)" false
    echo "  → Run: colima start"
    all_good=false
fi

# Check Docker
if docker info > /dev/null 2>&1; then
    check_service "Docker Daemon" true
else
    check_service "Docker Daemon" false
    echo "  → Run: colima start"
    all_good=false
fi

# Check PostgreSQL
if docker ps --filter "name=crud_test_postgres" --filter "status=running" | grep -q crud_test_postgres; then
    if docker ps --filter "name=crud_test_postgres" --filter "health=healthy" | grep -q crud_test_postgres; then
        check_service "PostgreSQL Database" true
    else
        echo -e "${YELLOW}⚠${NC} PostgreSQL Database: Starting (not healthy yet)"
        all_good=false
    fi
else
    check_service "PostgreSQL Database" false
    echo "  → Run: docker-compose up -d"
    all_good=false
fi

# Check Backend
if lsof -ti:8080 > /dev/null 2>&1; then
    check_service "Backend (Spring Boot)" true

    # Try to hit health endpoint
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "  → Health endpoint responding"
    else
        echo -e "${YELLOW}  → Warning: Health endpoint not responding${NC}"
    fi
else
    check_service "Backend (Spring Boot)" false
    echo "  → Run in IntelliJ or: mvn spring-boot:run"
fi

# Check Frontend
if lsof -ti:5173 > /dev/null 2>&1; then
    check_service "Frontend (React)" true
else
    check_service "Frontend (React)" false
    echo "  → Run: cd frontend && npm run dev"
fi

echo ""
echo "========================================"
if [ "$all_good" = true ]; then
    echo -e "${GREEN}All critical services are running!${NC}"
else
    echo -e "${YELLOW}Some services need attention.${NC}"
    echo ""
    echo "Quick fix: Run ./startup.sh"
fi
echo "========================================"
echo ""
