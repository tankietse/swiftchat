#!/bin/bash

# Script to ensure Jenkins has Docker permissions
# Run this on the Jenkins host with sudo

# Add the jenkins user to the docker group
sudo usermod -aG docker jenkins

# Restart the Jenkins service
sudo systemctl restart jenkins

# Wait for Jenkins to restart
echo "Waiting for Jenkins to restart..."
sleep 30

# Verify permissions
echo "Verifying Docker permissions for Jenkins user..."
sudo -u jenkins docker info

echo "Setup complete. Jenkins should now be able to run Docker commands."
