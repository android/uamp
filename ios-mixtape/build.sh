#!/bin/bash

# Mixtape iOS Build Script
# Builds the iOS app for simulator and device

set -e

echo "🎵 Building Mixtape iOS App..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_NAME="Mixtape"
SCHEME="Mixtape"
BUILD_DIR="build"

# Clean build directory
echo -e "${BLUE}📁 Cleaning build directory...${NC}"
rm -rf $BUILD_DIR
mkdir -p $BUILD_DIR

# Build for iOS Simulator
echo -e "${BLUE}📱 Building for iOS Simulator...${NC}"
xcodebuild -project ${PROJECT_NAME}.xcodeproj \
           -scheme $SCHEME \
           -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
           -derivedDataPath $BUILD_DIR/DerivedData \
           clean build | xcpretty

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ iOS Simulator build successful!${NC}"
else
    echo -e "${RED}❌ iOS Simulator build failed!${NC}"
    exit 1
fi

# Build for iOS Device (requires signing)
echo -e "${BLUE}📱 Building for iOS Device...${NC}"
xcodebuild -project ${PROJECT_NAME}.xcodeproj \
           -scheme $SCHEME \
           -destination 'generic/platform=iOS' \
           -derivedDataPath $BUILD_DIR/DerivedData \
           clean build | xcpretty

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ iOS Device build successful!${NC}"
else
    echo -e "${YELLOW}⚠️  iOS Device build failed (likely due to code signing)${NC}"
    echo -e "${YELLOW}   This is normal if you haven't configured signing in Xcode${NC}"
fi

# Archive for App Store (optional)
if [ "$1" = "archive" ]; then
    echo -e "${BLUE}📦 Creating archive for App Store...${NC}"
    xcodebuild -project ${PROJECT_NAME}.xcodeproj \
               -scheme $SCHEME \
               -destination 'generic/platform=iOS' \
               -archivePath $BUILD_DIR/${PROJECT_NAME}.xcarchive \
               archive | xcpretty
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Archive created successfully!${NC}"
        echo -e "${GREEN}   Location: $BUILD_DIR/${PROJECT_NAME}.xcarchive${NC}"
    else
        echo -e "${RED}❌ Archive creation failed!${NC}"
        exit 1
    fi
fi

echo ""
echo -e "${GREEN}🎉 Build process completed!${NC}"
echo ""
echo -e "${BLUE}📋 Next steps:${NC}"
echo -e "   • Open ${PROJECT_NAME}.xcodeproj in Xcode"
echo -e "   • Configure your development team for code signing"
echo -e "   • Run on simulator or device from Xcode"
echo -e "   • Test CarPlay functionality with CarPlay Simulator"
echo ""
echo -e "${BLUE}🚗 CarPlay Testing:${NC}"
echo -e "   • iOS Simulator → I/O → External Displays → CarPlay"
echo -e "   • Launch app and start playing music"
echo -e "   • Browse library through CarPlay interface"
echo "" 