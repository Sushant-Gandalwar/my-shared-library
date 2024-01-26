def call(Map pipelineParams) {
    pipeline {
        agent any

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            IMAGE = "${pipelineParams.dockerImage}"
            IMAGE_TAG = 'demo'
            NEW_IMAGE_NAME = "react"  // Specify the new name for the image
            CREDENTIALS_ID = "${pipelineParams.dockerCredentialsId}"
            PROJECT_ID = 'jenkins-407204'
            CLUSTER_NAME = 'k8s-cluster'
            LOCATION =  'us-central1-c'
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
                            error("Initialization code has an error for ${APP_Name}")
                        }
                    }
                }
            }

            stage('Build, Rename, and Push Docker Image') {
                steps {
                    script {
                        withDockerRegistry([credentialsId: "gcr:${env.CREDENTIALS_ID}", url: "https://gcr.io"]) {
                            sh "cd ${env.DOCKERDIRECTORY} && docker build -t '${env.IMAGE}:${env.IMAGE_TAG}' -f Dockerfile ."
                            sh "docker tag '${env.IMAGE}:${env.IMAGE_TAG}' '${env.IMAGE}:${env.NEW_IMAGE_NAME}'"

                            sh """
                                docker push '${env.IMAGE}:${env.IMAGE_TAG}'
                                docker push '${env.IMAGE}:${env.NEW_IMAGE_NAME}'
                            """
                        }
                    }
                }
            }
        }
    }
}
