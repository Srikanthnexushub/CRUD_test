# 🚀 QUICK START GUIDE

## Every Time You Start Working

### ONE COMMAND TO RULE THEM ALL:
```bash
./startup.sh
```

That's it! This script will:
- ✅ Start Colima (Docker runtime)
- ✅ Start PostgreSQL database
- ✅ Wait for everything to be ready
- ✅ Show you what to do next

### Then Open IntelliJ and Run:
1. Open `CrudTestApplication.java`
2. Click the green ▶️ (Run) or 🐛 (Debug) button
3. Wait for "Started CrudTestApplication" message

### Start Frontend (Optional):
```bash
cd frontend
npm run dev
```

---

## When You're Done Working

```bash
./shutdown.sh
```

This stops everything cleanly.

---

## If Something Goes Wrong

### Problem: Script says "command not found"
```bash
chmod +x startup.sh shutdown.sh
```

### Problem: IntelliJ can't find classes
1. File → Invalidate Caches → Invalidate and Restart
2. Wait for IntelliJ to re-index
3. Try running again

### Problem: Port 8080 already in use
```bash
lsof -ti:8080 | xargs kill -9
```

### Problem: Database connection refused
```bash
docker-compose down
./startup.sh
```

---

## Default Login Credentials

**Admin Account:**
- Username: `admin`
- Password: `admin123`

**API Base URL:** http://localhost:8080
**Frontend URL:** http://localhost:5173

---

## The Problem You Were Having (Now Fixed!)

**Before:** When you restarted IntelliJ, these things weren't running:
1. Colima (Docker runtime) - NOT auto-starting
2. PostgreSQL database - NOT auto-starting
3. This caused connection errors

**Now:** Just run `./startup.sh` every time before opening IntelliJ!

---

## Workflow

```
Morning:
  1. ./startup.sh
  2. Open IntelliJ
  3. Run CrudTestApplication
  4. Code! 💻

Evening:
  1. Stop IntelliJ run
  2. ./shutdown.sh
  3. Close IntelliJ
```

---

**Full documentation:** See `STARTUP_GUIDE.md` and `PROJECT_STATUS.md`
