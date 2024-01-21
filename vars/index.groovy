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
                            echo "Initialization code has an error for ${APP_Name}"
                        }
                    }
                }
            }
            stage('Build and Push Docker Image') {
                steps {
                    script {
                         withDockerRegistry([credentialsId: "gcr:${env.CREDENTIALS_ID}", url: "https://gcr.io"]) {
                      sh "cd ${env.DOCKERDIRECTORY} && docker build -t '${env.APP_Name}:${env.IMAGETAG}' -f Dockerfile ."
                      sh """
                         docker push '${env.APP_Name}:${env.IMAGETAG}'
                         docker rmi '${env.APP_Name}:${env.IMAGETAG}'
                         
                         """
                    }
                    script {
                        echo "Published Docker image ${env.APP_Name} to GCR"
                    }
                        // Build the Docker image
                        // dir(env.DOCKERDIRECTORY) {
                        //     sh "docker build -t ${env.APP_Name}:${env.IMAGE_TAG} -f Dockerfile ."
                        // }

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
