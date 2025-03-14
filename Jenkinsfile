pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = 'your-registry-url'
        DOCKER_CREDENTIALS_ID = 'docker-cred-id'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build Microservices') {
            parallel {
                stage('Build API Gateway') {
                    steps {
                        dir('api-gateway') {
                            sh 'mvn clean package -DskipTests'
                            sh 'docker build -t swiftchat/api-gateway:${BUILD_NUMBER} -t swiftchat/api-gateway:latest .'
                        }
                    }
                }
                
                stage('Build Service Registry') {
                    steps {
                        dir('service-registry') {
                            sh 'mvn clean package -DskipTests'
                            sh 'docker build -t swiftchat/service-registry:${BUILD_NUMBER} -t swiftchat/service-registry:latest .'
                        }
                    }
                }
                
                stage('Build Auth Service') {
                    steps {
                        dir('auth-service') {
                            sh 'mvn clean package -DskipTests'
                            sh 'docker build -t swiftchat/auth-service:${BUILD_NUMBER} -t swiftchat/auth-service:latest .'
                        }
                    }
                }
                
                stage('Build User Service') {
                    steps {
                        dir('user-service') {
                            sh 'mvn clean package -DskipTests'
                            sh 'docker build -t swiftchat/user-service:${BUILD_NUMBER} -t swiftchat/user-service:latest .'
                        }
                    }
                }
                
                stage('Build Chat Service') {
                    steps {
                        dir('chat-service') {
                            sh 'mvn clean package -DskipTests'
                            sh 'docker build -t swiftchat/chat-service:${BUILD_NUMBER} -t swiftchat/chat-service:latest .'
                        }
                    }
                }
                
                stage('Build Notification Service') {
                    steps {
                        dir('notification-service') {
                            sh 'mvn clean package -DskipTests'
                            sh 'docker build -t swiftchat/notification-service:${BUILD_NUMBER} -t swiftchat/notification-service:latest .'
                        }
                    }
                }
                
                stage('Build File Service') {
                    steps {
                        dir('file-service') {
                            sh 'mvn clean package -DskipTests'
                            sh 'docker build -t swiftchat/file-service:${BUILD_NUMBER} -t swiftchat/file-service:latest .'
                        }
                    }
                }
            }
        }
        
        stage('Run Tests') {
            steps {
                sh 'mvn test'
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
        }
        failure {
            echo 'Pipeline failed!'
            // Add notification steps here (email, Slack, etc.)
        }
    }
}