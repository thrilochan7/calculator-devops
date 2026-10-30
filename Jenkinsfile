pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'thrilochan7/calculator-devops'
        NOTIFY_EMAIL = 'thrilochanreddy7@gmail.com'
        IMAGE_TAG    = "${env.BUILD_NUMBER}"
    }

    triggers {
        // Jenkins runs locally (WSL), so GitHub can't reach a webhook; poll GitHub every 2 minutes instead
        pollSCM('H/2 * * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t $DOCKER_IMAGE:$IMAGE_TAG -t $DOCKER_IMAGE:latest .'
            }
        }

        stage('Push') {
            steps {
                // 'dockerhub-creds' = Username/Password credential stored in Jenkins
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds',
                                                  usernameVariable: 'DOCKER_USER',
                                                  passwordVariable: 'DOCKER_PASS')]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker push $DOCKER_IMAGE:$IMAGE_TAG
                        docker push $DOCKER_IMAGE:latest
                        docker logout
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                // up -d only recreates containers whose image/config changed, so re-runs are safe
                sh 'docker compose up -d'
                // wait for the container's /actuator/health check to report healthy
                sh '''
                    for i in $(seq 1 20); do
                        status=$(docker inspect -f '{{.State.Health.Status}}' calculator)
                        if [ "$status" = "healthy" ]; then
                            echo "Deployment healthy"
                            exit 0
                        fi
                        sleep 3
                    done
                    echo "Health check failed"
                    docker compose logs
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            mail to: "${NOTIFY_EMAIL}",
                 subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Build succeeded and was deployed.\n${env.BUILD_URL}"
        }
        failure {
            mail to: "${NOTIFY_EMAIL}",
                 subject: "FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Build failed. Check the console output:\n${env.BUILD_URL}console"
        }
    }
}
