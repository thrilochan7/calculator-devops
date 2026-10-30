// Written for a Jenkins controller running on Windows (uses bat / powershell steps)
pipeline {
    agent any

    environment {
        // Change these two values to your own
        DOCKER_IMAGE = 'YOUR_DOCKERHUB_USERNAME/calculator-devops'
        NOTIFY_EMAIL = 'sasanklingareddy@gmail.com'
        IMAGE_TAG    = "${env.BUILD_NUMBER}"
    }

    triggers {
        // Jenkins runs on localhost, so GitHub can't reach a webhook; poll GitHub every 2 minutes instead
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
                bat 'mvn -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                bat 'mvn -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                bat 'docker build -t %DOCKER_IMAGE%:%IMAGE_TAG% -t %DOCKER_IMAGE%:latest .'
            }
        }

        stage('Push') {
            steps {
                // 'dockerhub-creds' = Username/Password credential stored in Jenkins
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds',
                                                  usernameVariable: 'DOCKER_USER',
                                                  passwordVariable: 'DOCKER_PASS')]) {
                    powershell '''
                        $ErrorActionPreference = "Stop"
                        $env:DOCKER_PASS | docker login -u $env:DOCKER_USER --password-stdin
                        if ($LASTEXITCODE -ne 0) { exit 1 }
                        docker push "$($env:DOCKER_IMAGE):$($env:IMAGE_TAG)"
                        if ($LASTEXITCODE -ne 0) { exit 1 }
                        docker push "$($env:DOCKER_IMAGE):latest"
                        if ($LASTEXITCODE -ne 0) { exit 1 }
                        docker logout
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                // up -d only recreates containers whose image/config changed, so re-runs are safe
                bat 'docker compose up -d'
                powershell '''
                    for ($i = 0; $i -lt 20; $i++) {
                        try {
                            $r = Invoke-RestMethod http://localhost:8081/actuator/health -TimeoutSec 3
                            if ($r.status -eq "UP") { Write-Output "Deployment healthy"; exit 0 }
                        } catch { }
                        Start-Sleep -Seconds 3
                    }
                    Write-Output "Health check failed"
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
