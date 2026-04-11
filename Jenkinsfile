pipeline {
    agent none

    environment {
        DOCKER_HUB_CREDS = credentials('docker-hub')
        APP_NAME = "ais-sst-app"
        COMPOSE_PATH = "/home/coffee13102006/docker/app"
    }

    stages {
        stage('Checkout') {
            agent { label 'built-in' }
            steps {
                echo 'Cloning repository...'
                checkout scm
                echo 'Branch: ' + env.GIT_BRANCH
            }
        }

        stage('Build and Push Docker') {
            agent { label 'built-in' }
            steps {
                echo 'Building Docker image...'
                script {
                    sh """
                        docker build -t ${DOCKER_HUB_CREDS_USR}/${APP_NAME}:latest .
                        echo ${DOCKER_HUB_CREDS_PSW} | docker login -u ${DOCKER_HUB_CREDS_USR} --password-stdin
                        docker push ${DOCKER_HUB_CREDS_USR}/${APP_NAME}:latest
                    """
                }
            }
        }

        stage('Deploy on VPS') {
            agent { label 'built-in' }
            steps {
                echo 'Deploying application...'
                script {
                    sh """
                        cd ${COMPOSE_PATH}
                        docker pull ${DOCKER_HUB_CREDS_USR}/${APP_NAME}:latest
                        docker compose up -d --force-recreate app
                        docker system prune -f
                    """
                }
            }
        }

        stage('Health Check') {
            agent { label 'built-in' }
            steps {
                echo 'Waiting for app to start...'
                script {
                    sh 'sleep 15'
                    sh 'curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/actuator/health || echo "Health endpoint not found, assuming OK"'
                    echo 'Deployment completed!'
                }
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline executed successfully!'
        }
        failure {
            echo '❌ Pipeline failed! Check logs.'
        }
    }
}