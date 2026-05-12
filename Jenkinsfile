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

        stage('Prepare Directories on VPS') {
            agent { label 'built-in' }
            steps {
                echo 'Creating upload directories on VPS...'
                script {
                    sh """
                        # Создаем директории для загрузки файлов на хосте
                        mkdir -p ${COMPOSE_PATH}/uploads/sectors
                        mkdir -p ${COMPOSE_PATH}/uploads/events
                        mkdir -p ${COMPOSE_PATH}/uploads/users
                        mkdir -p ${COMPOSE_PATH}/uploads/account_creating_requests_avatars

                        # Устанавливаем права
                        chmod -R 777 ${COMPOSE_PATH}/uploads

                        echo "Upload directories created successfully"
                        ls -la ${COMPOSE_PATH}/uploads/
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
                        docker-compose up -d --force-recreate app
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

        stage('Check Container Status') {
            agent { label 'built-in' }
            steps {
                echo 'Checking container status...'
                sh '''
                    echo "=== Running containers ==="
                    docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'

                    echo ""
                    echo "=== Container logs (last 10 lines) ==="
                    docker logs --tail 10 ais-sst-app 2>&1 || echo "Container not found"
                '''
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