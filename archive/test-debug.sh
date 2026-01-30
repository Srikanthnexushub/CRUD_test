#!/bin/bash

echo "🔍 Testing Debug Setup"
echo "===================="
echo ""

# Test if frontend is running
echo "1. Testing Frontend..."
if curl -s http://localhost:3000 > /dev/null; then
    echo "   ✅ Frontend is running on http://localhost:3000"
else
    echo "   ❌ Frontend is NOT running"
fi

echo ""

# Test if backend is running
echo "2. Testing Backend..."
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "   ✅ Backend is running on http://localhost:8080"
    echo "   📊 Health check:"
    curl -s http://localhost:8080/actuator/health | jq '.' 2>/dev/null || curl -s http://localhost:8080/actuator/health
else
    echo "   ❌ Backend is NOT running on port 8080"
    echo "   📝 Please start the backend in Debug mode from IntelliJ"
    echo "   👉 See DEBUG_SETUP_GUIDE.md for instructions"
fi

echo ""
echo "===================="
echo ""
echo "🎯 To test debugging:"
echo "1. Set a breakpoint in AuthController.java on the login() method"
echo "2. Open http://localhost:3000 in your browser"
echo "3. Try logging in with username: admin, password: admin123"
echo "4. IntelliJ should stop at your breakpoint!"
echo ""
echo "📖 Full guide: DEBUG_SETUP_GUIDE.md"
