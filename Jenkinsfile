pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = 'your-registry-url'
        DOCKER_CREDENTIALS_ID = 'docker-cred-id'
        AUTH_SERVICE_PORT = '8081'
        // Use a direct docker maven command instead of relying on scripts
        MVN_CMD = 'docker run --rm -v "$(pwd)":/app -w /app maven:3.8.6-eclipse-temurin-17 mvn'
        DOCKER_AVAILABLE = false
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Setup Environment') {
            steps {
                script {
                    try {
                        sh 'docker --version'
                        echo "Docker is available"
                        env.DOCKER_AVAILABLE = 'true'
                    } catch (Exception e) {
                        echo "Docker is not available: ${e.message}"
                        echo "Will attempt alternative build approaches"
                        env.DOCKER_AVAILABLE = 'false'
                    }
                }
                // Create a scripts directory in workspace if it doesn't exist
                sh 'mkdir -p scripts'
            }
        }
        
        stage('Build Microservices') {
            parallel {
                stage('Build API Gateway') {
                    steps {
                        dir('api-gateway') {
                            script {
                                if (env.DOCKER_AVAILABLE == 'true') {
                                    sh '${MVN_CMD} clean package -DskipTests'
                                    sh 'docker build -t swiftchat/api-gateway:${BUILD_NUMBER} -t swiftchat/api-gateway:latest .'
                                } else {
                                    sh 'mvn clean package -DskipTests'
                                }
                            }
                        }
                    }
                }
                
                stage('Build Service Registry') {
                    steps {
                        dir('service-registry') {
                            script {
                                if (env.DOCKER_AVAILABLE == 'true') {
                                    sh '${MVN_CMD} clean package -DskipTests'
                                    sh 'docker build -t swiftchat/service-registry:${BUILD_NUMBER} -t swiftchat/service-registry:latest .'
                                } else {
                                    sh 'mvn clean package -DskipTests'
                                }
                            }
                        }
                    }
                }
                
                stage('Build Auth Service') {
                    steps {
                        dir('auth-service') {
                            script {
                                if (env.DOCKER_AVAILABLE == 'true') {
                                    sh '${MVN_CMD} clean package'
                                    sh 'docker build -t swiftchat/auth-service:${BUILD_NUMBER} -t swiftchat/auth-service:latest .'
                                    sh 'echo "Auth service build completed successfully"'
                                } else {
                                    sh 'mvn clean package'
                                }
                            }
                        }
                    }
                    post {
                        success {
                            echo 'Auth Service build successful'
                            archiveArtifacts artifacts: 'auth-service/target/*.jar', fingerprint: true
                        }
                        failure {
                            echo 'Auth Service build failed'
                        }
                    }
                }
                
                stage('Build User Service') {
                    steps {
                        dir('user-service') {
                            script {
                                if (env.DOCKER_AVAILABLE == 'true') {
                                    sh '${MVN_CMD} clean package -DskipTests'
                                    sh 'docker build -t swiftchat/user-service:${BUILD_NUMBER} -t swiftchat/user-service:latest .'
                                } else {
                                    sh 'mvn clean package -DskipTests'
                                }
                            }
                        }
                    }
                }
                
                stage('Build Chat Service') {
                    steps {
                        dir('chat-service') {
                            script {
                                if (env.DOCKER_AVAILABLE == 'true') {
                                    sh '${MVN_CMD} clean package -DskipTests'
                                    sh 'docker build -t swiftchat/chat-service:${BUILD_NUMBER} -t swiftchat/chat-service:latest .'
                                } else {
                                    sh 'mvn clean package -DskipTests'
                                }
                            }
                        }
                    }
                }
                
                stage('Build Notification Service') {
                    steps {
                        dir('notification-service') {
                            script {
                                if (env.DOCKER_AVAILABLE == 'true') {
                                    sh '${MVN_CMD} clean package -DskipTests'
                                    sh 'docker build -t swiftchat/notification-service:${BUILD_NUMBER} -t swiftchat/notification-service:latest .'
                                } else {
                                    sh 'mvn clean package -DskipTests'
                                }
                            }
                        }
                    }
                }
                
                stage('Build File Service') {
                    steps {
                        dir('file-service') {
                            script {
                                if (env.DOCKER_AVAILABLE == 'true') {
                                    sh '${MVN_CMD} clean package -DskipTests'
                                    sh 'docker build -t swiftchat/file-service:${BUILD_NUMBER} -t swiftchat/file-service:latest .'
                                } else {
                                    sh 'mvn clean package -DskipTests'
                                }
                            }
                        }
                    }
                }
            }
        }
        
        stage('Run Tests') {
            steps {
                script {
                    if (env.DOCKER_AVAILABLE == 'true') {
                        sh '${MVN_CMD} test'
                    } else {
                        sh 'mvn test'
                    }
                }
            }
        }
        
        stage('Test Auth Service') {
            steps {
                dir('auth-service') {
                    script {
                        if (env.DOCKER_AVAILABLE == 'true') {
                            sh '${MVN_CMD} test'
                        } else {
                            sh 'mvn test'
                        }
                    }
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
                script {
                    if (env.DOCKER_AVAILABLE == 'true') {
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
                    } else {
                        echo "Docker is not available, skipping image push"
                    }
                }
            }
        }
        
        stage('Deploy to Dev') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    if (env.DOCKER_AVAILABLE == 'true') {
                        sh 'docker-compose -f docker-compose.dev.yml up -d'
                    } else {
                        echo "Docker is not available, skipping deployment"
                    }
                }
            }
        }
        
        stage('Deploy Auth Service to Dev') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    if (env.DOCKER_AVAILABLE == 'true') {
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
                    } else {
                        echo "Docker is not available, skipping deployment"
                    }
                }
            }
        }
        
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                input message: 'Approve deployment to production?'
                script {
                    if (env.DOCKER_AVAILABLE == 'true') {
                        sh 'docker-compose -f docker-compose.prod.yml up -d'
                    } else {
                        echo "Docker is not available, skipping deployment"
                    }
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}