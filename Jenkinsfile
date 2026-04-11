pipeline {
    agent any

    environment {
        DOCKER_HUB_CREDS = credentials('docker-hub')
        APP_NAME = "ais-sst-app"
        CONTAINER_NAME = "ais-sst-app"
        COMPOSE_PATH = "/home/coffee13102006/docker/app"
    }

    triggers {
        // Проверка GitHub каждую минуту (для пулов)
        pollSCM('* * * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Cloning repository...'
                checkout scm
                echo 'Branch: ' + env.GIT_BRANCH
                echo 'Commit: ' + env.GIT_COMMIT
            }
        }

        stage('Build Docker Image') {
            steps {
                echo 'Building Docker image...'
                script {
                    docker.build("${env.DOCKER_HUB_CREDS_USR}/${env.APP_NAME}:latest")
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                echo 'Pushing to Docker Hub...'
                script {
                    docker.withRegistry('', DOCKER_HUB_CREDS) {
                        docker.image("${env.DOCKER_HUB_CREDS_USR}/${env.APP_NAME}:latest").push()
                    }
                }
            }
        }

        stage('Deploy on VPS') {
            steps {
                echo 'Deploying on VPS...'
                script {
                    sh """
                        cd ${env.COMPOSE_PATH}
                        docker pull ${DOCKER_HUB_CREDS_USR}/${env.APP_NAME}:latest
                        docker compose up -d --force-recreate ${env.CONTAINER_NAME}
                        docker system prune -f
                    """
                }
            }
        }

        stage('Health Check') {
            steps {
                echo 'Checking if app is healthy...'
                script {
                    sh 'sleep 10'
                    def response = sh(script: 'curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health', returnStdout: true).trim()
                    if (response == '200') {
                        echo 'Application is healthy!'
                    } else {
                        error("Health check failed with HTTP ${response}")
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline executed successfully!'
            // Можно отправить уведомление
        }
        failure {
            echo 'Pipeline failed! Check logs.'
            // Можно отправить уведомление об ошибке
        }
    }
}a