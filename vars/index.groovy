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
                            sh "docker build -t ${env.APP_Name}:${env.IMAGE_TAG} -f Dockerfile ."
                        }
                        //  withCredentials([string(credentialsId: env.DOCKER_HUB_CREDENTIALS_ID, variable: 'DOCKER_HUB_CREDENTIALS')]) {
                        //     sh "echo ${DOCKER_HUB_CREDENTIALS} | docker login -u sushantgandalwar --password-stdin"
                        // }

                          withDockerRegistry([credentialsId: "gcr:${env.CREDENTIALS_ID}", url: "https://hub.docker.com/"]) {
                      sh "cd ${env.DOCKERDIRECTORY} && docker build -t '${env.IMAGE}:${env.IMAGETAG}' -f Dockerfile ."
                          }

                       
                        // Login to Docker Hub
                        // sh "docker login -u ${env.DOCKER_HUB_USERNAME} -p ${env.DOCKER_HUB_PASSWORD}"

                        // Tag the Docker image
                        // sh "docker tag ${env.IMAGE}:${env.IMAGE_TAG} ${env.DOCKER_HUB_USERNAME}/${env.APP_Name}:latest"

                        // Push the Docker image to Docker Hub
                        // sh "docker push ${env.DOCKER_HUB_USERNAME}/${env.APP_Name}:latest"
                    }
                }
            }
        }
    }
}
