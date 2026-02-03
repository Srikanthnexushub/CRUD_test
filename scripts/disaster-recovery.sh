#!/bin/bash

################################################################################
# Disaster Recovery Script
# Complete system restore including database, configuration, and application
################################################################################

set -e  # Exit on error
set -u  # Exit on undefined variable

# Configuration
BACKUP_DIR="${BACKUP_DIR:-/var/backups/crud_app}"
RESTORE_DIR="${RESTORE_DIR:-/tmp/restore_$(date +%Y%m%d_%H%M%S)}"
LOG_FILE="${RESTORE_DIR}/disaster_recovery.log"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Create restore directory
mkdir -p "$RESTORE_DIR"

# Logging
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$LOG_FILE"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$LOG_FILE"
}

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$LOG_FILE"
}

# Usage
usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Disaster Recovery - Complete system restore

OPTIONS:
    --backup-dir DIR        Backup directory (default: /var/backups/crud_app)
    --restore-db            Restore database
    --restore-config        Restore configuration files
    --restore-app           Restore application code
    --full                  Full system restore (all components)
    --help                  Show this help message

EXAMPLES:
    # Full system restore
    $0 --full

    # Restore only database
    $0 --restore-db

    # Restore database and config
    $0 --restore-db --restore-config

EOF
    exit 1
}

# Parse arguments
RESTORE_DB=false
RESTORE_CONFIG=false
RESTORE_APP=false

if [ $# -eq 0 ]; then
    usage
fi

while [[ $# -gt 0 ]]; do
    case $1 in
        --backup-dir)
            BACKUP_DIR="$2"
            shift 2
            ;;
        --restore-db)
            RESTORE_DB=true
            shift
            ;;
        --restore-config)
            RESTORE_CONFIG=true
            shift
            ;;
        --restore-app)
            RESTORE_APP=true
            shift
            ;;
        --full)
            RESTORE_DB=true
            RESTORE_CONFIG=true
            RESTORE_APP=true
            shift
            ;;
        --help)
            usage
            ;;
        *)
            log_error "Unknown option: $1"
            usage
            ;;
    esac
done

log "=========================================="
log "DISASTER RECOVERY PROCESS"
log "=========================================="
log_info "Backup directory: $BACKUP_DIR"
log_info "Restore directory: $RESTORE_DIR"
log_info "Components to restore:"
log_info "  - Database: $RESTORE_DB"
log_info "  - Configuration: $RESTORE_CONFIG"
log_info "  - Application: $RESTORE_APP"
log "=========================================="

# Confirmation
echo ""
log_warning "⚠️  DISASTER RECOVERY MODE"
log_warning "⚠️  This will restore system from backups"
echo ""
read -p "Do you want to proceed? (yes/no): " -r
echo ""
if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    log "Recovery cancelled by user"
    exit 0
fi

# Step 1: Restore Database
if [ "$RESTORE_DB" = true ]; then
    log "=========================================="
    log "STEP 1: Database Restore"
    log "=========================================="

    # Find latest database backup
    LATEST_DB_BACKUP=$(ls -t "$BACKUP_DIR"/db/crud_test_db_*.sql.gz 2>/dev/null | head -1)

    if [ -z "$LATEST_DB_BACKUP" ]; then
        log_error "No database backup found in $BACKUP_DIR/db/"
        exit 1
    fi

    log_info "Latest backup: $LATEST_DB_BACKUP"

    # Call restore script
    if ./scripts/restore-database.sh --file "$LATEST_DB_BACKUP" --no-confirm; then
        log_success "Database restored successfully"
    else
        log_error "Database restore failed"
        exit 1
    fi
fi

