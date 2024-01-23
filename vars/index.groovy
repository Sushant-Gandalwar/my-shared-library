def call(Map pipelineParams) {
    pipeline {
        agent any

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            IMAGE = "${pipelineParams.dockerImage}"
            IMAGE_TAG = "${params.Parameter}"
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
                            error("Initialization code has an error for ${APP_Name}")
                        }
                    }
                }
            }

            stage('Build and Push Docker Image') {
                steps {
                    script {
                        withDockerRegistry([credentialsId: "gcr:${env.CREDENTIALS_ID}", url: "https://gcr.io"]) {
                            sh "cd ${env.DOCKERDIRECTORY} && docker build -t '${env.IMAGE}:${env.IMAGETAG}' -f Dockerfile ."
                             sh """
                                docker push '${env.IMAGE}:${env.IMAGETAG}'
                                docker rmi '${env.IMAGE}:${env.IMAGETAG}'
                                
                                """
                        }
                    }
                }
            }

            stage('Approval') {
                steps {
                    script {
                        def userInput = input(
                            id: 'userInput', message: 'Approve Deployment!',
                            parameters: [
                                [$class: 'BooleanParameterDefinition', defaultValue: false, description: 'Click to skip', name: 'skip'],
                            ]
                        )

                        if (userInput) {
                            echo 'Proceeding to the next stage.'
                        } else {
                            echo 'Skipping the next stage.'
                            currentBuild.result = 'ABORTED'
                            error('Deployment skipped as per user input.')
                        }
                    }
                }
            }

            stage('Next Stage') {
                when {
                    expression { currentBuild.result != 'ABORTED' }
                }
                steps {
                    script {
                        echo 'This is the next stage after approval.'
                    }
                }
            }
        }
    }
}
