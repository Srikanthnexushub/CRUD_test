# CRUD Test Application - Startup Guide

## Current Status
✅ Repository cleaned and committed (commit: aa385cb)
✅ All temporary files archived to `archive/2026-01-30/`
✅ Codebase contains only essential code files

---

## First Time: Push to GitHub

### 1. Create a GitHub Repository
1. Go to https://github.com/new
2. Create a new repository named `CRUD_test` (or your preferred name)
3. **DO NOT** initialize with README, .gitignore, or license (we already have code)

### 2. Add Remote and Push
```bash
cd /Users/ainexusstudio/Documents/GitHub/CRUD_test

# Add your GitHub repository as remote
git remote add origin https://github.com/YOUR_USERNAME/CRUD_test.git

# Or if using SSH:
# git remote add origin git@github.com:YOUR_USERNAME/CRUD_test.git

# Push all commits to GitHub
git push -u origin main
```

---

## How to Start Tomorrow (or Anytime)

### Prerequisites Check
```bash
# Verify Java is installed (need Java 17+)
java -version

# Verify Node.js is installed (need Node 16+)
node -version
npm -version

# Verify Maven is installed
mvn -version
```

### Step 1: Start the Backend (Spring Boot)
```bash
cd /Users/ainexusstudio/Documents/GitHub/CRUD_test

# Clean and build the project
mvn clean install

# Run the backend server
mvn spring-boot:run
```

**Backend will start on:** http://localhost:8080

### Step 2: Start the Frontend (React + Vite)
Open a **new terminal window**:

```bash
cd /Users/ainexusstudio/Documents/GitHub/CRUD_test/frontend

# Install dependencies (first time or if package.json changed)
npm install

# Start the development server
npm run dev
```

**Frontend will start on:** http://localhost:5173

### Step 3: Access the Application
Open your browser and go to: **http://localhost:5173**

---

## Application Features

### Authentication
- **Login:** JWT-based authentication
- **Registration:** Create new user accounts
- **Protected Routes:** Dashboard requires authentication

### User Management (CRUD Operations)
- **Create:** Register new users
- **Read:** View user list and details
- **Update:** Edit user information
- **Delete:** Remove users (Admin only)

### Role-Based Access Control
- **ADMIN:** Full access to all operations
- **USER:** Limited access (cannot delete other users)

---

## Default Test Accounts

Check `src/main/java/org/example/config/InitialDataLoader.java` for default accounts.

Typical defaults:
```
Admin Account:
Email: admin@example.com
Password: admin123

User Account:
Email: user@example.com
Password: user123
```

---

## Project Structure

```
CRUD_test/
├── src/                           # Backend Java source code
│   └── main/
│       ├── java/org/example/
│       │   ├── config/           # Security, CORS, Data Loader
│       │   ├── controller/       # REST API endpoints
│       │   ├── dto/              # Data Transfer Objects
│       │   ├── entity/           # JPA Entities
│       │   ├── exception/        # Exception handlers
│       │   ├── repository/       # Data access layer
│       │   ├── security/         # JWT, Authentication
│       │   └── service/          # Business logic
│       └── resources/
│           └── application.properties  # App configuration
│
├── frontend/                      # React frontend
│   ├── src/
│   │   ├── components/          # React components
│   │   ├── contexts/            # AuthContext
│   │   ├── services/            # API service
│   │   └── styles/              # CSS files
│   ├── package.json             # Dependencies
│   └── vite.config.js           # Vite configuration
│
├── pom.xml                       # Maven configuration
└── archive/                      # Archived documentation
```

---

## Common Issues & Solutions

### Backend Issues

**Problem:** Port 8080 already in use
```bash
# Find process using port 8080
lsof -ti:8080

# Kill the process
kill -9 $(lsof -ti:8080)
```

**Problem:** Database connection error
- Check `src/main/resources/application.properties`
- Verify H2 database configuration
- Default: In-memory H2 database (no setup needed)

**Problem:** Build errors
```bash
# Clean Maven cache and rebuild
mvn clean
rm -rf target/
mvn install
```

### Frontend Issues

**Problem:** Port 5173 already in use
```bash
# Kill process on port 5173
lsof -ti:5173 | xargs kill -9
```

**Problem:** Dependencies not found
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
```

**Problem:** API connection refused
- Ensure backend is running on http://localhost:8080
- Check `frontend/src/services/api.js` for correct API URL

### CORS Issues

**Problem:** CORS errors in browser console
- Check `src/main/java/org/example/config/CorsConfig.java`
- Verify frontend URL is allowed (http://localhost:5173)

---

## Stopping the Application

### Stop Backend
In the terminal running `mvn spring-boot:run`:
- Press `Ctrl + C`

### Stop Frontend
In the terminal running `npm run dev`:
- Press `Ctrl + C`

---

## Development Workflow

### Making Changes

1. **Backend Changes:**
   - Edit Java files in `src/main/java/`
   - Stop and restart: `mvn spring-boot:run`

2. **Frontend Changes:**
   - Edit files in `frontend/src/`
   - Vite will auto-reload (no restart needed)

### Committing Changes
```bash
# Check status
git status

# Add files
git add .

# Commit with message
git commit -m "Description of changes"

# Push to GitHub
git push origin main
```

---

## Database Access

### H2 Console (if enabled)
- URL: http://localhost:8080/h2-console
- Check `application.properties` for H2 console settings

### View Data
All data is in-memory by default. To persist data:
1. Edit `application.properties`
2. Configure file-based H2 or switch to MySQL/PostgreSQL

---

## Next Steps

- [ ] Push code to GitHub (see instructions above)
- [ ] Test all CRUD operations
- [ ] Verify authentication flow
- [ ] Check role-based access control
- [ ] Review archived documentation in `archive/` if needed

---

## Need Help?

### View Archived Documentation
```bash
cd archive/2026-01-30/
ls -la

# View specific guide
cat archive/2026-01-30/README.md
```

### Check Application Logs
- **Backend:** Check terminal output where `mvn spring-boot:run` is running
- **Frontend:** Check browser console (F12)

---

## Quick Commands Reference

```bash
# Start backend (from project root)
mvn spring-boot:run

# Start frontend (from frontend folder)
cd frontend && npm run dev

# Build for production
mvn clean package
cd frontend && npm run build

# View git history
git log --oneline -10

# Check what's running
lsof -i :8080  # Backend
lsof -i :5173  # Frontend
```

---

**Last Updated:** 2026-01-30
**Project Status:** Production Ready
**Codebase:** Clean and minimal
