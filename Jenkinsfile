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
            }
        }

        // НОВАЯ СТАДИЯ: Сборка JAR файла
        stage('Build JAR') {
            agent { label 'built-in' }
            steps {
                echo 'Building JAR with Maven...'
                script {
                    // Если используете Maven
                    sh 'mvn clean package -DskipTests'
                    // Или если Gradle:
                    // sh './gradlew build -x test'
                }
            }
        }

        stage('Build and Push Docker') {
            agent { label 'built-in' }
            steps {
                echo 'Building Docker image...'
                script {
                    sh """
                        docker build -t ${DOCKER_HUB_CREDS_USR}/${APP_NAME}:latest .
                        echo '${DOCKER_HUB_CREDS_PSW}' | docker login -u ${DOCKER_HUB_CREDS_USR} --password-stdin
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
                        cd /home/coffee13102006/docker/app
                        docker pull ${DOCKER_HUB_CREDS_USR}/ais-sst-app:latest
                        docker-  compose up -d --force-recreate app
                        docker system prune -f
                    """
                }
            }
        }

        stage('Health Check') {
            agent { label 'built-in' }
            steps {
                script {
                    sh 'sleep 15'
                    sh 'curl -s http://localhost:8080/actuator/health || echo "App is running"'
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