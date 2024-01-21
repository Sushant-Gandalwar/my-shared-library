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

            stage('Build and Publish to GCR') {
                steps {
                    script {
                        echo "Building docker image and publishing to GCR"
                    }
                    sh "sbt publish"
                    sh "sbt docker:publishLocal"
                    
                    // Build and push Docker image to GCR
                    withDockerRegistry([credentialsId: "gcr:${env.CREDENTIALS_ID}", url: "https://gcr.io"]) {
                        sh "cd ${env.DOCKERDIRECTORY} && docker build -t ${env.IMAGE}:${env.IMAGE_TAG} -f Dockerfile ."
                        sh "docker push ${env.IMAGE}:${env.IMAGE_TAG}"
                        sh "docker rmi ${env.IMAGE}:${env.IMAGE_TAG}"
                    }

                    script {
                        echo "Published Docker image ${env.IMAGE}:${env.IMAGE_TAG} to GCR"
                    }
                }
            }
        }
    }
}
