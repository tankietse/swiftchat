#!/bin/bash

# Script to install Docker in Jenkins environment
# This should be run with appropriate permissions

# Exit on any error
set -e

echo "Starting installation of tools for Jenkins..."

# Check if we can use sudo
CAN_USE_SUDO=0
if command -v sudo &> /dev/null; then
  CAN_USE_SUDO=1
fi

# Function to run command with sudo if available
run_cmd() {
  if [ $CAN_USE_SUDO -eq 1 ]; then
    sudo $@
  else
    $@
  }
}

# Install Docker if not present
if ! command -v docker &> /dev/null; then
  echo "Installing Docker..."
  
  # Update package lists
  run_cmd apt-get update
  
  # Install prerequisites
  run_cmd apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release
    
  # Add Docker's official GPG key
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | run_cmd gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
  
  # Add Docker repository
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \
    $(lsb_release -cs) stable" | run_cmd tee /etc/apt/sources.list.d/docker.list > /dev/null
  
  # Install Docker
  run_cmd apt-get update
  run_cmd apt-get install -y docker-ce docker-ce-cli containerd.io
  
  # Add jenkins user to docker group
  if id "jenkins" &>/dev/null; then
    run_cmd usermod -aG docker jenkins
    echo "Added jenkins user to docker group"
  fi
  
  echo "Docker installation complete!"
else
  echo "Docker is already installed"
fi

# Install Maven if not present
if ! command -v mvn &> /dev/null; then
  echo "Installing Maven..."
  run_cmd apt-get update
  run_cmd apt-get install -y maven
  echo "Maven installation complete!"
else
  echo "Maven is already installed"
fi

echo "All tools installed successfully"
echo "NOTE: You may need to restart Jenkins for the changes to take effect"
echo "If Docker commands still fail, ensure the Jenkins user has permissions to use Docker"
