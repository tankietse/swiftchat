pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = 'your-registry-url'
        DOCKER_CREDENTIALS_ID = 'docker-cred-id'
        AUTH_SERVICE_PORT = '8081'
        // Use a direct docker maven command instead of relying on scripts
        MVN_CMD = 'docker run --rm -v "$(pwd)":/app -w /app maven:3.8.6-eclipse-temurin-17 mvn'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Setup Environment') {
            steps {
                // Make sure docker is accessible
                sh 'docker --version'
                // Create a scripts directory in workspace if it doesn't exist
                sh 'mkdir -p scripts'
            }
        }
        
        stage('Build Microservices') {
            parallel {
                stage('Build API Gateway') {
                    steps {
                        dir('api-gateway') {
                            sh '${MVN_CMD} clean package -DskipTests'
                            sh 'docker build -t swiftchat/api-gateway:${BUILD_NUMBER} -t swiftchat/api-gateway:latest .'
                        }
                    }
                }
                
                stage('Build Service Registry') {
                    steps {
                        dir('service-registry') {
                            sh '${MVN_CMD} clean package -DskipTests'
                            sh 'docker build -t swiftchat/service-registry:${BUILD_NUMBER} -t swiftchat/service-registry:latest .'
                        }
                    }
                }
                
                stage('Build Auth Service') {
                    steps {
                        dir('auth-service') {
                            sh '${MVN_CMD} clean package'
                            sh 'docker build -t swiftchat/auth-service:${BUILD_NUMBER} -t swiftchat/auth-service:latest .'
                            sh 'echo "Auth service build completed successfully"'
                        }
                    }
                    post {
                        success {
                            echo 'Auth Service build successful'
                            archiveArtifacts artifacts: 'auth-service/target/*.jar', fingerprint: true
                        }
                        failure {
                            echo 'Auth Service build failed'
                            // Remove email step to avoid connection errors
                        }
                    }
                }
                
                stage('Build User Service') {
                    steps {
                        dir('user-service') {
                            sh '${MVN_CMD} clean package -DskipTests'
                            sh 'docker build -t swiftchat/user-service:${BUILD_NUMBER} -t swiftchat/user-service:latest .'
                        }
                    }
                }
                
                stage('Build Chat Service') {
                    steps {
                        dir('chat-service') {
                            sh '${MVN_CMD} clean package -DskipTests'
                            sh 'docker build -t swiftchat/chat-service:${BUILD_NUMBER} -t swiftchat/chat-service:latest .'
                        }
                    }
                }
                
                stage('Build Notification Service') {
                    steps {
                        dir('notification-service') {
                            sh '${MVN_CMD} clean package -DskipTests'
                            sh 'docker build -t swiftchat/notification-service:${BUILD_NUMBER} -t swiftchat/notification-service:latest .'
                        }
                    }
                }
                
                stage('Build File Service') {
                    steps {
                        dir('file-service') {
                            sh '${MVN_CMD} clean package -DskipTests'
                            sh 'docker build -t swiftchat/file-service:${BUILD_NUMBER} -t swiftchat/file-service:latest .'
                        }
                    }
                }
            }
        }
        
        stage('Run Tests') {
            steps {
                sh '${MVN_CMD} test'
            }
        }
        
        stage('Test Auth Service') {
            steps {
                dir('auth-service') {
                    sh '${MVN_CMD} test'
                    junit '**/target/surefire-reports/*.xml'
                    jacoco execPattern: 'target/jacoco.exec'
                }
            }
        }
        
        stage('Push Images') {
            when {
                branch 'main'
            }
            steps {
                withCredentials([string(credentialsId: "${DOCKER_CREDENTIALS_ID}", variable: 'DOCKER_PWD')]) {
                    sh 'echo $DOCKER_PWD | docker login ${DOCKER_REGISTRY} -u username --password-stdin'
                    
                    sh 'docker tag swiftchat/api-gateway:latest ${DOCKER_REGISTRY}/swiftchat/api-gateway:latest'
                    sh 'docker push ${DOCKER_REGISTRY}/swiftchat/api-gateway:latest'
                    
                    sh 'docker tag swiftchat/service-registry:latest ${DOCKER_REGISTRY}/swiftchat/service-registry:latest'
                    sh 'docker push ${DOCKER_REGISTRY}/swiftchat/service-registry:latest'
                    
                    sh 'docker tag swiftchat/auth-service:latest ${DOCKER_REGISTRY}/swiftchat/auth-service:latest'
                    sh 'docker push ${DOCKER_REGISTRY}/swiftchat/auth-service:latest'
                    
                    sh 'docker tag swiftchat/user-service:latest ${DOCKER_REGISTRY}/swiftchat/user-service:latest'
                    sh 'docker push ${DOCKER_REGISTRY}/swiftchat/user-service:latest'
                    
                    sh 'docker tag swiftchat/chat-service:latest ${DOCKER_REGISTRY}/swiftchat/chat-service:latest'
                    sh 'docker push ${DOCKER_REGISTRY}/swiftchat/chat-service:latest'
                    
                    sh 'docker tag swiftchat/notification-service:latest ${DOCKER_REGISTRY}/swiftchat/notification-service:latest'
                    sh 'docker push ${DOCKER_REGISTRY}/swiftchat/notification-service:latest'
                    
                    sh 'docker tag swiftchat/file-service:latest ${DOCKER_REGISTRY}/swiftchat/file-service:latest'
                    sh 'docker push ${DOCKER_REGISTRY}/swiftchat/file-service:latest'
                }
            }
        }
        
        stage('Deploy to Dev') {
            when {
                branch 'develop'
            }
            steps {
                sh 'docker-compose -f docker-compose.dev.yml up -d'
            }
        }
        
        stage('Deploy Auth Service to Dev') {
            when {
                branch 'develop'
            }
            steps {
                sh 'docker-compose -f docker-compose.dev.yml up -d auth-service'
                sh """
                    # Wait for auth-service to be healthy
                    max_attempts=12
                    counter=0
                    echo "Waiting for Auth Service to become available..."
                    until curl -s http://localhost:${AUTH_SERVICE_PORT}/actuator/health | grep -q "UP" || [ \$counter -eq \$max_attempts ]
                    do
                      sleep 10
                      counter=\$((counter + 1))
                      echo "Attempt \$counter of \$max_attempts"
                    done
                    
                    if [ \$counter -eq \$max_attempts ]; then
                      echo "Failed to start Auth Service"
                      exit 1
                    else
                      echo "Auth Service started successfully"
                    fi
                """
            }
        }
        
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                input message: 'Approve deployment to production?'
                sh 'docker-compose -f docker-compose.prod.yml up -d'
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo 'Pipeline completed successfully!'
            // Remove slackSend until properly configured
        }
        failure {
            echo 'Pipeline failed!'
            // Remove notifications until properly configured
        }
    }
}