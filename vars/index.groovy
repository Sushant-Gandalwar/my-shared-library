def call(Map pipelineParams) {
    pipeline {
        agent any

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            IMAGE_TAG = "${params.Parameter}"
            IMAGE = "${pipelineParams.dockerImage}"
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
                        // Build the Docker image
                        dir(env.DOCKERDIRECTORY) {
                            sh "docker build -t ${env.IMAGE}:${env.IMAGE_TAG} -f Dockerfile ."
                        }

                        // Login to Docker Hub
                        withCredentials([usernamePassword(credentialsId: env.CREDENTIALS_ID, usernameVariable: 'DOCKER_HUB_USERNAME', passwordVariable: 'DOCKER_HUB_PASSWORD')]) {
                            sh "docker login -u ${DOCKER_HUB_USERNAME} -p ${DOCKER_HUB_PASSWORD}"
                        }

                        // Tag the Docker image
                        sh "docker tag ${env.IMAGE}:${env.IMAGE_TAG} ${env.DOCKER_HUB_USERNAME}/${env.IMAGE}:${env.IMAGE_TAG}"

                        // Push the Docker image to Docker Hub
                        sh "docker push ${env.DOCKER_HUB_USERNAME}/${env.IMAGE}:${env.IMAGE_TAG}"
                    }
                }
            }
        }
    }
}
