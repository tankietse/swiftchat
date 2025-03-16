#!/usr/bin/env groovy

def call(String serviceName, String port = "8080") {
  stage("Build ${serviceName}") {
    dir(serviceName) {
      sh "mvn clean package"
      sh "docker build -t swiftchat/${serviceName}:\${BUILD_NUMBER} -t swiftchat/${serviceName}:latest ."
    }
  }
  
  stage("Test ${serviceName}") {
    dir(serviceName) {
      sh "mvn test"
      junit "**/target/surefire-reports/*.xml"
    }
  }
  
  stage("Deploy ${serviceName} to Dev") {
    when(env.BRANCH_NAME == 'develop') {
      sh "docker-compose -f docker-compose.dev.yml up -d ${serviceName}"
      sh "../scripts/verify-deployment.sh ${serviceName} ${port}"
    }
  }
}
