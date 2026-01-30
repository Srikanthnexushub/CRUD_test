#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}    Stopping CRUD Test Application${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

echo -e "${YELLOW}Stopping containers...${NC}"
docker-compose down

echo -e "${GREEN}✓ All services stopped${NC}"
echo ""

echo -e "${YELLOW}To remove volumes (delete database data), run:${NC}"
echo "docker volume rm crud-test-postgres-data"
