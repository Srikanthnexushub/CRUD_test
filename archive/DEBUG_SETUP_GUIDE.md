# IntelliJ Debug Setup Guide

## Current Status
- ✅ Frontend: Running on http://localhost:3000
- ⏳ Backend: Needs to be started in Debug mode from IntelliJ

## Step-by-Step Debugging Instructions

### 1. Start Backend in Debug Mode

1. **In IntelliJ IDEA:**
   - Open the `CrudTestApplication.java` file
   - Look for the green play button next to `public static void main`
   - **Right-click** on the play button → Select **"Debug 'CrudTestApplication'"**
   - OR use the bug icon in the toolbar and select "Debug"

2. **Wait for the backend to start:**
   - You'll see the console output in the "Debug" tab (not "Run" tab)
   - Wait for the message: `Started CrudTestApplication in X seconds`
   - The debugger console should show: `Connected to the target VM`

### 2. Set Breakpoints Correctly

**Test with Login Endpoint (Easy to trigger):**

1. Open `AuthController.java` located at:
   ```
   src/main/java/org/example/controller/AuthController.java
   ```

2. Find the `login` method (around line 42-60)

3. Click in the **left gutter** (gray area next to line numbers) on this line:
   ```java
   public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
   ```
   - A RED DOT should appear ● (this is your breakpoint)

4. **Verify breakpoint is active:**
   - Go to: Run → View Breakpoints (Cmd+Shift+F8)
   - Make sure the breakpoint is checked and enabled
   - Condition: Leave empty for now
   - Suspend: Should be "Thread"

### 3. Trigger the Breakpoint

1. **Open browser to:**
   ```
   http://localhost:3000
   ```

2. **Try to login with:**
   - Username: `admin`
   - Password: `admin123`
   - Click "Login"

3. **What should happen:**
   - IntelliJ should STOP at your breakpoint
   - The line with the breakpoint will be highlighted
   - You'll see the "Variables" panel with `loginRequest` object
   - Browser will be waiting (loading)

### 4. Debug Controls (when stopped at breakpoint)

- **F8** - Step Over (execute current line, move to next)
- **F7** - Step Into (go inside method calls)
- **F9** - Resume Program (continue execution)
- **Alt+F8** - Evaluate Expression
- **View Variables** panel to see current values

### 5. Troubleshooting

**If breakpoint doesn't trigger:**

1. **Check the breakpoint is RED (not gray)**
   - Gray means disabled or not matched with running code

2. **Rebuild the project:**
   - Build → Rebuild Project
   - Then restart in Debug mode

3. **Check module source:**
   - Run → View Breakpoints
   - Click on your breakpoint
   - Make sure "Class:" shows: `org.example.controller.AuthController`

4. **Verify Debug mode is active:**
   - Look at the Debug tab at bottom
   - Should show "Debugger" tab with threads
   - Should NOT be in Run tab

**If port 8080 is already in use:**
```bash
lsof -ti:8080 | xargs kill -9
```

## Alternative: Simple Test Breakpoint

If login doesn't work, try this simpler endpoint:

1. Open `AuthController.java`
2. Set breakpoint on the `@PostMapping("/register")` method
3. In browser console or Postman:
   ```javascript
   fetch('http://localhost:8080/api/auth/register', {
     method: 'POST',
     headers: { 'Content-Type': 'application/json' },
     body: JSON.stringify({
       username: 'testuser',
       email: 'test@example.com',
       password: 'Test123!',
       firstName: 'Test',
       lastName: 'User'
     })
   })
   ```

## Quick Reference

- **Start Debug**: Right-click main() → Debug
- **Set Breakpoint**: Click left gutter
- **Remove Breakpoint**: Click the red dot
- **View All Breakpoints**: Cmd+Shift+F8
- **Resume**: F9
- **Step Over**: F8

## Success Indicators

✅ Debug tab open (not Run tab)
✅ "Connected to the target VM" message
✅ Red breakpoint dots (not gray)
✅ Backend logs showing in Debug console
✅ When you trigger the endpoint, IntelliJ comes to foreground and highlights the line

---

**Next Steps After Successful Breakpoint:**
1. Examine variables in the Variables panel
2. Use F8 to step through code line by line
3. Use Evaluate Expression (Alt+F8) to check values
4. Use F9 to continue when done
