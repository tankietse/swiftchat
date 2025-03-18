#!/bin/bash

# Script to deploy SwiftChat services to production
# Usage: deploy.sh [service-name]

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

# Check if a specific service is specified
SERVICE_NAME=${1:-all}
echo "Deploying $SERVICE_NAME to production..."

# Create Docker secrets if they don't exist
function create_secrets() {
  existing_secrets=$(docker secret ls --format '{{.Name}}')
  
  # Check and create secrets if they don't exist
  if [[ ! $existing_secrets == *"db_password"* ]]; then
    echo "Creating Docker secrets..."
    bash ./secrets/create-secrets.sh
  else
    echo "Docker secrets already exist."
  fi
}

# Initialize swarm if needed
if ! docker info | grep -q "Swarm: active"; then
    echo "Initializing Docker Swarm..."
    docker swarm init
fi

# Make sure all services are built and pushed to registry
echo "Building and pushing Docker images with tag: $TAG"
for service in api-gateway service-registry auth-service user-service chat-service notification-service file-service; do
    echo "Building $service..."
    docker build -t swiftchat/$service:$TAG ./$service
    docker push swiftchat/$service:$TAG
done

# Create required Docker secrets
create_secrets

# Deploy specific service or all services
if [ "$SERVICE_NAME" == "auth-service" ]; then
    echo "Deploying Auth Service..."
    docker stack deploy -c docker-compose.prod.yml auth
elif [ "$SERVICE_NAME" == "all" ]; then
    echo "Deploying all services..."
    docker stack deploy -c docker-compose.prod.yml swiftchat
else
    echo "Unknown service: $SERVICE_NAME"
    echo "Available services: auth-service, all"
    exit 1
fi

echo "Deployment initiated. Check status with 'docker service ls'"
