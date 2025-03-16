#!/bin/bash

# This script runs Maven commands in a Docker container if Maven is not available locally

# Make script more resilient
set -e

# Check script location - needs to run from project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

# Check if Maven is installed locally
if command -v mvn &>/dev/null; then
    echo "Using local Maven installation"
    mvn "$@"
else
    echo "Using Docker container for Maven"
    # Ensure docker-compose file exists
    if [ -f "docker-compose.jenkins.yml" ]; then
        docker-compose -f docker-compose.jenkins.yml run --rm maven-builder mvn "$@"
    else
        echo "Error: docker-compose.jenkins.yml not found. Running Maven via Docker directly."
        docker run --rm -v "$(pwd)":/app -w /app maven:3.8.6-eclipse-temurin-17 mvn "$@"
    fi
fi
