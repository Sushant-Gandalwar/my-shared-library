def call(Map pipelineParams) {
    pipeline {
        agent any

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            DOCKER_IMAGE_NAME = 'hello-world-html'
            DOCKER_IMAGE_TAG = 'latest' // You can parameterize this based on your needs
            CREDENTIALS_ID = '404b3183-6431-48ad-b984-2316e79f2cfd'
        }

        stages {
            stage('INITIALIZE') {
                steps {
                    script {
                        echo "Initializing environment for webstore delivery pipeline"
                        echo "Git URL: ${env.scmUrl}"
                    }
                }
                post {
                    failure {
                        script {
                            log.error("Initialization code has an error for ${APP_Name}")
                        }
                    }
                }
            }

            stage('Build and Push Docker Image') {
                steps {
                    script {
                        // Build and push Docker image using Google Cloud credentials
                        withCredentials([googleServiceAccount(credentialsId: CREDENTIALS_ID, 
                                                            jsonKeyVariable: 'GOOGLE_APPLICATION_CREDENTIALS')]) {
                            docker.withRegistry('https://gcr.io', 'gcr:${CREDENTIALS_ID}') {
                                def customImage = docker.build("${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}", "${DOCKERDIRECTORY}")
                                customImage.push()
                            }
                        }
                    }
                }
            }
        }
    }
}
