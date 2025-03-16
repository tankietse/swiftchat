#!/bin/bash

# This script runs Maven commands in a Docker container if Maven is not available locally

# Check if Maven is installed locally
if command -v mvn &>/dev/null; then
    echo "Using local Maven installation"
    mvn "$@"
else
    echo "Using Docker container for Maven"
    docker-compose -f docker-compose.jenkins.yml run --rm maven-builder mvn "$@"
fi
