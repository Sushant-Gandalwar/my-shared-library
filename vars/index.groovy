def call(Map pipelineParams) {
    pipeline {
        agent any

        parameters {
            string(name: 'Parameter', defaultValue: 'default', description: 'Pass the Docker image id if choosed DEPLOY_ONLY OR pass the sbt release command if choosed Publish_to_Release', )
        }

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            IMAGE = "${pipelineParams.dockerImage}"
            IMAGE_TAG = "${params.Parameter}"
              COMMAND = "${params.Parameter}"
            NEW_IMAGE_NAME = "react"  // Specify the new name for the image
            CREDENTIALS_ID = "${pipelineParams.dockerCredentialsId}"
            PROJECT_ID = 'jenkins-407204'
            CLUSTER_NAME = 'demo'
            LOCATION =  'us-central1'
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
                             sh """
                                docker push '${env.IMAGE}:${env.IMAGE_TAG}'
                             """
                        }
                    }
                }
            }
            
        }
    }
}