# Step 2: Restore Configuration
if [ "$RESTORE_CONFIG" = true ]; then
    log "=========================================="
    log "STEP 2: Configuration Restore"
    log "=========================================="

    CONFIG_BACKUP="$BACKUP_DIR/config/application_config_latest.tar.gz"

    if [ ! -f "$CONFIG_BACKUP" ]; then
        log_error "Configuration backup not found: $CONFIG_BACKUP"
        exit 1
    fi

    log "Extracting configuration..."
    tar -xzf "$CONFIG_BACKUP" -C "$RESTORE_DIR"

    log "Restoring configuration files..."
    cp -r "$RESTORE_DIR"/config/* src/main/resources/ 2>> "$LOG_FILE" || true
    cp "$RESTORE_DIR"/.env .env 2>> "$LOG_FILE" || true

    log_success "Configuration restored successfully"
fi

# Step 3: Restore Application
if [ "$RESTORE_APP" = true ]; then
    log "=========================================="
    log "STEP 3: Application Restore"
    log "=========================================="

    APP_BACKUP="$BACKUP_DIR/app/application_code_latest.tar.gz"

    if [ ! -f "$APP_BACKUP" ]; then
        log_warning "Application backup not found. Skipping..."
    else
        log "Extracting application code..."
        tar -xzf "$APP_BACKUP" -C "$RESTORE_DIR"

        log "Restoring application files..."
        cp -r "$RESTORE_DIR"/app/* ./ 2>> "$LOG_FILE" || true

        log_success "Application code restored successfully"
    fi
fi

# Step 4: Post-Restore Verification
log "=========================================="
log "STEP 4: Post-Restore Verification"
log "=========================================="

# Verify database connection
log "Verifying database connection..."
if PGPASSWORD="${DB_PASSWORD}" psql -h localhost -U postgres -d crud_test_db -c "SELECT 1;" &> /dev/null; then
    log_success "Database connection verified"
else
    log_warning "Database connection failed"
fi

# Verify configuration files
log "Verifying configuration files..."
if [ -f "src/main/resources/application.properties" ]; then
    log_success "Configuration files present"
else
    log_warning "Configuration files missing"
fi

# Step 5: Restart Services
log "=========================================="
log "STEP 5: Service Restart"
log "=========================================="

log "Stopping application..."
./shutdown.sh 2>> "$LOG_FILE" || true

log "Starting application..."
if ./startup.sh >> "$LOG_FILE" 2>&1; then
    log_success "Application started successfully"
else
    log_warning "Application startup may have issues. Check logs."
fi

# Step 6: Health Check
log "=========================================="
log "STEP 6: Health Check"
log "=========================================="

log "Waiting for application to start (30 seconds)..."
sleep 30

log "Checking application health..."
if curl -f http://localhost:8080/actuator/health &> /dev/null; then
    log_success "Application is healthy"
else
    log_warning "Application health check failed"
fi

# Step 7: Generate Recovery Report
log "=========================================="
log "STEP 7: Recovery Report"
log "=========================================="

REPORT_FILE="${RESTORE_DIR}/recovery_report.txt"
cat > "$REPORT_FILE" << EOF
===================================================================
DISASTER RECOVERY REPORT
===================================================================

Recovery Date: $(date)
Backup Directory: $BACKUP_DIR
Restore Directory: $RESTORE_DIR

Components Restored:
  - Database: $RESTORE_DB
  - Configuration: $RESTORE_CONFIG
  - Application: $RESTORE_APP

Database Backup: $LATEST_DB_BACKUP
Configuration Backup: $CONFIG_BACKUP
Application Backup: $APP_BACKUP

Recovery Log: $LOG_FILE

Post-Recovery Checklist:
  [ ] Verify database integrity
  [ ] Check application functionality
  [ ] Test user authentication
  [ ] Verify API endpoints
  [ ] Check email notifications
  [ ] Review audit logs
  [ ] Test MFA functionality
  [ ] Verify scheduled tasks
  [ ] Check monitoring dashboards
  [ ] Update DNS if needed
  [ ] Notify stakeholders

===================================================================
EOF

cat "$REPORT_FILE" | tee -a "$LOG_FILE"

log "=========================================="
log_success "DISASTER RECOVERY COMPLETED"
log "=========================================="
log_info "Recovery report: $REPORT_FILE"
log_info "Recovery log: $LOG_FILE"
log ""
log_warning "IMPORTANT: Please verify all functionality before declaring recovery complete!"

exit 0
