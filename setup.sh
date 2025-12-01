#!/bin/bash

# Racing Bank - Quick Setup Script
# This script helps you set up the Racing Bank project quickly

set -e  # Exit on error

echo "🏦 Racing Bank - Quick Setup"
echo "=============================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Check prerequisites
check_prerequisites() {
    echo -e "${CYAN}Checking prerequisites...${NC}"
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}❌ Docker not found. Please install Docker first.${NC}"
        echo "   Visit: https://docs.docker.com/get-docker/"
        exit 1
    fi
    echo -e "${GREEN}✓ Docker found${NC}"
    
    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        echo -e "${RED}❌ Docker Compose not found. Please install Docker Compose.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Docker Compose found${NC}"
    
    echo ""
}

# Setup API
setup_api() {
    echo -e "${CYAN}Setting up API...${NC}"
    
    cd api
    
    # Start Docker containers
    echo -e "${CYAN}Starting Docker containers...${NC}"
    docker-compose up -d
    
    # Wait for services to be ready
    echo -e "${YELLOW}Waiting for services to start...${NC}"
    sleep 10
    
    # Check if API is running
    if curl -s http://localhost:8000/docs > /dev/null; then
        echo -e "${GREEN}✓ API is running at http://localhost:8000${NC}"
        echo -e "${GREEN}✓ API docs at http://localhost:8000/docs${NC}"
    else
        echo -e "${RED}❌ API failed to start. Check logs with: docker-compose logs${NC}"
        exit 1
    fi
    
    # Show demo user credentials
    echo ""
    echo -e "${CYAN}📝 Demo User Credentials:${NC}"
    docker-compose logs api 2>&1 | grep "TOTP secret" | while read -r line; do
        echo -e "${YELLOW}   $line${NC}"
    done
    
    cd ..
    echo ""
}

# Setup Android app (optional)
setup_android() {
    echo -e "${CYAN}Android App Setup:${NC}"
    echo "1. Open Android Studio"
    echo "2. File → Open → Select racing-bank/app directory"
    echo "3. Wait for Gradle sync"
    echo ""
    echo -e "${YELLOW}Note: If testing on a physical device, update the API URL in:${NC}"
    echo "   app/src/main/java/.../data/api/RetrofitClient.kt"
    echo ""
}

# Show next steps
show_next_steps() {
    echo -e "${GREEN}✅ Setup Complete!${NC}"
    echo ""
    echo -e "${CYAN}🚀 Next Steps:${NC}"
    echo ""
    echo "1. Access API documentation:"
    echo "   http://localhost:8000/docs"
    echo ""
    echo "2. Test the API with demo users:"
    echo "   Username: alice, Password: alice123"
    echo "   Username: bob, Password: bob123"
    echo ""
    echo "3. Setup 2FA:"
    echo "   - Use Google Authenticator or similar"
    echo "   - Enter the TOTP secret shown above"
    echo ""
    echo "4. Open Android app in Android Studio"
    echo ""
    echo -e "${YELLOW}⚠️  Remember: This is an educational project with intentional vulnerabilities!${NC}"
    echo -e "${RED}   DO NOT USE IN PRODUCTION!${NC}"
    echo ""
    echo -e "${CYAN}📚 Documentation:${NC}"
    echo "   - README.md - Project overview"
    echo "   - api/README.md - API documentation"
    echo "   - app/README.md - Android app documentation"
    echo ""
}

# Main execution
main() {
    echo -e "${YELLOW}⚠️  WARNING: This project contains intentional security vulnerabilities${NC}"
    echo -e "${YELLOW}   for educational purposes. DO NOT USE IN PRODUCTION!${NC}"
    echo ""
    
    read -p "Do you want to continue? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Setup cancelled."
        exit 1
    fi
    echo ""
    
    check_prerequisites
    setup_api
    setup_android
    show_next_steps
}

# Run main function
main