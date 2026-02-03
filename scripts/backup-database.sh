#!/bin/bash

################################################################################
# Database Backup Script
# Performs PostgreSQL database backup with compression and retention management
################################################################################

set -e  # Exit on error
set -u  # Exit on undefined variable

# Configuration
BACKUP_DIR="${BACKUP_DIR:-/var/backups/postgres}"
DB_NAME="${DB_NAME:-crud_test_db}"
DB_USER="${DB_USER:-postgres}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
S3_BUCKET="${S3_BUCKET:-}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql.gz"
LOG_FILE="${BACKUP_DIR}/backup.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

log "=========================================="
log "Starting database backup process"
log "Database: $DB_NAME"
log "Backup file: $BACKUP_FILE"
log "=========================================="

# Check if PostgreSQL client is installed
if ! command -v pg_dump &> /dev/null; then
    log_error "pg_dump command not found. Please install PostgreSQL client."
    exit 1
fi

# Perform backup
log "Creating database dump..."
if PGPASSWORD="${DB_PASSWORD}" pg_dump \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --username="$DB_USER" \
    --dbname="$DB_NAME" \
    --format=custom \
    --verbose \
    --file="${BACKUP_FILE%.gz}" 2>> "$LOG_FILE"; then

    log_success "Database dump created successfully"

    # Compress backup
    log "Compressing backup..."
    gzip -f "${BACKUP_FILE%.gz}"

    if [ -f "$BACKUP_FILE" ]; then
        BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
        log_success "Backup compressed successfully (Size: $BACKUP_SIZE)"
    else
        log_error "Compression failed"
        exit 1
    fi
else
    log_error "Database dump failed"
    exit 1
fi

# Upload to S3 if configured
if [ -n "$S3_BUCKET" ]; then
    log "Uploading backup to S3..."
    if command -v aws &> /dev/null; then
        if aws s3 cp "$BACKUP_FILE" "s3://${S3_BUCKET}/backups/$(basename $BACKUP_FILE)" 2>> "$LOG_FILE"; then
            log_success "Backup uploaded to S3 successfully"
        else
            log_warning "S3 upload failed (backup still available locally)"
        fi
    else
        log_warning "AWS CLI not found. Skipping S3 upload."
    fi
fi

# Verify backup integrity
log "Verifying backup integrity..."
if gzip -t "$BACKUP_FILE" 2>> "$LOG_FILE"; then
    log_success "Backup integrity verified"
else
    log_error "Backup integrity check failed"
    exit 1
fi

# Calculate backup statistics
TOTAL_BACKUPS=$(find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" | wc -l)
TOTAL_SIZE=$(du -sh "$BACKUP_DIR" | cut -f1)
log "Total backups: $TOTAL_BACKUPS"
log "Total backup size: $TOTAL_SIZE"

# Clean up old backups
log "Cleaning up backups older than $RETENTION_DAYS days..."
DELETED_COUNT=$(find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -type f -mtime +$RETENTION_DAYS -delete -print | wc -l)

if [ "$DELETED_COUNT" -gt 0 ]; then
    log_success "Deleted $DELETED_COUNT old backup(s)"
else
    log "No old backups to delete"
fi

# Create backup manifest
MANIFEST_FILE="${BACKUP_DIR}/backup_manifest.txt"
cat > "$MANIFEST_FILE" << EOF
Last Backup: $(date)
Database: $DB_NAME
Backup File: $BACKUP_FILE
Size: $BACKUP_SIZE
Total Backups: $TOTAL_BACKUPS
Retention Days: $RETENTION_DAYS
S3 Bucket: ${S3_BUCKET:-Not configured}
EOF

log "=========================================="
log_success "Backup process completed successfully"
log "=========================================="

# Send notification (optional)
if [ -n "${BACKUP_NOTIFICATION_WEBHOOK:-}" ]; then
    curl -X POST "$BACKUP_NOTIFICATION_WEBHOOK" \
        -H "Content-Type: application/json" \
        -d "{\"text\":\"Database backup completed successfully: $BACKUP_FILE ($BACKUP_SIZE)\"}" \
        &> /dev/null || true
fi

exit 0
