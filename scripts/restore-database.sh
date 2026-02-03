#!/bin/bash

################################################################################
# Database Restore Script
# Restores PostgreSQL database from backup
################################################################################

set -e  # Exit on error
set -u  # Exit on undefined variable

# Configuration
BACKUP_DIR="${BACKUP_DIR:-/var/backups/postgres}"
DB_NAME="${DB_NAME:-crud_test_db}"
DB_USER="${DB_USER:-postgres}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
LOG_FILE="${BACKUP_DIR}/restore.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
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

# Usage function
usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Restore PostgreSQL database from backup

OPTIONS:
    -f, --file BACKUP_FILE    Backup file to restore (required)
    -d, --database DB_NAME    Database name (default: crud_test_db)
    -h, --host DB_HOST        Database host (default: localhost)
    -p, --port DB_PORT        Database port (default: 5432)
    -u, --user DB_USER        Database user (default: postgres)
    --no-confirm              Skip confirmation prompt
    --help                    Show this help message

EXAMPLES:
    # Restore from specific backup
    $0 -f /var/backups/postgres/crud_test_db_20260203_120000.sql.gz

    # Restore to different database
    $0 -f backup.sql.gz -d crud_test_db_restored

    # Restore without confirmation
    $0 -f backup.sql.gz --no-confirm

EOF
    exit 1
}

# Parse command line arguments
BACKUP_FILE=""
CONFIRM=true

while [[ $# -gt 0 ]]; do
    case $1 in
        -f|--file)
            BACKUP_FILE="$2"
            shift 2
            ;;
        -d|--database)
            DB_NAME="$2"
            shift 2
            ;;
        -h|--host)
            DB_HOST="$2"
            shift 2
            ;;
        -p|--port)
            DB_PORT="$2"
            shift 2
            ;;
        -u|--user)
            DB_USER="$2"
            shift 2
            ;;
        --no-confirm)
            CONFIRM=false
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

# Validate backup file
if [ -z "$BACKUP_FILE" ]; then
    log_error "Backup file not specified"
    usage
fi

if [ ! -f "$BACKUP_FILE" ]; then
    log_error "Backup file not found: $BACKUP_FILE"
    exit 1
fi

log "=========================================="
log "Database Restore Process"
log "=========================================="
log_info "Backup file: $BACKUP_FILE"
log_info "Database: $DB_NAME"
log_info "Host: $DB_HOST:$DB_PORT"
log_info "User: $DB_USER"
log "=========================================="

# Confirmation prompt
if [ "$CONFIRM" = true ]; then
    echo ""
    log_warning "⚠️  WARNING: This will OVERWRITE the current database!"
    log_warning "⚠️  All existing data in '$DB_NAME' will be LOST!"
    echo ""
    read -p "Are you sure you want to continue? (yes/no): " -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        log "Restore cancelled by user"
        exit 0
    fi
fi

# Check if PostgreSQL client is installed
if ! command -v pg_restore &> /dev/null; then
    log_error "pg_restore command not found. Please install PostgreSQL client."
    exit 1
fi

# Verify backup file integrity
log "Verifying backup file integrity..."
if [[ "$BACKUP_FILE" == *.gz ]]; then
    if gzip -t "$BACKUP_FILE" 2>> "$LOG_FILE"; then
        log_success "Backup file integrity verified"
    else
        log_error "Backup file is corrupted"
        exit 1
    fi
fi

# Create backup of current database before restore (safety measure)
SAFETY_BACKUP="${BACKUP_DIR}/${DB_NAME}_pre_restore_$(date +%Y%m%d_%H%M%S).sql.gz"
log "Creating safety backup of current database..."
if PGPASSWORD="${DB_PASSWORD}" pg_dump \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --username="$DB_USER" \
    --dbname="$DB_NAME" \
    --format=custom \
    | gzip > "$SAFETY_BACKUP" 2>> "$LOG_FILE"; then
    log_success "Safety backup created: $SAFETY_BACKUP"
else
    log_warning "Safety backup failed (continuing anyway)"
fi

# Decompress if needed
RESTORE_FILE="$BACKUP_FILE"
if [[ "$BACKUP_FILE" == *.gz ]]; then
    log "Decompressing backup file..."
    RESTORE_FILE="${BACKUP_FILE%.gz}"
    gunzip -c "$BACKUP_FILE" > "$RESTORE_FILE"
    log_success "Backup decompressed"
fi

# Drop existing connections
log "Terminating existing connections..."
PGPASSWORD="${DB_PASSWORD}" psql \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --username="$DB_USER" \
    --dbname="postgres" \
    -c "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = '$DB_NAME' AND pid <> pg_backend_pid();" \
    2>> "$LOG_FILE" || true

# Drop and recreate database
log "Dropping database '$DB_NAME'..."
PGPASSWORD="${DB_PASSWORD}" dropdb \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --username="$DB_USER" \
    --if-exists \
    "$DB_NAME" 2>> "$LOG_FILE"

log "Creating database '$DB_NAME'..."
PGPASSWORD="${DB_PASSWORD}" createdb \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --username="$DB_USER" \
    "$DB_NAME" 2>> "$LOG_FILE"

# Restore database
log "Restoring database from backup..."
if PGPASSWORD="${DB_PASSWORD}" pg_restore \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --username="$DB_USER" \
    --dbname="$DB_NAME" \
    --verbose \
    --no-owner \
    --no-privileges \
    "$RESTORE_FILE" 2>> "$LOG_FILE"; then

    log_success "Database restored successfully"
else
    log_error "Database restore failed"
    log_info "Safety backup available at: $SAFETY_BACKUP"
    exit 1
fi

# Clean up decompressed file
if [[ "$BACKUP_FILE" == *.gz ]]; then
    rm -f "$RESTORE_FILE"
fi

# Verify restore
log "Verifying restored database..."
TABLE_COUNT=$(PGPASSWORD="${DB_PASSWORD}" psql \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --username="$DB_USER" \
    --dbname="$DB_NAME" \
    -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>> "$LOG_FILE" | tr -d ' ')

log_info "Tables restored: $TABLE_COUNT"

if [ "$TABLE_COUNT" -gt 0 ]; then
    log_success "Database verification passed"
else
    log_warning "No tables found after restore. This may be normal for an empty database."
fi

log "=========================================="
log_success "Restore process completed successfully"
log "=========================================="
log_info "Safety backup: $SAFETY_BACKUP"
log_info "You can delete the safety backup if the restore was successful"

exit 0
