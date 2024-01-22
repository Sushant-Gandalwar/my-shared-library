def call(Map pipelineParams) {
    pipeline {
        agent any

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            DOCKER_IMAGE_NAME = 'hello-world-html'
            DOCKER_IMAGE_TAG = 'latest' // You can parameterize this based on your needs
            DOCKERHUB_CREDENTIALS = 'env.dockerCredentialsId' // Replace with your Docker Hub credentials ID
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
                            // Build the Docker image
                            sh "docker build -t ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG} -f Dockerfile ."

                            // Push the Docker image to Docker Hub
                            withDockerRegistry([credentialsId: "${DOCKERHUB_CREDENTIALS}", url: "https://index.docker.io/v1/"]) {
                                sh "docker login -u _json_key -p \$(cat \${DOCKERHUB_CREDENTIALS}_json_key.json) https://index.docker.io/v1/"
                                sh "docker push ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}"
                            }
                        }
                    }
                }
            }
        }
    }
}
