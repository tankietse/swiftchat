pipeline {
    agent any
    
    parameters {
        booleanParam(name: 'BUILD_API_GATEWAY', defaultValue: true, description: 'Build API Gateway')
        booleanParam(name: 'BUILD_SERVICE_REGISTRY', defaultValue: true, description: 'Build Service Registry')
        booleanParam(name: 'BUILD_AUTH_SERVICE', defaultValue: true, description: 'Build Auth Service')
        booleanParam(name: 'BUILD_USER_SERVICE', defaultValue: true, description: 'Build User Service')
        booleanParam(name: 'BUILD_CHAT_SERVICE', defaultValue: true, description: 'Build Chat Service')
        booleanParam(name: 'BUILD_NOTIFICATION_SERVICE', defaultValue: true, description: 'Build Notification Service')
        booleanParam(name: 'BUILD_FILE_SERVICE', defaultValue: true, description: 'Build File Service')
        
        choice(name: 'DEPLOY_MODE', choices: ['none', 'individual', 'all'], description: 'Deployment mode')
        choice(name: 'DEPLOY_SERVICE', choices: ['api-gateway', 'service-registry', 'auth-service', 'user-service', 'chat-service', 'notification-service', 'file-service'], description: 'Select service to deploy when using individual mode')
        choice(name: 'DEPLOY_ENV', choices: ['dev', 'prod'], description: 'Deployment environment')
    }
    
    environment {
        DOCKER_REGISTRY = 'your-registry-url'
        DOCKER_CREDENTIALS_ID = 'docker-cred-id'
        AUTH_SERVICE_PORT = '8081'
        // Use a direct docker maven command instead of relying on scripts
        MVN_CMD = 'docker run --rm -v "$(pwd)":/app -v maven-repo:/root/.m2 -w /app maven:3.8.6-eclipse-temurin-17 mvn'
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
        
        stage('Build Shared Libraries') {
            steps {
                script {
                    echo "Building common shared libraries..."
                    
                    // Build the parent POM first
                    if (env.DOCKER_AVAILABLE == 'true') {
                        sh '${MVN_CMD} clean install -N -DskipTests'
                        echo "Built parent POM successfully"
                    } else {
                        sh 'mvn clean install -N -DskipTests'
                        echo "Built parent POM successfully"
                    }
                    
                    // Build shared-libs parent project first
                    dir('shared-libs') {
                        if (env.DOCKER_AVAILABLE == 'true') {
                            sh '${MVN_CMD} clean install -N -DskipTests'
                            echo "Built shared-libs parent successfully"
                        } else {
                            sh 'mvn clean install -N -DskipTests'
                            echo "Built shared-libs parent successfully"
                        }
                        
                        // Build common-utils
                        dir('common-utils') {
                            if (env.DOCKER_AVAILABLE == 'true') {
                                sh '${MVN_CMD} clean install -DskipTests'
                                echo "Built common-utils successfully"
                            } else {
                                sh 'mvn clean install -DskipTests'
                                echo "Built common-utils successfully"
                            }
                        }
                        
                        // Build security-core
                        dir('security-core') {
                            if (env.DOCKER_AVAILABLE == 'true') {
                                sh '${MVN_CMD} clean install -DskipTests'
                                echo "Built security-core successfully"
                            } else {
                                sh 'mvn clean install -DskipTests'
                                echo "Built security-core successfully"
                            }
                        }
                    }
                    
                    echo "Shared libraries built successfully"
                }
            }
        }
        
        stage('Build Microservices') {
            parallel {
                stage('Build API Gateway') {
                    when {
                        expression { return params.BUILD_API_GATEWAY }
                    }
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
                    when {
                        expression { return params.BUILD_SERVICE_REGISTRY }
                    }
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
                    when {
                        expression { return params.BUILD_AUTH_SERVICE }
                    }
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
                    when {
                        expression { return params.BUILD_USER_SERVICE }
                    }
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
                    when {
                        expression { return params.BUILD_CHAT_SERVICE }
                    }
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
                    when {
                        expression { return params.BUILD_NOTIFICATION_SERVICE }
                    }
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
                    when {
                        expression { return params.BUILD_FILE_SERVICE }
                    }
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
                        sh '${MVN_CMD} -N test'
                    } else {
                        sh 'mvn -N test'
                    }
                }
            }
        }
        
        stage('Test Auth Service') {
            when {
                expression { return params.BUILD_AUTH_SERVICE }
            }
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
                allOf {
                    branch 'main'
                    expression { return env.DOCKER_AVAILABLE == 'true' }
                }
            }
            steps {
                script {
                    withCredentials([string(credentialsId: "${DOCKER_CREDENTIALS_ID}", variable: 'DOCKER_PWD')]) {
                        sh 'echo $DOCKER_PWD | docker login ${DOCKER_REGISTRY} -u username --password-stdin'
                        
                        if (params.BUILD_API_GATEWAY) {
                            sh 'docker tag swiftchat/api-gateway:latest ${DOCKER_REGISTRY}/swiftchat/api-gateway:latest'
                            sh 'docker push ${DOCKER_REGISTRY}/swiftchat/api-gateway:latest'
                        }
                        
                        if (params.BUILD_SERVICE_REGISTRY) {
                            sh 'docker tag swiftchat/service-registry:latest ${DOCKER_REGISTRY}/swiftchat/service-registry:latest'
                            sh 'docker push ${DOCKER_REGISTRY}/swiftchat/service-registry:latest'
                        }
                        
                        if (params.BUILD_AUTH_SERVICE) {
                            sh 'docker tag swiftchat/auth-service:latest ${DOCKER_REGISTRY}/swiftchat/auth-service:latest'
                            sh 'docker push ${DOCKER_REGISTRY}/swiftchat/auth-service:latest'
                        }
                        
                        if (params.BUILD_USER_SERVICE) {
                            sh 'docker tag swiftchat/user-service:latest ${DOCKER_REGISTRY}/swiftchat/user-service:latest'
                            sh 'docker push ${DOCKER_REGISTRY}/swiftchat/user-service:latest'
                        }
                        
                        if (params.BUILD_CHAT_SERVICE) {
                            sh 'docker tag swiftchat/chat-service:latest ${DOCKER_REGISTRY}/swiftchat/chat-service:latest'
                            sh 'docker push ${DOCKER_REGISTRY}/swiftchat/chat-service:latest'
                        }
                        
                        if (params.BUILD_NOTIFICATION_SERVICE) {
                            sh 'docker tag swiftchat/notification-service:latest ${DOCKER_REGISTRY}/swiftchat/notification-service:latest'
                            sh 'docker push ${DOCKER_REGISTRY}/swiftchat/notification-service:latest'
                        }
                        
                        if (params.BUILD_FILE_SERVICE) {
                            sh 'docker tag swiftchat/file-service:latest ${DOCKER_REGISTRY}/swiftchat/file-service:latest'
                            sh 'docker push ${DOCKER_REGISTRY}/swiftchat/file-service:latest'
                        }
                    }
                }
            }
        }
        
        // Individual Service Deployment Stages
        stage('Deploy Individual Service') {
            when {
                expression { 
                    return params.DEPLOY_MODE == 'individual' && env.DOCKER_AVAILABLE == 'true' 
                }
            }
            steps {
                script {
                    def composeFile = params.DEPLOY_ENV == 'prod' ? 'docker-compose.prod.yml' : 'docker-compose.dev.yml'
                    def service = params.DEPLOY_SERVICE
                    
                    echo "Deploying individual service: ${service} to ${params.DEPLOY_ENV} environment"
                    sh "docker-compose -f ${composeFile} up -d ${service}"
                    
                    // For essential services like Service Registry and API Gateway
                    if (service == 'service-registry' || service == 'api-gateway') {
                        sh """
                            # Wait for ${service} to be healthy
                            max_attempts=12
                            counter=0
                            echo "Waiting for ${service} to become available..."
                            until curl -s http://localhost:\$(docker-compose -f ${composeFile} port ${service} 8080 | cut -d':' -f2)/actuator/health | grep -q "UP" || [ \$counter -eq \$max_attempts ]
                            do
                              sleep 5
                              counter=\$((counter + 1))
                              echo "Attempt \$counter of \$max_attempts"
                            done
                            
                            if [ \$counter -eq \$max_attempts ]; then
                              echo "Failed to start ${service}"
                              exit 1
                            else
                              echo "${service} started successfully"
                            fi
                        """
                    }
                }
            }
        }
        
        stage('Deploy to Dev') {
            when {
                allOf {
                    expression { return params.DEPLOY_MODE == 'all' && env.DOCKER_AVAILABLE == 'true' }
                }
            }
            steps {
                script {
                    sh 'docker-compose -f docker-compose.dev.yml up -d'
                }
            }
        }
        
        stage('Deploy Auth Service to Dev') {
            when {
                allOf {
                    expression { return params.DEPLOY_MODE == 'all' && env.DOCKER_AVAILABLE == 'true' }
                }
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
                allOf {
                    expression { return params.DEPLOY_MODE == 'all' && env.DOCKER_AVAILABLE == 'true' }
                }
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