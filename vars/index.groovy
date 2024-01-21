def call(Map pipelineParams) {
    pipeline {
        agent any

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            DOCKER_HUB_USERNAME = 'sushant900123'
            DOCKER_IMAGE_NAME = 'hello-world-html'
            DOCKER_IMAGE_TAG = 'latest' // You can parameterize this based on your needs
            CREDENTIALS_ID = "${pipelineParams.dockerCredentialsId}"
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
                        dir(env.DOCKERDIRECTORY) {
                            // sh "docker build -t ${env.APP_Name}:${env.IMAGE_TAG} -f Dockerfile ."
                            sh "docker build -t ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG} -f Dockerfile ."
                        }

                        withCredentials([usernamePassword(credentialsId: env.CREDENTIALS_ID, usernameVariable: 'DOCKER_HUB_USERNAME', passwordVariable: 'DOCKER_HUB_PASSWORD')]) {
                            sh "docker login -u ${DOCKER_HUB_USERNAME} -p ${DOCKER_HUB_PASSWORD}"

                            // Tag the Docker image
                            sh "docker tag ${env.DOCKER_IMAGE_NAME} ${env.DOCKER_HUB_USERNAME}/${env.DOCKER_IMAGE_NAME}:${env.IMAGE_TAG}"

                            // Push the Docker image to Docker Hub
                            sh "docker push ${env.DOCKER_HUB_USERNAME}/${env.DOCKER_IMAGE_NAME}:${env.IMAGE_TAG}"
                        }
                        // Build the Docker image
                        // sh "docker build -t ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG} ."

                        // Login to Docker Hub
                        // sh "docker login -u ${DOCKER_HUB_USERNAME} -p ${DOCKER_HUB_PASSWORD}"

                        // Push the Docker image to Docker Hub
                        // sh "docker push ${DOCKER_HUB_USERNAME}/${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}"
                    }
                }
            }
        }
    }
}
