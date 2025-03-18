#!/bin/bash

# Script to wait for a service to become healthy
# Usage: wait-for-service.sh <service-name> <port-number> [protocol]

SERVICE_NAME=$1
PORT=$2
PROTOCOL=${3:-https}
MAX_ATTEMPTS=30

echo "Waiting for $SERVICE_NAME to become available..."
counter=0

while [ $counter -lt $MAX_ATTEMPTS ]
do
  echo "Attempt $((counter+1)) of $MAX_ATTEMPTS"
  
  if [ "$PROTOCOL" = "https" ]; then
    response=$(curl -k -s -o /dev/null -w "%{http_code}" ${PROTOCOL}://localhost:${PORT}/actuator/health)
  else
    response=$(curl -s -o /dev/null -w "%{http_code}" ${PROTOCOL}://localhost:${PORT}/actuator/health)
  fi
  
  if [ "$response" = "200" ]; then
    echo "$SERVICE_NAME is up and running!"
    exit 0
  fi
  
  sleep 10
  counter=$((counter+1))
done

echo "Failed to start $SERVICE_NAME after $MAX_ATTEMPTS attempts"
exit 1
