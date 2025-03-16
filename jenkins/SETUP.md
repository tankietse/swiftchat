# Jenkins Setup Guide for SwiftChat

## Prerequisites

1. Jenkins server with Docker installed
2. Docker permissions for Jenkins user
3. Required Jenkins plugins:
   - Docker
   - Pipeline
   - Git
   - JUnit
   - JaCoCo

## Configure Jenkins for Docker

Run these commands on the Jenkins server:

```bash
# Add Jenkins user to Docker group
sudo usermod -aG docker jenkins

# Restart Jenkins
sudo systemctl restart jenkins
```

## Configure Jenkins Credentials

1. Add GitHub credentials:
   - Go to Dashboard > Manage Jenkins > Credentials > System > Global credentials > Add Credentials
   - Kind: Username with password
   - ID: github-credentials
   - Description: GitHub credentials
   - Username: your-github-username
   - Password: your-github-password-or-token

2. Add Docker registry credentials:
   - Kind: Secret text
   - ID: docker-cred-id
   - Description: Docker registry credentials
   - Secret: your-docker-registry-password

## Configure Email Notifications (Optional)

1. Go to Dashboard > Manage Jenkins > System Configuration
2. Configure E-mail Notification:
   - SMTP Server: smtp.your-company.com
   - Default user e-mail suffix: @your-company.com
   - Use SMTP Authentication: Yes
   - User Name: your-email@your-company.com
   - Password: your-email-password
   - Use SSL: Yes
   - SMTP Port: 465
   - Test configuration by sending test e-mail

## Configure Slack Notifications (Optional)

1. Install Slack Notification Plugin
2. Configure Slack:
   - Team Domain: your-slack-domain
   - Integration Token: your-slack-token
   - Channel: #jenkins-notifications

## Create Jenkins Pipeline Job

1. Go to Dashboard > New Item
2. Enter name: swiftchat-app-ci
3. Select Pipeline
4. Configure Pipeline:
   - Definition: Pipeline script from SCM
   - SCM: Git
   - Repository URL: https://github.com/tankietse/swiftchat.git
   - Credentials: github-credentials
   - Branch Specifier: */develop
   - Script Path: Jenkinsfile.minimal

5. After successful minimal pipeline, update to full Jenkinsfile
