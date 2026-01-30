#!/bin/bash

# MFA Dependencies Installation Script
# Run this script to install required packages for MFA components

echo "================================================"
echo "  Installing MFA Component Dependencies"
echo "================================================"
echo ""

# Check if we're in the frontend directory
if [ ! -f "package.json" ]; then
    echo "Error: package.json not found!"
    echo "Please run this script from the frontend directory"
    exit 1
fi

echo "Installing qrcode.react..."
npm install qrcode.react

echo ""
echo "Checking existing dependencies..."
echo ""

# Check if required dependencies are installed
check_dependency() {
    local package=$1
    if npm list "$package" > /dev/null 2>&1; then
        echo "✓ $package is installed"
    else
        echo "✗ $package is NOT installed"
        return 1
    fi
}

echo "Required dependencies:"
check_dependency "react"
check_dependency "react-dom"
check_dependency "react-router-dom"
check_dependency "axios"
check_dependency "qrcode.react"

echo ""
echo "================================================"
echo "  Installation Complete!"
echo "================================================"
echo ""
echo "Next steps:"
echo "1. Update App.jsx to wrap with MFAProvider"
echo "2. Update LoginForm.jsx to handle MFA verification"
echo "3. Add MFA settings route to your router"
echo "4. Test the MFA flow"
echo ""
echo "See MFA_INTEGRATION_GUIDE.md for detailed instructions"
echo ""
