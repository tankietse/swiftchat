#!/bin/bash

# Script to verify the deployment of auth-service
# Usage: ./verify-deployment.sh <service-name> <port> <health-endpoint>

SERVICE_NAME=$1
PORT=$2
HEALTH_ENDPOINT=${3:-/actuator/health}
MAX_ATTEMPTS=24
SLEEP_SECONDS=5

echo "Verifying deployment of $SERVICE_NAME on port $PORT..."

counter=0
while [ $counter -lt $MAX_ATTEMPTS ]
do
  counter=$((counter+1))
  echo "Attempt $counter of $MAX_ATTEMPTS"
  
  health_status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT$HEALTH_ENDPOINT)
  
  if [ "$health_status" == "200" ]; then
    echo "$SERVICE_NAME is up and healthy"
    exit 0
  else
    echo "$SERVICE_NAME is not ready yet (HTTP status: $health_status)"
    sleep $SLEEP_SECONDS
  fi
done

echo "Failed to verify $SERVICE_NAME deployment after $(($MAX_ATTEMPTS * $SLEEP_SECONDS)) seconds"
exit 1
