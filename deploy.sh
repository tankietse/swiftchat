#!/bin/bash

# Script to deploy SwiftChat to production environment

set -e

# Load environment variables
if [ -f .env.prod ]; then
    source .env.prod
else
    echo "Error: .env.prod file not found. Please create it from .env.prod.example"
    exit 1
fi

# Check if we have the required environment variables
if [ -z "$TAG" ] || [ -z "$ENVIRONMENT" ]; then
    echo "Error: Required environment variables are not set"
    exit 1
fi

# Check if Docker Swarm is initialized
if ! docker info | grep -q "Swarm: active"; then
    echo "Error: Docker Swarm is not active. Please initialize Swarm first."
    exit 1
fi

# Make sure all services are built and pushed to registry
echo "Building and pushing Docker images with tag: $TAG"
for service in api-gateway service-registry auth-service user-service chat-service notification-service file-service; do
    echo "Building $service..."
    docker build -t swiftchat/$service:$TAG ./$service
    docker push swiftchat/$service:$TAG
done

# Create required secrets if they don't exist
echo "Checking Docker secrets..."
if ! docker secret ls | grep -q "db_password"; then
    echo "Creating required secrets..."
    bash ./secrets/create-secrets.sh
fi

# Deploy the stack
echo "Deploying SwiftChat stack to production..."
docker stack deploy -c docker-compose.prod.yml swiftchat

echo "Deployment initiated. Check stack status with: docker stack ps swiftchat"
